package com.jerzymaj.file_researcher_backend.exceptions;

public class SentHistoryNotFoundException extends RuntimeException {
    public SentHistoryNotFoundException(String message) {
        super(message);
    }
}
