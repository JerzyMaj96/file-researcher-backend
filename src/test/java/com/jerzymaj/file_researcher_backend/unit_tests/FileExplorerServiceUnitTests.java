package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.FileTreeNodeDTO;
import com.jerzymaj.file_researcher_backend.exceptions.PathNotFoundException;
import com.jerzymaj.file_researcher_backend.services.FileExplorerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileExplorerServiceUnitTests {

    FileExplorerService fileExplorerService;

    @BeforeEach
    public void setUp(){
        fileExplorerService = new FileExplorerService();
    }

    @Test
    public void shouldReturnFileTreeNodeDTO_ForFile(@TempDir Path tempDir) throws IOException {

        Path testFile = Files.createFile(tempDir.resolve("test.txt"));
        Files.writeString(testFile, "Hello World");

        FileTreeNodeDTO actualResult = fileExplorerService.scanPath(testFile);

        assertThat(actualResult.getName()).isEqualTo("test.txt");
        assertThat(actualResult.getPath()).isEqualTo(testFile.toFile().getAbsolutePath());
        assertThat(actualResult.isDirectory()).isFalse();
        assertThat(actualResult.getSize()).isEqualTo(Files.size(testFile));
        assertThat(actualResult.getChildren()).isNull();
    }

    @Test
    public void shouldReturnFileTreeNodeDTO_WithChildren_ForDirectory(@TempDir Path tempDir) throws IOException {

        Path subDir = Files.createDirectory(tempDir.resolve("dir"));
        Files.createFile(subDir.resolve("file1.txt"));
        Files.createFile(subDir.resolve("file2.txt"));

        FileTreeNodeDTO actualResult = fileExplorerService.scanPath(subDir);

        assertThat(actualResult.getName()).isEqualTo("dir");
        assertThat(actualResult.isDirectory()).isTrue();
        assertThat(actualResult.getChildren()).hasSize(2);
    }

    @Test
    public void shouldThrowPathNotFoundException_ForNonExistingPath() {
        Path nonExistingPath = Path.of("some/nonexisting/path");

        assertThatThrownBy(() -> fileExplorerService.scanPath(nonExistingPath))
                .isInstanceOf(PathNotFoundException.class);
    }
}
