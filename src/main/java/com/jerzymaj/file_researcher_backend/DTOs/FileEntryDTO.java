package com.jerzymaj.file_researcher_backend.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntryDTO {

    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String path;

    @Min(0)
    private Long size;

    @NotBlank
    @Size(max = 10)
    private String extension;

}
