import { useState, useCallback } from "react";
import { sendChat } from "../api/client";

export function useChat() {
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      text: "Hello! I'm your sovereign AI agent. Upload a document or ask me anything — I'll search the knowledge base and generate the right output.",
      model_used: null,
      download_url: null,
    },
  ]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const sendMessage = useCallback(async (text) => {
    if (!text.trim()) return;

    const userMsg = { role: "user", text };
    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);
    setError(null);

    try {
      const data = await sendChat(text);
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          text: data.reply,
          model_used: data.model_used || null,
          download_url: data.download_url || null,
        },
      ]);
    } catch (err) {
      const errText =
        err.response?.data?.detail ||
        err.message ||
        "Agent unreachable — is Spring Boot running on port 8080?";
      setError(errText);
      setMessages((prev) => [
        ...prev,
        { role: "assistant", text: `⚠️ ${errText}`, model_used: null, download_url: null },
      ]);
    } finally {
      setLoading(false);
    }
  }, []);

  return { messages, loading, error, sendMessage };
}