package com.jerzymaj.file_researcher_backend.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanPathResponseDTO {
    private String name;
    private String path;
    private boolean directory;
    private Long size;
    private List<ScanPathResponseDTO> children;
}

