package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.SentHistoryDTO;
import com.jerzymaj.file_researcher_backend.configuration.ApiRoutes;
import com.jerzymaj.file_researcher_backend.services.SentHistoryService;
import com.jerzymaj.file_researcher_backend.tranlator.Translator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping(ApiRoutes.ZIP_ARCHIVES)
@RequiredArgsConstructor
public class SentHistoryController {

    private final SentHistoryService sentHistoryService;

    @GetMapping("/history")
    public List<SentHistoryDTO> retrieveAllSentHistoryForUser() {
        return sentHistoryService.getAllSentHistory().stream()
                .map(Translator::convertSentHistoryToDTO)
                .toList();
    }

    @GetMapping("/{zipArchiveId}/history")
    public ResponseEntity<List<SentHistoryDTO>> retrieveAllSentHistoryForZipArchive(@PathVariable Long zipArchiveId) throws AccessDeniedException {

        List<SentHistoryDTO> sentHistoryDTOList = sentHistoryService.getAllSentHistoryForZipArchive(zipArchiveId).stream()
                .map(Translator::convertSentHistoryToDTO)
                .toList();

        if (sentHistoryDTOList.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(sentHistoryDTOList);
    }

    @GetMapping("/{zipArchiveId}/history/last-recipient")
    public ResponseEntity<String> retrieveLastRecipient(@PathVariable Long zipArchiveId) throws AccessDeniedException {

        String email = sentHistoryService.getLastRecipient(zipArchiveId);

        if (email == null || email.isBlank()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(email);
    }

    @GetMapping("/{zipArchiveId}/history/{sentHistoryId}")
    public ResponseEntity<SentHistoryDTO> retrieveSentHistoryById(@PathVariable Long zipArchiveId,
                                                                  @PathVariable Long sentHistoryId) throws AccessDeniedException {

        SentHistoryDTO sentHistoryDTO = Translator.convertSentHistoryToDTO(
                sentHistoryService.getSentHistoryById(zipArchiveId, sentHistoryId));

        return ResponseEntity.ok(sentHistoryDTO);
    }

    @DeleteMapping("/{zipArchiveId}/history/{sentHistoryId}")
    public ResponseEntity<Void> deleteSentHistoryById(@PathVariable Long zipArchiveId,
                                                      @PathVariable Long sentHistoryId) throws AccessDeniedException {

        sentHistoryService.deleteSentHistoryById(zipArchiveId, sentHistoryId);

        return ResponseEntity.noContent().build();
    }
}
