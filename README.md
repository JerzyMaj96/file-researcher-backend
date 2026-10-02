# File Researcher Backend

A Spring Boot backend that lets users upload sets of files, pack them into a ZIP archive **in the background**, send the archive by email, and follow the progress **live over WebSocket**. Every send attempt is recorded, and all data is scoped to the authenticated user.

---

## How it works

```
Client ── POST /file-sets/{id}/zip-archives/send-uploaded-files
   │
   ▼
ZipArchiveService
   1. FileStager copies uploaded files to a per-task temp directory (UUID)
   2. hands the job to ZipArchiveProcessor (@Async, dedicated thread pool)
   3. returns taskId immediately
   │
   ▼                                   ZipArchiveProcessor (background thread "zip-*")
Client subscribes to                     a. ZipArchiveCreator builds the ZIP ........ progress 0–90%
/topic/progress/{taskId}  ◄── STOMP ──   b. archive saved as PENDING
                                         c. ZipEmailSender sends the email .......... 95%
                                         d. ZipArchiveStatusService (@Transactional):
                                            archive → SUCCESS, file set → SENT ...... 100%
                                         e. finally: temp ZIP and staging dir removed
```

---

## Features

- **JWT authentication** – stateless; the user is resolved from the token on every request.
- **Ownership checks** – every operation on file sets, archives and history verifies that the resource belongs to the current user.
- **Background ZIP processing** – packing and sending run on a bounded thread pool, so the HTTP request returns right away.
- **Live progress** – percentage and status messages are pushed over WebSocket (STOMP); updates are throttled (only on percentage increase and at most every 150 ms).
- **Email delivery** – archives are sent via `JavaMailSender` over SMTP with TLS.
- **History and statistics** – every send attempt is logged (success or failure); stats and "large archive" queries are available per user.
- **Safe file handling** – uploaded file names are validated against path traversal, and temporary files are always cleaned up.

---

## Engineering notes

Problems found and solved while building and refactoring the project.

**1. `@Async` silently not working (self-invocation)**
The async method was called from another method of the same class, which bypasses Spring's proxy, so the whole ZIP-and-send flow ran synchronously in the request thread. Fixed by extracting it into a separate bean, `ZipArchiveProcessor`.

**2. Default executor shadowed by WebSocket executors**
After the fix, logs showed `@Async` tasks running on `SimpleAsyncTaskExecutor` (a new thread per task, unbounded). The STOMP broker registers its own executors, so Spring Boot did not create the default one. Added `AsyncConfig` with a bounded `ThreadPoolTaskExecutor` (`zip-` thread prefix).

**3. Atomic status updates**
The archive status (`SUCCESS`) and the file set status (`SENT`) must change together. `@Transactional` was ignored for the same proxy reason as above, so the writes ran separately. Moving them into `ZipArchiveStatusService` makes both updates a single transaction.

**4. Uploaded files disappearing before background processing**
`MultipartFile` content is removed when the HTTP request ends, while packing happens later in another thread. Files are first copied to a per-task staging directory, which is deleted in a `finally` block.

**5. Path traversal in uploaded file names**
File names come from the client and can contain `../`. The destination path is normalized and rejected if it resolves outside the staging directory.

**6. Progress tracking across multiple files**
Progress counters are shared between methods through a small `Progress` record holding `AtomicLong` / `AtomicInteger` (Java passes arguments by value). While refactoring, a throttling bug was fixed: the conditions were joined with `||` instead of `&&`, so the time limit had no effect.

**7. Gmail `552-5.7.0` security warning**
Gmail may flag ZIP attachments and return this error even though the message is delivered. This specific error is logged as a warning and the send is recorded as successful with a note in the history; every other error marks the archive as `FAILED`.

---

## Known limitations and next steps

- **Attachment size** – Gmail limits attachments to 25 MB, while uploads allow up to 200 MB per file. A better approach would be to store archives (e.g. in object storage) and email a download link.
- **SMTP 5xx handling** – treating `552-5.7.0` as success is a pragmatic trade-off; a dedicated `WARNING` status would be more accurate.
- **Thread pool sizing** – pool and queue sizes are fixed in code; in production they should come from configuration and be tuned under load, with explicit handling of rejected tasks (e.g. HTTP 503).

---

## Tech stack

- **Java 21**, **Spring Boot 3** (Web, Data JPA, Security, Mail, WebSocket)
- **JWT** (jjwt)
- **MySQL** (dev), **PostgreSQL** (prod), **H2** (tests)
- **JUnit 5**, **Mockito** – unit and integration tests
- **Docker** – multi-stage build, non-root runtime user

---

## API overview

All endpoints are prefixed with `/file-researcher`. Everything except login and registration requires a JWT (`Authorization: Bearer <token>`).

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/login` | Log in and receive a JWT |
| `POST` | `/users` | Register a new user |
| `GET` | `/users/authentication` | Get the current user |
| `DELETE` | `/users/delete-me` | Delete the current user |
| `POST` | `/explorer/upload` | Scan uploaded files and return a directory tree |
| `POST` | `/file-sets/upload` | Upload files and create a file set |
| `GET` | `/file-sets` | List the user's file sets |
| `GET` | `/file-sets/{id}` | Get a file set |
| `DELETE` | `/file-sets/{id}` | Delete a file set (cascades to archives and history) |
| `PATCH` | `/file-sets/{id}/status` | Update status |
| `PATCH` | `/file-sets/{id}/recipientEmail` | Update recipient email |
| `PATCH` | `/file-sets/{id}/name` | Update name |
| `PATCH` | `/file-sets/{id}/description` | Update description |
| `POST` | `/file-sets/{id}/zip-archives/send-uploaded-files` | Start ZIP creation and sending; returns `taskId` |
| `GET` | `/zip-archives` | List the user's archives |
| `GET` | `/file-sets/{id}/zip-archives` | List archives of a file set |
| `GET` | `/file-sets/{id}/zip-archives/{zipId}` | Get an archive |
| `DELETE` | `/file-sets/{id}/zip-archives/{zipId}` | Delete an archive |
| `GET` | `/zip-archives/stats` | Success / failure statistics |
| `GET` | `/zip-archives/large` | Archives above a size threshold |
| `GET` | `/zip-archives/history` | All send history of the user |
| `GET` | `/zip-archives/{zipId}/history` | Send history of an archive |
| `GET` | `/zip-archives/{zipId}/history/last-recipient` | Last recipient of an archive |
| `GET` | `/zip-archives/{zipId}/history/{historyId}` | Single history entry |
| `DELETE` | `/zip-archives/{zipId}/history/{historyId}` | Delete a history entry |

**WebSocket:** connect to `/ws` and subscribe to `/topic/progress/{taskId}`. Messages contain `percent` (0–100, or `-1` on error) and `status` (a human-readable message).

---

## Project structure

```
├── configuration/   # Security, JWT, CORS, WebSocket, async thread pool, API routes
├── controllers/     # REST controllers
├── DTOs/            # Request/response objects
├── exceptions/      # Custom exceptions and global exception handler
├── mapper/          # Entity ↔ DTO mapping
├── models/          # JPA entities and enums
├── repositories/    # Spring Data JPA repositories
├── security/        # AuthFacade, UserDetailsService
└── services/        # Business logic (staging, ZIP creation, processing, email, statuses)
```

---

## Getting started

### Run locally

Requirements: Java 21, MySQL.

Set the environment variables used by the `dev` profile:

```bash
export LOCAL_DB_USERNAME=...
export LOCAL_DB_PASSWORD=...
export LOCAL_MAIL_USERNAME=your_email@gmail.com
export LOCAL_MAIL_PASSWORD=your_app_password
export JWT_SECRET=your_base64_encoded_secret
```

Then:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080/file-researcher`.

### Run with Docker

The backend ships with a multi-stage `Dockerfile` (Maven build stage, slim JRE runtime, non-root user).

The full stack (backend, frontend, database and [Mailpit](https://mailpit.axllent.org/)) is started with Docker Compose from the parent project folder. In that setup the backend runs with the `dev-docker` profile, which expects a database service named `db` and a Mailpit service named `fr_mailpit`, so emails land in a local test inbox instead of being sent.

To build and run only the backend image:

```bash
docker build -t file-researcher-backend .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... \
  -e MAIL_USERNAME=... -e MAIL_PASSWORD=... \
  -e JWT_SECRET=... \
  file-researcher-backend
```

### Run tests

```bash
./mvnw test
```

---

## License

MIT – see [LICENSE](LICENSE).

Created by **Jerzy Maj**
