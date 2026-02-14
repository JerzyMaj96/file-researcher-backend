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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileSetService {

    private final FileEntryRepository fileEntryRepository;
    private final FileSetRepository fileSetRepository;
    private final UserRepository userRepository;

    @Transactional
    public FileSet createFileSetFromUploadedFiles(String name,
                                                  String description,
                                                  String recipientEmail,
                                                  MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new NoFilesSelectedException("Files haven't been uploaded");
        }

        Long currentUserId = getCurrentUserId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User " + currentUserId + " not found"));


        List<FileEntry> fileEntries = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalPathString = file.getOriginalFilename();

            FileEntry fileEntry = fileEntryRepository.findByPath(file.getOriginalFilename())
                    .orElseGet(() -> {
                        Path originalPath = Path.of(originalPathString);
                        String extension = getExtension(originalPath);

                        return fileEntryRepository.save(FileEntry.builder()
                                .name(originalPath.getFileName().toString())
                                .path(originalPathString)
                                .size(file.getSize())
                                .extension(extension)
                                .build()
                        );
                    });
            fileEntries.add(fileEntry);
        }

        FileSet fileSet = fileSetRepository.save(
                FileSet.builder()
                        .name(name)
                        .description(description)
                        .recipientEmail(recipientEmail)
                        .status(FileSetStatus.ACTIVE)
                        .user(currentUser)
                        .files(fileEntries)
                        .build()
        );

        return fileSetRepository.save(fileSet);
    }

    /**
     * Creates a new {@link FileSet} for the currently authenticated user.
     * <p>
     * This method validates the list of selected file paths, ensures that at least one file is selected,
     * converts each path into a {@link FileEntry} (creating new entries if they do not already exist),
     * and associates them with the newly created FileSet. The FileSet is initialized with status {@link FileSetStatus#ACTIVE}.
     *
     * @param name           the name of the new FileSet
     * @param description    optional description for the FileSet
     * @param recipientEmail the email of the recipient who can access the FileSet
     * @param selectedPaths  list of absolute or relative file paths to include in the FileSet
     * @return the newly created {@link FileSet} with all associated {@link FileEntry} objects
     */

    @Transactional
    public FileSet createFileSet(String name,
                                 String description,
                                 String recipientEmail,
                                 List<String> selectedPaths
    ) {

        if (selectedPaths == null || selectedPaths.isEmpty()) {
            throw new NoFilesSelectedException("At least one file must be selected");
        }

        Long currentUserId = getCurrentUserId();// todo to check if I need to use AccesDeniedException

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User " + currentUserId + " not found"));

        FileSet fileSet = fileSetRepository.save(
                FileSet.builder()
                        .name(name)
                        .description(description)
                        .recipientEmail(recipientEmail)
                        .status(FileSetStatus.ACTIVE)
                        .user(currentUser)
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

    /**
     * Converts a given file system path into a {@link FileEntry} object.
     * <p>
     * Checks whether the path represents a directory or a regular file,
     * calculates file size if applicable, and extracts the file extension.
     *
     * @param path absolute or relative path to a file or directory
     * @return a {@link FileEntry} object representing the file or directory
     * @throws UncheckedIOException if reading file attributes fails
     */

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

    /**
     * Extracts the file extension from a {@link Path}.
     * <p>
     * If the file name does not contain a dot, an empty string is returned.
     *
     * @param path path of the file
     * @return file extension (without the dot) or empty string if none
     */

    protected static String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return (index > 0) ? fileName.substring(index + 1) : "";
    }

    /**
     * Returns the ID of the currently authenticated user.
     *
     * @return the current user ID
     * @throws UserNotFoundException if the user cannot be found
     */

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        return userRepository.findByName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userName))
                .getId();
    }

    /**
     * Changes the status of a {@link FileSet} if the current user has permissions.
     *
     * @param fileSetId        ID of the FileSet
     * @param newFileSetStatus new status to set
     * @return the updated {@link FileSet}
     * @throws AccessDeniedException    if the user cannot change the status
     * @throws FileSetNotFoundException if the FileSet cannot be found
     */

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

    /**
     * Updates the recipient email of a {@link FileSet} if the current user has permissions.
     *
     * @param fileSetId ID of the FileSet
     * @param newEmail  new recipient email
     * @return the updated {@link FileSet}
     * @throws AccessDeniedException    if the user cannot change the email
     * @throws FileSetNotFoundException if the FileSet cannot be found
     */

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

    /**
     * Updates the name of a {@link FileSet} if the current user has permissions.
     *
     * @param fileSetId ID of the FileSet
     * @param newName   new name to set
     * @return the updated {@link FileSet}
     * @throws AccessDeniedException    if the user cannot change the name
     * @throws FileSetNotFoundException if the FileSet cannot be found
     */

    public FileSet changeFileSetName(Long fileSetId, String newName) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        Long currentUserId = getCurrentUserId();

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to change the name.");
        }

        fileSet.setName(newName);
        fileSetRepository.save(fileSet);

        return fileSet;
    }

    /**
     * Updates the description of a {@link FileSet} if the current user has permissions.
     *
     * @param fileSetId      ID of the FileSet
     * @param newDescription new description
     * @return the updated {@link FileSet}
     * @throws AccessDeniedException    if the user cannot change the description
     * @throws FileSetNotFoundException if the FileSet cannot be found
     */

    public FileSet changeFileSetDescription(Long fileSetId, String newDescription) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        Long currentUserId = getCurrentUserId();

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to change the description.");
        }

        fileSet.setDescription(newDescription);
        fileSetRepository.save(fileSet);

        return fileSet;
    }
}
