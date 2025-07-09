package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file-researcher/file-sets/{fileSetId}/zip")
@RequiredArgsConstructor
public class ZipArchiveController {
//PROPERTIES---------------------------------------------------------------------

    private final ZipArchiveService zipArchiveService;
}
