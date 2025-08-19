package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.ZipArchiveDTO;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/file-researcher/zip-archives")
@RequiredArgsConstructor
public class UserZipStatsController {

    private final ZipArchiveService zipArchiveService;

    @GetMapping("/stats")
    public Map<String, Object> retrieveSentStatistics() {
        return zipArchiveService.getZipStatsForCurrentUser();
    }

    @GetMapping("/large")
    public List<ZipArchiveDTO> retrieveLargeZipArchives(@RequestParam(defaultValue = "10000000") Long minSize) { // check which default size to set
        return zipArchiveService.getLargeZipFiles(minSize);
    }
}
