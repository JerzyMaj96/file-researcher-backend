package com.jerzymaj.file_researcher_backend.services;

import com.jerzymaj.file_researcher_backend.DTOs.FileTreeNodeDTO;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
public class FileExplorerService {

    public FileTreeNodeDTO scanPath(Path path){
        File file = path.toFile();

        if(!file.exists()){
            throw new IllegalArgumentException("Path " + path + " doesn't exist");
        }

        FileTreeNodeDTO fileTreeNodeDTO = FileTreeNodeDTO.builder()
                .name(file.getName())
                .path(file.getAbsolutePath())
                .isDirectory(file.isDirectory())
                .size(file.isFile() ? file.length() : null)
                .children(file.isDirectory() ? getChildren(file) : null)
                .build();

        return fileTreeNodeDTO;
    }

    private List<FileTreeNodeDTO> getChildren(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return List.of();

        return Arrays.stream(files)
                .map(file -> scanPath(file.toPath()))
                .toList();
    }
}
