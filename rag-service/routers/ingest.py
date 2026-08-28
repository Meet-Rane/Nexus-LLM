"""
POST /ingest/file   — upload a document, OCR/extract, chunk, embed, store in ChromaDB
POST /ingest/text   — ingest raw text directly (useful for pre-loading SOPs via curl)
GET  /ingest/status — how many chunks are stored
"""
import uuid
from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel

from services.extractor import extract_text
from services.chunker import chunk_text
from services.vector_store import collection

router = APIRouter()


class TextIngestRequest(BaseModel):
    text: str
    source_name: str = "manual"


class IngestResponse(BaseModel):
    source: str
    chunks_stored: int
    total_collection_size: int


@router.post("/file", response_model=IngestResponse)
async def ingest_file(file: UploadFile = File(...)):
    """Upload a PDF, image, or .txt file — extracted, chunked, and embedded into ChromaDB."""
    contents = await file.read()
    try:
        text = extract_text(contents, file.filename)
    except ValueError as e:
        raise HTTPException(status_code=415, detail=str(e))

    if not text.strip():
        raise HTTPException(status_code=422, detail="No text could be extracted from the file.")

    chunks = chunk_text(text)
    _store_chunks(chunks, source=file.filename)

    return IngestResponse(
        source=file.filename,
        chunks_stored=len(chunks),
        total_collection_size=collection.count(),
    )


@router.post("/text", response_model=IngestResponse)
def ingest_text(req: TextIngestRequest):
    """Ingest raw text directly — handy for pre-loading SOP documents without a file upload."""
    chunks = chunk_text(req.text)
    _store_chunks(chunks, source=req.source_name)

    return IngestResponse(
        source=req.source_name,
        chunks_stored=len(chunks),
        total_collection_size=collection.count(),
    )


@router.get("/status")
def ingest_status():
    return {
        "total_chunks": collection.count(),
        "collection_name": collection.name,
    }


# ── helpers ──────────────────────────────────────────────────────────────────

def _store_chunks(chunks: list[str], source: str):
    if not chunks:
        return
    ids = [str(uuid.uuid4()) for _ in chunks]
    metadatas = [{"source": source, "chunk_index": i} for i, _ in enumerate(chunks)]
    collection.add(documents=chunks, ids=ids, metadatas=metadatas)