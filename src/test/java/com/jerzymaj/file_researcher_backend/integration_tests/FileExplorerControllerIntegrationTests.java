package com.jerzymaj.file_researcher_backend.integration_tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FileExplorerControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    MockMultipartFile file1;
    MockMultipartFile file2;
    MockMultipartFile[] files;

    @BeforeEach
    public void setup() {
        file1 = new MockMultipartFile("files", "test1.txt",
                "text/plain", "content1".getBytes());
        file2 = new MockMultipartFile("files", "directory/test2.pdf",
                "text/plain", "content2".getBytes());
        files = new MockMultipartFile[]{file1, file2};
    }

    @Test
    @WithMockUser
    public void shouldScanPath() throws Exception {

        mockMvc.perform(multipart("/file-researcher/explorer/upload")
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Root"))
                .andExpect(jsonPath("$.directory").value(true))
                .andExpect(jsonPath("$.children").isArray())
                .andExpect(jsonPath("$.children[0].name").value("test1.txt"))
                .andExpect(jsonPath("$.children[0].directory").value(false));
    }

    @Test
    @WithMockUser
    public void shouldScanPathWithFilter() throws Exception {
        mockMvc.perform(multipart("/file-researcher/explorer/upload")
                        .file(file1)
                        .file(file2)
                        .param("extension","pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Root"))
                .andExpect(jsonPath("$.directory").value(true))
                .andExpect(jsonPath("$.children").isArray())
                .andExpect(jsonPath("$.children[0].name").value("directory"))
                .andExpect(jsonPath("$.children[0].directory").value(true))
                .andExpect(jsonPath("$.children[0].children[0].name").value("test2.pdf"))
                .andExpect(jsonPath("$.children[0].children[0].directory").value(false));
    }
}
