package com.jerzymaj.file_researcher_backend.exceptions;

import java.time.LocalDateTime;

public class ErrorDetails {

    private LocalDateTime timeStamp;
    private String description;
    private String details;

    public ErrorDetails(LocalDateTime timeStamp, String description, String details){
        this.timeStamp = timeStamp;
        this.description = description;
        this.details = details;
    }

    public LocalDateTime getTimeStamp() { return timeStamp; }

    public String getDescription() { return description; }

    public String getDetails() { return details; }

}
