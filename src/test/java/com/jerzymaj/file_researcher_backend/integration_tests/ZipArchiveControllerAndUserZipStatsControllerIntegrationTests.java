package com.jerzymaj.file_researcher_backend.integration_tests;

import com.jerzymaj.file_researcher_backend.configuration.TestMailConfig;
import com.jerzymaj.file_researcher_backend.models.*;
import com.jerzymaj.file_researcher_backend.models.enum_classes.FileSetStatus;
import com.jerzymaj.file_researcher_backend.models.enum_classes.ZipArchiveStatus;
import com.jerzymaj.file_researcher_backend.repositories.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestMailConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
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

        fileSet.setFiles(new ArrayList<>(List.of(fileEntry)));

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
    public void shouldSendZipArchive() throws Exception {
        String taskId = mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertNotNull(taskId);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    FileSet updatedFileSet = fileSetRepository.findById(fileSet.getId()).orElseThrow();
                    assertEquals(FileSetStatus.SENT, updatedFileSet.getStatus());

                    List<ZipArchive> archives = zipArchiveRepository.findAllByFileSetId(fileSet.getId());
                    assertFalse(archives.isEmpty());
                    assertEquals(ZipArchiveStatus.SUCCESS, archives.getFirst().getStatus());
                });
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldRetrieveAllZipArchives() throws Exception {
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        waitTillArchivesFinished();

        mockMvc.perform(get("/file-researcher/file-sets/{fileSetId}/zip-archives", fileSet.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].fileSetId").value(fileSet.getId()));
    }


    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldRetrieveZipArchiveById() throws Exception {
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> zipArchiveRepository.findByFileSetId(fileSet.getId()).isPresent());

        ZipArchive zipArchive = zipArchiveRepository.findByFileSetId(fileSet.getId()).orElseThrow();

        mockMvc.perform(get("/file-researcher/file-sets/{fileSetId}/zip-archives/{zipArchiveId}", fileSet.getId(), zipArchive.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(zipArchive.getId()));
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldDeleteZipArchiveById() throws Exception { //REPAIR
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> zipArchiveRepository.findByFileSetId(fileSet.getId()).isPresent());

        ZipArchive zipArchive = zipArchiveRepository.findByFileSetId(fileSet.getId()).orElseThrow();

        mockMvc.perform(delete("/file-researcher/file-sets/{fileSetId}/zip-archives/{zipArchiveId}", fileSet.getId(), zipArchive.getId()))
                .andExpect(status().isNoContent());

        Optional<ZipArchive> deleteZip = zipArchiveRepository.findById(zipArchive.getId());

        assertTrue(deleteZip.isEmpty(), "ZipArchive should be deleted from repository");
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldRetrieveSentStatistics() throws Exception {
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        waitTillArchivesFinished();

        mockMvc.perform(get("/file-researcher/zip-archives/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").exists())
                .andExpect(jsonPath("$.failureCount").exists());
    }

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldRetrieveLargeZipArchives() throws Exception {
        mockMvc.perform(post("/file-researcher/file-sets/{fileSetId}/zip-archives/send-progress", fileSet.getId())
                        .param("recipientEmail", "email@mail.com"))
                .andExpect(status().isOk());

        waitTillArchivesFinished();

        mockMvc.perform(get("/file-researcher/zip-archives/large")
                        .param("minSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileSetId").value(fileSet.getId()));
    }

    private void waitTillArchivesFinished() {
        Awaitility.await()
                .untilAsserted(() -> {
                    List<ZipArchive> archives = zipArchiveRepository.findAllByFileSetId(fileSet.getId());
                    assertFalse(archives.isEmpty());
                    assertEquals(ZipArchiveStatus.SUCCESS, archives.getFirst().getStatus());
                });
    }
}
