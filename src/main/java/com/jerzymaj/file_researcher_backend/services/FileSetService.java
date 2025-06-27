package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.FileEntryDTO;
import com.jerzymaj.file_researcher_backend.DTOs.FileSetDTO;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
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
                                    List<String> selectedPaths,
                                    Long userId) throws IOException {

        if (selectedPaths == null || selectedPaths.isEmpty()) {
            throw new NoFilesSelectedException("At least one file must be selected");
        }

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));

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
                .map(path -> convertPathToFileEntry(path, fileSet))
                .toList();

        fileEntryRepository.saveAll(entries);

        fileSet.setFiles(entries);

        return convertFileSetToDTO(fileSet);

    }
//MAPPERS -----------------------------------------------------------------------------------------------------------


    private FileEntry convertPathToFileEntry(String path, FileSet parentFileSet) {
        try {
            Path p = Path.of(path);
            boolean directory = Files.isDirectory(p);

            return FileEntry.builder()
                    .name(p.getFileName().toString())
                    .path(p.toAbsolutePath().toString())
                    .size(directory ? null : Files.size(p))
                    .extension(directory ? null : getExtension(p))
                    .fileSet(parentFileSet)
                    .build();
        } catch (IOException exception){
            throw new UncheckedIOException("Unable to read file info: " + path, exception);
        }
    }
//SUPPLEMENTARY METHOD
    private String getExtension(Path path){
        String node = path.getFileName().toString();
        int index = node.lastIndexOf('.');
        return (index > 0) ? node.substring(index + 1) : "";
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
                .fileSetId(fileEntry.getFileSet().getId())
                .build();
    }
}
