"""
POST /ingest/file   — upload a document, OCR/extract, chunk, embed, store in ChromaDB
POST /ingest/text   — ingest raw text directly (useful for pre-loading SOPs via curl)
GET  /ingest/status — how many chunks are stored
"""
import asyncio
import hashlib
import os
import threading
import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel
from pytesseract import TesseractNotFoundError
from pdf2image.exceptions import PDFInfoNotInstalledError

from services.extractor import extract_text, get_page_count
from services.chunker import chunk_text
from services.vector_store import collection

router = APIRouter()
_write_lock = threading.Lock()


class TextIngestRequest(BaseModel):
    text: str
    source_name: str = "manual"


class IngestResponse(BaseModel):
    source: str
    chunks_stored: int
    total_collection_size: int
    replaced_existing: bool = False


@router.post("/file", response_model=IngestResponse)
async def ingest_file(file: UploadFile = File(...)):
    """Upload a PDF, image, or .txt file — extracted, chunked, and embedded into ChromaDB."""
    contents = await file.read()
    try:
        # OCR and PDF parsing are CPU-heavy. Run them outside FastAPI's event
        # loop so health checks and retrieval stay responsive during indexing.
        text = await asyncio.to_thread(extract_text, contents, file.filename)
    except ValueError as e:
        raise HTTPException(status_code=415, detail=str(e))
    except TesseractNotFoundError as e:
        raise HTTPException(
            status_code=503,
            detail="This document requires OCR, but Tesseract was not found. Install Tesseract or configure TESSERACT_CMD, then restart the RAG service.",
        ) from e
    except PDFInfoNotInstalledError as e:
        raise HTTPException(
            status_code=503,
            detail="This scanned PDF requires Poppler (pdftoppm), but it was not found. Install Poppler, add it to PATH, then restart the RAG service.",
        ) from e
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Document processing failed: {e}") from e

    if not text.strip():
        raise HTTPException(status_code=422, detail="No text could be extracted from the file.")

    chunks = chunk_text(text)
    page_count = await asyncio.to_thread(get_page_count, contents, file.filename)
    added_at = datetime.now(timezone.utc).isoformat()
    document_id = hashlib.sha256(contents).hexdigest()[:24]
    metadata = {
        "document_id": document_id,
        "file_type": os.path.splitext(file.filename)[-1].lstrip(".").upper() or "FILE",
        "file_size": len(contents),
        "page_count": page_count,
        "added_at": added_at,
    }
    replaced_existing = await asyncio.to_thread(_replace_chunks, chunks, file.filename, metadata)

    return IngestResponse(
        source=file.filename,
        chunks_stored=len(chunks),
        total_collection_size=collection.count(),
        replaced_existing=replaced_existing,
    )


@router.post("/text", response_model=IngestResponse)
def ingest_text(req: TextIngestRequest):
    """Ingest raw text directly — handy for pre-loading SOP documents without a file upload."""
    chunks = chunk_text(req.text)
    replaced_existing = _replace_chunks(chunks, source=req.source_name, extra_metadata={
        "document_id": hashlib.sha256(req.text.encode("utf-8")).hexdigest()[:24],
        "file_type": "TEXT",
        "file_size": len(req.text.encode("utf-8")),
        "page_count": 1,
        "added_at": datetime.now(timezone.utc).isoformat(),
    })

    return IngestResponse(
        source=req.source_name,
        chunks_stored=len(chunks),
        total_collection_size=collection.count(),
        replaced_existing=replaced_existing,
    )


@router.get("/status")
def ingest_status():
    return {
        "total_chunks": collection.count(),
        "collection_name": collection.name,
    }


@router.get("/documents")
def list_documents():
    """List the persistent document library, grouped by original source file."""
    if collection.count() == 0:
        return []

    stored = collection.get(include=["metadatas"])
    grouped = {}
    for metadata in stored.get("metadatas") or []:
        metadata = metadata or {}
        source = metadata.get("source", "unknown")
        current = grouped.setdefault(source, {
            "id": metadata.get("document_id") or hashlib.sha256(source.encode("utf-8")).hexdigest()[:24],
            "name": source,
            "type": metadata.get("file_type") or os.path.splitext(source)[-1].lstrip(".").upper() or "FILE",
            "size": int(metadata.get("file_size") or 0),
            "pages": int(metadata.get("page_count") or 0),
            "addedAt": metadata.get("added_at"),
            "chunks": 0,
            "status": "Indexed",
        })
        current["chunks"] += 1

    return sorted(grouped.values(), key=lambda item: item.get("addedAt") or "", reverse=True)


@router.delete("/documents")
def delete_document(source: str):
    """Remove all indexed chunks for one exact source filename."""
    existing = collection.get(where={"source": source}, include=[])
    ids = existing.get("ids") or []
    if not ids:
        raise HTTPException(status_code=404, detail="Document was not found in the local knowledge base.")
    with _write_lock:
        collection.delete(ids=ids)
    return {"source": source, "chunks_deleted": len(ids), "total_collection_size": collection.count()}


# ── helpers ──────────────────────────────────────────────────────────────────

def _store_chunks(chunks: list[str], source: str, extra_metadata: dict | None = None):
    if not chunks:
        return
    ids = [str(uuid.uuid4()) for _ in chunks]
    common = extra_metadata or {}
    metadatas = [{**common, "source": source, "chunk_index": i} for i, _ in enumerate(chunks)]
    collection.add(documents=chunks, ids=ids, metadatas=metadatas)


def _replace_chunks(chunks: list[str], source: str, extra_metadata: dict | None = None) -> bool:
    """Atomically replace a source so repeated uploads never duplicate its chunks."""
    with _write_lock:
        existing = collection.get(where={"source": source}, include=[])
        existing_ids = existing.get("ids") or []
        if existing_ids:
            collection.delete(ids=existing_ids)
        _store_chunks(chunks, source, extra_metadata)
        return bool(existing_ids)
