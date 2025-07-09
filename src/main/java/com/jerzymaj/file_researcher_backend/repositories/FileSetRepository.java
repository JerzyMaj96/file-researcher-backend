package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.FileSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileSetRepository extends JpaRepository<FileSet, Long> {

    List<FileSet> findAllByUserId(Long userId);

    Optional<FileSet> findByUserId(Long userId);
}
