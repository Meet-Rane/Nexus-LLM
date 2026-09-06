# Nexus Sovereign AI Workbench

React frontend for the local Spring agent and FastAPI RAG services.

## Local services

Start the services in this order:

1. Ollama on `http://localhost:11434` with the configured model installed.
2. RAG service on `http://localhost:8001`.
3. Spring backend on `http://localhost:8090`.
4. This frontend on `http://localhost:3000`.

```powershell
# RAG service
cd ..\rag-service
$env:HF_HUB_OFFLINE="1"
$env:TRANSFORMERS_OFFLINE="1"
.\venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8001

# Spring backend (use a model already installed in Ollama)
cd ..\ai-backend\sovereign-ai-workbench
$env:OLLAMA_MODEL="llama3.1:8b"
mvn spring-boot:run

# Frontend
cd ..\..\frontend-vite
npm install
npm run dev -- --host 127.0.0.1
```

The frontend defaults to the URLs above. Override them with `VITE_AGENT_URL` and `VITE_RAG_URL` when needed.
The backend enforces loopback-only Ollama and RAG endpoints by default. Only set `SOVEREIGN_ENFORCE_LOCAL_ONLY=false` for an explicitly approved non-sovereign environment.

## Checks

```powershell
npm run lint
npm run build
```

Chat uses Spring Server-Sent Events, Documents and Knowledge Base use the local RAG API, and Artifacts uses the current browser-session conversation ID. Agents and Models read Spring runtime-status endpoints. Security combines the application destination audit with an administrator-only Windows TCP monitor that distinguishes tracked Nexus processes from unrelated host traffic.
