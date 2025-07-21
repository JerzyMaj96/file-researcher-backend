package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.SentHistoryDTO;
import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.suplementary_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.SentHistoryRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ZipArchiveService {
//PROPERTIES----------------------------------------------------------------------

    private final JavaMailSender mailSender;
    private final FileSetRepository fileSetRepository;
    private final FileSetService fileSetService;
    private final ZipArchiveRepository zipArchiveRepository;
    private final SentHistoryService sentHistoryService;

//MAIN METHODS---------------------------------------------------------------------------

    public ZipArchiveDTO createAndSendZipArchive(Long fileSetId, String recipientEmail) throws IOException, MessagingException {
        Long currentUserId = fileSetService.getCurrentUserId();

        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        if (fileSet.getFiles() == null || fileSet.getFiles().isEmpty()) {
            throw new FileSetNotFoundException("FileSet has no files to archive.");
        }

        Path zipFilePath;
        String archiveName;
        Long size;

        try {
            zipFilePath = createZipFromFileSet(fileSetId);
            archiveName = zipFilePath.getFileName().toString();
            size = Files.size(zipFilePath);
        } catch (IOException e) {
            throw new IOException("Failed to create ZIP archive.", e);
        }

        ZipArchive zipArchive = ZipArchive.builder()
                .archiveName(archiveName)
                .archivePath(zipFilePath.toAbsolutePath().toString())
                .size(size)
                .status(ZipArchiveStatus.PENDING)
                .recipientEmail(recipientEmail)
                .fileSet(fileSet)
                .user(fileSet.getUser())
                .creationDate(LocalDateTime.now())
                .build();

        zipArchive = zipArchiveRepository.save(zipArchive);

        try {
            sendZipArchiveByEmail(
                    recipientEmail,
                    zipFilePath,
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
        return convertZipArchiveToDTO(zipArchive);
    }

    public void resendExistingZip(Long fileSetId, Long zipArchiveId, String recipientEmail) throws AccessDeniedException, MessagingException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if(!zipArchive.getUser().getId().equals(currentUserId)
           || !zipArchive.getFileSet().getId().equals(fileSetId)) {
            throw new AccessDeniedException("You do not have permission to resend this archive.");
        }

        Path zipPath = Path.of(zipArchive.getArchiveName());

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


    public List<ZipArchiveDTO> getAllZipArchives(Long fileSetId) throws AccessDeniedException {
        Long currentUserId = fileSetService.getCurrentUserId();

        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

        if (!fileSet.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        return zipArchiveRepository.findAllByUserId(currentUserId).stream()
                .map(this::convertZipArchiveToDTO)
                .toList();
    }

    public ZipArchiveDTO getZipArchiveById(Long fileSetId, Long zipArchiveId) throws AccessDeniedException {
        Long currentUserId = fileSetService.getCurrentUserId();

        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        if (!zipArchive.getUser().getId().equals(currentUserId)
              || !zipArchive.getFileSet().getId().equals(fileSetId)) {
            throw new AccessDeniedException("You do not have permission to access this FileSet.");
        }

        return convertZipArchiveToDTO(zipArchive);
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

    public List<ZipArchiveDTO> getLargeZipFiles(Long minSize) {
        Long userId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.findLargeZipArchives(userId, minSize).stream()
                .map(this::convertZipArchiveToDTO)
                .toList();
    }



//SUPPLEMENTARY METHODS--------------------------------------------------------------
private Path createZipFromFileSet(Long fileSetId) throws IOException {
    FileSet fileSet = fileSetRepository.findById(fileSetId)
            .orElseThrow(() -> new FileSetNotFoundException("FileSet not found: " + fileSetId));

    String zipFileName = "fileset-" + fileSetId + ".zip";
    Path zipFilePath = Path.of(System.getProperty("java.io.tmpdir"), zipFileName);

    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
        for (FileEntry fileEntry : fileSet.getFiles()) {
            Path filePath = Path.of(fileEntry.getPath());
            if (Files.exists(filePath)) {
                addToZipFile(filePath, zos);
            }
        }
    }
    return zipFilePath;
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
//MAPPERS
    private ZipArchiveDTO convertZipArchiveToDTO(ZipArchive zipArchive){

        return ZipArchiveDTO.builder()
                .id(zipArchive.getId())
                .archiveName(zipArchive.getArchiveName())
                .archivePath(zipArchive.getArchivePath())
                .size(zipArchive.getSize())
                .creationDate(zipArchive.getCreationDate())
                .status(zipArchive.getStatus())
                .recipientEmail(zipArchive.getRecipientEmail())
                .fileSetId(zipArchive.getFileSet().getId())
                .userId(zipArchive.getUser().getId())
                .sentHistoryList(zipArchive.getSentHistoryList().stream()
                        .map(this::convertSentHistoryToDTO)
                        .toList())
                .build();
    }

    private SentHistoryDTO convertSentHistoryToDTO(SentHistory sentHistory) {
        return SentHistoryDTO.builder()
                .id(sentHistory.getId())
                .zipArchiveId(sentHistory.getZipArchive().getId())
                .sentAttemptDate(sentHistory.getSendAttemptDate())
                .status(sentHistory.getStatus())
                .errorMessage(sentHistory.getErrorMessage())
                .sentToEmail(sentHistory.getSentToEmail())
                .build();
    }
}
