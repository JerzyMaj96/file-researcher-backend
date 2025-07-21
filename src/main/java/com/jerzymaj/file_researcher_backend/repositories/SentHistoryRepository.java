package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.SentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SentHistoryRepository extends JpaRepository<SentHistory, Long> {

    @Query(value = """
            SELECT * FROM sent_history
            WHERE zip_archive_id = :zipArchiveId
            ORDER BY send_attempt_date DESC
            """, nativeQuery = true)
    List<SentHistory> findAllByZipArchiveIdSorted(Long zipArchiveId);

    @Query(value = """
            SELECT sent_to_email
            FROM sent_history
            WHERE zip_archive_id = :ziparchiveId
            ORDER BY sent_attempt_date DESC
            LIMIT 1
            """,nativeQuery = true)
    String findLastRecipient(@Param("zipArchiveId") Long zipArchiveId);


}
