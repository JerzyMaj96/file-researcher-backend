package com.jerzymaj.file_researcher_backend.exceptions;

public class ExistingUserException extends RuntimeException {
    public ExistingUserException(String message) {
        super(message);
    }
}
