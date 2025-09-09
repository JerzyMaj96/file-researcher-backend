package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.FileTreeNodeDTO;
import com.jerzymaj.file_researcher_backend.DTOs.ScanRequest;
import com.jerzymaj.file_researcher_backend.services.FileExplorerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/file-researcher/explorer")
@RequiredArgsConstructor
public class FileExplorerController {
//CONTROLLER PROPERTIES------------------------------------------------------------------------

    private final FileExplorerService fileExplorerService;

//METHODS---------------------------------------------------------------------------
    @PostMapping("/scan")
    public ResponseEntity<?> scanPath(@RequestBody ScanRequest request) {
        if (request.path() == null || request.path().isBlank()) {
            return ResponseEntity.badRequest().body("Path cannot be empty");
        }

        try {
            Path path = Path.of(request.path());
            log.info("Scanning path: {}", path);

            FileTreeNodeDTO scannedPaths = fileExplorerService.scanPath(path);
            return ResponseEntity.ok(scannedPaths);
        } catch (Exception e) {
            log.error("Error scanning path: {}", request.path(), e);
            return ResponseEntity.status(500).body("Error scanning path: " + e.getMessage());
        }
    }

    @PostMapping("/scan/filtered")
    public ResponseEntity<?> scanFilteredPath(@RequestBody ScanRequest request) {
        if (request.path() == null || request.path().isBlank()) {
            return ResponseEntity.badRequest().body("Path cannot be empty");
        }
        if (request.extension() == null || request.extension().isBlank()) {
            return ResponseEntity.badRequest().body("Extension cannot be empty");
        }

        try {
            Path path = Path.of(request.path());
            log.info("Scanning filtered path: {} with extension: {}", path, request.extension());

            FileTreeNodeDTO scannedPaths = fileExplorerService.scanFilteredPath(path, request.extension());
            return ResponseEntity.ok(scannedPaths);
        } catch (Exception e) {
            log.error("Error scanning filtered path: {} with extension: {}", request.path(), request.extension(), e);
            return ResponseEntity.status(500).body("Error scanning path: " + e.getMessage());
        }
    }
}
