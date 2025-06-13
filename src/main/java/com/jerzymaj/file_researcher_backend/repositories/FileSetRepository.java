package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.FileSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileSetRepository extends JpaRepository<FileSet, Long> {
}
