# Nexus Sovereign AI Workbench — Frontend

Responsive React interface for the on-premise agentic AI workbench. The UI is intentionally self-contained: it uses system fonts and local assets, so loading the frontend does not require calls to a font CDN or any other external service.

## Run locally

```bash
npm install
npm run dev
```

The development server starts at `http://localhost:3000`.

## Local service configuration

Create a `.env.local` file when the services use non-default addresses:

```env
VITE_AGENT_URL=http://localhost:8080
VITE_RAG_URL=http://localhost:8001
```

The frontend expects:

- Agent API: `GET /health`, `POST /chat`
- RAG API: `GET /ingest/status`, `POST /ingest/file`
- Chat response: `{ "text": "...", "model_used": "...", "download_url": "..." }`
- Ingest response: `{ "source": "...", "chunks_stored": 10, "total_collection_size": 120 }`

## Product surfaces

- **Workbench** — multimodal task entry, agent responses, model routing and live execution trace
- **Documents** — local confidential file library and import flow
- **Knowledge** — collections, index health and retrieval testing
- **Agent runs** — auditable plans, tool events and execution history
- **Model registry** — open-weight model status and routing policy
- **Artifacts** — generated DOCX, PPTX, XLSX and code deliverables
- **Sovereignty** — network egress evidence, controls and audit logs

The operational data shown in non-chat screens is representative UI data until the corresponding telemetry endpoints are connected. Do not use placeholder monitor entries as the final air-gap proof; wire the security view to the host firewall or packet-capture service for the SIH demonstration.

## Verification

```bash
npm run lint
npm run build
```
