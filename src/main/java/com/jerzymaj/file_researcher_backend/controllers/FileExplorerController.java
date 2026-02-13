package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.ScanPathResponseDTO;
import com.jerzymaj.file_researcher_backend.DTOs.ScanPathWithFilterRequest;
import com.jerzymaj.file_researcher_backend.DTOs.ScanPathRequest;
import com.jerzymaj.file_researcher_backend.configuration.ApiRoutes;
import com.jerzymaj.file_researcher_backend.services.FileExplorerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping(ApiRoutes.FILE_EXPLORER)
@RequiredArgsConstructor
public class FileExplorerController {

    private final FileExplorerService fileExplorerService;

    @PostMapping("/scan")
    public ResponseEntity<ScanPathResponseDTO> scanPath(@RequestParam("files")MultipartFile[] files) {

        log.info("Received {} files for scanning", files.length);

        return ResponseEntity.ok(fileExplorerService.scanUploadedFiles(files));
    }

//    @PostMapping("/scan")
//    public ResponseEntity<ScanPathResponseDTO> scanPath(@Valid @RequestBody ScanPathRequest request) {
//
//        Path path = Path.of(request.path());
//        log.info("Scanning path: {}", path);
//
//        return ResponseEntity.ok(fileExplorerService.scanPath(path));
//    }

//    @PostMapping("/scan/filtered")
//    public ResponseEntity<ScanPathResponseDTO> scanFilteredPath(@Valid @RequestBody ScanPathWithFilterRequest request) {
//
//        Path path = Path.of(request.path());
//        log.info("Scanning filtered path: {} with extension: {}", path, request.extension());
//
//        return ResponseEntity.ok(fileExplorerService.scanFilteredPath(path, request.extension()));
//    }
}
