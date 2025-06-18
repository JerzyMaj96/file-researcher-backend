package com.jerzymaj.file_researcher_backend.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFileSetDTO {

    @NotBlank
    private String name;

    private String description;

    @Email
    @NotBlank
    private String recipientEmail;

    @NotEmpty
    private List<String> selectedPaths;

    @NotNull
    private Long userId;
}
