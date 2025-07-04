package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.RegisterUserDTO;
import com.jerzymaj.file_researcher_backend.DTOs.UserDTO;
import com.jerzymaj.file_researcher_backend.exceptions.ExistingUserException;
import com.jerzymaj.file_researcher_backend.exceptions.UserNotFoundException;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

//PROPERTIES--------------------------------------------------------------------

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

//METHODS-------------------------------------------------------------------------

    public List<UserDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertUserToDTO)
                .toList();
    }

    public UserDTO findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User " + id + " not found"));
        return convertUserToDTO(user);
    }

    public UserDTO registerUser(RegisterUserDTO registerUserDTO) {

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

        return convertUserToDTO(userRepository.save(user));
    }

    public void deleteUserById(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));
        userRepository.delete(user);
    }

//DTO MAPPER----------------------------------------------------------------------------------------------

    private UserDTO convertUserToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .creationDate(user.getCreationDate())
                .build();
    }
}
