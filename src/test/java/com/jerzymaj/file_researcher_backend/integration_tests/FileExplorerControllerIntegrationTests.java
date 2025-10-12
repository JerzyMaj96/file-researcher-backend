package com.jerzymaj.file_researcher_backend.integration_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerzymaj.file_researcher_backend.DTOs.ScanFilteredRequest;
import com.jerzymaj.file_researcher_backend.DTOs.ScanRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
public class FileExplorerControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    public void shouldScanPath() throws Exception {
        Path tempFile = Files.createTempFile("testFile", ".txt");

        ScanRequest request = new ScanRequest(tempFile.toString());
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/file-researcher/explorer/scan")
                        .contentType(APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(tempFile.getFileName().toString()))
                .andExpect(jsonPath("$.path").value(tempFile.toAbsolutePath().toString()))
                .andExpect(jsonPath("$.directory").value(false))
                .andExpect(jsonPath("$.children").doesNotExist());
    }

    @Test
    public void shouldScanFilteredPath() throws Exception {
        Path tempSubDir = Files.createTempDirectory("dir");
        Files.createFile(tempSubDir.resolve("test1.txt"));
        Files.createFile(tempSubDir.resolve("test2.pdf"));
        Files.createFile(tempSubDir.resolve("test3.txt"));

        ScanFilteredRequest request = new ScanFilteredRequest(tempSubDir.toString(), "txt");
        String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/file-researcher/explorer/scan/filtered")
                        .contentType(APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(tempSubDir.getFileName().toString()))
                .andExpect(jsonPath("$.path").value(tempSubDir.toAbsolutePath().toString()))
                .andExpect(jsonPath("$.directory").value(true))
                .andExpect(jsonPath("$.children").isArray())
                .andExpect(jsonPath("$.children.length()").value(2))
                .andExpect(jsonPath("$.children[*].name").value(containsInAnyOrder("test1.txt", "test3.txt")));
    }
}
