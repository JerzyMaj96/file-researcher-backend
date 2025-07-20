package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.SentHistoryDTO;
import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.exceptions.SentHistoryNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.SentHistory;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.repositories.SentHistoryRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SentHistoryService {
//PROPERTIES--------------------------------------------------------------------

    private final FileSetService fileSetService;
    private final ZipArchiveRepository zipArchiveRepository;
    private final SentHistoryRepository sentHistoryRepository;

//METHODS-----------------------------------------------------------------------

    public SentHistoryDTO createSentHistory(ZipArchiveDTO zipArchiveDTO) {

        return ;
    }

    public List<SentHistoryDTO> getAllSentHistory() {

       return sentHistoryRepository.findAll().stream()
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
