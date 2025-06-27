package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.FileSetDTO;
import com.jerzymaj.file_researcher_backend.DTOs.UserDTO;
import com.jerzymaj.file_researcher_backend.exceptions.NoFilesSelectedException;
import com.jerzymaj.file_researcher_backend.exceptions.UserNotFoundException;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.repositories.FileEntryRepository;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileSetServiceUnitTests {

    @Mock
    private FileEntryRepository fileEntryRepository;

    @Mock
    private FileSetRepository fileSetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FileSetService fileSetService;

    private String name;
    private String description;
    private String recipientEmail;
    private UserDTO ownerDTO;
    private List<String> selectedPaths;

    private User user;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException {
        name = "test";
        description = "This is a test fileset description";
        recipientEmail = "someone@mail.com";

        Path tempFile1 = Files.createFile(tempDir.resolve("test1.txt"));
        Path tempFile2 = Files.createFile(tempDir.resolve("test2.txt"));
        Path tempFile3 = Files.createFile(tempDir.resolve("test3.csv"));

        selectedPaths = List.of(
                tempFile1.toString(),
                tempFile2.toString(),
                tempFile3.toString());

        ownerDTO = new UserDTO(1L,"jerzy","jerzy@mail.com", LocalDateTime.now());

        user = new User();
        user.setId(ownerDTO.getId());
        user.setName(ownerDTO.getName());
        user.setEmail(ownerDTO.getEmail());
    }

    @Test
    public void shouldCreateFileSet_IfSuccess() throws IOException {
        when(userRepository.findById(ownerDTO.getId())).thenReturn(Optional.of(user));
        when(fileSetRepository.save(any(FileSet.class))).thenAnswer(invocationOnMock -> {
            FileSet fileSet = invocationOnMock.getArgument(0);
            fileSet.setId(1L);
            return fileSet;
        });

        FileSetDTO actualResult = fileSetService
                .createFileSet(name,description,recipientEmail,selectedPaths,ownerDTO.getId());

        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getId()).isEqualTo(1L);
        assertThat(actualResult.getName()).isEqualTo(name);
        assertThat(actualResult.getDescription()).isEqualTo(description);
        assertThat(actualResult.getRecipientEmail()).isEqualTo(recipientEmail);
    }

    @Test
    public void shouldThrowNoFilesSelectedException_WhenNoFilesSelected() {
        assertThatThrownBy(() ->
                fileSetService.createFileSet(name, description, recipientEmail, List.of(), ownerDTO.getId())
        ).isInstanceOf(NoFilesSelectedException.class);
    }


    @Test
    public void shouldThrowUserNotFoundException_IfFailed() {
        when(userRepository.findById(ownerDTO.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileSetService.createFileSet(name,description,recipientEmail,selectedPaths,ownerDTO.getId()))
                .isInstanceOf(UserNotFoundException.class);
    }
}
