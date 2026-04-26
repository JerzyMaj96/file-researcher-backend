package com.jerzymaj.file_researcher_backend.DTOs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public record StagedUpload(UUID taskId, Path uploadDir, List<Files> files) {}
