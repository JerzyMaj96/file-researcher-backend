package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.models.enum_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ZipArchiveService {


    private final JavaMailSender mailSender;
    private final FileSetRepository fileSetRepository;
    private final FileSetService fileSetService;
    private final ZipArchiveRepository zipArchiveRepository;
    private final SentHistoryService sentHistoryService;

    /**
     * Creates a ZIP archive from the files in the specified {@link FileSet} and sends it to the given email address.
     * <p>
     * The method performs the following steps:
     * <ol>
     *     <li>Verifies that the current user has access to the specified FileSet.</li>
     *     <li>Generates a ZIP file containing all files from the FileSet.</li>
     *     <li>Sends the ZIP file as an email attachment to the provided recipient.</li>
     *     <li>Updates the {@link ZipArchive} status and {@link FileSet} status in the database and records the sending history.</li>
     * </ol>
     * </p>
     *
     * @param fileSetId      the ID of the FileSet whose files will be archived; must exist and belong to the current user
     * @param recipientEmail the recipient's email address; must be a valid email
     * @return a {@link ZipArchive} object representing the saved archive, including the final send status
     * @throws IOException              if an I/O error occurs during ZIP creation or file deletion
     * @throws MessagingException       if an error occurs while sending the email
     * @throws AccessDeniedException    if the current user does not have permission to access the FileSet
     * @throws FileSetNotFoundException if the specified FileSet does not exist or contains no files
     */

    public ZipArchive createAndSendZipArchive(Long fileSetId,
                                              String recipientEmail) throws IOException, MessagingException {
        Long currentUserId = fileSetService.getCurrentUserId();

        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        if (fileSet.getFiles() == null || fileSet.getFiles().isEmpty()) {
            throw new FileSetNotFoundException("FileSet has no files to archive.");
        }

        int sendCounter = zipArchiveRepository.findMaxSendNumberByFileSetId(fileSetId) + 1;

        ZipFileResult zipFileResult = createZipFromFileSetParallel(fileSetId, sendCounter);

        ZipArchive zipArchive = zipArchiveRepository.save(ZipArchive.builder()
                .archiveName(zipFileResult.fileName())
                .archivePath(zipFileResult.filePath().toAbsolutePath().toString())
                .size(zipFileResult.size())
                .status(ZipArchiveStatus.PENDING)
                .recipientEmail(recipientEmail)
                .fileSet(fileSet)
                .user(fileSet.getUser())
                .creationDate(LocalDateTime.now())
                .sendNumber(sendCounter)
                .build()
        );

        try {
            sendZipArchiveByEmail(
                    recipientEmail,
                    zipFileResult.filePath(),
                    "Files",
                    "Please find attached the ZIP archive of requested files."
            );

            zipArchive.setStatus(ZipArchiveStatus.SUCCESS);
            fileSet.setStatus(FileSetStatus.SENT);
            sentHistoryService.saveSentHistory(zipArchive, recipientEmail, true, null);

        } catch (MessagingException exception) {
            zipArchive.setStatus(ZipArchiveStatus.FAILED);
            sentHistoryService.saveSentHistory(zipArchive, recipientEmail, false, exception.getMessage());
            throw exception;
        } finally {
            Files.deleteIfExists(zipFileResult.filePath());
        }

//        zipArchiveRepository.save(zipArchive);

        return zipArchive;
    }

    //    POSSIBLY USELESS
    public void resendExistingZip(Long fileSetId, Long zipArchiveId, String recipientEmail) throws AccessDeniedException, MessagingException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)
                || !zipArchive.getFileSet().getId().equals(fileSetId)) {
            throw new AccessDeniedException("You do not have permission to resend this archive.");
        }

        Path zipPath = Path.of(zipArchive.getArchivePath());

        try {
            sendZipArchiveByEmail(
                    recipientEmail,
                    zipPath,
                    "Files",
                    "Please find attached the ZIP archive of your selected files."
            );

            zipArchive.setStatus(ZipArchiveStatus.SUCCESS);
            sentHistoryService.saveSentHistory(zipArchive, recipientEmail, true, null);

        } catch (MessagingException exception) {
            zipArchive.setStatus(ZipArchiveStatus.FAILED);
            zipArchiveRepository.save(zipArchive);
            sentHistoryService.saveSentHistory(zipArchive, recipientEmail, false, exception.getMessage());
            throw exception;
        }

        zipArchiveRepository.save(zipArchive);
    }

    public List<ZipArchive> getAllZipArchives() {
        Long currentUserId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.findAllByUserId(currentUserId);
    }

    public List<ZipArchive> getAllZipArchivesForFileSet(Long fileSetId) throws AccessDeniedException {
        Long currentUserId = fileSetService.getCurrentUserId();

        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        return zipArchiveRepository.findAllByUserId(currentUserId);
    }

    public ZipArchive getZipArchiveById(Long fileSetId, Long zipArchiveId) throws AccessDeniedException {
        Long currentUserId = fileSetService.getCurrentUserId();

        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        if (!zipArchive.getUser().getId().equals(currentUserId)
                || !zipArchive.getFileSet().getId().equals(fileSetId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        return zipArchive;
    }

    public void deleteZipArchive(Long fileSetId, Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)
                || !zipArchive.getFileSet().getId().equals(fileSetId)) {
            throw new AccessDeniedException("You do not have permission to delete this FileSet.");
        }

        zipArchiveRepository.deleteById(zipArchiveId);
    }

    /**
     * Retrieves statistics about ZIP archive sending results for the currently authenticated user.
     * <p>
     * The statistics include the total number of successfully sent archives and the number of failed send attempts.
     * This information can be used to display user performance metrics or diagnostic data in the application dashboard.
     * </p>
     *
     * @return a {@link Map} containing key-value pairs with statistical data,
     * typically including counts of successful and failed ZIP sends
     */

    public Map<String, Object> getZipStatsForCurrentUser() {
        Long userId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.countSuccessAndFailuresByUser(userId);
    }

    /**
     * Retrieves all ZIP archives created by the current user that exceed a specified size threshold.
     * <p>
     * This method can be used to identify unusually large ZIP files for monitoring storage usage
     * or performing cleanup operations.
     * </p>
     *
     * @param minSize the minimum file size (in bytes) used as a filter;
     *                only ZIP files larger than this value will be returned
     * @return a {@link List} of {@link ZipArchive} objects that meet the size criteria
     */

    public List<ZipArchive> getLargeZipFiles(Long minSize) {
        Long userId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.findLargeZipArchives(userId, minSize);
    }

    private record ZipFileResult(Path filePath, String fileName, long size) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFileSetAsSent(FileSet fileSet) {

        fileSetRepository.save(fileSet);
    }

    /**
     * Creates a ZIP archive containing all files from the specified {@link FileSet}.
     * <p>
     * This method reads all file entries belonging to the FileSet and writes them
     * into a single compressed ZIP file located in the system temporary directory.
     * </p>
     *
     * @param fileSetId   the ID of the FileSet whose files will be archived
     * @param sendCounter the sequential number used to differentiate multiple sends of the same FileSet
     * @return a {@link ZipFileResult} containing information about the generated ZIP file,
     * such as its path, name, and size
     * @throws IOException if an I/O error occurs during file copying or ZIP creation
     */

    private ZipFileResult createZipFromFileSet(Long fileSetId, int sendCounter) throws IOException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        String zipFileName = "fileset-" + fileSetId + "-" + sendCounter + ".zip";
        Path zipFilePath = Path.of(System.getProperty("java.io.tmpdir"), zipFileName);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            for (FileEntry fileEntry : fileSet.getFiles()) {
                Path filePath = Path.of(fileEntry.getPath());
                if (Files.exists(filePath)) {
                    addToZipFile(filePath, zos);
                }
            }
        }
        long size = Files.size(zipFilePath);
        return new ZipFileResult(zipFilePath, zipFileName, size);
    }

    /**
     * Creates a ZIP archive from a given {@link FileSet} using a multithreaded approach.
     * <p>
     * This method copies all files from the specified FileSet into a temporary directory
     * using an {@link ExecutorService} with a fixed thread pool. Once all files are copied,
     * the temporary directory is compressed into a ZIP file.
     * </p>
     *
     * @param fileSetId   the ID of the FileSet whose files will be archived
     * @param sendCounter the sequential number used to differentiate multiple sends of the same FileSet
     * @return a {@link ZipFileResult} containing information about the generated ZIP file,
     * such as its path, name, and size
     * @throws IOException if an I/O error occurs during file copying or ZIP creation
     */

    //ALTERNATIVE METHOD - MULTITHREAD
    private ZipFileResult createZipFromFileSetParallel(Long fileSetId, int sendCounter) throws IOException {
        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        String zipFileName = "fileset-" + fileSetId + "-" + sendCounter + ".zip";
        Path zipFilePath = Path.of(System.getProperty("java.io.tmpdir"), zipFileName);
        Path tempDir = Files.createTempDirectory("zip_parallel");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (FileEntry fileEntry : fileSet.getFiles()) {
            Path source = Path.of(fileEntry.getPath());
            Path destination = tempDir.resolve(source.getFileName());

            executor.submit(() -> {
                try {
                    Files.copy(source, destination);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
        }

        executor.shutdown();

        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        createZipFromDirectory(tempDir, zipFilePath);

        deleteDirectory(tempDir);

        long size = Files.size(zipFilePath);
        return new ZipFileResult(zipFilePath, zipFileName, size);
    }

    /**
     * Compresses all files from the specified source directory into a single ZIP file.
     *
     * @param sourceDir   the directory containing files to be compressed
     * @param zipFilePath the path where the resulting ZIP file will be created
     * @throws IOException if an error occurs during file reading or ZIP creation
     */

    private void createZipFromDirectory(Path sourceDir, Path zipFilePath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            ZipEntry entry = new ZipEntry(path.getFileName().toString());
                            zos.putNextEntry(entry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    });
        }
    }

    /**
     * Recursively deletes the specified directory and all its contents.
     * <p>
     * The files and subdirectories are sorted in reverse order to ensure that
     * nested files are deleted before their parent directories.
     * </p>
     *
     * @param dir the directory to delete
     * @throws IOException if an error occurs during file deletion
     */

    private void deleteDirectory(Path dir) throws IOException {
        Files.walk(dir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });
    }

    /**
     * Adds a single file to an open {@link ZipOutputStream}.
     *
     * @param filePath the path to the file to be added
     * @param zos      the active ZIP output stream
     * @throws IOException if an error occurs while reading the file or writing to the ZIP stream
     */

    private void addToZipFile(Path filePath, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(filePath.getFileName().toString());
        zos.putNextEntry(zipEntry);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }

    /**
     * Sends a ZIP archive as an email attachment to the specified recipient.
     *
     * @param recipientEmail the recipient's email address
     * @param zipFilePath    the path to the ZIP file to be sent
     * @param subject        the subject of the email
     * @param text           the body text of the email
     * @throws MessagingException if an error occurs while sending the email
     */

    private void sendZipArchiveByEmail(String recipientEmail,
                                       Path zipFilePath,
                                       String subject,
                                       String text) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(text);
        helper.addAttachment(zipFilePath.getFileName().toString(),
                new FileSystemResource(zipFilePath.toFile()));

        mailSender.send(message);
    }
}
