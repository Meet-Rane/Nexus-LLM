FROM python:3.12-slim

WORKDIR /sandbox

RUN pip install --no-cache-dir \
    fpdf2 \
    reportlab \
    python-docx \
    openpyxl \
    pandas \
    pillow

RUN useradd -m sandboxuser

USER sandboxuser

CMD ["python", "--version"]