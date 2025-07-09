package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.SentHistoryDTO;
import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.UserNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.SentHistory;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.suplementary_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ZipArchiveService {
//PROPERTIES----------------------------------------------------------------------

    private final UserRepository userRepository;
    private final FileSetRepository fileSetRepository;
    private final FileSetService fileSetService;
    private final ZipArchiveRepository zipArchiveRepository;

//MAIN METHODS---------------------------------------------------------------------------

    public ZipArchiveDTO createZipArchive(String archiveName,
                                   String archivePath,
                                   Long size,
                                   String recipientEmail
                                   ){

        Long currentUserId = fileSetService.getCurrentUserId();

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User " + currentUserId + " not found"));


        FileSet fileSet = fileSetRepository.findByUserId(owner.getId())
               .orElseThrow(() -> new FileSetNotFoundException("FileSet not found"));

        ZipArchive zipArchive = zipArchiveRepository.save(
                ZipArchive.builder()
                        .archiveName(archiveName)
                        .archivePath(archivePath)
                        .size(size)
                        .status(ZipArchiveStatus.SUCCESS)
                        .recipientEmail(recipientEmail)
                        .fileSet(fileSet)
                        .user(owner)
                        .build());


        return convertZipArchiveToDTO(zipArchive);
    }

    public List<ZipArchiveDTO> getAllZipArchives(){
        Long currentUserId = fileSetService.getCurrentUserId();

        return zipArchiveRepository.findAllByUserId(currentUserId).stream()
                .map(this::convertZipArchiveToDTO)
                .toList();
    }

    public ZipArchiveDTO getZipArchiveById(Long zipArchiveId) throws AccessDeniedException {
        Long currentUserId = fileSetService.getCurrentUserId();

        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        if (!zipArchive.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this FileSet.");
        }

        return convertZipArchiveToDTO(zipArchive);
    }

    public void deleteZipArchive(Long zipArchiveId) throws AccessDeniedException {
        ZipArchive zipArchive = zipArchiveRepository.findById(zipArchiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("ZipArchive not found: " + zipArchiveId));

        Long currentUserId = fileSetService.getCurrentUserId();

        if (!zipArchive.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this FileSet.");
        }

        zipArchiveRepository.deleteById(zipArchiveId);
    }

//SUPPLEMENTARY METHODS--------------------------------------------------------------

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
