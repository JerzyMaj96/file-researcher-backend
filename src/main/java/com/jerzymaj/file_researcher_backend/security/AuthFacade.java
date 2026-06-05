package com.jerzymaj.file_researcher_backend.security;

import com.jerzymaj.file_researcher_backend.models.User;

public interface AuthFacade {
    User getCurrentUser();
    Long getCurrentUserId();
}
