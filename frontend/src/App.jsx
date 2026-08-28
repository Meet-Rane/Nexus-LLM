import React, { useEffect, useRef } from "react";
import { useChat } from "./hooks/useChat";
import { ChatMessage } from "./components/ChatMessage";
import { ChatInput } from "./components/ChatInput";
import { getRagStatus } from "./api/client";

export default function App() {
  const { messages, loading, sendMessage } = useChat();
  const [ragChunks, setRagChunks] = React.useState(null);
  const bottomRef = useRef();

  // Auto-scroll to latest message
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Poll RAG chunk count every 5s so judges can see it update live
  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const s = await getRagStatus();
        setRagChunks(s.total_chunks);
      } catch {
        setRagChunks(null);
      }
    };
    fetchStatus();
    const id = setInterval(fetchStatus, 5000);
    return () => clearInterval(id);
  }, []);

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        height: "100vh",
        background: "#0a0f1e",
        color: "#f1f5f9",
        fontFamily: "'Inter', 'Segoe UI', sans-serif",
      }}
    >
      {/* Header */}
      <header
        style={{
          padding: "14px 24px",
          borderBottom: "1px solid #1e293b",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          background: "#0f172a",
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: "18px", fontWeight: 700, color: "#f1f5f9" }}>
            🔒 Sovereign AI Agent
          </h1>
          <p style={{ margin: 0, fontSize: "12px", color: "#64748b" }}>
            PS-117 · Fully Local · No External Calls
          </p>
        </div>

        {/* Live KB status badge */}
        <div
          style={{
            padding: "6px 14px",
            borderRadius: "20px",
            background: "#1e293b",
            border: "1px solid #334155",
            fontSize: "12px",
            color: "#94a3b8",
          }}
        >
          📚 KB:{" "}
          <span style={{ color: "#22c55e", fontWeight: 700 }}>
            {ragChunks === null ? "—" : `${ragChunks} chunks`}
          </span>
        </div>
      </header>

      {/* Message list */}
      <main
        style={{
          flex: 1,
          overflowY: "auto",
          padding: "20px 16px",
          display: "flex",
          flexDirection: "column",
        }}
      >
        {messages.map((msg, i) => (
          <ChatMessage key={i} message={msg} />
        ))}

        {/* Loading indicator */}
        {loading && (
          <div style={{ display: "flex", justifyContent: "flex-start", marginBottom: "12px" }}>
            <div
              style={{
                padding: "12px 18px",
                borderRadius: "18px 18px 18px 4px",
                background: "#1e293b",
                color: "#64748b",
                fontSize: "14px",
              }}
            >
              ⏳ Agent thinking…
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </main>

      {/* Input */}
      <ChatInput onSend={sendMessage} loading={loading} />
    </div>
  );
}