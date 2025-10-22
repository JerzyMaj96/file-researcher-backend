package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.SentHistoryDTO;
import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import com.jerzymaj.file_researcher_backend.tranlator.Translator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

        ZipFileResult zipFileResult = createZipFromFileSet(fileSetId, sendCounter);

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
            sentHistoryService.saveSentHistory(zipArchive, recipientEmail, true, null);

        } catch (MessagingException exception) {
            zipArchive.setStatus(ZipArchiveStatus.FAILED);
            zipArchiveRepository.save(zipArchive);
            sentHistoryService.saveSentHistory(zipArchive, recipientEmail, false, exception.getMessage());
            throw exception;
        } finally {
            Files.deleteIfExists(zipFileResult.filePath());
        }

        zipArchiveRepository.save(zipArchive);
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

    public Map<String, Object> getZipStatsForCurrentUser() {
        Long userId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.countSuccessAndFailuresByUser(userId);
    }

    public List<ZipArchive> getLargeZipFiles(Long minSize) {
        Long userId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.findLargeZipArchives(userId, minSize);
    }


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

    private record ZipFileResult(Path filePath, String fileName, long size) {
    }

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

    private void deleteDirectory(Path dir) throws IOException{
        Files.walk(dir)
                .sorted((a,b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });
    }

    private void addToZipFile(Path filePath, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(filePath.getFileName().toString());
        zos.putNextEntry(zipEntry);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }

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
