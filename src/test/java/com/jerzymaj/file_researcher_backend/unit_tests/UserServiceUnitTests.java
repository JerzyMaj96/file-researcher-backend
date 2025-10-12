package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.RegisterUserDTO;
import com.jerzymaj.file_researcher_backend.DTOs.UserDTO;
import com.jerzymaj.file_researcher_backend.exceptions.ExistingUserException;
import com.jerzymaj.file_researcher_backend.models.User;
import com.jerzymaj.file_researcher_backend.repositories.UserRepository;
import com.jerzymaj.file_researcher_backend.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTests {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    public void shouldRegisterUser_IfSuccess() {
        RegisterUserDTO registerUserDTO = new RegisterUserDTO("jerzy", "jerzy@mail.com", "secret123");

        when(userRepository.existsByName("jerzy")).thenReturn(false);
        when(userRepository.existsByEmail("jerzy@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> {
            User user = invocationOnMock.getArgument(0);
            user.setId(1L);
            return user;
        });

        User actualResult = userService.registerUser(registerUserDTO);

        assertThat(actualResult.getId()).isEqualTo(1L);
        assertThat(actualResult.getEmail()).isEqualTo("jerzy@mail.com");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    public void shouldThrowExistingUserException_IfUserExists() {
        when(userRepository.existsByName("jerzy")).thenReturn(true);

        RegisterUserDTO registerUserDTO = new RegisterUserDTO("jerzy", "x@y.com", "password");

        assertThatThrownBy(() -> userService.registerUser(registerUserDTO))
                .isInstanceOf(ExistingUserException.class);
    }
}
