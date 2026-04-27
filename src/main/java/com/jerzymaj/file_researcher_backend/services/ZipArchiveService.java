package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.ProgressUpdate;
import com.jerzymaj.file_researcher_backend.DTOs.StagedUpload;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipArchiveService {

    private final ZipArchiveCreator zipArchiveCreator;
    private final ZipEmailSender zipEmailSender;
    private final FileStager fileStager;
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
    public String startZipProcessFromUploaded(Long fileSetId, String recipientEmail, MultipartFile[] files) throws IOException {
        StagedUpload staged = fileStager.stageUpload(files);

        createAndSendZipAsync(fileSetId, recipientEmail, staged);

        return staged.taskId();
    }

    /**
     * Asynchronously creates a ZIP from staged files and dispatches it via email.
     * <p>
     * <b>Workflow:</b>
     * <ol>
     * <li>Fetches FileSet metadata from the database.</li>
     * <li>Creates a ZIP archive from staged files with progress reporting (0-90%).</li>
     * <li>Registers the archive in PENDING status.</li>
     * <li>Sends email (95%) and finalizes status to SUCCESS or FAILED.</li>
     * <li>Purges all temporary resources (ZIP and staging folder) in the finally block.</li>
     * </ol>
     * </p>
     *
     * @param fileSetId      The ID of the associated FileSet.
     * @param recipientEmail Target email address.
     * @param stagedUpload   The staged upload containing taskId, file paths, and upload directory.
     */
    @Async
    public void createAndSendZipAsync(Long fileSetId, String recipientEmail, StagedUpload stagedUpload) {
        Path zipPath = null;
        try {
            FileSet fileSet = fetchFileSet(fileSetId);

            int sendCounter = zipArchiveRepository
                    .findMaxSendNumberByFileSetId(fileSetId) + 1;

            zipPath = zipArchiveCreator.prepareTempPath(fileSetId, sendCounter);

            zipArchiveCreator.createZipArchiveFromPaths(stagedUpload.files(), zipPath, stagedUpload.uploadDir(),
                    (percent, msg) -> notifyProgress(stagedUpload.taskId(), percent, msg));

            ZipArchive archive = registerZipArchive(fileSet, zipPath, recipientEmail, sendCounter);

            sendAndFinalize(archive, fileSet, zipPath, stagedUpload.taskId());

        } catch (Exception ex) {
            handleError(stagedUpload.taskId(), ex);
        } finally {
            cleanUp(zipPath);
            recursiveDelete(stagedUpload.uploadDir());
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

        return zipArchiveRepository.findAllByFileSetId(fileSetId);
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
    private ZipArchive registerZipArchive(FileSet fileSet, Path zipPath, String recipientEmail, int sendCounter) throws IOException {

        return zipArchiveRepository.save(ZipArchive.builder()
                .archiveName(zipPath.getFileName().toString())
                .archivePath(zipPath.toAbsolutePath().toString())
                .size(Files.size(zipPath))
                .status(ZipArchiveStatus.PENDING)
                .recipientEmail(recipientEmail)
                .fileSet(fileSet)
                .user(fileSet.getUser())
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
            zipEmailSender.sendZipArchiveByEmail(zipArchive.getRecipientEmail(), zipPath, "Files", "Please find attached the ZIP archive of requested files");

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
}
