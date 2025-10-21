package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.RegisterUserDTO;
import com.jerzymaj.file_researcher_backend.DTOs.UserDTO;
import com.jerzymaj.file_researcher_backend.configuration.ApiRoutes;
import com.jerzymaj.file_researcher_backend.services.UserService;
import com.jerzymaj.file_researcher_backend.tranlator.Translator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(ApiRoutes.USERS)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDTO> retrieveAllUsers() {
        return userService.findAllUsers().stream()
                .map(Translator::convertUserToDTO)
                .toList();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> retrieveUserById(@PathVariable Long userId) {

        UserDTO userDTO = Translator.convertUserToDTO(userService.findUserById(userId));

        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/authentication")
    public ResponseEntity<UserDTO> retrieveCurrentUser(Authentication authentication) {
        String userName = authentication.getName();
        UserDTO userDTO = Translator.convertUserToDTO(userService.findUserByName(userName));

        return ResponseEntity.ok(userDTO);
    }

    @PostMapping
    public ResponseEntity<UserDTO> createNewUser(@Valid @RequestBody RegisterUserDTO registerUserDTO) {
        UserDTO createdUserDTO = Translator.convertUserToDTO(userService.registerUser(registerUserDTO));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUserDTO.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdUserDTO);
    }

    @DeleteMapping("/delete-me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteCurrentUser() {
        userService.deleteCurrentUser();
        return ResponseEntity.noContent().build();
    }
}
