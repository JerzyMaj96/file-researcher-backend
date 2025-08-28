package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.FileTreeNodeDTO;
import com.jerzymaj.file_researcher_backend.exceptions.PathNotFoundException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
public class FileExplorerService {

    public FileTreeNodeDTO scanPath(Path path){
        File file = path.toFile();

        System.out.println("SCANNED PATH: " + file.getAbsolutePath() +
                " | isDirectory=" + file.isDirectory() +
                " | isFile=" + file.isFile());

        if(!file.exists()){
            throw new PathNotFoundException("Path " + path + " doesn't exist");
        }

        if (!file.canRead()) {
            throw new IllegalArgumentException("Path " + path + " is not readable");
        }

        FileTreeNodeDTO fileTreeNodeDTO = FileTreeNodeDTO.builder()
                .name(file.getName())
                .path(file.getAbsolutePath())
                .directory(file.isDirectory())
                .size(file.isFile() ? file.length() : null)
                .children(file.isDirectory() ? getChildren(file) : null)
                .build();

        return fileTreeNodeDTO;
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

}
