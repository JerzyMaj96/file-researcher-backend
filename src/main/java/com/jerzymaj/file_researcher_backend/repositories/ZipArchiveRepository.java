package com.jerzymaj.file_researcher_backend.repositories;

import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface ZipArchiveRepository extends JpaRepository<ZipArchive, Long> {

    List<ZipArchive> findAllByUserId(Long userId);

    @Query("SELECT COALESCE(MAX(z.sendNumber),0) FROM ZipArchive z WHERE z.fileSet.id = :fileSetId")
    int findMaxSendNumberByFileSetId(@Param("fileSetId") Long fileSetId);

    @Query("SELECT new map(" +
            "SUM(CASE WHEN z.status = 'SUCCESS' THEN 1 ELSE 0 END) as successCount, " +
            "SUM(CASE WHEN z.status = 'FAILED' THEN 1 ELSE 0 END) as failureCount) " +
            "FROM ZipArchive z WHERE z.user.id = :userId")
    Map<String, Object> countSuccessAndFailuresByUser(@Param("userId") Long userId);

    @Query(value = """
            SELECT * FROM zip_archive
            WHERE user_id = :userId AND size > :minSize
            """, nativeQuery = true)
    List<ZipArchive> findLargeZipArchives(@Param("userId") Long userId, @Param("minSize") Long minSize);
}
