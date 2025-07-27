package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.SentHistoryDTO;
import com.jerzymaj.file_researcher_backend.exceptions.SentHistoryNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.SentHistory;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.suplementary_classes.SendStatus;
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
//PROPERTIES--------------------------------------------------------------------

    private final FileSetService fileSetService;
    private final ZipArchiveRepository zipArchiveRepository;
    private final SentHistoryRepository sentHistoryRepository;

//METHODS-----------------------------------------------------------------------

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

    public List<SentHistoryDTO> getAllSentHistoryForZipArchive(Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new  ZipArchiveNotFoundException("Zip archive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this history.");
        }

       return sentHistoryRepository.findAllByZipArchiveIdSorted(zipArchiveId).stream()
               .map(this::convertToSentHistoryDTO)
               .toList();
    }

    public SentHistoryDTO getSentHistoryById(Long sentHistoryId) {

        SentHistory sentHistory = sentHistoryRepository.findById(sentHistoryId)
                .orElseThrow(() -> new SentHistoryNotFoundException("Sent history not found: " + sentHistoryId));

        return convertToSentHistoryDTO(sentHistory);
    }

    public void deleteSentHistoryById(Long sentHistoryId) {

        sentHistoryRepository.deleteById(sentHistoryId);
    }

    public String getLastRecipient(Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("Zip archive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if(!zipArchive.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this history.");
        }

        return sentHistoryRepository.findLastRecipient(zipArchiveId);
    }
//MAPPER--------------------------------------------------------------------------

    private SentHistoryDTO convertToSentHistoryDTO(SentHistory sentHistory) {

        return SentHistoryDTO.builder()
                .id(sentHistory.getId())
                .zipArchiveId(sentHistory.getZipArchive().getId())
                .sentAttemptDate(sentHistory.getSendAttemptDate())
                .status(sentHistory.getStatus())
                .errorMessage(sentHistory.getErrorMessage())
                .sentToEmail(sentHistory.getSentToEmail())
                .build();
    }
}
