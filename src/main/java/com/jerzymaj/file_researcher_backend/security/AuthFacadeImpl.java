package com.jerzymaj.file_researcher_backend.security;

import com.jerzymaj.file_researcher_backend.exceptions.UnauthorizedException;
import com.jerzymaj.file_researcher_backend.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthFacadeImpl implements AuthFacade {
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }

        throw new UnauthorizedException("Invalid principal type");
    }

    @Override
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
