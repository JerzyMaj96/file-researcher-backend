package com.jerzymaj.file_researcher_backend.integration_tests;

import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FileSetControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Test
    @WithMockUser(username = "tester", roles = "USER")
    public void shouldCreateNewFileSetFromUploaded() throws Exception {
        User testUser = new User();
        testUser.setName("tester");
        testUser.setEmail("tester@mail.com");
        testUser.setPassword("password");
        userRepository.save(testUser);

        MockMultipartFile file1 = new MockMultipartFile("files", "test1.txt",
                "text/plain", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "directory/test2.pdf",
                "text/plain", "content2".getBytes());

        mockMvc.perform(multipart("/file-researcher/file-sets/upload")
                        .file(file1)
                        .file(file2)
                        .param("name", "testUser")
                        .param("description", "This is a test fileset description")
                        .param("recipientEmail", "jerzy@mail.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("testUser"))
                .andExpect(jsonPath("$.description").value("This is a test fileset description"))
                .andExpect(jsonPath("$.recipientEmail").value("jerzy@mail.com"))
                .andExpect(jsonPath("$.files", hasSize(2)))
                .andExpect(jsonPath("$.files[*].name", containsInAnyOrder("test1.txt", "test2.pdf")))
                .andExpect(jsonPath("$.files[*].extension", containsInAnyOrder("txt", "pdf")));
    }
}
