package com.jerzymaj.file_researcher_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FileResearcherBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileResearcherBackendApplication.class, args);
	}

}
