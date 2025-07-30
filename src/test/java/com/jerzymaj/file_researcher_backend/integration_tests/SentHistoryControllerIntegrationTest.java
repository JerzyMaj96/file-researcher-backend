package com.jerzymaj.file_researcher_backend.integration_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerzymaj.file_researcher_backend.models.FileEntry;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileEntryRepository;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
@Transactional
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

    private FileSet fileSet;

    @BeforeEach
    public void setUp() throws IOException {
        User user = new User();
        user.setName("tester");
        user.setEmail("tester@mail.com");
        user.setPassword("secret123");
        userRepository.save(user);

        FileEntry fileEntry = new FileEntry();
        fileEntry.setName("test1.txt");
        fileEntry.setPath(Files.createTempFile("test1", ".txt").toString());
        fileEntry.setExtension("txt");
        fileEntry.setSize(123L);
        fileEntryRepository.save(fileEntry);

        fileSet = new FileSet();
        fileSet.setName("test set");
        fileSet.setDescription("description");
        fileSet.setStatus(FileSetStatus.ACTIVE);
        fileSet.setRecipientEmail("tester@mail.com");
        fileSet.setCreationDate(LocalDateTime.now());
        fileSet.setUser(user);
        fileSet.setFiles(List.of(fileEntry));
        fileSetRepository.save(fileSet);

    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldRetrieveAllSentHistoryForZipArchive() throws Exception {

        String response = mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long zipArchiveId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history", zipArchiveId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].zipArchiveId").value(zipArchiveId));
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldRetrieveLastRecipient() throws Exception {

        String response = mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long zipArchiveId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history/last-recipient", zipArchiveId))
                .andExpect(status().isOk())
                .andExpect(content().string("email@mail.com"));
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldRetrieveSentHistoryById() throws Exception {

        String sendZipResponse = mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long zipArchiveId = objectMapper.readTree(sendZipResponse).get("id").asLong();

        String historyResponse = mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history", zipArchiveId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long sentHistoryId = objectMapper.readTree(historyResponse)
                .get(0)
                .get("id")
                .asLong();

        mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history/{sentHistoryId}",
                        zipArchiveId,
                        sentHistoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sentHistoryId));
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldDeleteSentHistoryById() throws Exception {

        String sendZipResponse = mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long zipArchiveId = objectMapper.readTree(sendZipResponse).get("id").asLong();

        String historyResponse = mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history", zipArchiveId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long sentHistoryId = objectMapper.readTree(historyResponse)
                .get(0)
                .get("id")
                .asLong();

        mockMvc.perform(delete("/file-researcher/zip-archives/{zipArchiveId}/history/{sentHistoryId}",
                zipArchiveId,
                sentHistoryId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/file-researcher/zip-archives/{zipArchiveId}/history/{sentHistoryId}",
                        zipArchiveId,
                        sentHistoryId))
                .andExpect(status().isNotFound());
    }
}
