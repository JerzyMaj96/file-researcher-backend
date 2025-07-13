package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
    private List<FileEntry> entries;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException {
        user = new User();
        user.setId(1L);
        user.setName("jerzy");


        Path tempFile1 = Files.createFile(tempDir.resolve("test1.txt"));
        Path tempFile2 = Files.createFile(tempDir.resolve("test2.txt"));
        Path tempFile3 = Files.createFile(tempDir.resolve("test3.csv"));


        FileEntry fe1 = new FileEntry();
        fe1.setPath(tempFile1.toString());
        FileEntry fe2 = new FileEntry();
        fe2.setPath(tempFile2.toString());
        FileEntry fe3 = new FileEntry();
        fe3.setPath(tempFile3.toString());

        entries = List.of(
                fe1,
                fe2,
                fe3
        );

        fileSet = new FileSet();
        fileSet.setId(1L);
        fileSet.setName("test");
        fileSet.setDescription("This is a test fileset description");
        fileSet.setCreationDate(LocalDateTime.now());
        fileSet.setUser(user);
        fileSet.setRecipientEmail("someone@mail.com");
        fileSet.setStatus(FileSetStatus.ACTIVE);
        fileSet.setFiles(entries);

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getName());

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        lenient().when(fileSetRepository.findById(fileSet.getId())).thenReturn(Optional.of(fileSet));
        lenient().when(fileSetService.getCurrentUserId()).thenReturn(user.getId());

        MimeMessage dummyMessage = new MimeMessage((jakarta.mail.Session)null);
        when(mailSender.createMimeMessage()).thenReturn(dummyMessage);

        when(zipArchiveRepository.save(Mockito.any())).thenAnswer(invocation -> {
            ZipArchive za = invocation.getArgument(0);
            za.setSentHistoryList(List.of());
            return za;
        });
    }


    @Test
    public void shouldCreateAndSendZipArchive_IfSuccess() throws MessagingException, IOException {

        ZipArchiveDTO actualResult = zipArchiveService.createAndSendZipArchive(fileSet.getId(), fileSet.getRecipientEmail());

        assertNotNull(actualResult, "ZipArchiveDTO should not be null");
        assertTrue(actualResult.getFileSetId() > 0, "FileSet ID should be greater than zero");
        assertNotNull(actualResult.getCreationDate(), "Creation date should be set");
        assertTrue(actualResult.getArchivePath() != null && !actualResult.getArchivePath().isEmpty(), "File path should not be empty");

        System.out.println("ZipArchiveDTO: " + actualResult); }

}
