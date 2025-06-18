package com.jerzymaj.file_researcher_backend.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorDetails {

    private final LocalDateTime timeStamp;
    private final String description;
    private final String details;

    public ErrorDetails(LocalDateTime timeStamp, String description, String details){
        this.timeStamp = timeStamp;
        this.description = description;
        this.details = details;
    }

}
