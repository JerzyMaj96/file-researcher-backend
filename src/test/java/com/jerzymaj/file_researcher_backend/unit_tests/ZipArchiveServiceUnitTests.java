package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.ProgressUpdate;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import com.jerzymaj.file_researcher_backend.services.SentHistoryService;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveStatusService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;

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

    @Mock
    private ZipArchiveStatusService zipArchiveStatusService;

    @InjectMocks
    private ZipArchiveService zipArchiveService;

    private User user;
    private FileSet fileSet;

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

        zipArchiveService = new ZipArchiveService(
                mailSender,
                fileSetRepository,
                fileSetService,
                zipArchiveRepository,
                zipArchiveStatusService,
                sentHistoryService,
                messagingTemplate
        );

        lenient().when(fileSetRepository.findByIdWithFiles(fileSet.getId())).thenReturn(Optional.of(fileSet));
        lenient().when(zipArchiveRepository.findMaxSendNumberByFileSetId(anyLong())).thenReturn(0);
        lenient().when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        lenient().doNothing().when(zipArchiveStatusService).updateDatabaseAfterSuccess(anyLong(), anyLong());
        lenient().doNothing().when(zipArchiveStatusService).updateDatabaseAfterFailure(anyLong(), anyString());
    }

    @Test
    public void shouldStartZipUploadProcess_AndStageFilesCorrectly() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("files", "test1.txt", "text/plain", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.txt", "text/plain", "content2".getBytes());
        MockMultipartFile[] files = {file1, file2};

        when(zipArchiveRepository.save(any(ZipArchive.class))).thenAnswer(i -> i.getArgument(0));

        String returnedTaskId = zipArchiveService.startZipUploadProcess(fileSet.getId(), "test@mail.com", files);

        assertNotNull(returnedTaskId);

        verify(mailSender, timeout(2000)).send(any(MimeMessage.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(contains(returnedTaskId), any(ProgressUpdate.class));
    }

    @Test
    public void shouldHandleError_WhenStagingFilesFails() {
        MockMultipartFile brokenFile = new MockMultipartFile("files", null, null, (byte[]) null);
        MockMultipartFile[] files = {brokenFile};

        assertThrows(Exception.class, () -> {
            zipArchiveService.startZipUploadProcess(fileSet.getId(), "test@mail.com", files);
        });
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