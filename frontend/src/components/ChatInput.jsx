import React, { useState, useRef } from "react";
import { ingestFile } from "../api/client";

export function ChatInput({ onSend, loading }) {
  const [text, setText] = useState("");
  const [ingesting, setIngesting] = useState(false);
  const [ingestMsg, setIngestMsg] = useState(null);
  const fileRef = useRef();

  const handleSend = () => {
    if (!text.trim() || loading) return;
    onSend(text.trim());
    setText("");
  };

  const handleKey = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setIngesting(true);
    setIngestMsg(null);
    try {
      const result = await ingestFile(file);
      setIngestMsg(`✅ "${result.source}" ingested — ${result.chunks_stored} chunks added (${result.total_collection_size} total)`);
    } catch (err) {
      setIngestMsg(`❌ Ingest failed: ${err.response?.data?.detail || err.message}`);
    } finally {
      setIngesting(false);
      e.target.value = "";
    }
  };

  return (
    <div style={{ padding: "12px 16px", borderTop: "1px solid #1e293b", background: "#0f172a" }}>
      {/* Ingest status message */}
      {ingestMsg && (
        <div
          style={{
            marginBottom: "8px",
            padding: "8px 12px",
            borderRadius: "8px",
            background: "#1e293b",
            color: "#94a3b8",
            fontSize: "12px",
          }}
        >
          {ingestMsg}
        </div>
      )}

      <div style={{ display: "flex", gap: "8px", alignItems: "flex-end" }}>
        {/* Document upload button */}
        <button
          onClick={() => fileRef.current.click()}
          disabled={ingesting}
          title="Upload document to knowledge base"
          style={{
            padding: "10px 14px",
            borderRadius: "10px",
            border: "1px solid #334155",
            background: "#1e293b",
            color: "#94a3b8",
            cursor: ingesting ? "wait" : "pointer",
            fontSize: "18px",
            flexShrink: 0,
          }}
        >
          {ingesting ? "⏳" : "📎"}
        </button>
        <input
          ref={fileRef}
          type="file"
          accept=".pdf,.png,.jpg,.jpeg,.tiff,.txt"
          style={{ display: "none" }}
          onChange={handleFileUpload}
        />

        {/* Text input */}
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKey}
          placeholder="Ask the agent, or upload a document with 📎 to add it to the knowledge base…"
          rows={2}
          style={{
            flex: 1,
            padding: "10px 14px",
            borderRadius: "10px",
            border: "1px solid #334155",
            background: "#1e293b",
            color: "#f1f5f9",
            fontSize: "14px",
            resize: "none",
            outline: "none",
            lineHeight: "1.5",
          }}
        />

        {/* Send button */}
        <button
          onClick={handleSend}
          disabled={loading || !text.trim()}
          style={{
            padding: "10px 18px",
            borderRadius: "10px",
            border: "none",
            background: loading ? "#334155" : "#2563eb",
            color: "#fff",
            fontWeight: 700,
            fontSize: "14px",
            cursor: loading ? "wait" : "pointer",
            flexShrink: 0,
            height: "44px",
          }}
        >
          {loading ? "…" : "Send"}
        </button>
      </div>
    </div>
  );
}