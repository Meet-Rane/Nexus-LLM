import { useRef, useState } from "react";
import { ArrowUp, CheckCircle2, LoaderCircle, Paperclip, ShieldCheck, XCircle } from "lucide-react";
import { ingestFile } from "../api/client";

export function ChatInput({ onSend, loading, compact = false }) {
  const [text, setText] = useState("");
  const [ingesting, setIngesting] = useState(false);
  const [ingestMessage, setIngestMessage] = useState(null);
  const fileRef = useRef(null);
  const textareaRef = useRef(null);

  const handleSend = () => {
    const prompt = text.trim();
    if (!prompt || loading) return;
    onSend(prompt);
    setText("");
    if (textareaRef.current) textareaRef.current.style.height = "auto";
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setIngesting(true);
    setIngestMessage({ ok: null, text: `Processing ${file.name} entirely on-device…` });
    try {
      const result = await ingestFile(file);
      setIngestMessage({
        ok: true,
        text: `${result.source || file.name} indexed · ${result.chunks_stored ?? "document"} local chunks`,
      });
    } catch (error) {
      setIngestMessage({
        ok: false,
        text: error?.response?.data?.detail || error?.message || "Document indexing failed",
      });
    } finally {
      setIngesting(false);
      event.target.value = "";
    }
  };

  const UploadStatusIcon = ingestMessage?.ok === true
    ? CheckCircle2
    : ingestMessage?.ok === false ? XCircle : LoaderCircle;

  return (
    <div className="w-full">
      {ingestMessage && (
        <div className={`mb-2 flex items-center gap-2 rounded-xl border px-3 py-2 text-[11px] ${
          ingestMessage.ok === true
            ? "border-ok/25 bg-ok/[0.055] text-ok"
            : ingestMessage.ok === false
              ? "border-danger/30 bg-danger/[0.06] text-danger"
              : "border-amber/25 bg-amber/[0.06] text-amber"
        }`}>
          <UploadStatusIcon size={14} className={ingesting ? "animate-spin" : ""} />
          <span className="min-w-0 flex-1 truncate">{ingestMessage.text}</span>
          <button type="button" onClick={() => setIngestMessage(null)} className="text-current opacity-60 hover:opacity-100" aria-label="Dismiss upload status">×</button>
        </div>
      )}

      <div className={`nexus-glow rounded-2xl border border-edge2 bg-surface2 p-3 transition focus-within:border-amber/55 ${compact ? "shadow-[0_-14px_34px_var(--color-base)]" : ""}`}>
        <textarea
          ref={textareaRef}
          value={text}
          onChange={(event) => {
            setText(event.target.value);
            event.target.style.height = "auto";
            event.target.style.height = `${Math.min(event.target.scrollHeight, 180)}px`;
          }}
          onKeyDown={(event) => {
            if (event.key === "Enter" && !event.shiftKey) {
              event.preventDefault();
              handleSend();
            }
          }}
          placeholder="Describe a task for Nexus…"
          rows={compact ? 1 : 2}
          className={`block max-h-[180px] w-full resize-none bg-transparent px-1 py-1 text-[14px] leading-6 text-ink outline-none placeholder:text-ink3 ${compact ? "min-h-[40px]" : "min-h-[64px]"}`}
          aria-label="Task prompt"
        />

        <div className="mt-2 flex items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-1.5 sm:gap-3">
            <button type="button" onClick={() => fileRef.current?.click()} disabled={ingesting} className="flex h-8 items-center gap-2 rounded-lg px-2 text-[11px] text-ink2 transition hover:bg-surface3 hover:text-ink disabled:cursor-wait disabled:opacity-50" title="Index a local document">
              {ingesting ? <LoaderCircle size={15} className="animate-spin" /> : <Paperclip size={15} />}
              <span className="hidden sm:inline">Attach</span>
            </button>
            <div className="flex items-center gap-1.5 text-[10px] text-ink3">
              <ShieldCheck size={13} className="text-ok" />
              <span className="hidden sm:inline">processed on-device</span>
              <span className="sm:hidden">local</span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <span className="hidden text-[9px] text-ink3 lg:inline">Enter to send · Shift+Enter for new line</span>
            <button type="button" onClick={handleSend} disabled={loading || !text.trim()} className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber text-white transition enabled:hover:bg-[#827bff] enabled:active:scale-95 disabled:bg-surface3 disabled:text-ink3" aria-label={loading ? "Agent is working" : "Send message"}>
              {loading ? <LoaderCircle size={16} className="animate-spin" /> : <ArrowUp size={17} strokeWidth={2.2} />}
            </button>
          </div>
        </div>
      </div>

      <input ref={fileRef} type="file" accept=".pdf,.png,.jpg,.jpeg,.tiff,.bmp,.txt,.docx,.xlsx,.xlsm,.pptx" className="hidden" onChange={handleFileUpload} />
    </div>
  );
}
