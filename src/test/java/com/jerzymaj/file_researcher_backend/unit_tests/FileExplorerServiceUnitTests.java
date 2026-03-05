package com.jerzymaj.file_researcher_backend.unit_tests;

import com.jerzymaj.file_researcher_backend.DTOs.ScanPathResponseDTO;
import com.jerzymaj.file_researcher_backend.services.FileExplorerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FileExplorerServiceUnitTests {

    FileExplorerService fileExplorerService;

    MockMultipartFile file1;
    MockMultipartFile file2;
    MockMultipartFile[]  files;

    @BeforeEach
    public void setUp() {
        fileExplorerService = new FileExplorerService();

        file1 = new MockMultipartFile("files", "test1.txt",
                "text/plain", "content1".getBytes());
        file2 = new MockMultipartFile("files", "directory/test2.pdf",
                "text/plain", "content2".getBytes());
        files = new MockMultipartFile[]{file1, file2};
    }

    @Test
    public void shouldReturnScanPathResponseDTO_ForFilesWithoutFilter() {

        ScanPathResponseDTO rootNode = fileExplorerService.scanUploadedFiles(files, null);

        assertThat(rootNode.getName()).isEqualTo("Root");
        assertThat(rootNode.isDirectory()).isTrue();
        assertThat(rootNode.getChildren()).hasSize(2);

        List<ScanPathResponseDTO> children = rootNode.getChildren();

        assertThat(children.getFirst().getName()).isEqualTo("test1.txt");
        assertThat(children.getFirst().isDirectory()).isFalse();
        assertThat(children.getFirst().getSize()).isEqualTo(file1.getSize());

        ScanPathResponseDTO pdfNode = rootNode.getChildren().getLast().getChildren().getFirst();

        assertThat(pdfNode.getName()).isEqualTo("test2.pdf");
        assertThat(pdfNode.isDirectory()).isFalse();
        assertThat(pdfNode.getSize()).isEqualTo(file2.getSize());
    }

    @Test
    public void shouldReturnScanPathResponseDTO_ForFilesWithFilter() {

        ScanPathResponseDTO rootNode = fileExplorerService.scanUploadedFiles(files, "txt");

        assertThat(rootNode.getName()).isEqualTo("Root");
        assertThat(rootNode.isDirectory()).isTrue();
        assertThat(rootNode.getChildren()).hasSize(1);

        ScanPathResponseDTO child = rootNode.getChildren().getFirst();

        assertThat(child.getName()).isEqualTo("test1.txt");
        assertThat(child.isDirectory()).isFalse();
        assertThat(child.getSize()).isEqualTo(file1.getSize());
    }
}
