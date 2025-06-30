package com.jerzymaj.file_researcher_backend.exceptions;

public class FileSetNotFoundException extends RuntimeException {
    public FileSetNotFoundException(String message) {
        super(message);
    }
}
