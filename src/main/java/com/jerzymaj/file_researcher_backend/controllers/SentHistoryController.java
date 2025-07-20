package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.services.SentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
@RequiredArgsConstructor
public class SentHistoryController {

    private final SentHistoryService sentHistoryService;
}
