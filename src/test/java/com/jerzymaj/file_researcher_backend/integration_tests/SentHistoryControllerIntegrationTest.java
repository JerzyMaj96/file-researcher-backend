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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
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

        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
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

        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
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

        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
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

        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
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
        return  mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history", zipArchiveId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private Long getHistoryId (String historyResponse) throws JsonProcessingException {
        return objectMapper.readTree(historyResponse)
                .get(0)
                .get("id")
                .asLong();
    }
}
