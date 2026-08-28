import React, { useState, useRef } from "react";
import { ingestFile } from "../api/client";

export function ChatInput({ onSend, loading }) {
  const [text, setText] = useState("");
  const [ingesting, setIngesting] = useState(false);
  const [ingestMsg, setIngestMsg] = useState(null); // { ok: boolean, text: string }
  const fileRef = useRef();
  const textareaRef = useRef();

  const handleSend = () => {
    if (!text.trim() || loading) return;
    onSend(text.trim());
    setText("");
    if (textareaRef.current) textareaRef.current.style.height = "auto";
  };

  const handleKey = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const autoGrow = (e) => {
    setText(e.target.value);
    e.target.style.height = "auto";
    e.target.style.height = `${Math.min(e.target.scrollHeight, 160)}px`;
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setIngesting(true);
    setIngestMsg(null);
    try {
      const result = await ingestFile(file);
      setIngestMsg({
        ok: true,
        text: `"${result.source}" ingested — ${result.chunks_stored} chunks added (${result.total_collection_size} total)`,
      });
    } catch (err) {
      setIngestMsg({
        ok: false,
        text: `Ingest failed: ${err.response?.data?.detail || err.message}`,
      });
    } finally {
      setIngesting(false);
      e.target.value = "";
    }
  };

  return (
    <div className="border-t border-amber bg-surface px-4 py-3 sm:px-6">
      {/* Ingest status */}
      {ingestMsg && (
        <div
          className={`mb-2.5 flex items-center gap-2 rounded-lg border px-3 py-2 font-mono text-[12px] ${
            ingestMsg.ok
              ? "border-ok/30 bg-ok/10 text-ok"
              : "border-danger/30 bg-danger/10 text-danger"
          }`}
        >
          <span>{ingestMsg.ok ? "✓" : "✕"}</span>
          <span className="truncate">{ingestMsg.text}</span>
        </div>
      )}

      <div className="flex items-end gap-2">
        {/* Document upload */}
        <button
          type="button"
          onClick={() => fileRef.current.click()}
          disabled={ingesting}
          title="Add a document to the knowledge base"
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-edge bg-surface2 text-ink2 transition hover:border-amber/40 hover:text-amber disabled:cursor-wait disabled:opacity-60"
        >
          {ingesting ? (
            <span className="animate-pulseDot text-lg">⏳</span>
          ) : (
            <span className="text-lg">📎</span>
          )}
        </button>
        <input
          ref={fileRef}
          type="file"
          accept=".pdf,.png,.jpg,.jpeg,.tiff,.txt"
          className="hidden"
          onChange={handleFileUpload}
        />

        {/* Message input */}
        <textarea
          ref={textareaRef}
          value={text}
          onChange={autoGrow}
          onKeyDown={handleKey}
          placeholder="Ask the workbench, or attach a document with 📎 to add it to the knowledge base…"
          rows={1}
          className="flex-1 resize-none rounded-xl border border-edge bg-surface2 px-4 py-3 text-[14px] leading-relaxed text-ink placeholder-ink3 outline-none transition focus:border-amber/50"
        />

        {/* Send */}
        <button
          type="button"
          onClick={handleSend}
          disabled={loading || !text.trim()}
          className="h-11 shrink-0 rounded-xl bg-amber px-5 text-[14px] font-semibold text-base transition enabled:hover:brightness-110 disabled:cursor-not-allowed disabled:bg-surface2 disabled:text-ink3"
        >
          {loading ? "…" : "Send"}
        </button>
      </div>
    </div>
  );
}
