package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.RegisterUserDTO;
import com.jerzymaj.file_researcher_backend.DTOs.UserDTO;
import com.jerzymaj.file_researcher_backend.exceptions.ExistingUserException;
import com.jerzymaj.file_researcher_backend.exceptions.UserNotFoundException;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User " + id + " not found"));
    }

    public User findUserByName(String userName) {
        return userRepository.findByName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + userName));
    }

    public User registerUser(RegisterUserDTO registerUserDTO) {

        if (userRepository.existsByName(registerUserDTO.getName())) {
            throw new ExistingUserException("Name '" + registerUserDTO.getName() + "' is already taken");
        }
        if (userRepository.existsByEmail(registerUserDTO.getEmail())) {
            throw new ExistingUserException("Email '" + registerUserDTO.getEmail() + "' is already taken");
        }

        User user = User.builder()
                .name(registerUserDTO.getName())
                .email(registerUserDTO.getEmail())
                .password(passwordEncoder.encode(registerUserDTO.getPassword()))
                .build();

        return userRepository.save(user);
    }

    public void deleteCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User currentUser = userRepository.findByName(currentUsername)
                .orElseThrow(() -> new UserNotFoundException("Current user hasn't been found"));

        userRepository.delete(currentUser);
    }
}
