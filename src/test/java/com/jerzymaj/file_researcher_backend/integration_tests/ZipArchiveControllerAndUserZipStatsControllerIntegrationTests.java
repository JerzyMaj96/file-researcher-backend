package com.jerzymaj.file_researcher_backend.integration_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerzymaj.file_researcher_backend.configuration.TestMailConfig;
import com.jerzymaj.file_researcher_backend.models.FileEntry;
import com.jerzymaj.file_researcher_backend.models.FileSet;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.models.ZipArchive;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.repositories.FileEntryRepository;
import com.jerzymaj.file_researcher_backend.repositories.FileSetRepository;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import com.jerzymaj.file_researcher_backend.repositories.ZipArchiveRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestMailConfig.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
@Transactional
public class ZipArchiveControllerAndUserZipStatsControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileSetRepository fileSetRepository;

    @Autowired
    private FileEntryRepository fileEntryRepository;

    @Autowired
    private ZipArchiveRepository zipArchiveRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ObjectMapper objectMapper;

    private FileSet fileSet;

    @BeforeEach
    public void setUp() throws IOException {
        User user = new User();
        user.setName("tester");
        user.setEmail("tester@mail.com");
        user.setPassword("secret123");
        userRepository.save(user);
        userRepository.flush();

        FileEntry fileEntry = new FileEntry();
        fileEntry.setName("test1.txt");
        fileEntry.setPath(Files.createTempFile("test1", ".txt").toString());
        fileEntry.setExtension("txt");
        fileEntry.setSize(123L);
        fileEntryRepository.save(fileEntry);
        fileEntryRepository.flush();

        fileSet = new FileSet();
        fileSet.setName("test set");
        fileSet.setDescription("description");
        fileSet.setStatus(FileSetStatus.ACTIVE);
        fileSet.setRecipientEmail("tester@mail.com");
        fileSet.setCreationDate(LocalDateTime.now());
        fileSet.setUser(user);
        fileSet.setFiles(List.of(fileEntry));
        fileSetRepository.save(fileSet);
        fileEntryRepository.flush();
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldSendZipArchive() throws Exception {
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileSetId").value(fileSet.getId()))
                .andExpect(jsonPath("$.archivePath").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldRetrieveAllZipArchives() throws Exception {
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/file-researcher/file-sets/{fileSetId}/zip", fileSet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].fileSetId").value(fileSet.getId()));
    }


    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldRetrieveZipArchiveById() throws Exception {
        String response = mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long zipArchiveId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/file-researcher/file-sets/{fileSetId}/zip/{zipArchiveId}", fileSet.getId(), zipArchiveId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(zipArchiveId));
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldDeleteZipArchiveById() throws Exception { //REPAIR
        String response = mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long zipArchiveId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/file-researcher/file-sets/{fileSetId}/zip/{zipArchiveId}", fileSet.getId(), zipArchiveId))
                .andExpect(status().isNoContent());

        Optional<ZipArchive> deleteZip = zipArchiveRepository.findById(zipArchiveId);
        assertTrue(deleteZip.isEmpty(), "ZipArchive should be deleted from repository");
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldRetrieveSentStatistics() throws Exception {
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/file-researcher/zip/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").exists())
                .andExpect(jsonPath("$.failureCount").exists());

    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldRetrieveLargeZipArchives() throws Exception {
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip/send", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/file-researcher/zip/large")
                        .param("minSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileSetId").value(fileSet.getId()));
    }
}
