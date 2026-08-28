import React from "react";

const AGENT_URL = process.env.REACT_APP_AGENT_URL || "http://localhost:8080";

export function ChatMessage({ message }) {
  const isUser = message.role === "user";

  return (
    <div
      style={{
        display: "flex",
        justifyContent: isUser ? "flex-end" : "flex-start",
        marginBottom: "12px",
      }}
    >
      <div
        style={{
          maxWidth: "72%",
          padding: "12px 16px",
          borderRadius: isUser ? "18px 18px 4px 18px" : "18px 18px 18px 4px",
          background: isUser ? "#2563eb" : "#1e293b",
          color: "#f1f5f9",
          fontSize: "14px",
          lineHeight: "1.6",
          boxShadow: "0 1px 4px rgba(0,0,0,0.3)",
        }}
      >
        {/* Message text */}
        <p style={{ margin: 0, whiteSpace: "pre-wrap" }}>{message.text}</p>

        {/* Model badge — only on assistant messages */}
        {!isUser && message.model_used && (
          <span
            style={{
              display: "inline-block",
              marginTop: "8px",
              padding: "2px 8px",
              borderRadius: "10px",
              fontSize: "11px",
              background: "#0f172a",
              color: "#94a3b8",
              border: "1px solid #334155",
            }}
          >
            🤖 {message.model_used}
          </span>
        )}

        {/* Download link — if agent generated a file */}
        {!isUser && message.download_url && (
          <div style={{ marginTop: "10px" }}>
            <a
              href={`${AGENT_URL}${message.download_url}`}
              target="_blank"
              rel="noreferrer"
              style={{
                display: "inline-block",
                padding: "6px 14px",
                borderRadius: "8px",
                background: "#16a34a",
                color: "#fff",
                fontSize: "13px",
                textDecoration: "none",
                fontWeight: 600,
              }}
            >
              ⬇ Download Document
            </a>
          </div>
        )}
      </div>
    </div>
  );
}