package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.RegisterUserDTO;
import com.jerzymaj.file_researcher_backend.DTOs.UserDTO;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService (UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public Optional<User> getUserById(Long id){
        return userRepository.findById(id);
    }

    public User createUser(User user){
        return userRepository.save(user);
    }

    public RegisterUserDTO convertUserToRegisterDTO(User user){
        return new RegisterUserDTO(user.getName(),
                                   user.getEmail(),
                                   user.getPassword());
    }

    public UserDTO convertUserToDTO(User user){
        return new UserDTO(user.getId(),
                           user.getName(),
                           user.getEmail(),
                           user.getCreationDate());
    }


}
