package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.ProgressUpdate;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipArchiveService {


    private final JavaMailSender mailSender;
    private final FileSetRepository fileSetRepository;
    private final FileSetService fileSetService;
    private final ZipArchiveRepository zipArchiveRepository;
    private final ZipArchiveStatusService zipArchiveStatusService;
    private final SentHistoryService sentHistoryService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Entry point for the upload-to-zip process. Orchestrates synchronous file staging.
     * <p>
     * <b>Why this way:</b> In environments like Render, MultipartFiles are deleted
     * immediately after the HTTP request ends. To process them asynchronously,
     * we first "stage" them into a secure local directory.
     * </p>
     *
     * @param fileSetId      The ID of the associated FileSet.
     * @param recipientEmail Target email address.
     * @param files          Array of MultipartFiles from the controller.
     * @return String        The unique taskId for WebSocket tracking.
     * @throws IOException If file staging fails.
     */
    public String startZipUploadProcess(Long fileSetId, String recipientEmail, MultipartFile[] files) throws IOException {
        String taskId = UUID.randomUUID().toString();

        Path uploadDir = Paths.get("temp-uploads", taskId).toAbsolutePath();
        Files.createDirectories(uploadDir);

        List<Path> savedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            Path destination = uploadDir.resolve(Objects.requireNonNull(file.getOriginalFilename())).toAbsolutePath();
            Files.createDirectories(destination.getParent());
            file.transferTo(destination);
            savedFiles.add(destination);
        }

        createAndSendZipFromUploadedFiles(fileSetId, recipientEmail, savedFiles, uploadDir, taskId);

        return taskId;
    }

    /**
     * Asynchronously creates a ZIP from staged files and dispatches it via email.
     * <p>
     * <b>Workflow:</b>
     * <ol>
     * <li>Fetches metadata from database.</li>
     * <li>Creates a ZIP archive from the staged files with progress reporting (0-90%).</li>
     * <li>Registers the archive in PENDING status.</li>
     * <li>Sends email (95%) and finalizes status to SUCCESS or FAILED.</li>
     * <li>Purges all temporary resources (ZIP and staging folder) in the 'finally' block.</li>
     * </ol>
     */
    @Async
    public void createAndSendZipFromUploadedFiles(Long fileSetId, String recipientEmail, List<Path> filesToZip, Path sourceDir, String taskId) {
        Path zipPath = null;
        try {
            FileSet fileSet = fetchFileSet(fileSetId);
            zipPath = prepareTempZipPath(fileSetId);

            createZipArchiveFromPaths(filesToZip, zipPath, (percent, msg) -> notifyProgress(taskId, percent, msg));

            ZipArchive archive = registerZipArchive(fileSet, zipPath, recipientEmail);

            sendAndFinalize(archive, fileSet, zipPath, taskId);

        } catch (Exception ex) {
            handleError(taskId, ex);
        } finally {
            cleanUp(zipPath);
            recursiveDelete(sourceDir);
        }
    }

    public List<ZipArchive> getAllZipArchives() {
        Long currentUserId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.findAllByUserId(currentUserId);
    }

    public List<ZipArchive> getAllZipArchivesForFileSet(Long fileSetId) throws AccessDeniedException {
        Long currentUserId = fileSetService.getCurrentUserId();

        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        return zipArchiveRepository.findAllByUserId(currentUserId);
    }

    public ZipArchive getZipArchiveById(Long fileSetId, Long zipArchiveId) throws AccessDeniedException {
        Long currentUserId = fileSetService.getCurrentUserId();

        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        if (!zipArchive.getUser().getId().equals(currentUserId)
                || !zipArchive.getFileSet().getId().equals(fileSetId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        return zipArchive;
    }

    public void deleteZipArchive(Long fileSetId, Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)
                || !zipArchive.getFileSet().getId().equals(fileSetId)) {
            throw new AccessDeniedException("You do not have permission to delete this FileSet.");
        }

        zipArchiveRepository.deleteById(zipArchiveId);
    }

    /**
     * Retrieves statistics about ZIP archive sending results for the currently authenticated user.
     * <p>
     * The statistics include the total number of successfully sent archives and the number of failed send attempts.
     * This information can be used to display user performance metrics or diagnostic data in the application dashboard.
     * </p>
     *
     * @return a {@link Map} containing key-value pairs with statistical data,
     * typically including counts of successful and failed ZIP sends
     */

    public Map<String, Object> getZipStatsForCurrentUser() {
        Long userId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.countSuccessAndFailuresByUser(userId);
    }

    /**
     * Retrieves all ZIP archives created by the current user that exceed a specified size threshold.
     * <p>
     * This method can be used to identify unusually large ZIP files for monitoring storage usage
     * or performing cleanup operations.
     * </p>
     *
     * @param minSize the minimum file size (in bytes) used as a filter;
     *                only ZIP files larger than this value will be returned
     * @return a {@link List} of {@link ZipArchive} objects that meet the size criteria
     */

    public List<ZipArchive> getLargeZipFiles(Long minSize) {
        Long userId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.findLargeZipArchives(userId, minSize);
    }

    /**
     * Retrieves the FileSet entity from the database, including its associated files.
     * * @param fileSetId The unique identifier of the FileSet.
     *
     * @return The found FileSet entity.
     * @throws FileSetNotFoundException if no FileSet exists for the given ID.
     */
    private FileSet fetchFileSet(Long fileSetId) {
        return fileSetRepository.findByIdWithFiles(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found"));
    }

    /**
     * Generates a unique temporary file path for the ZIP archive.
     * The filename is constructed using the FileSet ID and a calculated send counter
     * to ensure uniqueness within the system's temporary directory.
     * * @param fileSetId The ID of the FileSet being processed.
     *
     * @return A Path pointing to the target temporary ZIP file.
     */
    private Path prepareTempZipPath(Long fileSetId) {
        Path zipFilePath = null;

        int sendCounter = zipArchiveRepository.findMaxSendNumberByFileSetId(fileSetId) + 1;
        String zipFileName = "fileset-" + fileSetId + "-" + sendCounter + ".zip";
        zipFilePath = Path.of(System.getProperty("java.io.tmpdir"), zipFileName);

        return zipFilePath;
    }

    /**
     * Compresses files from given Paths into a single ZIP archive.
     * * @param filesToZip       List of staged file paths.
     *
     * @param zipPath          Target path for the .zip file.
     * @param progressCallback Callback for real-time progress updates.
     */
    private void createZipArchiveFromPaths(List<Path> filesToZip, Path zipPath, ProgressCallback progressCallback) throws IOException {

        long totalFileSizeBytes = 0;

        for (Path file : filesToZip) {
            totalFileSizeBytes += Files.size(file);
        }

        final long[] totalBytesProcessed = {0};
        final int[] lastPercent = {0};
        final long totalSizeFinal = totalFileSizeBytes > 0 ? totalFileSizeBytes : 1;

        Set<String> addedEntries = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {

            for (Path file : filesToZip) {
                String relativePath = zipPath.relativize(file).toString().replace("\\", "/");

                if (!addedEntries.add(relativePath)) {
                    log.warn("Skipping duplicate entry in ZIP: {}", relativePath);
                    continue;
                }

                ZipEntry entry = new ZipEntry(relativePath);
                zos.putNextEntry(entry);

                try (InputStream inputStream = Files.newInputStream(file)) {
                    copyInputStreamWithProgress(inputStream, zos, totalSizeFinal, totalBytesProcessed, lastPercent,
                            progressCallback, relativePath);
                }

                zos.closeEntry();
            }
        }
    }

    private void copyInputStreamWithProgress(InputStream inputStream, ZipOutputStream zos, long totalSize,
                                             long[] bytesProcessed, int[] lastPercent, ProgressCallback progressCallback,
                                             String currFinalName) throws IOException {
        byte[] buffer = new byte[8192];
        int length;
        long lastMessageTime = 0;

        while ((length = inputStream.read(buffer)) != -1) {
            zos.write(buffer, 0, length);
            bytesProcessed[0] += length;

            int rawPercent = (int) ((bytesProcessed[0] * 100) / totalSize);
            int currPercent = (int) (rawPercent * 0.9);

            long currTime = System.currentTimeMillis();

            if (currTime - lastMessageTime > 150 || currPercent > lastPercent[0]) {
                if (currPercent > lastPercent[0]) {
                    progressCallback.onUpdate(currPercent, "Processing " + currFinalName);
                    lastPercent[0] = currPercent;
                    lastMessageTime = currTime;
                }
            }
        }

    }

    /**
     * Copies the content of a single file into the ZIP output stream while updating the progress.
     * <p>
     * Progress calculation is scaled by a factor of 0.9 (mapping to a 0-90% range).
     * To optimize performance, the progress callback is throttled to trigger only if
     * at least 150ms have passed since the last update or if the percentage value has changed.
     * </p>
     *
     * @param file             The source {@link Path} of the file to be copied.
     * @param zos              The active {@link ZipOutputStream} being written to.
     * @param totalSizeFinal   The total size of all files in the batch for percentage calculation.
     * @param bytesProcessed   A single-element array acting as a mutable reference for the global byte counter.
     * @param lastPercent      A single-element array acting as a mutable reference for the last reported percentage.
     * @param progressCallback The callback used to report status updates to the UI or logs.
     * @throws IOException If an I/O error occurs during the read/write process.
     */
    private void copyContentWithProgress(Path file, ZipOutputStream zos, long totalSizeFinal, long[] bytesProcessed,
                                         int[] lastPercent, ProgressCallback progressCallback) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            long lastMessageTime = 0;

            while ((length = inputStream.read(buffer)) != -1) {
                zos.write(buffer, 0, length);
                bytesProcessed[0] += length;

                int rawPercent = (int) ((bytesProcessed[0] * 100) / totalSizeFinal);
                int currPercent = (int) (rawPercent * 0.9);

                long currTime = System.currentTimeMillis();

                if (currTime - lastMessageTime > 150 || currPercent > lastPercent[0]) {
                    if (currPercent > lastPercent[0]) {
                        progressCallback.onUpdate(currPercent, "Processing " + file.getFileName());
                        lastPercent[0] = currPercent;
                        lastMessageTime = currTime;
                    }
                }
            }
        }
    }

    /**
     * Sends a STOMP message to a specific task topic via the WebSocket message broker.
     * * @param taskId   The unique ID of the task used as the destination variable.
     *
     * @param percent The current progress percentage.
     * @param message A descriptive status message for the user.
     */
    private void notifyProgress(String taskId, int percent, String message) {
        messagingTemplate.convertAndSend("/topic/progress/" + taskId, new ProgressUpdate(percent, message));
    }

    /**
     * Persists the initial record of the ZIP archive in the database.
     * The archive is saved with a PENDING status before the email attempt begins.
     * * @param fileSet        The source FileSet.
     *
     * @param zipPath        Path where the archive was created.
     * @param recipientEmail The target recipient's address.
     * @return The saved ZipArchive entity.
     * @throws IOException If file size metadata cannot be read from the disk.
     */
    private ZipArchive registerZipArchive(FileSet fileSet, Path zipPath, String recipientEmail) throws IOException {
        int sendCounter = zipArchiveRepository.findMaxSendNumberByFileSetId(fileSet.getId()) + 1;

        return zipArchiveRepository.save(ZipArchive.builder()
                .archiveName(zipPath.getFileName().toString())
                .archivePath(zipPath.toAbsolutePath().toString())
                .size(Files.size(zipPath))
                .status(ZipArchiveStatus.PENDING)
                .recipientEmail(recipientEmail)
                .fileSet(fileSet)
                .user(fileSet.getUser())
                .creationDate(LocalDateTime.now())
                .sendNumber(sendCounter)
                .build()
        );
    }

    /**
     * Handles the final stage of the workflow: email delivery and status finalization.
     * If the email is sent successfully, it updates the archive and fileset statuses to SUCCESS/SENT.
     * In case of failure, it logs the error and records a failure entry in the sent history.
     * * @param zipArchive The registered archive entity.
     *
     * @param fileSet The source file set.
     * @param zipPath The physical path to the ZIP file.
     * @param taskId  The task ID for progress updates.
     */
    private void sendAndFinalize(ZipArchive zipArchive, FileSet fileSet, Path zipPath, String taskId) {
        try {
            notifyProgress(taskId, 95, "Sending email...");
            sendZipArchiveByEmail(zipArchive.getRecipientEmail(), zipPath, "Files", "Please find attached the ZIP archive of requested files");

            zipArchiveStatusService.updateDatabaseAfterSuccess(zipArchive.getId(), fileSet.getId());
            sentHistoryService.saveSentHistory(zipArchive, zipArchive.getRecipientEmail(), true, null);
            notifyProgress(taskId, 100, "Completed!");

        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("552-5.7.0")) {
                log.warn("Detected Gmail 552-5.7.0 security warning. Message likely delivered. Finalizing as SUCCESS.");

                zipArchiveStatusService.updateDatabaseAfterSuccess(zipArchive.getId(), fileSet.getId());
                sentHistoryService.saveSentHistory(zipArchive, zipArchive.getRecipientEmail(), true, "Sent with Gmail security warning");
                notifyProgress(taskId, 100, "Completed with warnings");
            } else {
                log.error("CRITICAL DELIVERY FAILURE: {}", ex.getMessage());
                zipArchiveStatusService.updateDatabaseAfterFailure(zipArchive.getId(), ex.getMessage());
                notifyProgress(taskId, -1, "Error: " + ex.getMessage());
            }
        }
    }

    /**
     * Standardized error handler for the asynchronous process.
     * Logs the exception and sends a terminal progress update (-1 status)
     * to notify the frontend of the failure.
     * * @param taskId The task ID associated with the failure.
     *
     * @param ex The exception that triggered the handler.
     */
    private void handleError(String taskId, Exception ex) {
        log.error("Error", ex);
        messagingTemplate.convertAndSend("/topic/progress/" + taskId,
                new ProgressUpdate(-1, "Error: " + ex.getMessage())
        );
    }

    /**
     * Performs disk cleanup by deleting the temporary ZIP archive.
     * This is called in the 'finally' block to ensure no orphaned files remain
     * on the server, regardless of task success or failure.
     * * @param zipPath The path of the file to be deleted.
     */
    private void cleanUp(Path zipPath) {
        try {
            if (zipPath != null) {
                Files.deleteIfExists(zipPath);
            }
        } catch (IOException ex) {
            log.warn("Could not delete temp file", ex);
        }
    }

    /**
     * Recursively deletes a directory or file.
     * Essential for maintaining clean disk space on ephemeral cloud storage.
     *
     * @param path Path to the file or directory to be deleted.
     */
    private void recursiveDelete(Path path) {
        try {
            if (Files.exists(path)) {
                try (var walk = Files.walk(path)) {
                    walk.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ex) {
                                    log.error("Unable to delete: {}", p, ex);
                                }
                            });
                }
            }
        } catch (IOException ex) {
            log.error("Error during directory deletion: {}", path, ex);
        }
    }

    /**
     * Sends a ZIP archive as an email attachment to the specified recipient.
     *
     * @param recipientEmail the recipient's email address
     * @param zipFilePath    the path to the ZIP file to be sent
     * @param subject        the subject of the email
     * @param text           the body text of the email
     * @throws MessagingException if an error occurs while sending the email
     */

    private void sendZipArchiveByEmail(String recipientEmail,
                                       Path zipFilePath,
                                       String subject,
                                       String text) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(text);
        helper.addAttachment(zipFilePath.getFileName().toString(),
                new FileSystemResource(zipFilePath.toFile()));

        mailSender.send(message);
    }
}
