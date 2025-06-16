package com.jerzymaj.file_researcher_backend.controllers;

import com.jerzymaj.file_researcher_backend.services.FileSetService;
import com.jerzymaj.file_researcher_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/file-researcher/users/{userId}/file-sets")
@RequiredArgsConstructor
public class FileSetController {
//CONTROLLER PROPERTIES --------------------------------------------------------

    private final FileSetService fileSetService;
    private final UserService userService;

//METHODS ------------------------------------------------------------------------


}
