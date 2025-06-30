package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.FileEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileEntryRepository extends JpaRepository<FileEntry, Long> {

    Optional<FileEntry> findByPath(String path);
}
