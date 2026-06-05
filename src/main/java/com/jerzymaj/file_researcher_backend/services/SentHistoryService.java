package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.exceptions.SentHistoryNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.SentHistory;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.enum_classes.SendStatus;
import com.jerzymaj.file_researcher_backend.repositories.SentHistoryRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import com.jerzymaj.file_researcher_backend.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SentHistoryService {

    private final AuthFacade authFacade;
    private final ZipArchiveRepository zipArchiveRepository;
    private final SentHistoryRepository sentHistoryRepository;


    public SentHistory saveSentHistory(ZipArchive zipArchive, String sentToEmail,
                                       boolean success, String errorMessage) {

        SentHistory sentHistory = SentHistory.builder()
                .zipArchive(zipArchive)
                .sentToEmail(sentToEmail)
                .status(success ? SendStatus.SUCCESS : SendStatus.FAILURE)
                .errorMessage(errorMessage)
                .build();

        return sentHistoryRepository.save(sentHistory);
    }

    public List<SentHistory> getAllSentHistory() {

        Long currentUserId = authFacade.getCurrentUserId();

        return sentHistoryRepository.findAllByUserIdSorted(currentUserId);
    }

    public List<SentHistory> getAllSentHistoryForZipArchive(Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = getZipArchiveForCurrentUser(zipArchiveId);
        return sentHistoryRepository.findAllByZipArchiveIdSorted(zipArchive.getId());
    }

    public SentHistory getSentHistoryById(Long zipArchiveId, Long sentHistoryId) throws AccessDeniedException {
        return getSentHistoryForCurrentUser(zipArchiveId, sentHistoryId);
    }

    public void deleteSentHistoryById(Long zipArchiveId, Long sentHistoryId) throws AccessDeniedException {
        SentHistory sentHistory = getSentHistoryForCurrentUser(zipArchiveId, sentHistoryId);
        sentHistoryRepository.deleteById(sentHistory.getId());
    }

    /**
     * Retrieves the email address of the last recipient of a given {@link ZipArchive}.
     *
     * @param zipArchiveId the ID of the ZIP archive
     * @return the email address of the most recent recipient
     */

    public String getLastRecipient(Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = getZipArchiveForCurrentUser(zipArchiveId);
        return sentHistoryRepository.findLastRecipient(zipArchive.getId());
    }

    /**
     * Retrieves a {@link ZipArchive} by ID and validates that it belongs to the current user.
     *
     * @param zipArchiveId ID of the ZipArchive to retrieve
     * @return the {@link ZipArchive} if found and owned by the current user
     * @throws AccessDeniedException       if the current user does not own the archive
     * @throws ZipArchiveNotFoundException if no ZipArchive exists for the given ID
     */
    private ZipArchive getZipArchiveForCurrentUser(Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("Zip archive not found: " + zipArchiveId));

        if (!zipArchive.getUser().getId().equals(authFacade.getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to access this archive.");
        }

        return zipArchive;
    }

    /**
     * Retrieves a {@link SentHistory} by ID and validates that it belongs to the current user and the given ZipArchive.
     *
     * @param zipArchiveId  ID of the ZipArchive the history should belong to
     * @param sentHistoryId ID of the SentHistory to retrieve
     * @return the {@link SentHistory} if found and owned by the current user
     * @throws AccessDeniedException      if the current user does not own the history or it does not belong to the given ZipArchive
     * @throws SentHistoryNotFoundException if no SentHistory exists for the given ID
     */
    private SentHistory getSentHistoryForCurrentUser(Long zipArchiveId, Long sentHistoryId) throws AccessDeniedException {
        SentHistory sentHistory = sentHistoryRepository.findById(sentHistoryId)
                .orElseThrow(() -> new SentHistoryNotFoundException("Sent history not found: " + sentHistoryId));

        if (!sentHistory.getZipArchive().getUser().getId().equals(authFacade.getCurrentUserId())
                || !sentHistory.getZipArchive().getId().equals(zipArchiveId)) {
            throw new AccessDeniedException("You do not have permission to access this history.");
        }

        return sentHistory;
    }
}
