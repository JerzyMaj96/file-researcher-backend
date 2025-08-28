package com.jerzymaj.file_researcher_backend.exceptions;

public class PathNotFoundException extends RuntimeException {
    public PathNotFoundException(String message) {
        super(message);
    }
}
