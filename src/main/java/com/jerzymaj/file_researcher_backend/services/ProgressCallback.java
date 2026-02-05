package com.jerzymaj.file_researcher_backend.services;

@FunctionalInterface
public interface ProgressCallback {
    void onUpdate(int percent,  String message);
}
