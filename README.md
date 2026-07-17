# DocSync — Real-Time Collaborative Editor

> A Google Docs–style backend for simultaneous multi-user document editing,
> built with Spring Boot 4, WebSockets (STOMP), and PostgreSQL.

![Status](https://img.shields.io/badge/status-in%20progress-yellow)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/license-MIT-green)

---

# Overview

DocSync is a production-architected backend system that enables multiple users to edit a shared document simultaneously in real time. Users authenticate with JWT, create or join rooms, and receive live document updates broadcast via WebSocket (STOMP protocol).

The project is being built incrementally across six phases—from authentication and persistence through real-time editing, cursor tracking, and document versioning.

---

# Architecture

## Two-Layer State Model

```text
┌─────────────────────────────────────────────────────────┐
│ Client (Browser)                                        │
│ STOMP over WebSocket + REST (HTTP)                      │
└──────────────────┬──────────────────┬───────────────────┘
                   │ WebSocket        │ REST
                   ▼                  ▼
┌──────────────────────────────────────────────────────────┐
│ Spring Boot 4                                            │
│                                                          │
│ ┌────────────────────┐   ┌───────────────────────────┐   │
│ │ CollaborationService│   │ REST Controllers         │   │
│ │ (In-Memory Layer)   │   │ Auth / Room / Document   │   │
│ │                    │   └──────────┬────────────────┘   │
│ │ ConcurrentHashMap  │              │                    │
│ │ <UUID, RoomState>  │              │                    │
│ │ ReentrantLock      │              │                    │
│ │ AtomicLong version │              │                    │
│ └────────┬───────────┘              │                    │
│          │ Flush every 30 seconds   │                    │
│          ▼                          ▼                    │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ PostgreSQL (Spring Data JPA + Hibernate)             │ │
│ │                                                      │ │
│ │ users                                                │ │
│ │ rooms                                                │ │
│ │ room_participants                                    │ │
│ │ documents                                            │ │
│ │ document_versions                                    │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

## Real-Time Edit Flow

```text
User A types
     │
     ▼
STOMP SEND → /app/room/{roomId}/edit
     │
     ▼
StompAuthChannelInterceptor
(validates JWT principal)
     │
     ▼
CollaborationController.handleEdit()
(overwrites userId using trusted JWT)
     │
     ▼
CollaborationService.applyEdit()
     ├─ Acquire per-room ReentrantLock
     ├─ Validate client version
     ├─ Apply line delta
     ├─ Increment AtomicLong version
     ├─ Release lock
     └─ Broadcast update
           │
           ▼
SimpMessagingTemplate.convertAndSend("/topic/room/{roomId}")
           │
           ▼
All connected clients receive BroadcastEditMessage
```

---

# Tech Stack

| Layer | Technology |
|--------|------------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Real-Time | Spring WebSocket + STOMP |
| Security | Spring Security 6 + JWT |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Build Tool | Maven |

---

# Project Structure

```text
src/main/java/danielscode/docsync/

├── shared/         # BaseEntity, shared utilities
├── security/       # JWT, authentication, Spring Security
├── user/           # User entity, services, controllers
├── room/           # Room entities, repositories, services
├── document/       # Documents, versioning, persistence
├── collaboration/  # In-memory collaboration engine
└── websocket/      # STOMP configuration & controllers
```

---

# REST API

## Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register user |
| POST | `/api/auth/login` | Login and receive JWT |

## Rooms

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/rooms` | Create room |
| GET | `/api/rooms` | List active rooms |
| GET | `/api/rooms/{roomId}` | Room details |
| POST | `/api/rooms/{roomId}/join` | Join room |
| GET | `/api/rooms/{roomId}/presence` | Connected users |

## Documents

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/documents/{roomId}` | Retrieve document |
| PUT | `/api/documents/{roomId}/save` | Save document |

---

# WebSocket (STOMP)

| Direction | Destination | Description |
|-----------|-------------|-------------|
| Subscribe | `/topic/room/{roomId}` | Receive room updates |
| Publish | `/app/room/{roomId}/join` | User joins |
| Publish | `/app/room/{roomId}/leave` | User leaves |
| Publish | `/app/room/{roomId}/edit` | Send edits |
| Publish | `/app/room/{roomId}/cursor` | Cursor updates |
| Private | `/user/queue/errors` | User-specific errors |

---

# Key Design Decisions

### Per-room ReentrantLock

Each room owns its own `ReentrantLock`, ensuring edits are serialized while allowing different rooms to process edits concurrently.

### Unlock Before I/O

Locks are always released before broadcasting over WebSocket or writing to PostgreSQL to minimize contention.

### Optimistic Locking

Documents use Hibernate's `@Version` annotation to detect concurrent database updates.

### JWT Authentication

JWT authentication secures both REST endpoints and WebSocket connections, ensuring only authenticated users can collaborate.

---

# Database Schema

```sql
users
(id UUID PK, username, email, password_hash, created_at)

rooms
(id UUID PK, name, created_by → users, active, created_at)

room_participants
(room_id + user_id composite PK, joined_at)

documents
(id UUID PK, room_id UNIQUE → rooms,
content TEXT, version BIGINT, updated_at)

document_versions
(id UUID PK,
document_id → documents,
content,
saved_at,
saved_by → users)
```

---

# Getting Started

## Prerequisites

- Java 17+
- PostgreSQL
- Maven

## Clone

```bash
git clone https://github.com/danielxrodas/docsync.git
cd docsync
```

## Create Database

```bash
psql -U postgres -c "CREATE DATABASE docsync;"
```

## Configure

Edit:

```
src/main/resources/application.yml
```

Configure:

- PostgreSQL username
- PostgreSQL password
- JWT secret

## Run

```bash
./mvnw spring-boot:run
```

Flyway automatically applies all migrations.

---

# Development Roadmap

| Phase | Description | Status |
|------|-------------|--------|
| 1 | User Authentication + JWT | ✅ Complete |
| 2 | Rooms + Document Persistence | ✅ Complete |
| 3 | WebSocket + STOMP | 🔄 In Progress |
| 4 | Real-Time Editing | 🔄 In Progress |
| 5 | Cursor Tracking + Version History | ⏳ Planned |
| 6 | Testing | ⏳ Planned |

---

# Future Improvements

- Redis Pub/Sub for horizontal scaling
- Operational Transformation / CRDT support
- Rate limiting
- React frontend with Monaco Editor
- Docker deployment
- CI/CD with GitHub Actions

---

# Author

**Daniel Rodas**
