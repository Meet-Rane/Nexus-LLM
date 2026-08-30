FROM python:3.12-slim

WORKDIR /sandbox

RUN useradd -m sandboxuser

USER sandboxuser

CMD ["python", "--version"]