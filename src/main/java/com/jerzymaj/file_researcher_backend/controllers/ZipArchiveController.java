package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.configuration.ApiRoutes;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import com.jerzymaj.file_researcher_backend.tranlator.Translator;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/file-sets/{fileSetId}/zip-archives/send")
    public ResponseEntity<ZipArchiveDTO> sendZipArchive(@PathVariable Long fileSetId,
                                                        @RequestParam String recipientEmail) throws MessagingException, IOException {

        log.info("Sending ZIP archive for fileSetId={} to recipient={}", fileSetId, recipientEmail);

        ZipArchiveDTO zipArchiveDTO = Translator.convertZipArchiveToDTO(
                zipArchiveService.createAndSendZipArchive(fileSetId, recipientEmail));

        log.info("ZIP archive for fileSetId={} successfully sent to {}", fileSetId, recipientEmail);

        return ResponseEntity.ok(zipArchiveDTO);
    }

    @PutMapping("/file-sets/{fileSetId}/zip-archives/{zipArchiveId}/resend")
    public ResponseEntity<?> resendZipArchive(@PathVariable Long fileSetId,
                                              @PathVariable Long zipArchiveId,
                                              @RequestParam String recipientEmail)
            throws AccessDeniedException, MessagingException {

        zipArchiveService.resendExistingZip(fileSetId, zipArchiveId, recipientEmail);

        return ResponseEntity.ok("ZIP archive resent successfully.");
    }

    @DeleteMapping("/file-sets/{fileSetId}/zip-archives/{zipArchiveId}")
    public ResponseEntity<Void> deleteZipArchiveById(@PathVariable Long fileSetId,
                                                     @PathVariable Long zipArchiveId) throws AccessDeniedException {

        zipArchiveService.deleteZipArchive(fileSetId, zipArchiveId);

        return ResponseEntity.noContent().build();
    }
}
