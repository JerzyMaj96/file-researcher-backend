package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.CreateFileSetDTO;
import com.jerzymaj.file_researcher_backend.DTOs.FileSetDTO;
import com.jerzymaj.file_researcher_backend.models.suplementary_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.List;

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
                createFileSetDTO.getSelectedPaths());

        return ResponseEntity.created(URI.create("/file-researcher/file-sets/" + createdFileSet.getId()))
                .body(createdFileSet);
    }

    @GetMapping
    public List<FileSetDTO> retrieveAllFileSets(){
        return fileSetService.getAllFileSets();
    }

    @GetMapping("/{fileSetId}")
    public ResponseEntity<FileSetDTO> retrieveFileSetById(@PathVariable Long fileSetId){
        FileSetDTO fileSetDTO = fileSetService.getFileSetById(fileSetId);

        return ResponseEntity.ok(fileSetDTO);
    }

    @DeleteMapping("/{fileSetId}")
    public void deleteFileSetById(@PathVariable Long fileSetId) throws AccessDeniedException { //Check response status
        fileSetService.deleteFileSetById(fileSetId);
    }

    @PatchMapping("/{fileSetId}/status")
    public ResponseEntity<FileSetDTO> updateFileSetStatus(@PathVariable Long fileSetId,
                                                          @RequestParam FileSetStatus status) throws AccessDeniedException {

        FileSetDTO updatedFileSetStatus = fileSetService.changeFileSetStatus(fileSetId, status);

        return ResponseEntity.ok(updatedFileSetStatus);
    }
}
