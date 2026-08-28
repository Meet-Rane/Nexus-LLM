import axios from "axios";

export const AGENT_URL = import.meta.env.VITE_AGENT_URL || "http://localhost:8080";

const client = axios.create({ baseURL: AGENT_URL, timeout: 60000 });

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
  const { data } = await client.post("/ingest", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

/** Optional: ping the agent to confirm the on-prem connection is alive. */
export async function pingAgent() {
  const { data } = await client.get("/health");
  return data;
}

export default client;
