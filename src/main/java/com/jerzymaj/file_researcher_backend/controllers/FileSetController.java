package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.CreateFileSetDTO;
import com.jerzymaj.file_researcher_backend.DTOs.FileSetDTO;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/file-researcher/file-sets")
@RequiredArgsConstructor
public class FileSetController {
//CONTROLLER PROPERTIES --------------------------------------------------------

    private final FileSetService fileSetService;

//METHODS ------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<FileSetDTO> createNewFileSet(@Valid @RequestBody CreateFileSetDTO createFileSetDTO)
            throws IOException {

        FileSetDTO createdFileSet = fileSetService.createFileSet(
                createFileSetDTO.getName(),
                createFileSetDTO.getDescription(),
                createFileSetDTO.getRecipientEmail(),
                createFileSetDTO.getSelectedPaths(),
                createFileSetDTO.getUserId());

        return ResponseEntity.created(URI.create("/file-researcher/file-sets/" + createdFileSet.getId()))
                .body(createdFileSet);
    }
}
