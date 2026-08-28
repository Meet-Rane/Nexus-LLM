"""
Splits extracted text into overlapping chunks for embedding.
~300 words per chunk, 50-word overlap so context isn't lost at boundaries.
"""
from typing import List


def chunk_text(text: str, chunk_size: int = 300, overlap: int = 50) -> List[str]:
    words = text.split()
    if not words:
        return []

    chunks = []
    start = 0
    while start < len(words):
        end = start + chunk_size
        chunk = " ".join(words[start:end])
        chunks.append(chunk)
        start += chunk_size - overlap  # slide window with overlap

    return chunks