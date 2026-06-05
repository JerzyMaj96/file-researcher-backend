package com.jerzymaj.file_researcher_backend.security;

import com.jerzymaj.file_researcher_backend.models.User;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthFacadeImpl implements AuthFacade {
    @Override
    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
