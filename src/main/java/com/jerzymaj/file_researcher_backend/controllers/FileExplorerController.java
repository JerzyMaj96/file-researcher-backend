package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.DTOs.FileTreeNodeDTO;
import com.jerzymaj.file_researcher_backend.services.FileExplorerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/file-researcher/explorer")
@RequiredArgsConstructor
public class FileExplorerController {
//CONTROLLER PROPERTIES ---------------------------------------------------------------------------

    private final FileExplorerService fileExplorerService;

//METHODS -----------------------------------------------------------------------------------------

    @PostMapping("/scan")
    public ResponseEntity<FileTreeNodeDTO> scanPath(@RequestBody Map<String, String> request){
        String pathString = request.get("path");

        if(pathString == null || pathString.isBlank()){
            return ResponseEntity.badRequest().build();
        }

        Path path = Path.of(pathString);
        FileTreeNodeDTO scannedPaths = fileExplorerService.scanPath(path);

        return ResponseEntity.ok(scannedPaths);
    }
}
