package com.jerzymaj.file_researcher_backend.integration_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jerzymaj.file_researcher_backend.DTOs.RegisterUserDTO;
import com.jerzymaj.file_researcher_backend.models.User;
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

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
@Transactional
public class UserControllerIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        if (userRepository.findByName("tester").isEmpty()) {
            User tester = new User();
            tester.setName("tester");
            tester.setEmail("tester@mail.com");
            tester.setPassword("secret123");
            userRepository.save(tester);
        }
    }

    @Test
    public void shouldRegisterUser() throws Exception {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO("jerzy","jerzy@mail.com","secret123");

        mockMvc.perform(post("/file-researcher/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jerzy@mail.com"));
    }

    @Test
    @WithMockUser(username = "tester", roles = "ADMIN")
    public void shouldDeleteUser() throws Exception {
        String uniqueName = "jerzy_" + System.currentTimeMillis();
        String uniqueEmail = "jerzy_" + System.currentTimeMillis() + "@mail.com";

        RegisterUserDTO registerUserDTO = new RegisterUserDTO(uniqueName,uniqueEmail,"secret123");

        String response = mockMvc.perform(post("/file-researcher/users")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUserDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/file-researcher/users/{userId}", userId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/file-researcher/users/{userId}", userId))
                .andExpect(status().isNotFound());
    }
}

