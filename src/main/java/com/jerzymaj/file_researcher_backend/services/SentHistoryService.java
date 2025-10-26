package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.exceptions.SentHistoryNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.SentHistory;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.enum_classes.SendStatus;
import com.jerzymaj.file_researcher_backend.repositories.SentHistoryRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SentHistoryService {

    private final FileSetService fileSetService;
    private final ZipArchiveRepository zipArchiveRepository;
    private final SentHistoryRepository sentHistoryRepository;


    public SentHistory saveSentHistory(ZipArchive zipArchive, String sentToEmail,
                                       boolean success, String errorMessage) {

        SentHistory sentHistory = new SentHistory();
        sentHistory.setZipArchive(zipArchive);
        sentHistory.setSentToEmail(sentToEmail);
        sentHistory.setSendAttemptDate(LocalDateTime.now());
        sentHistory.setStatus(success ? SendStatus.SUCCESS : SendStatus.FAILURE);
        sentHistory.setErrorMessage(errorMessage);

        return sentHistoryRepository.save(sentHistory);
    }

    public List<SentHistory> getAllSentHistory() {

        Long currentUserId = fileSetService.getCurrentUserId();

        return sentHistoryRepository.findAllByUserIdSorted(currentUserId);
    }

    public List<SentHistory> getAllSentHistoryForZipArchive(Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("Zip archive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this history.");
        }

        return sentHistoryRepository.findAllByZipArchiveIdSorted(zipArchiveId);
    }

    public SentHistory getSentHistoryById(Long zipArchiveId, Long sentHistoryId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("Zip archive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId))
            throw new AccessDeniedException("You do not have permission to access this history.");

        return sentHistoryRepository.findById(sentHistoryId)
                .orElseThrow(() -> new SentHistoryNotFoundException("Sent history not found: " + sentHistoryId));
    }

    public void deleteSentHistoryById(Long zipArchiveId, Long sentHistoryId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("Zip archive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this history.");
        }

        SentHistory sentHistory = sentHistoryRepository.findById(sentHistoryId)
                .orElseThrow(() -> new SentHistoryNotFoundException("History not found: " + sentHistoryId));

        if (!sentHistory.getZipArchive().getId().equals(zipArchiveId)) {
            throw new AccessDeniedException("History does not belong to this archive.");
        }

        sentHistoryRepository.deleteById(sentHistoryId);
    }

    /**
     * Retrieves the email address of the last recipient of a given {@link ZipArchive}.
     *
     * @param zipArchiveId the ID of the ZIP archive
     * @return the email address of the most recent recipient
     * @throws AccessDeniedException       if the current user does not own the archive
     * @throws ZipArchiveNotFoundException if the ZIP archive does not exist
     */

    public String getLastRecipient(Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("Zip archive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this history.");
        }

        return sentHistoryRepository.findLastRecipient(zipArchiveId);
    }
}
