package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.services.ProgressCallback;
import com.jerzymaj.file_researcher_backend.services.ZipArchiveCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ZipArchiveCreatorTest {

    private ZipArchiveCreator zipArchiveCreator ;

    @BeforeEach
    public void setUp() {
        zipArchiveCreator = new ZipArchiveCreator();
    }

    @Test
    public void shouldCreateZipArchiveFromPaths_IfSuccess(@TempDir Path tempDir) throws IOException {

        Path sourceDir = Files.createDirectories(tempDir.resolve("source"));

        Path file1 = Files.writeString(sourceDir.resolve("test.txt"), "some txt content for testing");
        Path file2 = Files.writeString(sourceDir.resolve("test2.pdf"), "some pdf content for testing");

        List<Path> filesToZip = List.of(file1, file2);
        Path zipPath = Files.createFile(tempDir.resolve("test.zip"));

        ProgressCallback progressCallback = mock(ProgressCallback.class);

        zipArchiveCreator.createZipArchiveFromPaths(filesToZip,zipPath,sourceDir, progressCallback);

        assertTrue(Files.exists(zipPath));
        assertTrue(Files.size(zipPath) > 0);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            assertNotNull(zipFile.getEntry("test.txt"));
            assertNotNull(zipFile.getEntry("test2.pdf"));
        }

        verify(progressCallback, atLeastOnce()).onUpdate(anyInt(), anyString());
    }
}
