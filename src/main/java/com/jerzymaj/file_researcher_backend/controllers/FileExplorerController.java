package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.ScanPathResultDTO;
import com.jerzymaj.file_researcher_backend.DTOs.ScanFilteredRequest;
import com.jerzymaj.file_researcher_backend.DTOs.ScanRequest;
import com.jerzymaj.file_researcher_backend.configuration.ApiRoutes;
import com.jerzymaj.file_researcher_backend.services.FileExplorerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping(ApiRoutes.FILE_EXPLORER)
@RequiredArgsConstructor
public class FileExplorerController {

    private final FileExplorerService fileExplorerService;

    @PostMapping("/scan")
    public ResponseEntity<ScanPathResultDTO> scanPath(@Valid @RequestBody ScanRequest request) {

        Path path = Path.of(request.path());
        log.info("Scanning path: {}", path);

        return ResponseEntity.ok(fileExplorerService.scanPath(path));
    }

    @PostMapping("/scan/filtered")
    public ResponseEntity<ScanPathResultDTO> scanFilteredPath(@Valid @RequestBody ScanFilteredRequest request) {

        Path path = Path.of(request.path());
        log.info("Scanning filtered path: {} with extension: {}", path, request.extension());

        return ResponseEntity.ok(fileExplorerService.scanFilteredPath(path, request.extension()));
    }
}
