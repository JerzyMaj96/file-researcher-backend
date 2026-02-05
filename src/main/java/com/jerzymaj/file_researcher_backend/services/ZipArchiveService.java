package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.ProgressUpdate;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final SentHistoryService sentHistoryService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Asynchronously orchestrates the creation of a ZIP archive from a FileSet and
     * dispatches it via email while providing real-time progress updates.
     * <p>
     * The workflow follows these atomic steps:
     * 1. Data Retrieval: Fetches the FileSet metadata and validates existence.
     * 2. Preparation: Generates a unique temporary path for the ZIP archive.
     * 3. Compression: Executes the ZIP process with a 0-90% progress reporting range.
     * 4. Persistence: Registers the archive in the database with a PENDING status.
     * 5. Delivery: Sends the email and updates statuses to SUCCESS (90-100%).
     * 6. Cleanup: Ensures the temporary disk resources are purged in the finally block.
     *
     * @param fileSetId      The ID of the file set to be processed.
     * @param recipientEmail The destination email address.
     * @param taskId         A unique Task UUID used for WebSocket topic subscription.
     * @throws FileSetNotFoundException If the provided ID does not match any record.
     */
    @Async
    public void createAndSendZipFromFileSetWithProgress(Long fileSetId, String recipientEmail, String taskId) {
        FileSet fileSet = fetchFileSet(fileSetId);
        Path zipPath = prepareTempZipPath(fileSetId);

        try {
            createZipArchive(fileSet.getFiles(), zipPath, (percent, msg) -> notifyProgress(taskId, percent, msg));

            ZipArchive archive = registerZipArchive(fileSet, zipPath, recipientEmail);

            sendAndFinalize(archive, fileSet, zipPath, taskId);
        } catch (Exception ex) {
            handleError(taskId, ex);
        } finally {
            cleanUp(zipPath);
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
     * Performs the actual file compression into a ZIP archive.
     * This method reads source files from the disk, writes them to the ZipOutputStream,
     * and calculates processing progress based on byte-size comparison.
     * * @param files             List of file entries to be compressed.
     * @param zipPath           The destination path for the ZIP archive.
     * @param progressCallback  A functional interface used to report progress percentage (0-90%).
     * @throws IOException      If any file access or write operation fails.
     */
    private void createZipArchive(List<FileEntry> files, Path zipPath, ProgressCallback progressCallback) throws IOException {

        long totalFileSizeBytes = files.stream()
                .mapToLong(FileEntry::getSize)
                .sum();

        if (totalFileSizeBytes == 0) {
            totalFileSizeBytes = 1;
        }

        long totalBytesProcessed = 0;
        int lastPercent = 0;

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {

            for (FileEntry fileEntry : files) {
                Path sourcePath = Path.of(fileEntry.getPath());

                if (Files.exists(sourcePath)) {
                    ZipEntry zipEntry = new ZipEntry(sourcePath.getFileName().toString());
                    zos.putNextEntry(zipEntry);

                    try (InputStream inputStream = Files.newInputStream(sourcePath)) {
                        byte[] buffer = new byte[8192];
                        int length;
                        long lastMessageTime = 0;

                        while ((length = inputStream.read(buffer)) != -1) {
                            zos.write(buffer, 0, length);
                            totalBytesProcessed += length;
                            int rawPercent = (int) ((totalBytesProcessed * 100) / totalFileSizeBytes);
                            int currPercent = (int) (rawPercent * 0.9);

                            long currentTime = System.currentTimeMillis();

                            if (currentTime - lastMessageTime > 150 || currPercent == 90) {
                                if (currPercent > lastPercent) {
                                    progressCallback.onUpdate(currPercent, "Processing: " + fileEntry.getName());
                                    lastPercent = currPercent;
                                    lastMessageTime = currentTime;
                                }
                            }
                        }
                    }
                    zos.closeEntry();
                }
            }
        }

    }

    /**
     * Sends a STOMP message to a specific task topic via the WebSocket message broker.
     * * @param taskId   The unique ID of the task used as the destination variable.
     * @param percent  The current progress percentage.
     * @param message  A descriptive status message for the user.
     */
    private void notifyProgress(String taskId, int percent, String message) {
        messagingTemplate.convertAndSend("/topic/progress/" + taskId, new ProgressUpdate(percent, message));
    }

    /**
     * Persists the initial record of the ZIP archive in the database.
     * The archive is saved with a PENDING status before the email attempt begins.
     * * @param fileSet        The source FileSet.
     * @param zipPath        Path where the archive was created.
     * @param recipientEmail The target recipient's address.
     * @return The saved ZipArchive entity.
     * @throws IOException   If file size metadata cannot be read from the disk.
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
     * @param fileSet    The source file set.
     * @param zipPath    The physical path to the ZIP file.
     * @param taskId     The task ID for progress updates.
     */
    private void sendAndFinalize(ZipArchive zipArchive, FileSet fileSet, Path zipPath, String taskId) {

        try {
            notifyProgress(taskId, 95, "Sending email to " + zipArchive.getRecipientEmail() + "...");

            sendZipArchiveByEmail(
                    zipArchive.getRecipientEmail(),
                    zipPath,
                    "Files",
                    "Please find attached the ZIP archive of requested files."
            );

            zipArchive.setStatus(ZipArchiveStatus.SUCCESS);
            fileSet.setStatus(FileSetStatus.SENT);
            zipArchiveRepository.save(zipArchive);
            fileSetRepository.save(fileSet);
            sentHistoryService.saveSentHistory(zipArchive, zipArchive.getRecipientEmail(), true, null);

            notifyProgress(taskId, 100, "Completed!");

        } catch (MessagingException me) {
            log.error("Failed to send email for task: {}", taskId, me);

            zipArchive.setStatus(ZipArchiveStatus.FAILED);
            zipArchiveRepository.save(zipArchive);
            sentHistoryService.saveSentHistory(zipArchive, zipArchive.getRecipientEmail(), false, me.getMessage());
        }
    }

    /**
     * Standardized error handler for the asynchronous process.
     * Logs the exception and sends a terminal progress update (-1 status)
     * to notify the frontend of the failure.
     * * @param taskId The task ID associated with the failure.
     * @param ex     The exception that triggered the handler.
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
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(text);
        helper.addAttachment(zipFilePath.getFileName().toString(),
                new FileSystemResource(zipFilePath.toFile()));

        mailSender.send(message);
    }
}
