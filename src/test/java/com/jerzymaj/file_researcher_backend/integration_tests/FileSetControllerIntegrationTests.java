package com.jerzymaj.file_researcher_backend.integration_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerzymaj.file_researcher_backend.DTOs.CreateFileSetDTO;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
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
@Transactional
public class FileSetControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldCreateNewFileSet(@TempDir Path tempDir) throws Exception {
        User testUser = new User();
        testUser.setName("tester");
        testUser.setEmail("tester@mail.com");
        testUser.setPassword("password");
        userRepository.save(testUser);

        Path tempFile1 = Files.createFile(tempDir.resolve("test1.txt"));
        Path tempFile2 = Files.createFile(tempDir.resolve("test2.txt"));
        Path tempFile3 = Files.createFile(tempDir.resolve("test3.csv"));

        CreateFileSetDTO createFileSetDTO = new CreateFileSetDTO("test","This is a test fileset description","jerzy@mail.com",
                List.of(tempFile1.toString(),
                        tempFile2.toString(),
                        tempFile3.toString())
                );

        mockMvc.perform(post("/file-researcher/file-sets")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createFileSetDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test"))
                .andExpect(jsonPath("$.description").value("This is a test fileset description"))
                .andExpect(jsonPath("$.recipientEmail").value("jerzy@mail.com"))
                .andExpect(jsonPath("$.files", hasSize(3)))
                .andExpect(jsonPath("$.files[*].name", containsInAnyOrder("test1.txt", "test2.txt", "test3.csv")))
                .andExpect(jsonPath("$.files[*].extension", containsInAnyOrder("txt","txt","csv")));

    }
}
