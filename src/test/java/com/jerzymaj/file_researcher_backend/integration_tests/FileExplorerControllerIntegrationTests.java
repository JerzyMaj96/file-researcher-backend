package com.jerzymaj.file_researcher_backend.integration_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldScanPath() throws Exception {
        Path tempFile = Files.createTempFile("testFile", ".txt");

        String jsonRequest = objectMapper.writeValueAsString(Map.of("path", tempFile.toString()));

        mockMvc.perform(post("/file-researcher/explorer/scan")
                        .contentType(APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(tempFile.getFileName().toString()))
                .andExpect(jsonPath("$.path").value(tempFile.toAbsolutePath().toString()))
                .andExpect(jsonPath("$.directory").value(false))
                .andExpect(jsonPath("$.children").doesNotExist());
    }
}
