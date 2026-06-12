package com.jerzymaj.file_researcher_backend.DTOs;

import org.springframework.web.multipart.MultipartFile;

public record ScanRequest(MultipartFile[] files,
                          String extension) {
}
