# File Researcher Backend

A Spring Boot backend service that allows users to upload sets of files, compress them into ZIP archives, send them via email, and track the sending history with full security and ownership control.

## Key Features

### 1. Asynchronous File Processing & ZIP Compression

The application handles file set grouping and the packing of large directory structures (including recursive folder scanning) without blocking the main server thread.

Non-blocking I/O: Utilizes the @Async annotation to execute resource-intensive tasks in the background.

Smart Compression: Intelligent file filtering (e.g., skipping duplicates, handling node_modules) and efficient ZIP generation.

### 2. Real-Time Progress Tracking (WebSockets)

Integrated WebSocket (STOMP) support provides immediate feedback to the frontend.

Live Updates: Users see an exact percentage progress bar (0-100%) for file processing and upload.

Status Broadcasting: Detailed status messages (e.g., "Processing: file.txt", "Sending email...") are pushed to a dedicated subscription channel.

### 3. Robust Email Delivery System

A complete solution for delivering archives, built on JavaMailSender and secure SMTP.

Secure Transport: Supports SMTP with authentication (e.g., Gmail App Password) and TLS.

Resiliency: Configurable timeouts prevent thread hanging on slow network connections.

Retry & Resend: Built-in functionality to resend previously generated archives without re-processing files.

### 4. Comprehensive History & Analytics

Full tracking of every operation within the system.

Audit Log: detailed history tracking of all send attempts, recording both successes and failures.

Statistics: Dashboard-ready data for archive statistics and filtering based on size, date, or status.

### 5. Security & Access Control

Strict data isolation policies ensure privacy and security.

User-Based Access: All file operations, archives, and history logs are strictly scoped to the authenticated user.

Ownership Validation: Users cannot view, manage, or delete data belonging to others.

### 6. Data Integrity & Performance

Advanced database management ensures consistency in a multi-threaded environment.

Transaction Management: Uses @Transactional and saveAndFlush to guarantee immediate and accurate status updates (SENT/FAILED) across asynchronous threads.

Data Consistency: Cascading deletion (CascadeType.ALL, orphanRemoval) ensures that deleting a File Set automatically cleans up all related archives and logs.

Optimized Queries: SQL-enhanced JPA queries for efficient data retrieval, sorting, and filtering.

---

## Tech Stack

- **Java 17+**
- **Spring Boot 3(Web, Data JPA, Mail, WebSocket)**
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
├── translator/            # DTO mappers
└── FileResearcherApplication.java
```

---

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/JerzyMaj96/file-researcher-backend.git
2. Configure your database and SMTP settings in application.properties or application.yml.
3. Build and run the application:
./mvnw clean install
./mvnw spring-boot:run

## Development Branch
This repository also includes an additional branch used for testing and experimenting with new or alternative versions of methods and application features.
It serves as a sandbox for exploring different implementation ideas before merging stable changes into the main branch.

## License

This project is licensed under the MIT License.


 Feel free to contribute or open issues  
--- 
 Created by  **Jerzy Maj**
