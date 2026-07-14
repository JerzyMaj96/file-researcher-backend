package com.jerzymaj.file_researcher_backend.DTOs;

public record ZipStatsResponse(
        long successCount,
        long failureCount
) {
}
