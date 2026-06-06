package com.jerzymaj.file_researcher_backend.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public record CreateFileSetRequest(@NotBlank String name,
                                   @NotBlank String description,
                                   @Email @NotBlank String recipientEmail,
                                   MultipartFile[] files) {
}
