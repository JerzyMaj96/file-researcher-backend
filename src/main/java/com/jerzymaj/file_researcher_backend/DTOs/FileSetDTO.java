package com.jerzymaj.file_researcher_backend.DTOs;

import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSetDTO {

    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private Long userId;

    @NotBlank
    private String description;

    @Email
    @NotBlank
    private String recipientEmail;

    private FileSetStatus status;

    private LocalDateTime creationDate;

    private List<FileEntryDTO> files;

}
