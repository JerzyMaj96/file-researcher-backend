package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.ScanPathResponseDTO;
import com.jerzymaj.file_researcher_backend.exceptions.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class FileExplorerService {

    public ScanPathResponseDTO scanUploadedFiles(MultipartFile[] files) {
        ScanPathResponseDTO root = ScanPathResponseDTO.builder()
                .name("Root")
                .directory(true)
                .children(new ArrayList<>())
                .build();

        for (MultipartFile file : files) {
            addFileToTree(root, file);
        }

        return root;
    }

    private void addFileToTree(ScanPathResponseDTO root, MultipartFile file) {
        String fullPath = file.getOriginalFilename();

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

//        /**
//         * Recursively scans a validated path and builds a tree of {@link ScanPathResponseDTO} nodes.
//         *
//         * @param path the validated path to scan
//         * @return a {@link ScanPathResponseDTO} containing information about the file or directory and its children
//         */
//
//        public ScanPathResponseDTO scanPath (Path path){
//            File file = validateFile(path);
//
//            log.debug("SCANNED PATH: {} | isDirectory={} | isFile={}",
//                    file.getAbsolutePath(), file.isDirectory(), file.isFile());
//
//            if (file.isFile()) {
//                return buildNode(file, null);
//            } else {
//                return buildNode(file, getChildren(file));
//            }
//        }
//
//        /**
//         * Recursively scans a validated path and builds a tree of {@link ScanPathResponseDTO} nodes,
//         * filtering files by the specified extension.
//         *
//         * @param path      the validated path to scan
//         * @param extension the file extension used for filtering (case-insensitive)
//         * @return a {@link ScanPathResponseDTO} containing information about the file or directory
//         * and its filtered children, or {@code null} if no files match the extension
//         */
//
//        public ScanPathResponseDTO scanFilteredPath (Path path, String extension){
//            File file = validateFile(path);
//
//            if (file.isFile()) {
//                if (extension.equalsIgnoreCase(FileSetService.getExtension(path))) {
//                    return buildNode(file, null);
//                } else {
//                    return null;
//                }
//            } else {
//                List<ScanPathResponseDTO> children = getChildrenIfFiltered(file, extension);
//                if (children.isEmpty()) {
//                    return null;
//                }
//                return buildNode(file, children);
//            }
//        }
//
//        /**
//         * Validates if the specified path exists and is readable.
//         *
//         * @param path the path to validate
//         * @return a {@link File} object corresponding to the path
//         * @throws PathNotFoundException    if the path does not exist
//         * @throws IllegalArgumentException if the path is not readable
//         */
//
//        private File validateFile (Path path){
//            File file = path.toFile();
//
////        System.out.println("SCANNED PATH: " + file.getAbsolutePath() +
////                " | isDirectory=" + file.isDirectory() +
////                " | isFile=" + file.isFile());
//
//            if (!file.exists()) {
//                throw new PathNotFoundException("Path " + path + " doesn't exist");
//            }
//
//            if (!file.canRead()) {
//                throw new IllegalArgumentException("Path " + path + " is not readable");
//            }
//
//            return file;
//        }
//
//        /**
//         * Builds a {@link ScanPathResponseDTO} node for the given file.
//         *
//         * @param file     the file or directory to convert
//         * @param children a list of child nodes (for directories), or {@code null} for files
//         * @return a {@link ScanPathResponseDTO} representing the file or directory
//         */
//
//        private ScanPathResponseDTO buildNode (File file, List < ScanPathResponseDTO > children){
//            return ScanPathResponseDTO.builder()
//                    .name(file.getName())
//                    .path(file.getAbsolutePath())
//                    .directory(file.isDirectory())
//                    .size(file.isFile() ? file.length() : null)
//                    .children(file.isDirectory() ? (children != null ? children : List.of()) : null)
//                    .build();
//        }
//
//        /**
//         * Recursively scans the contents of the given directory.
//         *
//         * @param directory the directory to scan
//         * @return a list of {@link ScanPathResponseDTO} representing the filtered children
//         * @throws IllegalArgumentException if the directory cannot be read
//         */
//
//        private List<ScanPathResponseDTO> getChildren (File directory){
//            File[] files = directory.listFiles();
//
//            if (files == null) {
//                throw new IllegalArgumentException("Unable to list files in directory: " + directory.getAbsolutePath());
//            }
//
//            return Arrays.stream(files)
//                    .map(file -> scanPath(file.toPath()))
//                    .toList();
//        }
//
//        /**
//         * Recursively scans the contents of the given directory, applying a file extension filter.
//         *
//         * @param directory the directory to scan
//         * @param extension the file extension used for filtering
//         * @return a list of {@link ScanPathResponseDTO} representing the filtered children
//         * @throws IllegalArgumentException if the directory cannot be read
//         */
//
//        private List<ScanPathResponseDTO> getChildrenIfFiltered (File directory, String extension){
//            File[] files = directory.listFiles();
//
//            if (files == null) {
//                throw new IllegalArgumentException("Unable to list files in directory: " + directory.getAbsolutePath());
//            }
//
//            return Arrays.stream(files)
//                    .map(f -> scanFilteredPath(f.toPath(), extension))
//                    .filter(Objects::nonNull)
//                    .toList();
//        }

}
