package com.jerzymaj.file_researcher_backend.DTOs;

import com.jerzymaj.file_researcher_backend.models.suplementary_classes.SendStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentHistoryDTO {

    private Long id;

    @NotNull
    private Long zipArchiveId;

    private LocalDateTime sentAttemptDate;

    @NotNull
    private SendStatus status;

    private String errorMessage;

    @Email
    @NotBlank
    private String sentToEmail;
}
