package com.jerzymaj.file_researcher_backend.exceptions;

public class NoFilesSelectedException extends RuntimeException {
    public NoFilesSelectedException(String message) {
        super(message);
    }
}
