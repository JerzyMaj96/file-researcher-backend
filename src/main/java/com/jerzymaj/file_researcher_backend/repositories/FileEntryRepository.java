package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.FileEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileEntryRepository extends JpaRepository<FileEntry, Long> {
}
