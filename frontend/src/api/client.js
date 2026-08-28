import axios from "axios";

const agentClient = axios.create({
  baseURL: process.env.REACT_APP_AGENT_URL || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
});

const ragClient = axios.create({
  baseURL: process.env.REACT_APP_RAG_URL || "http://localhost:8001",
});

/**
 * Send a chat message to the Spring Boot agent.
 * @param {string} message
 * @returns {Promise<{reply: string, model_used: string, download_url?: string}>}
 */
export async function sendChat(message) {
  const res = await agentClient.post("/chat", { message });
  return res.data;
}

/**
 * Ingest a file directly into the RAG service.
 * Used for the live "upload SOP" demo button.
 * @param {File} file
 * @returns {Promise<{source: string, chunks_stored: number, total_collection_size: number}>}
 */
export async function ingestFile(file) {
  const formData = new FormData();
  formData.append("file", file);
  const res = await ragClient.post("/ingest/file", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data;
}

/**
 * Check RAG service chunk count.
 */
export async function getRagStatus() {
  const res = await ragClient.get("/ingest/status");
  return res.data;
}