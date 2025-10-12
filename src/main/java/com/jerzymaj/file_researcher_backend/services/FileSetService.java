package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.NoFilesSelectedException;
import com.jerzymaj.file_researcher_backend.exceptions.UserNotFoundException;
import com.jerzymaj.file_researcher_backend.models.FileEntry;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileEntryRepository;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    private final FileEntryRepository fileEntryRepository;
    private final FileSetRepository fileSetRepository;
    private final UserRepository userRepository;

    @Transactional
    public FileSet createFileSet(String name,
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

        return fileSet;
    }

    public List<FileSet> getAllFileSets() {
        Long currentUserId = getCurrentUserId();

        return fileSetRepository.findAllByUserId(currentUserId);
    }

    public FileSet getFileSetById(Long fileSetId) {
        return fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));
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
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read file info: " + path, exception);
        }
    }

    protected static String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return (index > 0) ? fileName.substring(index + 1) : "";
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        return userRepository.findByName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userName))
                .getId();
    }

    public FileSet changeFileSetStatus(Long fileSetId, FileSetStatus newFileSetStatus) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));


        Long currentUserId = getCurrentUserId();

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to change this FileSet status.");
        }

        fileSet.setStatus(newFileSetStatus);
        fileSetRepository.save(fileSet);

        return fileSet;
    }

    public FileSet changeRecipientEmail(Long fileSetId, String newEmail) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        Long currentUserId = getCurrentUserId();

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to change the recipient email.");
        }

        fileSet.setRecipientEmail(newEmail);
        fileSetRepository.save(fileSet);

        return fileSet;
    }

    public FileSet changeFileSetName(Long fileSetId, String newName) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        Long currentUserId = getCurrentUserId();

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to change the recipient email.");
        }

        fileSet.setName(newName);
        fileSetRepository.save(fileSet);

        return fileSet;
    }

    public FileSet changeFileSetDescription(Long fileSetId, String newDescription) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        Long currentUserId = getCurrentUserId();

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to change the recipient email.");
        }

        fileSet.setDescription(newDescription);
        fileSetRepository.save(fileSet);

        return fileSet;
    }
}
