package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.RegisterUserDTO;
import com.jerzymaj.file_researcher_backend.DTOs.UserDTO;
import com.jerzymaj.file_researcher_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/file-researcher/users")
@RequiredArgsConstructor
public class UserController {

//CONTROLLER PROPERTIES--------------------------------------------------------

    private final UserService userService;
//METHODS----------------------------------------------------------------------
    @GetMapping
    public List<UserDTO> retrieveAllUsers(){
        return userService.findAllUsers();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> retrieveUserById(@PathVariable Long userId){

        UserDTO userDTO = userService.findUserById(userId);

        return ResponseEntity.ok(userDTO);
    }

    @PostMapping
    public ResponseEntity<UserDTO> createNewUser(@Valid @RequestBody RegisterUserDTO registerUserDTO){
        UserDTO createdUserDTO = userService.registerUser(registerUserDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUserDTO.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdUserDTO);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long userId){
        userService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }
}
