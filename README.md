# File Researcher Backend

A Spring Boot backend service that allows users to upload sets of files, compress them into ZIP archives, send them via
email, and track the sending history with full security and ownership control.

---

## Key Features

### 1. Asynchronous File Processing & ZIP Compression

The application handles file set grouping and the packing of large directory structures (including recursive folder
scanning) without blocking the main server thread.

- **Non-blocking I/O:** Utilizes `@Async` to execute resource-intensive tasks in the background.
- **Smart Compression:** Intelligent file filtering (e.g., skipping duplicates, handling `node_modules`) and efficient ZIP generation.

### 2. Real-Time Progress Tracking (WebSockets)

Integrated WebSocket (STOMP) support provides immediate feedback to the frontend.

- **Live Updates:** Users see an exact percentage progress bar (0–100%) for file processing and upload.
- **Status Broadcasting:** Detailed status messages (e.g., `"Processing: file.txt"`, `"Sending email..."`) are pushed to a dedicated subscription channel (`/topic/progress/{taskId}`).

### 3. Robust Email Delivery System

A complete solution for delivering archives, built on `JavaMailSender` and secure SMTP.

- **Secure Transport:** Supports SMTP with authentication (e.g., Gmail App Password) and TLS.
- **Resiliency:** Configurable timeouts prevent thread hanging on slow network connections.
- **Retry & Resend:** Built-in functionality to resend previously generated archives without re-processing files.

### 4. Comprehensive History & Analytics

Full tracking of every operation within the system.

- **Audit Log:** Detailed history tracking of all send attempts, recording both successes and failures.
- **Statistics:** Dashboard-ready data for archive statistics and filtering based on size, date, or status.

### 5. Security & Access Control

Strict data isolation policies ensure privacy and security.

- **JWT Authentication:** Stateless authentication using JSON Web Tokens.
- **User-Based Access:** All file operations, archives, and history logs are strictly scoped to the authenticated user.
- **Ownership Validation:** Users cannot view, manage, or delete data belonging to others.

### 6. Data Integrity & Performance

Advanced database management ensures consistency in a multi-threaded environment.

- **Transaction Management:** Uses `@Transactional` and `saveAndFlush` to guarantee immediate and accurate status updates (`SENT`/`FAILED`) across asynchronous threads.
- **Data Consistency:** Cascading deletion (`CascadeType.ALL`, `orphanRemoval`) ensures that deleting a File Set automatically cleans up all related archives and logs.
- **Optimized Queries:** SQL-enhanced JPA queries for efficient data retrieval, sorting, and filtering.

---

## Technical Challenges & Solutions

### 1. Asynchronous State Management (Spring Proxy & Transactions)

**Problem:** During the ZIP creation and email dispatching process, updating the database status (e.g., from `ACTIVE` to
`SENT`) was failing due to Spring's AOP Proxy limitations. Internal method calls within the same service bypassed the
`@Transactional` context, preventing status persistence.

**Solution:** Decoupled the persistence logic into a dedicated `ZipArchiveStatusService`. This ensured that every
status change (Success/Failure) is handled in a clean, external transactional context, forcing Hibernate to flush
changes to the database even during complex asynchronous tasks.

### 2. SMTP Reliability & Provider-Specific Quirks (Gmail 552 5.7.0)

**Problem:** When deploying and testing, the `552 5.7.0` SMTP error was encountered. Gmail's security filters often flag
automated attachments (especially ZIPs containing system metadata like `.DS_Store`) as potential threats, even if the
email is physically delivered.

**Solution:**
- **Defensive Filtering:** Implemented logic to exclude OS-specific metadata and suspicious files from ZIP archives.
- **Resilient Exception Handling:** Developed a selective error-handling mechanism that differentiates between critical connection failures and provider-specific security warnings, ensuring the system correctly marks a task as completed if the message was accepted by the relay.

---

## Tech Stack

- **Java 21**
- **Spring Boot 3** (Web, Data JPA, Mail, WebSocket, Security)
- **JWT** (jjwt)
- **MySQL**
- **H2** (in-memory database for tests)
- **JUnit 5 + Mockito** (unit and integration testing)

---

## API Overview

All endpoints are prefixed with `/file-researcher`.

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/login` | Public | Authenticate and receive JWT token |
| `POST` | `/users` | Public | Register a new user |
| `GET` | `/users/authentication` | Required | Get current authenticated user |
| `DELETE` | `/users/delete-me` | Required | Delete current user |
| `POST` | `/file-sets/upload` | Required | Upload files and create a FileSet |
| `GET` | `/file-sets` | Required | Get all FileSets for current user |
| `GET` | `/file-sets/{id}` | Required | Get FileSet by ID |
| `DELETE` | `/file-sets/{id}` | Required | Delete FileSet |
| `PATCH` | `/file-sets/{id}/status` | Required | Update FileSet status |
| `PATCH` | `/file-sets/{id}/recipientEmail` | Required | Update recipient email |
| `POST` | `/file-sets/{id}/zip-archives/send-uploaded-files` | Required | Create and send ZIP archive |
| `GET` | `/file-sets/{id}/zip-archives` | Required | Get all ZIP archives for FileSet |
| `GET` | `/file-sets/{id}/zip-archives/{zipId}` | Required | Get ZIP archive by ID |
| `DELETE` | `/file-sets/{id}/zip-archives/{zipId}` | Required | Delete ZIP archive |
| `GET` | `/zip-archives/stats` | Required | Get ZIP sending statistics |
| `GET` | `/zip-archives/large` | Required | Get large ZIP archives |
| `GET` | `/zip-archives/{zipId}/history` | Required | Get send history for archive |
| `GET` | `/zip-archives/{zipId}/history/{histId}` | Required | Get single history entry |
| `DELETE` | `/zip-archives/{zipId}/history/{histId}` | Required | Delete history entry |
| `POST` | `/explorer/upload` | Required | Scan uploaded files |

**WebSocket:** Connect to `/ws` and subscribe to `/topic/progress/{taskId}` for real-time ZIP progress updates.

---

## Module Structure

```text
├── configuration/         # Security, CORS, JWT, API routes
├── controllers/           # REST API controllers
├── DTOs/                  # Data Transfer Objects and request records
├── exceptions/            # Custom exceptions and global exception handler
├── models/                # JPA entity models
├── repositories/          # Spring Data JPA repositories
├── security/              # AuthFacade, CustomUserDetailsService
├── services/              # Business logic
├── translator/            # DTO mappers
└── FileResearcherBackendApplication.java
```

---

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/JerzyMaj96/file-researcher-backend.git
   ```

2. Configure your database and SMTP settings in `application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/your_db
   spring.datasource.username=your_user
   spring.datasource.password=your_password

   spring.mail.host=smtp.gmail.com
   spring.mail.port=587
   spring.mail.username=your_email@gmail.com
   spring.mail.password=your_app_password

   app.jwt.secret=your_base64_encoded_secret
   app.jwt.expiration-ms=86400000
   ```

3. Build and run:
   ```bash
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

---

## Development Branch

This repository includes an additional branch used for testing and experimenting with new or alternative versions
of methods and features. It serves as a sandbox for exploring different implementation ideas before merging stable changes into the main branch.

---

## License

This project is licensed under the MIT License.

---

Created by **Jerzy Maj**
