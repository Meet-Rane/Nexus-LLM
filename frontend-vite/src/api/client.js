import axios from "axios";

export const AGENT_URL = import.meta.env.VITE_AGENT_URL || "http://localhost:8080";
export const RAG_URL = import.meta.env.VITE_RAG_URL || "http://localhost:8001";

const client = axios.create({ baseURL: AGENT_URL, timeout: 60000 });
const ragClient = axios.create({ baseURL: RAG_URL, timeout: 60000 });

/**
 * Send a user message to the agent and get back its reply.
 * Expected response shape: { text, model_used, download_url? }
 */
export async function sendMessage(text, history = []) {
  const { data } = await client.post("/chat", { message: text, history });
  return data;
}

/**
 * Upload a document into the agent's local knowledge base.
 * Expected response shape: { source, chunks_stored, total_collection_size }
 */
export async function ingestFile(file) {
  const form = new FormData();
  form.append("file", file);
  const { data } = await ragClient.post("/ingest/file", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

/** Read the size of the local document index. */
export async function getRagStatus() {
  const { data } = await ragClient.get("/ingest/status");
  return data;
}

/** Optional: ping the agent to confirm the on-prem connection is alive. */
export async function pingAgent() {
  const { data } = await client.get("/health");
  return data;
}

export default client;
