package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.FileTreeNodeDTO;
import com.jerzymaj.file_researcher_backend.services.FileExplorerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/file-researcher/explorer")
@RequiredArgsConstructor
public class FileExplorerController {
//CONTROLLER PROPERTIES ---------------------------------------------------------------------------

    private final FileExplorerService fileExplorerService;

//METHODS -----------------------------------------------------------------------------------------

    @PostMapping("/scan")
    public ResponseEntity<?> scanPath(@RequestBody Map<String, String> request) {
        String pathString = request.get("path");

        if (pathString == null || pathString.isBlank()) {
            return ResponseEntity.badRequest().body("Path cannot be empty");
        }

        try {
            Path path = Path.of(pathString);
            FileTreeNodeDTO scannedPaths = fileExplorerService.scanPath(path);
            return ResponseEntity.ok(scannedPaths);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error scanning path: " + e.getMessage());
        }
    }

    @PostMapping("/scan/filtered")
    public ResponseEntity<?> scanFilteredPath(@RequestBody Map<String, String> request,
                                              @RequestParam String extension) {

        String pathString = request.get("path");

        if (pathString == null || pathString.isBlank()) {
            return ResponseEntity.badRequest().body("Path cannot be empty");
        }

        try {
            Path path = Path.of(pathString);
            FileTreeNodeDTO scannedPaths = fileExplorerService.scanFilteredPath(path,extension);
            return ResponseEntity.ok(scannedPaths);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error scanning path: " + e.getMessage());
        }
    }
}

