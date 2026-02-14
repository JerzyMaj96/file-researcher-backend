package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.CreateFileSetDTO;
import com.jerzymaj.file_researcher_backend.DTOs.FileSetDTO;
import com.jerzymaj.file_researcher_backend.configuration.ApiRoutes;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import com.jerzymaj.file_researcher_backend.tranlator.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping(ApiRoutes.FILE_SETS)
@RequiredArgsConstructor
public class FileSetController {

    private final FileSetService fileSetService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileSetDTO> createNewFileSetFromUploaded(@RequestParam("name") String name,
                                                       @RequestParam("description") String description,
                                                       @RequestParam("recipientEmail") String recipientEmail,
                                                       @RequestParam("files") MultipartFile[] files) {

        FileSet createdFileSet = fileSetService.createFileSetFromUploadedFiles(
                name,
                description,
                recipientEmail,
                files);

        return ResponseEntity.created(URI.create("/file-researcher/file-sets/" + createdFileSet.getId()))
                .body(Translator.convertFileSetToDTO(createdFileSet));
    }

    @PostMapping
    public ResponseEntity<FileSetDTO> createNewFileSet(@Valid @RequestBody CreateFileSetDTO createFileSetDTO) {

        FileSet createdFileSet = fileSetService.createFileSet(
                createFileSetDTO.getName(),
                createFileSetDTO.getDescription(),
                createFileSetDTO.getRecipientEmail(),
                createFileSetDTO.getSelectedPaths());

        return ResponseEntity.created(URI.create("/file-researcher/file-sets/" + createdFileSet.getId()))
                .body(Translator.convertFileSetToDTO(createdFileSet));
    }

    @GetMapping
    public List<FileSetDTO> retrieveAllFileSets() {
        return fileSetService.getAllFileSets().stream()
                .map(Translator::convertFileSetToDTO)
                .toList();
    }

    @GetMapping("/{fileSetId}")
    public ResponseEntity<FileSetDTO> retrieveFileSetById(@PathVariable Long fileSetId) {
        FileSetDTO fileSetDTO = Translator.convertFileSetToDTO(fileSetService.getFileSetById(fileSetId));

        return ResponseEntity.ok(fileSetDTO);
    }

    @DeleteMapping("/{fileSetId}")
    public void deleteFileSetById(@PathVariable Long fileSetId) throws AccessDeniedException {
        fileSetService.deleteFileSetById(fileSetId);
    }

    @PatchMapping("/{fileSetId}/status")
    public ResponseEntity<FileSetDTO> updateFileSetStatus(@PathVariable Long fileSetId,
                                                          @RequestParam FileSetStatus status) throws AccessDeniedException {

        FileSetDTO updatedFileSetStatus = Translator.convertFileSetToDTO(fileSetService.changeFileSetStatus(fileSetId, status));

        return ResponseEntity.ok(updatedFileSetStatus);
    }

    @PatchMapping("/{fileSetId}/recipientEmail")
    public ResponseEntity<FileSetDTO> updateRecipientEmail(@PathVariable Long fileSetId,
                                                           @RequestParam String newRecipientEmail) throws AccessDeniedException {

        FileSetDTO updatedRecipientEmail = Translator.convertFileSetToDTO(fileSetService.changeRecipientEmail(fileSetId, newRecipientEmail));

        return ResponseEntity.ok(updatedRecipientEmail);
    }

    @PatchMapping("/{fileSetId}/name")
    public ResponseEntity<FileSetDTO> updateFileSetName(@PathVariable Long fileSetId,
                                                        @RequestParam String newName) throws AccessDeniedException {

        FileSetDTO updatedRecipientEmail = Translator.convertFileSetToDTO(fileSetService.changeFileSetName(fileSetId, newName));

        return ResponseEntity.ok(updatedRecipientEmail);
    }

    @PatchMapping("/{fileSetId}/description")
    public ResponseEntity<FileSetDTO> updateDescription(@PathVariable Long fileSetId,
                                                        @RequestParam String newDescription) throws AccessDeniedException {

        FileSetDTO updatedRecipientEmail = Translator.convertFileSetToDTO(fileSetService.changeFileSetDescription(fileSetId, newDescription));

        return ResponseEntity.ok(updatedRecipientEmail);
    }
}
