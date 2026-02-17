package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.exceptions.ZipArchiveNotFoundException;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.models.enum_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipArchiveStatusService {

    private final ZipArchiveRepository zipArchiveRepository;
    private final FileSetRepository fileSetRepository;
    private final SentHistoryService sentHistoryService;

    @Transactional
    public void updateDatabaseAfterSuccess(Long archiveId, Long fileSetId) {
        ZipArchive zipArchive = zipArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ZipArchiveNotFoundException("Archive not found"));

        FileSet fileSet = fileSetRepository.findById(fileSetId)
                .orElseThrow(() -> new FileSetNotFoundException("FileSet not found"));


        zipArchive.setStatus(ZipArchiveStatus.SUCCESS);
        fileSet.setStatus(FileSetStatus.SENT);

        zipArchiveRepository.save(zipArchive);
        fileSetRepository.saveAndFlush(fileSet);
        log.info("Successfully updated database after Zip Archive successfully");
    }

    @Transactional
    public void updateDatabaseAfterFailure(Long archiveId, String errorMessage) {
        zipArchiveRepository.findById(archiveId).ifPresent(archive -> {
            archive.setStatus(ZipArchiveStatus.FAILED);
            zipArchiveRepository.saveAndFlush(archive);
            sentHistoryService.saveSentHistory(archive, archive.getRecipientEmail(), false, errorMessage);
        });
    }
}
