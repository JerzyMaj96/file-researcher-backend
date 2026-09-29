package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.StagedUpload;
import com.jerzymaj.file_researcher_backend.DTOs.ZipStatsResponse;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import com.jerzymaj.file_researcher_backend.security.AuthFacade;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipArchiveService {

    private final ZipArchiveProcessor zipArchiveProcessor;
    private final FileStager fileStager;
    private final FileSetRepository fileSetRepository;
    private final AuthFacade authFacade;
    private final ZipArchiveRepository zipArchiveRepository;

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
     * @return {@link String} The unique taskId for WebSocket tracking.
     * @throws IOException If file staging fails.
     */
    public String startZipProcessFromUploaded(Long fileSetId, String recipientEmail, MultipartFile[] files) throws IOException {
        StagedUpload staged = fileStager.stageUpload(files);

        zipArchiveProcessor.createAndSendZipAsync(fileSetId, recipientEmail, staged);

        return staged.taskId();
    }

    public List<ZipArchive> getAllZipArchives() {
        Long currentUserId = authFacade.getCurrentUserId();

        return zipArchiveRepository.findAllByUserId(currentUserId);
    }

    public List<ZipArchive> getAllZipArchivesForFileSet(Long fileSetId) throws AccessDeniedException {
        Long currentUserId = authFacade.getCurrentUserId();

        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        return zipArchiveRepository.findAllByFileSetId(fileSetId);
    }

    public ZipArchive getZipArchiveById(Long fileSetId, Long zipArchiveId) throws AccessDeniedException {
        return getZipArchiveForCurrentUser(fileSetId, zipArchiveId);
    }

    public void deleteZipArchive(Long fileSetId, Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = getZipArchiveForCurrentUser(fileSetId, zipArchiveId);
        zipArchiveRepository.deleteById(zipArchive.getId());
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

    public ZipStatsResponse getZipStatsForCurrentUser() {
        Long userId = authFacade.getCurrentUserId();

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
        Long userId = authFacade.getCurrentUserId();

        return zipArchiveRepository.findLargeZipArchives(userId, minSize);
    }

    /**
     * Retrieves a {@link ZipArchive} by ID and validates that it belongs to the current user and the given FileSet.
     *
     * @param fileSetId    ID of the FileSet the archive should belong to
     * @param zipArchiveId ID of the ZipArchive to retrieve
     * @return the {@link ZipArchive} if found and owned by the current user
     * @throws AccessDeniedException      if the current user does not own the archive, or it does not belong to the given FileSet
     * @throws ZipArchiveNotFoundException if no ZipArchive exists for the given ID
     */

    private ZipArchive getZipArchiveForCurrentUser(Long fileSetId, Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        Long currentUserId = authFacade.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)
                || !zipArchive.getFileSet().getId().equals(fileSetId)) {
            throw new AccessDeniedException("You do not have permission to access this ZipArchive.");
        }

        return zipArchive;
    }
}
