package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.configuration.ApiRoutes;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import com.jerzymaj.file_researcher_backend.tranlator.Translator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiRoutes.BASE_API)
@RequiredArgsConstructor
public class ZipArchiveController {

    private final ZipArchiveService zipArchiveService;


    @GetMapping("/zip-archives")
    public List<ZipArchiveDTO> retrieveAllZipArchivesForUser() {
        return zipArchiveService.getAllZipArchives().stream()
                .map(Translator::convertZipArchiveToDTO)
                .toList();
    }

    @GetMapping("/file-sets/{fileSetId}/zip-archives")
    public List<ZipArchiveDTO> retrieveAllZipArchivesForFileSet(@PathVariable Long fileSetId)
            throws AccessDeniedException {
        return zipArchiveService.getAllZipArchivesForFileSet(fileSetId).stream()
                .map(Translator::convertZipArchiveToDTO)
                .toList();
    }

    @GetMapping("/file-sets/{fileSetId}/zip-archives/{zipArchiveId}")
    public ResponseEntity<ZipArchiveDTO> retrieveZipArchiveById(@PathVariable Long fileSetId,
                                                                @PathVariable Long zipArchiveId)
            throws AccessDeniedException {

        ZipArchiveDTO zipArchiveDTO = Translator.convertZipArchiveToDTO(
                zipArchiveService.getZipArchiveById(fileSetId, zipArchiveId));

        return ResponseEntity.ok(zipArchiveDTO);
    }

    @PostMapping(value = "/file-sets/{fileSetId}/zip-archives/send-uploaded-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> sendZipArchiveFromMultipart(@PathVariable Long fileSetId,
                                                             @RequestParam String recipientEmail,
                                                             @RequestParam("files") MultipartFile[] files) throws IOException {

        String taskId = zipArchiveService.startZipUploadProcess(fileSetId, recipientEmail, files);

        return ResponseEntity.ok(taskId);
    }

    @DeleteMapping("/file-sets/{fileSetId}/zip-archives/{zipArchiveId}")
    public ResponseEntity<Void> deleteZipArchiveById(@PathVariable Long fileSetId,
                                                     @PathVariable Long zipArchiveId) throws AccessDeniedException {

        zipArchiveService.deleteZipArchive(fileSetId, zipArchiveId);

        return ResponseEntity.noContent().build();
    }
}
