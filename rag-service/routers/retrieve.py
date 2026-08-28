"""
GET /retrieve?query=<string>&top_k=<int>

Returns top-k relevant chunks from ChromaDB.
This is the endpoint the Spring Boot agent calls via its @Tool method.

Contract (locked with Person A):
  Request:  GET /retrieve?query=string&top_k=int
  Response: { "chunks": ["...", "...", "..."] }
"""
from fastapi import APIRouter, Query
from pydantic import BaseModel
from typing import List

from services.vector_store import collection

router = APIRouter()


class RetrieveResponse(BaseModel):
    chunks: List[str]
    sources: List[str]  # bonus metadata — which doc each chunk came from


@router.get("", response_model=RetrieveResponse)
def retrieve(
    query: str = Query(..., description="Search query from the agent"),
    top_k: int = Query(5, ge=1, le=20, description="Number of chunks to return"),
):
    if collection.count() == 0:
        return RetrieveResponse(chunks=[], sources=[])

    results = collection.query(
        query_texts=[query],
        n_results=min(top_k, collection.count()),
    )

    chunks = results["documents"][0]          # list of strings
    metadatas = results["metadatas"][0]       # list of dicts
    sources = [m.get("source", "unknown") for m in metadatas]

    return RetrieveResponse(chunks=chunks, sources=sources)