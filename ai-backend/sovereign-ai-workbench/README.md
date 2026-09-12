# Sovereign AI Workbench — Agentic Backend Platform

**SIH 2026 | Problem Statement ID 26117**

*Sovereign On-Premise Agentic AI Workbench using Open-Weight Multimodal LLMs for Confidential Industrial Work*

**Target Organization**: Mangalore Refinery and Petrochemicals Limited (MRPL)
**Module Ownership**: AI Agent Loop, Dynamic Model Router, Sandboxed Tool Execution Engine, Document Generation, and Artifact File System.

---

## Overview

In high-security industrial environments such as refineries, defence manufacturing, and public sector undertakings, sensitive engineering data — P&ID drawings, heat and mass balance calculations, equipment inspection reports, vendor negotiations — cannot leave the organisation's network.

This platform is the **Sovereign Industrial AI Orchestration Engine** running entirely on-premise. It enables engineers and operations staff to perform complex knowledge work using open-weight large language models without any data leaving the local environment.

### Core Architectural Tenet

> The LLM is strictly the reasoning layer. Spring Boot is the sovereign execution, security, and governance controller.

The LLM never directly accesses the host filesystem, executes system commands, or initiates network calls. Spring Boot parses agent intent, enforces security boundaries, orchestrates on-premise models, and dispatches requests to isolated sandboxes and native document renderers.

```
+----------------------------------------------------------+
|                      React Frontend                      |
+----------------------------------------------------------+
              |                            ^
              | POST /ai/chat/stream       | Server-Sent Events
              | (JWT Bearer Token)         | (Router + Tool Status +
              v                            | Artifact Cards + Text)
+-----------------------------------------------------------------------+
|              Spring Boot Sovereign Backend (Java 21)                  |
|                                                                       |
|  Security & Identity      : JWT Token Filter, User Workspace Isolation|
|  Persistent Memory        : JDBC-Backed Spring AI Window Memory       |
|  Dynamic Model Router     : Intent Classifier (Coding/Reason/Docs)    |
|  Industrial System Prompt : MRPL Persona, File Path Concealment       |
+-----------------------------------------------------------------------+
         |                                        |
  Air-Gapped Sovereign Routing          Agentic Tool Invocation
         |                                        |
+--------+--------+                 +----------------------------+
|                 |                 | Sovereign AI Tool Suite    |
| Ollama (Local)  | NVIDIA NIM(Dev) |                            |
| qwen2.5-coder   | nemotron-3.5    | - Docker Python Sandbox    |
| llama3.1:8b     | llama-3.3-70b   | - Word and PDF Generator   |
| deepseek-r1:8b  |                 | - Workspace File CRUD      |
+-----------------+                 +----------------------------+
                                               |
                                    +--------------------+
                                    | MySQL + Disk       |
                                    | storage/artifacts/ |
                                    +--------------------+
```

---

## Subsystems

### 1. Dynamic Model Router with Explainable Routing

Rather than hardcoding a single model, the `ModelRouter` inspects user intent, domain keywords, and multi-turn conversation history to route each request to the optimal specialist model.

**Routing categories:**

| Category | Trigger | Example Model |
| :--- | :--- | :--- |
| `CODING` | Python, scripts, algorithms, debugging | `qwen2.5-coder:7b` |
| `CALCULATION_REASONING` | LMTD, thermodynamics, mass/energy balance | `deepseek-r1:8b` |
| `DOCUMENT_APPROVAL` | Approval notes, memos, inspection reports | `llama3.1:8b` |
| `GENERAL` | General queries, knowledge lookup | Default general model |

Every routing decision emits a `ROUTER` event over the SSE stream explaining the selection rationale, for example: *"Selected coding specialist: qwen2.5-coder:7b (Trigger: software engineering and script generation)"*. This gives engineers full observability into model selection during a session.

Model-to-category mappings are fully externalised in `application.properties`. Switching models requires no code changes.

---

### 2. Industrial Sovereign Persona and Agentic Loop

- **MRPL Sovereign Persona**: System prompt instructs the agent to follow industrial engineering conventions, use standard units (degrees C, bar, kg/h, kW), and structure formal deliverables with sections: Subject, Background, Technical Evaluation, Safety and Compliance, and Recommendation.
- **File Path Concealment**: All internal server paths (`storage/artifacts/...`, `output/...`) are sanitised. The LLM references files only by their clean filenames (for example, `reboiler_spec.pdf`). The SSE stream delivers structured download cards to the frontend.
- **Context Window Guard**: The `read_file` tool truncates file content at 8,000 characters to prevent context overflow when reading large sensor logs or datasets.
- **Open-Weight Tool Recovery**: An interceptor in `AgentService` detects when a smaller open-weight model outputs a markdown JSON tool call block in its text response instead of a native tool token, parses the intent, and executes the corresponding tool directly. This prevents raw JSON from appearing in chat responses.

---

### 3. Tool Execution Suite

```
execute_python_code         Isolated Docker sandbox (30 second timeout, --network none)
create_formatted_document   Native Java PDF via OpenPDF, Word via Apache POI
create_file                 Saves datasets, code, CSVs, and configuration files
read_file                   Reads workspace files with context-window truncation guard
write_file                  Updates files in-place within the conversation workspace
list_files                  Lists all deliverables and sizes in the active workspace
```

#### Isolated Docker Python Sandbox

The `execute_python_code` tool runs generated code inside a purpose-built Docker image (`sovereign-python-sandbox`) with the following security constraints:

- `--network none`: Zero outbound network access from inside the sandbox.
- Memory limited to 256 MB, CPU limited to 1.0 core, PID limit of 100.
- Non-root execution under `sandboxuser`.
- 30-second hard timeout with `destroyForcibly()` to protect against infinite loops.
- Stdin closed immediately after process start to prevent interactive `input()` calls from blocking the server thread.
- Pre-installed libraries: `pandas`, `openpyxl`, `fpdf2`, `reportlab`, `python-docx`, `pillow`.

#### Native Document Generation

The `create_formatted_document` tool produces professional deliverables without the LLM writing Python scripts:

- **PDF**: Generated natively via OpenPDF with navy blue executive title banners, timestamps, page number footers, and automatic word-wrapping.
- **Word (DOCX)**: Generated via Apache POI with matching typography.
- **Inline Markdown Renderer**: Parses `**bold**`, `*italic*`, inline code, bullet points, and numbered lists into native document formatting runs.

---

### 4. Artifact and Workspace Storage

- **Conversation-Scoped Workspaces**: Each conversation has a dedicated directory at `storage/artifacts/{conversationId}/`.
- **In-Place Update Handling**: When a file is regenerated, the MySQL row and the file on disk are updated in-place using the primary key. No duplicate constraint violations are thrown.
- **REST Download and Preview API**: Binary download endpoint (`/download?path=...`) and text preview endpoint (`/content?path=...`) for the frontend.

---

### 5. Multi-Tenant Authentication and Persistent Memory

- **JWT Authentication**: Spring Security with BCrypt password hashing, custom `AuthTokenFilter`, and stateless session management.
- **Conversation Isolation**: All conversation queries use `findByConversationIdAndUser()`. A user cannot access another user's conversation even if the UUID is known.
- **JDBC-Backed Chat Memory**: Spring AI `MessageWindowChatMemory` persists the last 20 messages to MySQL. Conversations survive application restarts. The conversation UUID from the `conversations` table is used directly as the Spring AI memory key.

---

## Real-Time SSE Event Specification

The frontend connects to `POST /ai/chat/stream` and receives newline-delimited JSON events:

```json
{ "type": "ROUTER",           "model": "qwen2.5-coder:7b", "detail": "Selected coding specialist ..." }
{ "type": "TOOL_START",       "toolName": "execute_python_code", "detail": "Executing script in Docker sandbox" }
{ "type": "TOOL_COMPLETE",    "toolName": "execute_python_code", "detail": "Execution finished with exit code 0" }
{ "type": "ARTIFACT_CREATED", "artifact": { "id": 18, "fileName": "report.pdf", "artifactType": "PDF", "fileSize": 4948 } }
{ "type": "TEXT",             "content": "The simulation completed with the following results..." }
{ "type": "DONE" }
```

---

## REST API Reference

### Authentication

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/signup` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive a JWT token |

### Conversations (Requires Bearer Token)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/conversations?conversationName=` | Create a new conversation |
| `GET` | `/api/conversations` | List all conversations for the authenticated user |
| `GET` | `/api/conversations/{conversationId}` | Get conversation details |

### AI Agent

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/ai/chat/stream` | Streaming chat with the agent (SSE) |
| `POST` | `/ai/chat` | Synchronous chat response |
| `GET` | `/ai/history/{conversationId}` | Retrieve full message history |

### Artifacts

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/conversations/{id}/artifacts` | List all artifacts in a conversation |
| `GET` | `/api/conversations/{id}/artifacts/download?path=` | Download a file binary |
| `GET` | `/api/conversations/{id}/artifacts/content?path=` | Get file text content |
| `DELETE` | `/api/conversations/{id}/artifacts?path=` | Delete an artifact |

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| Language and Runtime | Java 21, Spring Boot 4.0.8, Spring AI 2.0.0 |
| Security | Spring Security 6, JJWT 0.11.5, BCrypt |
| Database and ORM | MySQL 8.0, Spring Data JPA, Hibernate, HikariCP |
| AI Providers | Ollama (On-Premise GPU), NVIDIA NIM API (OpenAI-Compatible) |
| Document Generation | OpenPDF 2.0.3, Apache POI 5.3.0 |
| Code Sandbox | Docker Engine, Python 3.12-slim |
| Reactive Streaming | Project Reactor, Server-Sent Events |

---

## Setup and Deployment

### Prerequisites

- Java 21 JDK (Eclipse Temurin recommended)
- Apache Maven 3.9+
- MySQL Server 8.0 with database `nexusai` created
- Docker Desktop running
- Ollama (for on-premise air-gapped deployment)

### 1. Database Initialisation

```sql
CREATE DATABASE nexusai;
```

### 2. Build the Python Sandbox Docker Image

```powershell
cd sandbox
docker build -t sovereign-python-sandbox .
```

### 3. Configuration

Create a `local.properties` file in the project root:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/nexusai
spring.datasource.username=root
spring.datasource.password=your_password

# On-Premise Air-Gapped Ollama
ai.provider=ollama
spring.ai.ollama.base-url=http://localhost:11434
ai.coding.model=qwen2.5-coder:7b
ai.general.model=llama3.1:8b
ai.reasoning.model=deepseek-r1:8b

# Docker
sandbox.docker.command=docker
```

### 4. Run with Maven

```powershell
mvn spring-boot:run
```

### 5. Build and Run as Standalone JAR

```powershell
mvn clean package -DskipTests
java -jar target/sovereign-ai-workbench-0.0.1-SNAPSHOT.jar
```

---

## Deployment Notes

**Dev Laptop (NVIDIA NIM)**: Set `ai.provider=nvidia` and provide `AI_KEY` with your NVIDIA API key.

**Presentation GPU Server (Air-Gapped)**: Set `ai.provider=ollama`, pull models via `ollama pull`, and set `OLLAMA_HOST=0.0.0.0` on the Ollama host. Open port 11434 in the Windows Firewall if the backend and Ollama run on separate machines.

---

## Module Contribution

This repository covers the following components of the full Nexus-LLM system:

| Component | Owner |
| :--- | :--- |
| AI Agent Loop, Model Router, Tool Suite | This repository |
| Document Retrieval, RAG Pipeline | Teammate |
| React Frontend | Teammate |
