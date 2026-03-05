package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.ScanPathResponseDTO;
import com.jerzymaj.file_researcher_backend.DTOs.ScanPathWithFilterRequest;
import com.jerzymaj.file_researcher_backend.DTOs.ScanPathRequest;
import com.jerzymaj.file_researcher_backend.configuration.ApiRoutes;
import com.jerzymaj.file_researcher_backend.services.FileExplorerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScanPathResponseDTO> scanUploadedFiles(@RequestParam("files")MultipartFile[] files,
                                                        @RequestParam(required = false) String extension) {

        log.info("Received {} files for scanning", files.length);

        return ResponseEntity.ok(fileExplorerService.scanUploadedFiles(files, extension));
    }
}
