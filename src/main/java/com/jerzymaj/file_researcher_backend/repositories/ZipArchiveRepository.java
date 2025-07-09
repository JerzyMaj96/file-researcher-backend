package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZipArchiveRepository extends JpaRepository<ZipArchive, Long> {

    List<ZipArchive> findAllByUserId(Long userId);
}
