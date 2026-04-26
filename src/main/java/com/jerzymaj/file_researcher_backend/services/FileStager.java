package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.StagedUpload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStager {

    @Value("${storage.upload-dir:temp-uploads}")
    private String storageBaseDir;

    public StagedUpload stageUpload(MultipartFile[] files) throws IOException {
        String taskId = UUID.randomUUID().toString();

        Path uploadDir = Paths.get(storageBaseDir, taskId).toAbsolutePath();
        Files.createDirectories(uploadDir);

        List<Path> savedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            Path destination = uploadDir.resolve(Objects.requireNonNull(file.getOriginalFilename())).toAbsolutePath();
            Files.createDirectories(destination.getParent());
            file.transferTo(destination);
            savedFiles.add(destination);
        }

        return new StagedUpload(taskId, uploadDir, savedFiles);
    }

}
