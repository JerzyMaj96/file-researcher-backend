package com.jerzymaj.file_researcher_backend.DTOs;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(@NotBlank String path, @NotBlank String extension) {
}
