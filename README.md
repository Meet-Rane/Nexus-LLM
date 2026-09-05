# Nexus Local Workbench

This checkout contains the `codex/frontend-agent-engine` branch and is configured to run the complete Nexus stack locally.

## One-time setup

Open PowerShell in this directory and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-nexus.ps1
```

The setup installs frontend and RAG dependencies, caches the embedding model, downloads the configured general, coding, and engineering-reasoning Ollama models, writes ignored local configuration files, builds the Docker sandbox, and runs the frontend and backend checks.

The default local routes are:

- General and document work: `llama3.1:8b`
- Coding and data tasks: `qwen2.5-coder:7b`
- Engineering calculations: `deepseek-r1:8b`

On Windows, Docker Desktop requires both **Windows Subsystem for Linux** and **Virtual Machine Platform** to be enabled, followed by a restart. Wait for Docker Desktop to report that its engine is running before executing the setup command.

## Start Nexus

```powershell
powershell -ExecutionPolicy Bypass -File .\start-nexus.ps1
```

Then open <http://127.0.0.1:3000>.

The launcher starts missing services in the background and writes logs to:

```text
ai-backend/sovereign-ai-workbench/storage/runtime
```

## Stop Nexus

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-nexus.ps1
```

Only processes recorded by the launcher are stopped. An Ollama server that was already running before the launcher is left running.

## Local endpoints

- Frontend: <http://127.0.0.1:3000>
- RAG health: <http://127.0.0.1:8001/health>
- Agent health: <http://127.0.0.1:8090/ai/health>
- Ollama: <http://127.0.0.1:11434>

Docker is required for sandboxed code execution. Tesseract and Poppler are required for image and scanned-PDF OCR. Formatted PDF and Word artifact generation runs directly inside the Java backend and does not require Docker.
