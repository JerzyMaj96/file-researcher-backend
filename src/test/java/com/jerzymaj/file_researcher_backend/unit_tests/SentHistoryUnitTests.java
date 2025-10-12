package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.models.SentHistory;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.enum_classes.SendStatus;
import com.jerzymaj.file_researcher_backend.repositories.SentHistoryRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import com.jerzymaj.file_researcher_backend.services.FileSetService;
import com.jerzymaj.file_researcher_backend.services.SentHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SentHistoryUnitTests {

    @Mock
    private ZipArchiveRepository zipArchiveRepository;

    @Mock
    private SentHistoryRepository sentHistoryRepository;

    @Mock
    private FileSetService fileSetService;

    @InjectMocks
    private SentHistoryService sentHistoryService;

    private ZipArchive zipArchive;
    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(1L);

        zipArchive = new ZipArchive();
        zipArchive.setId(1L);
        zipArchive.setUser(user);
        zipArchive.setRecipientEmail("someone@mail.com");
    }

    @Test
    public void shouldSaveSentHistory_WhenSuccess() {

        when(sentHistoryRepository.save(any(SentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SentHistory actualResult = sentHistoryService.saveSentHistory(zipArchive, zipArchive.getRecipientEmail(), true, null);

        assertNotNull(actualResult);
        assertEquals(zipArchive, actualResult.getZipArchive());
        assertEquals(zipArchive.getRecipientEmail(), actualResult.getSentToEmail());
        assertEquals(SendStatus.SUCCESS, actualResult.getStatus());
        assertNull(actualResult.getErrorMessage());
        assertNotNull(actualResult.getSendAttemptDate());
    }

    @Test
    public void shouldSaveSentHistory_WhenFailure() {

        when(sentHistoryRepository.save(any(SentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SentHistory actualResult = sentHistoryService.saveSentHistory(zipArchive, zipArchive.getRecipientEmail(), false,
                "Test error message");

        assertNotNull(actualResult);
        assertEquals(zipArchive, actualResult.getZipArchive());
        assertEquals(zipArchive.getRecipientEmail(), actualResult.getSentToEmail());
        assertEquals(SendStatus.FAILURE, actualResult.getStatus());
        assertNotNull(actualResult.getErrorMessage());
        assertNotNull(actualResult.getSendAttemptDate());
    }


    @Test
    public void shouldGetAllSentHistoryForZipArchive() throws AccessDeniedException {

        SentHistory sentHistory = new SentHistory();
        sentHistory.setId(1L);
        sentHistory.setZipArchive(zipArchive);

        when(zipArchiveRepository.findById(zipArchive.getId())).thenReturn(Optional.of(zipArchive));
        when(fileSetService.getCurrentUserId()).thenReturn(user.getId());
        when(sentHistoryRepository.findAllByZipArchiveIdSorted(zipArchive.getId())).thenReturn(List.of(sentHistory));

        List<SentHistory> actualResult = sentHistoryService.getAllSentHistoryForZipArchive(zipArchive.getId());

        assertNotNull(actualResult);
        assertEquals(1, actualResult.size());
        assertEquals(sentHistory, actualResult.getFirst());
    }

    @Test
    public void shouldGetLastRecipient() throws AccessDeniedException {

        SentHistory sentHistory = new SentHistory();
        sentHistory.setId(1L);
        sentHistory.setZipArchive(zipArchive);

        String expectedEmail = "someone@mail.com";

        when(zipArchiveRepository.findById(zipArchive.getId())).thenReturn(Optional.of(zipArchive));
        when(fileSetService.getCurrentUserId()).thenReturn(user.getId());
        when(sentHistoryRepository.findLastRecipient(zipArchive.getId())).thenReturn(expectedEmail);

        String actualResult = sentHistoryService.getLastRecipient(zipArchive.getId());

        assertNotNull(actualResult);
        assertEquals(expectedEmail, actualResult);
    }
}
