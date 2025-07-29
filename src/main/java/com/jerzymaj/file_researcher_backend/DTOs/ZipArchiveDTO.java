package com.jerzymaj.file_researcher_backend.DTOs;

import com.jerzymaj.file_researcher_backend.models.enum_classes.ZipArchiveStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZipArchiveDTO {

    private Long id;

    @NotBlank
    private String archiveName;

    @NotBlank
    private String archivePath;

    @Min(0)
    private Long size;

    private LocalDateTime creationDate;

    @NotNull
    private ZipArchiveStatus status;

    @Email
    @NotBlank
    private String recipientEmail;

    @NotNull
    private Long fileSetId;

    @NotNull
    private Long userId;

    private List<SentHistoryDTO> sentHistoryList;
}
