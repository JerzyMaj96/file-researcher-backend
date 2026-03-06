package com.jerzymaj.file_researcher_backend.integration_tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SentHistoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileSetRepository fileSetRepository;

    @Autowired
    private FileEntryRepository fileEntryRepository;

    @Autowired
    private ZipArchiveRepository zipArchiveRepository;

    @Autowired
    private SentHistoryRepository sentHistoryRepository;

    private FileSet fileSet;
    MockMultipartFile file1;
    MockMultipartFile file2;

    @BeforeEach
    public void setUp() throws IOException {
        User user = new User();
        user.setName("tester");
        user.setEmail("tester@mail.com");
        user.setPassword("secret123");
        user = userRepository.save(user);

        FileEntry fileEntry = new FileEntry();
        fileEntry.setName("test1.txt");
        fileEntry.setPath(Files.createTempFile("test1", ".txt").toString());
        fileEntry.setExtension("txt");
        fileEntry.setSize(123L);
        fileEntry = fileEntryRepository.save(fileEntry);

        fileSet = new FileSet();
        fileSet.setName("test set");
        fileSet.setDescription("description");
        fileSet.setStatus(FileSetStatus.ACTIVE);
        fileSet.setRecipientEmail("tester@mail.com");
        fileSet.setCreationDate(LocalDateTime.now());
        fileSet.setUser(user);

        fileSet = fileSetRepository.save(fileSet);
        fileSet.getFiles().add(fileEntry);
        fileSet = fileSetRepository.save(fileSet);

        file1 = new MockMultipartFile("files", "test1.txt",
                "text/plain", "content1".getBytes());
        file2 = new MockMultipartFile("files", "directory/test2.pdf",
                "text/plain", "content2".getBytes());
    }

    @AfterEach
    public void tearDown() {
        sentHistoryRepository.deleteAll();
        zipArchiveRepository.deleteAll();
        fileSetRepository.deleteAll();
        fileEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldRetrieveAllSentHistoryForZipArchive() throws Exception {

        mockMvc.perform(multipart("/file-researcher/file-sets/{fileSetId}/zip-archives/send-uploaded-files", fileSet.getId())
                        .file(file1)
                        .file(file2)
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        Long zipArchiveId = waitForProcess();

        mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history", zipArchiveId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].zipArchiveId").value(zipArchiveId));
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldRetrieveLastRecipient() throws Exception {

        mockMvc.perform(multipart("/file-researcher/file-sets/{fileSetId}/zip-archives/send-uploaded-files", fileSet.getId())
                        .file(file1)
                        .file(file2)
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long zipArchiveId = waitForProcess();

        mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history/last-recipient", zipArchiveId))
                .andExpect(status().isOk())
                .andExpect(content().string("email@mail.com"));
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldRetrieveSentHistoryById() throws Exception {

        mockMvc.perform(multipart("/file-researcher/file-sets/{fileSetId}/zip-archives/send-uploaded-files", fileSet.getId())
                        .file(file1)
                        .file(file2)
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        Long zipArchiveId = waitForProcess();

        String historyResponse = getHistoryResponse(zipArchiveId);

        Long sentHistoryId = getHistoryId(historyResponse);

        mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history/{sentHistoryId}",
                        zipArchiveId,
                        sentHistoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sentHistoryId));
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldDeleteSentHistoryById() throws Exception {

        mockMvc.perform(multipart("/file-researcher/file-sets/{fileSetId}/zip-archives/send-uploaded-files", fileSet.getId())
                        .file(file1)
                        .file(file2)
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long zipArchiveId = waitForProcess();

        String historyResponse = getHistoryResponse(zipArchiveId);

        Long sentHistoryId = getHistoryId(historyResponse);

        mockMvc.perform(delete("/file-researcher/zip-archives/{zipArchiveId}/history/{sentHistoryId}",
                        zipArchiveId,
                        sentHistoryId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history/{sentHistoryId}",
                        zipArchiveId,
                        sentHistoryId))
                .andExpect(status().isNotFound());
    }

    private Long waitForProcess() {
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<ZipArchive> archives = zipArchiveRepository.findAllByFileSetId(fileSet.getId());
                    assertFalse(archives.isEmpty());

                    List<SentHistory> history = sentHistoryRepository.findAllByZipArchiveId(archives.getFirst().getId());
                    assertFalse(history.isEmpty());
                });

        return zipArchiveRepository.findAllByFileSetId(fileSet.getId()).getFirst().getId();
    }

    private String getHistoryResponse(Long zipArchiveId) throws Exception {
        return mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history", zipArchiveId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private Long getHistoryId(String historyResponse) throws JsonProcessingException {
        return objectMapper.readTree(historyResponse)
                .get(0)
                .get("id")
                .asLong();
    }
}
