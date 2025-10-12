package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.FileTreeNodeDTO;
import com.jerzymaj.file_researcher_backend.exceptions.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class FileExplorerService {


    public FileTreeNodeDTO scanPath(Path path) {
        File file = validateFile(path);

        log.debug("SCANNED PATH: {} | isDirectory={} | isFile={}",
                file.getAbsolutePath(), file.isDirectory(), file.isFile());

        if (file.isFile()) {
            return buildNode(file, null);
        } else {
            return buildNode(file, getChildren(file));
        }
    }

    public FileTreeNodeDTO scanFilteredPath(Path path, String extension) {
        File file = validateFile(path);

        if (file.isFile()) {
            if (extension.equalsIgnoreCase(FileSetService.getExtension(path))) {
                return buildNode(file, null);
            } else {
                return null;
            }
        } else {
            List<FileTreeNodeDTO> children = getChildrenIfFiltered(file, extension);
            return buildNode(file, children);
        }
    }


    private File validateFile(Path path) {
        File file = path.toFile();

//        System.out.println("SCANNED PATH: " + file.getAbsolutePath() +
//                " | isDirectory=" + file.isDirectory() +
//                " | isFile=" + file.isFile());

        if (!file.exists()) {
            throw new PathNotFoundException("Path " + path + " doesn't exist");
        }

        if (!file.canRead()) {
            throw new IllegalArgumentException("Path " + path + " is not readable");
        }

        return file;
    }

    private FileTreeNodeDTO buildNode(File file, List<FileTreeNodeDTO> children) {
        return FileTreeNodeDTO.builder()
                .name(file.getName())
                .path(file.getAbsolutePath())
                .directory(file.isDirectory())
                .size(file.isFile() ? file.length() : null)
                .children(file.isDirectory() ? (children != null ? children : List.of()) : null)
                .build();
    }

    private List<FileTreeNodeDTO> getChildren(File directory) {
        File[] files = directory.listFiles();

        if (files == null) {
            throw new IllegalArgumentException("Unable to list files in directory: " + directory.getAbsolutePath());
        }

        return Arrays.stream(files)
                .map(file -> scanPath(file.toPath()))
                .toList();
    }

    private List<FileTreeNodeDTO> getChildrenIfFiltered(File directory, String extension) {
        File[] files = directory.listFiles();

        if (files == null) {
            throw new IllegalArgumentException("Unable to list files in directory: " + directory.getAbsolutePath());
        }

        return Arrays.stream(files)
                .map(f -> scanFilteredPath(f.toPath(), extension))
                .filter(Objects::nonNull)
                .toList();
    }

}
