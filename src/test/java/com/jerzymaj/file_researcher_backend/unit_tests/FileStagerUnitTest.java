package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.StagedUpload;
import com.jerzymaj.file_researcher_backend.services.FileStager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileStagerUnitTest {

    private FileStager fileStager;

    @BeforeEach
    public void setUp() {
        fileStager = new FileStager();
    }

    @Test
    public void ShouldStageUpload_IfSuccess(@TempDir Path tempDir) throws IOException {

        ReflectionTestUtils.setField(fileStager, "storageBaseDir", tempDir.toString());

        MockMultipartFile file1 = new MockMultipartFile("files", "test1.txt", "text/plain", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.txt", "text/plain", "content2".getBytes());
        MockMultipartFile[] files = {file1, file2};

        StagedUpload actualResult = fileStager.stageUpload(files);

        assertNotNull(actualResult.taskId());
        assertFalse(actualResult.taskId().isBlank());
        assertTrue(actualResult.uploadDir().startsWith(tempDir));
        assertTrue(Files.exists(actualResult.files().getFirst()));
        assertTrue(Files.exists(actualResult.files().getLast()));
    }
}
