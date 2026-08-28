from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

load_dotenv()

from routers import ingest, retrieve

app = FastAPI(
    title="PS-117 RAG + OCR Service",
    description="Handles document ingestion (OCR + chunking + embedding) and retrieval for the agent.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # tighten before final demo if needed
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(ingest.router, prefix="/ingest", tags=["Ingest"])
app.include_router(retrieve.router, prefix="/retrieve", tags=["Retrieve"])


@app.get("/health")
def health():
    return {"status": "ok", "service": "rag-service"}