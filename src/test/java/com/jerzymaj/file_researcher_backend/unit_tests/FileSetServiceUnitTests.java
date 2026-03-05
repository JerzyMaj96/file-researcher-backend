package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.UserDTO;
import com.jerzymaj.file_researcher_backend.exceptions.NoFilesSelectedException;
import com.jerzymaj.file_researcher_backend.exceptions.UserNotFoundException;
import com.jerzymaj.file_researcher_backend.models.FileEntry;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.repositories.FileEntryRepository;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    MockMultipartFile file1;
    MockMultipartFile file2;
    MockMultipartFile[]  files;

    @BeforeEach
    public void setUp() throws IOException {
        name = "test";
        description = "This is a test fileset description";
        recipientEmail = "someone@mail.com";

        file1 = new MockMultipartFile("files", "test1.txt",
                "text/plain", "content1".getBytes());
        file2 = new MockMultipartFile("files", "directory/test2.pdf",
                "text/plain", "content2".getBytes());
        files = new MockMultipartFile[]{file1, file2};

        ownerDTO = new UserDTO(1L, "jerzy", "jerzy@mail.com", LocalDateTime.now());

        User user = new User();
        user.setId(ownerDTO.getId());
        user.setName(ownerDTO.getName());
        user.setEmail(ownerDTO.getEmail());

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getName());

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByName(user.getName())).thenReturn(Optional.of(user));
        when(userRepository.findById(ownerDTO.getId())).thenReturn(Optional.of(user));
        lenient().when(fileEntryRepository.findByPath(anyString())).thenReturn(Optional.empty());
        lenient().when(fileEntryRepository.save(any(FileEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(fileSetRepository.save(any(FileSet.class))).thenAnswer(invocation -> {
            FileSet fileSet = invocation.getArgument(0);
            fileSet.setId(1L);
            return fileSet;
        });
    }

    @Test
    public void shouldCreateFileSet_IfSuccess() throws IOException {
        FileSet actualResult = fileSetService
                .createFileSetFromUploadedFiles(name, description, recipientEmail, files);

        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getId()).isEqualTo(1L);
        assertThat(actualResult.getName()).isEqualTo(name);
        assertThat(actualResult.getDescription()).isEqualTo(description);
        assertThat(actualResult.getRecipientEmail()).isEqualTo(recipientEmail);
    }

    @Test
    public void shouldThrowNoFilesSelectedException_WhenNoFilesSelected() {
        assertThatThrownBy(() ->
                fileSetService.createFileSetFromUploadedFiles(name, description, recipientEmail, new MockMultipartFile[0])
        ).isInstanceOf(NoFilesSelectedException.class);
    }

    @Test
    public void shouldThrowUserNotFoundException_IfFailed() {
        when(userRepository.findByName(ownerDTO.getName())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileSetService.createFileSetFromUploadedFiles(name, description, recipientEmail, files))
                .isInstanceOf(UserNotFoundException.class);
    }
}
