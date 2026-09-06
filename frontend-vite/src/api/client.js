import axios from "axios";

export const AGENT_URL = import.meta.env.VITE_AGENT_URL || "http://localhost:8090";
export const RAG_URL = import.meta.env.VITE_RAG_URL || "http://localhost:8001";

const agentClient = axios.create({ baseURL: AGENT_URL, timeout: 300000 });
// Scanned manuals can take several minutes to OCR on a CPU-only workstation.
const ragClient = axios.create({ baseURL: RAG_URL, timeout: 600000 });

const CONVERSATION_KEY = "nexus.activeConversationId";
const AUTH_KEY = "nexus.authSession";

export function getStoredAuth() {
  try {
    return JSON.parse(window.localStorage.getItem(AUTH_KEY) || "null");
  } catch {
    return null;
  }
}

export function storeAuth(auth) {
  if (auth) window.localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
  else window.localStorage.removeItem(AUTH_KEY);
}

agentClient.interceptors.request.use((config) => {
  const token = getStoredAuth()?.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

agentClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && !error?.config?.url?.startsWith("/api/auth/")) {
      storeAuth(null);
      window.dispatchEvent(new Event("nexus:unauthorized"));
    }
    return Promise.reject(error);
  },
);

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
  const controller = new AbortController();
  let timedOut = false;
  const timeoutId = window.setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, 300000);
  const forwardAbort = () => controller.abort(signal?.reason);
  signal?.addEventListener("abort", forwardAbort, { once: true });

  try {
    const response = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "text/event-stream",
        ...(getStoredAuth()?.accessToken ? { Authorization: `Bearer ${getStoredAuth().accessToken}` } : {}),
      },
      signal: controller.signal,
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
  } catch (error) {
    if (timedOut) {
      throw new Error("Agent request timed out after 5 minutes.", { cause: error });
    }
    throw error;
  } finally {
    window.clearTimeout(timeoutId);
    signal?.removeEventListener("abort", forwardAbort);
  }
}

export async function pingAgent() {
  const { data } = await agentClient.get("/ai/health");
  return data;
}

export async function login(credentials) {
  const { data } = await agentClient.post("/api/auth/login", credentials);
  return data;
}

export async function signup(account) {
  const { data } = await agentClient.post("/api/auth/signup", account);
  return data;
}

export async function getCurrentUser() {
  const { data } = await agentClient.get("/api/auth/me");
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

export async function getOsNetworkMonitor() {
  const { data } = await agentClient.get("/api/system/os-network-monitor");
  return data;
}

export async function resetOsNetworkMonitor() {
  const { data } = await agentClient.post("/api/system/os-network-monitor/reset");
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

export async function listDocuments() {
  const { data } = await ragClient.get("/ingest/documents");
  return data;
}

export async function deleteDocument(source) {
  const { data } = await ragClient.delete("/ingest/documents", { params: { source } });
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

export async function downloadArtifact(path, conversationId = getConversationId(), fileName) {
  const response = await agentClient.get(`/api/conversations/${encodeURIComponent(conversationId)}/artifacts/download`, {
    params: { path },
    responseType: "blob",
  });
  const objectUrl = window.URL.createObjectURL(response.data);
  const link = document.createElement("a");
  link.href = objectUrl;
  link.download = fileName || path.split(/[\\/]/).pop() || "artifact";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(objectUrl);
}

export default agentClient;
