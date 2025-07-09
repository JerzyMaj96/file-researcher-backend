package com.jerzymaj.file_researcher_backend.exceptions;

public class ZipArchiveNotFoundException extends RuntimeException {
    public ZipArchiveNotFoundException(String message) {
        super(message);
    }
}
