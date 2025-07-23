# File Researcher Backend

A Spring Boot backend service that allows users to upload sets of files, compress them into ZIP archives, send them via email, and track the sending history with full security and ownership control.

## Features

- User-based access control for all file and archive operations
- File set grouping and ZIP compression
- Email delivery of ZIP archives using `JavaMailSender`
- Full history tracking of send attempts (success and failure)
- Archive resend functionality
- Archive statistics and filtering
- Access restriction: only archive/file owners can manage or view their data
- SQL-enhanced queries via JPA for performance and sorting

---

## Tech Stack

- **Java 17+**
- **Spring Boot 3**
- **Spring Security**
- **Spring Data JPA**
- **Java Mail Sender**
- **MySQL**
- **JUnit + Mockito + Testcontainers (for integration and unit testing)**

---

## Module Structure

```text
|-- configuration/         # Cors and Security configurations
├── controllers/           # REST API controllers
├── DTOs/                  # Data Transfer Objects
├── exceptions/            # Custom exceptions
├── models/                # JPA entity models
├── repositories/          # Spring Data JPA Repos
├── services/              # Business logic
└── FileResearcherApplication.java
```

 Feel free to contribute or open issues  
--- 
 Created by  **Jerzy Maj**
