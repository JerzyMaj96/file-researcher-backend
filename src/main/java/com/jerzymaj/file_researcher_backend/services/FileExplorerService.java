package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.ScanPathResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Service
public class FileExplorerService {

    /**
     * Processes an array of uploaded files and builds a virtual directory tree
     * based on their original file paths. Optionally filters files by extension.
     *
     * @param files     the array of multipart files uploaded from the client
     * @param extension the file extension to filter by (e.g., ".txt").
     *                  If null, all files are included.
     * @return a {@link ScanPathResponseDTO} representing the root of the virtual file tree
     */
    public ScanPathResponseDTO scanUploadedFiles(MultipartFile[] files, String extension) {
        ScanPathResponseDTO root = ScanPathResponseDTO.builder()
                .name("Root")
                .directory(true)
                .children(new ArrayList<>())
                .build();

        for (MultipartFile file : files) {
            String fullPath = file.getOriginalFilename();
            if (Objects.isNull(fullPath)) {
                continue;
            }

            if (extension == null || extension.isBlank() || fullPath.endsWith(extension)) {
                addFileToTree(root, file, fullPath);
            }
        }

        return root;
    }

    /**
     * A helper method that inserts a file into the tree structure.
     * It splits the full path into segments, creates missing directory nodes,
     * and attaches the file node to the appropriate parent.
     *
     * @param root     the root node of the tree where the file will be added
     * @param file     the multipart file object containing file metadata
     * @param fullPath the original full path of the file used to replicate the folder structure
     */
    private void addFileToTree(ScanPathResponseDTO root, MultipartFile file, String fullPath) {
        if (fullPath == null) {
            return;
        }

        String[] pathParts = fullPath.split("/");

        ScanPathResponseDTO currNode = root;

        String currPath = "";

        for (int i = 0; i < pathParts.length - 1; i++) {
            String directoryName = pathParts[i];
            currPath = currPath.isEmpty() ? directoryName : currPath + "/" + directoryName;

            ScanPathResponseDTO nextNode = currNode.getChildren()
                    .stream()
                    .filter(child -> child.isDirectory() && child.getName().equals(directoryName))
                    .findFirst()
                    .orElse(null);

            if (nextNode == null) {
                nextNode = ScanPathResponseDTO.builder()
                        .name(directoryName)
                        .path(currPath)
                        .directory(true)
                        .children(new ArrayList<>())
                        .build();

                currNode.getChildren().add(nextNode);
            }

            currNode = nextNode;
        }

        ScanPathResponseDTO fileNode = ScanPathResponseDTO.builder()
                .name(pathParts[pathParts.length - 1])
                .path(fullPath)
                .size(file.getSize())
                .directory(false)
                .build();

        currNode.getChildren().add(fileNode);
    }

}
