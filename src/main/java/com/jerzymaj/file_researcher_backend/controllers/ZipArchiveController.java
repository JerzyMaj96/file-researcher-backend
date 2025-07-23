package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file-researcher/file-sets/{fileSetId}/zip")
@RequiredArgsConstructor
public class ZipArchiveController {
//PROPERTIES---------------------------------------------------------------------

    private final ZipArchiveService zipArchiveService;

//MAIN METHODS---------------------------------------------------------------------

    @PostMapping("/send")
    public ResponseEntity<ZipArchiveDTO> sendZipArchive(@PathVariable Long fileSetId,
                                                        @RequestParam String recipientEmail) {
        try {
            ZipArchiveDTO zipArchiveDTO = zipArchiveService.createAndSendZipArchive(fileSetId, recipientEmail);
            return ResponseEntity.ok(zipArchiveDTO);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).build();
        } catch (IOException | MessagingException exception) {
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{zipArchiveId}/resend")
    public ResponseEntity<?> resendZipArchive(@PathVariable Long fileSetId,
                                              @PathVariable Long zipArchiveId,
                                              @RequestParam String recipientEmail) throws AccessDeniedException, MessagingException {

        zipArchiveService.resendExistingZip(fileSetId, zipArchiveId, recipientEmail);
        return ResponseEntity.ok("ZIP archive resent successfully.");
    }

    @GetMapping
    public List<ZipArchiveDTO> retrieveAllZipArchives(@PathVariable Long fileSetId) throws AccessDeniedException {
        return zipArchiveService.getAllZipArchives(fileSetId);
    }

    @GetMapping("/{zipArchiveId}")
    public ResponseEntity<ZipArchiveDTO> retrieveZipArchiveById(@PathVariable Long fileSetId,
                                                                @PathVariable Long zipArchiveId) throws AccessDeniedException {
        ZipArchiveDTO zipArchiveDTO = zipArchiveService.getZipArchiveById(fileSetId, zipArchiveId);

        return ResponseEntity.ok(zipArchiveDTO);
    }

    @DeleteMapping("/{zipArchiveId}")
    public ResponseEntity<Void> deleteZipArchiveById(@PathVariable Long fileSetId,
                                                     @PathVariable Long zipArchiveId) throws AccessDeniedException {
        zipArchiveService.deleteZipArchive(fileSetId, zipArchiveId);

        return ResponseEntity.noContent().build();
    }
}
