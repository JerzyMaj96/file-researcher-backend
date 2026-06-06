package com.jerzymaj.file_researcher_backend.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public record SendZipRequest(@NotBlank @Email String recipientEmail,
                             MultipartFile[] files) {
}
