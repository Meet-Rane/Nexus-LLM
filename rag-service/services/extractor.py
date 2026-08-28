"""
Extracts plain text from uploaded files.
- Text PDFs  → pdfplumber (fast, accurate)
- Scanned PDFs / images → pdf2image + pytesseract (OCR)
- Plain .txt files → direct read
"""
import io
import os
import pytesseract
import pdfplumber
from pdf2image import convert_from_bytes
from PIL import Image


def _is_scanned_pdf(pdf_bytes: bytes) -> bool:
    """Heuristic: if pdfplumber extracts <50 chars across all pages, treat as scanned."""
    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        total_text = "".join(
            (page.extract_text() or "") for page in pdf.pages
        )
    return len(total_text.strip()) < 50


def extract_text(file_bytes: bytes, filename: str) -> str:
    ext = os.path.splitext(filename)[-1].lower()

    if ext == ".txt":
        return file_bytes.decode("utf-8", errors="replace")

    if ext == ".pdf":
        if _is_scanned_pdf(file_bytes):
            return _ocr_pdf(file_bytes)
        else:
            return _extract_text_pdf(file_bytes)

    if ext in {".png", ".jpg", ".jpeg", ".tiff", ".bmp"}:
        return _ocr_image(file_bytes)

    raise ValueError(f"Unsupported file type: {ext}")


def _extract_text_pdf(pdf_bytes: bytes) -> str:
    pages = []
    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        for page in pdf.pages:
            pages.append(page.extract_text() or "")
    return "\n\n".join(pages)


def _ocr_pdf(pdf_bytes: bytes) -> str:
    images = convert_from_bytes(pdf_bytes, dpi=300)
    texts = [pytesseract.image_to_string(img) for img in images]
    return "\n\n".join(texts)


def _ocr_image(img_bytes: bytes) -> str:
    img = Image.open(io.BytesIO(img_bytes))
    return pytesseract.image_to_string(img)