package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.ProgressUpdate;
import com.jerzymaj.file_researcher_backend.DTOs.StagedUpload;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import com.jerzymaj.file_researcher_backend.services.*;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ZipArchiveProcessorUnitTest {

    @Mock
    private ZipArchiveCreator zipArchiveCreator;

    @Mock
    private ZipEmailSender zipEmailSender;

    @Mock
    private FileSetRepository fileSetRepository;

    @Mock
    private ZipArchiveRepository zipArchiveRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ZipArchiveStatusService zipArchiveStatusService;

    @Mock
    private SentHistoryService sentHistoryService;

    @InjectMocks
    private ZipArchiveProcessor zipArchiveProcessor;

    private FileSet fileSet;
    private StagedUpload stagedUpload;
    private String expectedTaskId;

    @BeforeEach
    public void setUp() {
        User user = new User();
        user.setId(1L);
        user.setName("jerzy");

        fileSet = new FileSet();
        fileSet.setId(1L);
        fileSet.setName("testSet");
        fileSet.setUser(user);
        fileSet.setRecipientEmail("someone@mail.com");
        fileSet.setStatus(FileSetStatus.ACTIVE);

        when(fileSetRepository.findByIdWithFiles(fileSet.getId())).thenReturn(Optional.of(fileSet));
        when(zipArchiveRepository.findMaxSendNumberByFileSetId(anyLong())).thenReturn(0);

        expectedTaskId = "mock-task-id";
        stagedUpload = new StagedUpload(
                expectedTaskId,
                Path.of("/tmp/mock-task-id"),
                List.of(Path.of("/tmp/mock-task-id/test1.txt"), Path.of("/tmp/mock-task-id/test2.txt")));
    }

    @Test
    public void shouldCreateAndSendZipAsync_IfSuccess(@TempDir Path tempDir) throws IOException, MessagingException {

        Path fakeZipPath = Files.createFile(tempDir.resolve("test-archive.zip"));

        when(zipArchiveRepository.save(any(ZipArchive.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(zipArchiveCreator.prepareTempPath(anyLong(), anyInt()))
                .thenReturn(fakeZipPath);

        zipArchiveProcessor.createAndSendZipAsync(fileSet.getId(), fileSet.getRecipientEmail(), stagedUpload);

        verify(zipEmailSender).sendZipArchiveByEmail(
                eq(fileSet.getRecipientEmail()),
                eq(fakeZipPath),
                any(),
                any());

        verify(zipArchiveStatusService).updateDatabaseAfterSuccess(any(), eq(fileSet.getId()));
        verify(sentHistoryService).saveSentHistory(any(), eq(fileSet.getRecipientEmail()), eq(true), any());
        verify(messagingTemplate).convertAndSend(
                contains(expectedTaskId),
                argThat((ProgressUpdate msg) -> msg.percent() == 100)
        );
    }
}
