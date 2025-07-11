package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/file-researcher/file-sets/{fileSetId}/zip")
@RequiredArgsConstructor
public class ZipArchiveController {
//PROPERTIES---------------------------------------------------------------------

    private final ZipArchiveService zipArchiveService;

//MAIN METHODS---------------------------------------------------------------------

    @PostMapping("/send")
    public ResponseEntity<ZipArchiveDTO> sendZipArchive(@RequestParam Long fileSetId,
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
}
