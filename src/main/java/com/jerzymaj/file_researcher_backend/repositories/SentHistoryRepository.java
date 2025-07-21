package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.SentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentHistoryRepository extends JpaRepository<SentHistory, Long> {

    List<SentHistory> findAllByZipArchiveId(Long zipArchiveId);
}
