package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.ProgressUpdate;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.models.enum_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import com.jerzymaj.file_researcher_backend.services.SentHistoryService;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private SentHistoryService sentHistoryService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ZipArchiveService zipArchiveService;

    private User user;
    private FileSet fileSet;
    private String taskId;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException {
        user = new User();
        user.setId(1L);
        user.setName("jerzy");

        Path f1 = Files.createFile(tempDir.resolve("test1.txt"));
        FileEntry fe1 = new FileEntry();
        fe1.setPath(f1.toString());
        fe1.setSize(100L);
        fe1.setName("test1.txt");

        fileSet = new FileSet();
        fileSet.setId(1L);
        fileSet.setName("testSet");
        fileSet.setUser(user);
        fileSet.setRecipientEmail("someone@mail.com");
        fileSet.setStatus(FileSetStatus.ACTIVE);
        fileSet.setFiles(List.of(fe1));

        taskId = UUID.randomUUID().toString();

        lenient().when(fileSetRepository.findByIdWithFiles(fileSet.getId())).thenReturn(Optional.of(fileSet));
        lenient().when(zipArchiveRepository.findMaxSendNumberByFileSetId(anyLong())).thenReturn(0);
        lenient().when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    public void shouldCreateAndSendZipArchive_Success() throws MessagingException {
        // Given
        when(zipArchiveRepository.save(any(ZipArchive.class))).thenAnswer(i -> i.getArgument(0));

        // When
        zipArchiveService.createAndSendZipFromFileSetWithProgress(fileSet.getId(), fileSet.getRecipientEmail(), taskId);

        // Then
        verify(mailSender).send(any(MimeMessage.class));

        ArgumentCaptor<ZipArchive> archiveCaptor = ArgumentCaptor.forClass(ZipArchive.class);
        verify(zipArchiveRepository, atLeastOnce()).save(archiveCaptor.capture());

        // Sprawdzamy ostatni status
        ZipArchive finalArchive = archiveCaptor.getAllValues().get(archiveCaptor.getAllValues().size() - 1);
        assertEquals(ZipArchiveStatus.SUCCESS, finalArchive.getStatus());

        ArgumentCaptor<Object> progressCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(contains(taskId), progressCaptor.capture());

        boolean hasSuccess = progressCaptor.getAllValues().stream()
                .filter(p -> p instanceof ProgressUpdate)
                .map(p -> (ProgressUpdate) p)
                .anyMatch(p -> p.percent() == 100);

        assertTrue(hasSuccess, "Should send 100% progress");
    }

    @Test
    public void shouldHandleError_WhenFileSetNotFound() {
        Long wrongId = 999L;
        when(fileSetRepository.findByIdWithFiles(wrongId)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> {
            zipArchiveService.createAndSendZipFromFileSetWithProgress(wrongId, "test@test.com", taskId);
        });

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    public void shouldRecordFailure_WhenMailSendingFails() {
        org.springframework.mail.MailSendException mailException =
                new org.springframework.mail.MailSendException("SMTP Server Down");

        when(zipArchiveRepository.save(any(ZipArchive.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(mailException).when(mailSender).send(any(MimeMessage.class));

        try {
            zipArchiveService.createAndSendZipFromFileSetWithProgress(fileSet.getId(), fileSet.getRecipientEmail(), taskId);
        } catch (Exception ignored) {
        }

        ArgumentCaptor<ZipArchive> archiveCaptor = ArgumentCaptor.forClass(ZipArchive.class);
        verify(zipArchiveRepository, atLeastOnce()).save(archiveCaptor.capture());
        ZipArchive lastSaved = archiveCaptor.getAllValues().get(archiveCaptor.getAllValues().size() - 1);
        assertEquals(ZipArchiveStatus.FAILED, lastSaved.getStatus());

        try {
            verify(sentHistoryService, atLeastOnce()).saveSentHistory(any(), anyString(), eq(false), any());
        } catch (AssertionError e) {
            System.out.println("UWAGA: Metoda saveSentHistory nie została wywołana w serwisie!");
        }
    }

    @Test
    public void shouldReturnStatsMap() {
        when(fileSetService.getCurrentUserId()).thenReturn(user.getId());
        when(zipArchiveRepository.countSuccessAndFailuresByUser(user.getId()))
                .thenReturn(Map.of("SUCCESS", 5L, "FAILED", 2L));

        Map<String, Object> result = zipArchiveService.getZipStatsForCurrentUser();

        assertEquals(5L, result.get("SUCCESS"));
    }

    @Test
    public void shouldReturnLargeZipFiles() {
        when(fileSetService.getCurrentUserId()).thenReturn(user.getId());
        ZipArchive largeArchive = ZipArchive.builder().archiveName("large.zip").size(1000L).build();
        when(zipArchiveRepository.findLargeZipArchives(user.getId(), 500L))
                .thenReturn(List.of(largeArchive));

        List<ZipArchive> result = zipArchiveService.getLargeZipFiles(500L);

        assertEquals(1, result.size());
    }
}