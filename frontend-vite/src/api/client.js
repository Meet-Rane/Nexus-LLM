import axios from "axios";

export const AGENT_URL = import.meta.env.VITE_AGENT_URL || "http://localhost:8090";
export const RAG_URL = import.meta.env.VITE_RAG_URL || "http://localhost:8001";

const agentClient = axios.create({ baseURL: AGENT_URL, timeout: 300000 });
const ragClient = axios.create({ baseURL: RAG_URL, timeout: 120000 });

const CONVERSATION_KEY = "nexus.activeConversationId";

export function getConversationId() {
  let conversationId = window.sessionStorage.getItem(CONVERSATION_KEY);
  if (!conversationId) {
    conversationId = window.crypto.randomUUID();
    window.sessionStorage.setItem(CONVERSATION_KEY, conversationId);
  }
  return conversationId;
}

export function startNewConversation() {
  const conversationId = window.crypto.randomUUID();
  window.sessionStorage.setItem(CONVERSATION_KEY, conversationId);
  return conversationId;
}

export async function sendMessage(text, conversationId = getConversationId()) {
  const { data } = await agentClient.get("/ai/chat", {
    params: { conversationId, message: text },
  });
  return { text: typeof data === "string" ? data : data?.text || "", model_used: null };
}

/**
 * Consume Spring's text/event-stream response and report each AgentStreamEvent.
 * Event types: ROUTER, TOOL_START, TOOL_COMPLETE, ARTIFACT_CREATED, TEXT, DONE, ERROR.
 */
export async function streamMessage(text, { conversationId = getConversationId(), onEvent, signal } = {}) {
  const url = new URL("/ai/chat/stream", AGENT_URL);
  url.searchParams.set("conversationId", conversationId);
  url.searchParams.set("message", text);

  const response = await fetch(url, {
    method: "GET",
    headers: { Accept: "text/event-stream" },
    signal,
  });

  if (!response.ok || !response.body) {
    throw new Error(`Agent request failed with status ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let completeText = "";
  let selectedModel = null;
  let lastArtifact = null;

  const dispatchBlock = (block) => {
    const data = block
      .split(/\r?\n/)
      .filter((line) => line.startsWith("data:"))
      .map((line) => line.slice(5).trimStart())
      .join("\n");
    if (!data) return;

    let event;
    try {
      event = JSON.parse(data);
    } catch {
      return;
    }

    if (event.type === "TEXT") completeText += event.content || "";
    if (event.type === "ROUTER") selectedModel = event.model;
    if (event.artifact) lastArtifact = event.artifact;
    onEvent?.(event);
  };

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
    const blocks = buffer.split(/\r?\n\r?\n/);
    buffer = blocks.pop() || "";
    blocks.forEach(dispatchBlock);
    if (done) break;
  }

  if (buffer.trim()) dispatchBlock(buffer);
  return { text: completeText, model_used: selectedModel, artifact: lastArtifact };
}

export async function pingAgent() {
  const { data } = await agentClient.get("/ai/health");
  return data;
}

export async function getSystemStatus() {
  const { data } = await agentClient.get("/api/system/status");
  return data;
}

export async function getNetworkAudit() {
  const { data } = await agentClient.get("/api/system/network-audit");
  return data;
}

export async function getChatHistory(conversationId = getConversationId()) {
  const { data } = await agentClient.get(`/ai/history/${encodeURIComponent(conversationId)}`);
  return data;
}

export async function ingestFile(file) {
  const form = new FormData();
  form.append("file", file);
  const { data } = await ragClient.post("/ingest/file", form);
  return data;
}

export async function getRagStatus() {
  const { data } = await ragClient.get("/ingest/status");
  return data;
}

export async function retrieveKnowledge(query, topK = 5) {
  const { data } = await ragClient.get("/retrieve", { params: { query, top_k: topK } });
  return data;
}

export async function listArtifacts(conversationId = getConversationId()) {
  const { data } = await agentClient.get(`/api/conversations/${encodeURIComponent(conversationId)}/artifacts`);
  return data;
}

export async function getArtifactContent(path, conversationId = getConversationId()) {
  const { data } = await agentClient.get(`/api/conversations/${encodeURIComponent(conversationId)}/artifacts/content`, {
    params: { path },
  });
  return data;
}

export function getArtifactDownloadUrl(path, conversationId = getConversationId()) {
  const url = new URL(`/api/conversations/${encodeURIComponent(conversationId)}/artifacts/download`, AGENT_URL);
  url.searchParams.set("path", path);
  return url.toString();
}

export default agentClient;
