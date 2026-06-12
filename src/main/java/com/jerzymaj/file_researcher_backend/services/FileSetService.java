package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.NoFilesSelectedException;
import com.jerzymaj.file_researcher_backend.models.FileEntry;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileEntryRepository;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.security.AuthFacade;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileSetService {

    private final FileEntryRepository fileEntryRepository;
    private final FileSetRepository fileSetRepository;
    private final AuthFacade authFacade;

    @Transactional
    public FileSet createFileSetFromUploadedFiles(String name,
                                                  String description,
                                                  String recipientEmail,
                                                  MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new NoFilesSelectedException("Files haven't been uploaded");
        }

        User currentUser = authFacade.getCurrentUser();

        List<FileEntry> fileEntries = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalPathString = file.getOriginalFilename();

            if (originalPathString == null || originalPathString.isBlank()) {
                continue;
            }

            FileEntry fileEntry = getOrCreateFileEntry(file, originalPathString);

            fileEntries.add(fileEntry);
        }

        return fileSetRepository.save(FileSet.builder()
                .name(name)
                .description(description)
                .recipientEmail(recipientEmail)
                .status(FileSetStatus.ACTIVE)
                .user(currentUser)
                .files(fileEntries)
                .build());
    }

    public List<FileSet> getAllFileSets() {
        Long currentUserId = authFacade.getCurrentUserId();

        return fileSetRepository.findAllByUserId(currentUserId);
    }

    public FileSet getFileSetById(Long fileSetId) {
        return fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));
    }

    public void deleteFileSetById(Long fileSetId) throws AccessDeniedException {
        FileSet fileSet = getFileSetForCurrentUser(fileSetId);
        fileSetRepository.deleteById(fileSet.getId());
    }

    /**
     * Changes the status of a {@link FileSet} if the current user has permissions.
     *
     * @param fileSetId        ID of the FileSet
     * @param newFileSetStatus new status to set
     * @return the updated {@link FileSet}
     */

    public FileSet changeFileSetStatus(Long fileSetId, FileSetStatus newFileSetStatus) throws AccessDeniedException {
        FileSet fileSet = getFileSetForCurrentUser(fileSetId);
        fileSet.setStatus(newFileSetStatus);
        return fileSetRepository.save(fileSet);
    }

    /**
     * Updates the recipient email of a {@link FileSet} if the current user has permissions.
     *
     * @param fileSetId ID of the FileSet
     * @param newEmail  new recipient email
     * @return the updated {@link FileSet}
     */

    public FileSet changeRecipientEmail(Long fileSetId, String newEmail) throws AccessDeniedException {
        FileSet fileSet = getFileSetForCurrentUser(fileSetId);
        fileSet.setRecipientEmail(newEmail);
        return fileSetRepository.save(fileSet);
    }

    /**
     * Updates the name of a {@link FileSet} if the current user has permissions.
     *
     * @param fileSetId ID of the FileSet
     * @param newName   new name to set
     * @return the updated {@link FileSet}
     */

    public FileSet changeFileSetName(Long fileSetId, String newName) throws AccessDeniedException {
        FileSet fileSet = getFileSetForCurrentUser(fileSetId);
        fileSet.setName(newName);
        return fileSetRepository.save(fileSet);
    }

    /**
     * Updates the description of a {@link FileSet} if the current user has permissions.
     *
     * @param fileSetId      ID of the FileSet
     * @param newDescription new description
     * @return the updated {@link FileSet}
     */

    public FileSet changeFileSetDescription(Long fileSetId, String newDescription) throws AccessDeniedException {
        FileSet fileSet = getFileSetForCurrentUser(fileSetId);
        fileSet.setDescription(newDescription);
        return fileSetRepository.save(fileSet);
    }

    /**
     * Retrieves a {@link FileSet} by ID and validates that it belongs to the current user.
     *
     * @param fileSetId ID of the FileSet
     * @return the {@link FileSet} if found and owned by the current user
     * @throws AccessDeniedException    if the current user does not own the FileSet
     * @throws FileSetNotFoundException if the FileSet cannot be found
     */

    private FileSet getFileSetForCurrentUser(Long fileSetId) throws AccessDeniedException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        if (!fileSet.getUser().getId().equals(authFacade.getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to modify this FileSet.");
        }

        return fileSet;
    }

    /**
     * Extracts the file extension from a {@link Path}.
     * <p>
     * If the file name does not contain a dot, an empty string is returned.
     *
     * @param path path of the file
     * @return file extension (without the dot) or empty string if none
     */

    private static String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return (index > 0) ? fileName.substring(index + 1) : "";
    }

    /**
     * Retrieves an existing {@link FileEntry} by path or creates and saves a new one.
     *
     * @param file               the uploaded file
     * @param originalPathString the original file path as a string
     * @return existing or newly created {@link FileEntry}
     */

    private FileEntry getOrCreateFileEntry(MultipartFile file, String originalPathString) {
        return fileEntryRepository.findByPath(originalPathString)
                .orElseGet(() -> {
                    Path originalPath = Path.of(originalPathString);
                    return fileEntryRepository.save(FileEntry.builder()
                            .name(originalPath.getFileName().toString())
                            .path(originalPathString)
                            .size(file.getSize())
                            .extension(getExtension(originalPath))
                            .build()
                    );
                });
    }
}
