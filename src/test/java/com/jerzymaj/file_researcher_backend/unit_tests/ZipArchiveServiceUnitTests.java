package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.exceptions.FileSetNotFoundException;
import com.jerzymaj.file_researcher_backend.models.FileEntry;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.suplementary_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ZipArchiveServiceUnitTests {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private FileSetRepository fileSetRepository;

    @Mock
    private FileSetService fileSetService;

    @Mock
    private ZipArchiveRepository zipArchiveRepository;

    @InjectMocks
    private ZipArchiveService zipArchiveService;

    private User user;
    private FileSet fileSet;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException {
        user = new User();
        user.setId(1L);
        user.setName("jerzy");


        FileEntry fe1 = new FileEntry();
        fe1.setPath(Files.createFile(tempDir.resolve("test1.txt")).toString());
        FileEntry fe2 = new FileEntry();
        fe2.setPath(Files.createFile(tempDir.resolve("test2.txt")).toString());
        FileEntry fe3 = new FileEntry();
        fe3.setPath(Files.createFile(tempDir.resolve("test3.csv")).toString());


        fileSet = new FileSet();
        fileSet.setId(1L);
        fileSet.setName("test");
        fileSet.setDescription("This is a test fileset description");
        fileSet.setCreationDate(LocalDateTime.now());
        fileSet.setUser(user);
        fileSet.setRecipientEmail("someone@mail.com");
        fileSet.setStatus(FileSetStatus.ACTIVE);
        fileSet.setFiles(List.of(fe1, fe2, fe3));

        lenient().when(fileSetRepository.findById(fileSet.getId())).thenReturn(Optional.of(fileSet));
        lenient().when(fileSetService.getCurrentUserId()).thenReturn(user.getId());

        MimeMessage dummyMessage = new MimeMessage((jakarta.mail.Session)null);
        lenient().when(mailSender.createMimeMessage()).thenReturn(dummyMessage);

        lenient().when(zipArchiveRepository.save(Mockito.any())).thenAnswer(invocation -> {
            ZipArchive za = invocation.getArgument(0);
            za.setSentHistoryList(List.of());
            return za;
        });
    }

    @Test
    public void shouldCreateAndSendZipArchive_IfSuccess() throws MessagingException, IOException {

        ZipArchiveDTO actualResult = zipArchiveService.createAndSendZipArchive(fileSet.getId(), fileSet.getRecipientEmail());

        assertNotNull(actualResult);
        assertEquals(fileSet.getId(), actualResult.getFileSetId());
        assertNotNull(actualResult.getArchivePath());
        assertFalse(actualResult.getArchivePath().isBlank());
        assertEquals("SUCCESS", actualResult.getStatus().name());

        verify(fileSetRepository, atLeastOnce()).findById(fileSet.getId());
        verify(fileSetService).getCurrentUserId();
        verify(mailSender).send((MimeMessage) any());
        verify(zipArchiveRepository, atLeastOnce()).save(any());
    }

    @Test
    public void shouldThrowException_WhenFileSetNotFound() {
        when(fileSetRepository.findById(789L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(
                RuntimeException.class,
                () -> zipArchiveService.createAndSendZipArchive(789L, "abc@de.com")
        );

        assertTrue(exception.getMessage().contains("FileSet not found"));
    }

    @Test
    public void shouldThrowException_WhenUserDoesNotHaveFileSet() {
        when(fileSetService.getCurrentUserId()).thenReturn(29L);

        Exception exception = assertThrows(
                AccessDeniedException.class,
                () -> zipArchiveService.createAndSendZipArchive(fileSet.getId(), "abc@de.com")
        );

        assertTrue(exception.getMessage().contains("You do not have permission to access this FileSet."));
    }

    @Test
    public void shouldThrowException_WhenFileSetIsEmpty() {
        fileSet.setFiles(List.of());
        when(fileSetRepository.findById(fileSet.getId())).thenReturn(Optional.of(fileSet));

        Exception ex = assertThrows(
                FileSetNotFoundException.class,
                () -> zipArchiveService.createAndSendZipArchive(fileSet.getId(), fileSet.getRecipientEmail())
        );

        assertTrue(ex.getMessage().contains("FileSet has no files to archive."));
    }
}
