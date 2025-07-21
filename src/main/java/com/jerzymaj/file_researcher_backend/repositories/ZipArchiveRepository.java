package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface ZipArchiveRepository extends JpaRepository<ZipArchive, Long> {

    List<ZipArchive> findAllByUserId(Long userId);

    @Query(value = """
            SELECT
               SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
               SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count
            FROM zip_archive
            WHERE user_id = :userId
           """, nativeQuery = true)
    Map<String, Object> countSuccessAndFailuresByUser(@Param("userId")Long userId);


    @Query(value = """
            SELECT * FROM zip_archive
            WHERE user_id = :userId AND size > :minSize
            """, nativeQuery = true)
    List<ZipArchive> findLargeZipArchives(@Param("userId") Long userId, @Param("minSize") Long minSize);
}
