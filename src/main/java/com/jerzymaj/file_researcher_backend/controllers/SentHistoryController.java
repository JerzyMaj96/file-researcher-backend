package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.SentHistoryDTO;
import com.jerzymaj.file_researcher_backend.services.SentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/file-researcher/zip-archives/{zipArchiveId}/history")
@RequiredArgsConstructor
public class SentHistoryController {
//PROPERTIES------------------------------------------------------------------------------------------------------------

    private final SentHistoryService sentHistoryService;

//METHODS---------------------------------------------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<SentHistoryDTO>> retrieveAllSentHistoryForZipArchive(@PathVariable Long zipArchiveId) throws AccessDeniedException {

        List<SentHistoryDTO> sentHistoryDTOList = sentHistoryService.getAllSentHistoryForZipArchive(zipArchiveId);

        if(sentHistoryDTOList.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(sentHistoryDTOList);
    }

    @GetMapping("/last-recipient")
    public ResponseEntity<String> retrieveLastRecipient(@PathVariable Long zipArchiveId) throws AccessDeniedException {

        String email = sentHistoryService.getLastRecipient(zipArchiveId);

        if (email == null || email.isBlank()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(email);
    }

    @GetMapping("/{sentHistoryId}")
    public ResponseEntity<SentHistoryDTO> retrieveSentHistoryById(@PathVariable Long zipArchiveId,
                                                                  @PathVariable Long sentHistoryId) throws AccessDeniedException {

        return ResponseEntity.ok(sentHistoryService.getSentHistoryById(zipArchiveId, sentHistoryId));
    }

    @DeleteMapping("/{sentHistoryId}")
    public  ResponseEntity<Void> deleteSentHistoryById(@PathVariable Long zipArchiveId,
                                                       @PathVariable Long sentHistoryId) throws AccessDeniedException {

        sentHistoryService.deleteSentHistoryById(zipArchiveId, sentHistoryId);

        return ResponseEntity.noContent().build();
    }
}
