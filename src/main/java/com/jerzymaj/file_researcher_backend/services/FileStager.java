package com.jerzymaj.file_researcher_backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStager {

    @Value("${storage.upload-dir:temp-uploads}")
    private String storageBaseDir;

    public StagedUpload stageUpload(MultipartFile[] files) {}

}
