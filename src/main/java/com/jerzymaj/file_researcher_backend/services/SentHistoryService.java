package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.repositories.SentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SentHistoryService {

    private final SentHistoryRepository sentHistoryRepository;
}
