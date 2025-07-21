package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.SentHistoryDTO;
import com.jerzymaj.file_researcher_backend.services.SentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/file-researcher/zip-archives/{zipArchiveId}/history")
@RequiredArgsConstructor
public class SentHistoryController {

    private final SentHistoryService sentHistoryService;

    @GetMapping
    public List<SentHistoryDTO> retrieveAllSentHistoryForZipArchive(@PathVariable Long zipArchiveId) throws AccessDeniedException {

        return sentHistoryService.getAllSentHistoryForZipArchive(zipArchiveId);
    }
}
