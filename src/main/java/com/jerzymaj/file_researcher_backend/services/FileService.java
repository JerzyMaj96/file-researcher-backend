package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.FileEntryDTO;
import com.jerzymaj.file_researcher_backend.DTOs.FileSetDTO;
import com.jerzymaj.file_researcher_backend.models.FileEntry;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.repositories.FileEntryRepository;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final FileEntryRepository fileEntryRepository;
    private final FileSetRepository fileSetRepository;

    public FileService(FileEntryRepository fileEntryRepository, FileSetRepository fileSetRepository) {
        this.fileEntryRepository = fileEntryRepository;
        this.fileSetRepository = fileSetRepository;
    }

    public FileEntry createFileEntry(FileEntry fileEntry){
        return fileEntryRepository.save(fileEntry);
    }

    public Optional<FileEntry> getFileEntryById(Long id){
        return fileEntryRepository.findById(id);
    }

    public FileSet createFileSet(FileSet fileSet){
        return fileSetRepository.save(fileSet);
    }

    public Optional<FileSet> getFileSetById(Long id){
        return fileSetRepository.findById(id);
    }

    public static FileEntryDTO convertFileEntryToDTO(FileEntry fileEntry){
        return new FileEntryDTO(fileEntry.getId(),
                                fileEntry.getName(),
                                fileEntry.getOriginalName(),
                                fileEntry.getPath(),
                                fileEntry.getSize(),
                                fileEntry.getExtension(),
                                fileEntry.getFileSet().getId());
    }

    public FileSetDTO convertFileSetToDTO(FileSet fileSet){
        return new FileSetDTO(fileSet.getId(),
                              fileSet.getName(),
                              fileSet.getUser().getId(),
                              fileSet.getDescription(),
                              fileSet.getRecipientEmail(),
                              fileSet.getStatus(),
                              fileSet.getCreationDate(),
                              fileSet.getFiles().stream()
                                                .map(FileService::convertFileEntryToDTO)
                                                .collect(Collectors.toList()));
    }
}
