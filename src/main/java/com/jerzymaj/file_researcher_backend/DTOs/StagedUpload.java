package com.jerzymaj.file_researcher_backend.DTOs;

import java.nio.file.Path;
import java.util.List;

public record StagedUpload(String taskId, Path uploadDir, List<Path> files) {}
