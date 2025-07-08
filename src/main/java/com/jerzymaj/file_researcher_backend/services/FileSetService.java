package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.FileEntryDTO;
import com.jerzymaj.file_researcher_backend.DTOs.FileSetDTO;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.NoFilesSelectedException;
import com.jerzymaj.file_researcher_backend.exceptions.UserNotFoundException;
import com.jerzymaj.file_researcher_backend.models.FileEntry;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.suplementary_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileEntryRepository;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileSetService {
//PROPERTIES-------------------------------------------------------------------------------------------------

    private final FileEntryRepository fileEntryRepository;
    private final FileSetRepository fileSetRepository;
    private final UserRepository userRepository;

//MAIN METHODS------------------------------------------------------------------------------------------------------

    @Transactional
    public FileSetDTO createFileSet(String name,
                                    String description,
                                    String recipientEmail,
                                    List<String> selectedPaths
                                    ) throws IOException {

        if (selectedPaths == null || selectedPaths.isEmpty()) {
            throw new NoFilesSelectedException("At least one file must be selected");
        }

        Long currentUserId = getCurrentUserId();

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User " + currentUserId + " not found"));

        FileSet fileSet = fileSetRepository.save(
                FileSet.builder()
                        .name(name)
                        .description(description)
                        .recipientEmail(recipientEmail)
                        .status(FileSetStatus.ACTIVE)
                        .user(owner)
                        .build()
        );

        List<FileEntry> entries = selectedPaths.stream()
                .map(path -> fileEntryRepository.findByPath(path)
                        .orElseGet(() -> fileEntryRepository.save(convertPathToFileEntry(path))))
                .toList();

        fileSet.setFiles(entries);

        return convertFileSetToDTO(fileSet);
    }

    public List<FileSetDTO> getAllFileSets(){
        Long currentUserId = getCurrentUserId();

        return fileSetRepository.findAllByUserId(currentUserId).stream()
                .map(this::convertFileSetToDTO)
                .toList();
    }

    public FileSetDTO getFileSetById (Long fileSetId){
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        return this.convertFileSetToDTO(fileSet);
    }

    public void deleteFileSetById(Long fileSetId) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        Long currentUserId = getCurrentUserId();

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this FileSet.");
        }

        fileSetRepository.deleteById(fileSetId);
    }
//MAPPERS -----------------------------------------------------------------------------------------------------------


    private FileEntry convertPathToFileEntry(String path) {
        try {
            Path p = Path.of(path);
            boolean directory = Files.isDirectory(p);

            return FileEntry.builder()
                    .name(p.getFileName().toString())
                    .path(p.toAbsolutePath().toString())
                    .size(directory ? null : Files.size(p))
                    .extension(directory ? null : getExtension(p))
                    .build();
        } catch (IOException exception){
            throw new UncheckedIOException("Unable to read file info: " + path, exception);
        }
    }

    private FileSetDTO convertFileSetToDTO(FileSet fileSet){
        return FileSetDTO.builder()
                .id(fileSet.getId())
                .name(fileSet.getName())
                .description(fileSet.getDescription())
                .recipientEmail(fileSet.getRecipientEmail())
                .status(fileSet.getStatus())
                .creationDate(fileSet.getCreationDate())
                .userId(fileSet.getUser().getId())
                .files(fileSet.getFiles().stream()
                        .map(this::convertFileEntryToDTO)
                        .toList())
                .build();
    }

    private FileEntryDTO convertFileEntryToDTO(FileEntry fileEntry){
        return FileEntryDTO.builder()
                .id(fileEntry.getId())
                .name(fileEntry.getName())
                .path(fileEntry.getPath())
                .size(fileEntry.getSize())
                .extension(fileEntry.getExtension())
                .build();
    }

// SUPPLEMENTARY METHODS------------------------------------------------------------------------------------------

    private String getExtension(Path path){
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return (index > 0) ? fileName.substring(index + 1) : "";
    }

    private Long getCurrentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        return userRepository.findByName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userName))
                .getId();
    }

    public FileSetDTO changeFileSetStatus(Long fileSetId, FileSetStatus newFileSetStatus) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new  FileSetNotFoundException("FileSet not found: " + fileSetId));


        Long currentUserId = getCurrentUserId();

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to change this FileSet status.");
        }

        fileSet.setStatus(newFileSetStatus);
        fileSetRepository.save(fileSet);

        return convertFileSetToDTO(fileSet);
    }
}
