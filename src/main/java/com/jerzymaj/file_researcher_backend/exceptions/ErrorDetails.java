package com.jerzymaj.file_researcher_backend.exceptions;

import java.time.LocalDateTime;

public record ErrorDetails(LocalDateTime timeStamp, String description,
                           String details) {
}
