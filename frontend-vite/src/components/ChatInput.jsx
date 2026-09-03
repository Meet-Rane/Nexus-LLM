import { useRef, useState } from "react";
import { ArrowUp, FileUp, LoaderCircle, Paperclip, ShieldCheck, X } from "lucide-react";
import { ingestFile } from "../api/client";

export function ChatInput({ onSend, loading, compact = false }) {
  const [text, setText] = useState("");
  const [ingesting, setIngesting] = useState(false);
  const [attachment, setAttachment] = useState(null);
  const [notice, setNotice] = useState(null);
  const fileRef = useRef();
  const textareaRef = useRef();

  const handleSend = () => {
    if (!text.trim() || loading) return;
    onSend(text.trim());
    setText("");
    setAttachment(null);
    if (textareaRef.current) textareaRef.current.style.height = "auto";
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setAttachment(file);
    setIngesting(true);
    setNotice(null);
    try {
      const result = await ingestFile(file);
      setNotice({ ok: true, text: `${result.chunks_stored ?? "Document"} chunks indexed locally` });
    } catch {
      setNotice({ ok: false, text: "Local ingestion service is not running" });
    } finally {
      setIngesting(false);
      event.target.value = "";
    }
  };

  return (
    <div className={`composer ${compact ? "composer-compact" : ""}`}>
      {(attachment || notice) && (
        <div className="mb-2.5 flex flex-wrap items-center gap-2 px-1">
          {attachment && (
            <span className="badge badge-neutral">
              <Paperclip size={11} /> {attachment.name}
              <button onClick={() => setAttachment(null)} className="ml-1 text-muted hover:text-white" aria-label="Remove attachment"><X size={11} /></button>
            </span>
          )}
          {notice && <span className={`badge ${notice.ok ? "badge-teal" : "badge-danger"}`}>{notice.text}</span>}
        </div>
      )}
      <textarea
        ref={textareaRef}
        value={text}
        onChange={(event) => {
          setText(event.target.value);
          event.target.style.height = "auto";
          event.target.style.height = `${Math.min(event.target.scrollHeight, 150)}px`;
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            handleSend();
          }
        }}
        placeholder="Describe a task for Nexus…"
        rows={1}
        aria-label="Task prompt"
      />
      <div className="mt-2 flex items-center justify-between gap-3">
        <div className="flex items-center gap-1.5">
          <button type="button" onClick={() => fileRef.current?.click()} disabled={ingesting} className="composer-tool" title="Attach a local document">
            {ingesting ? <LoaderCircle size={15} className="animate-spin" /> : <FileUp size={15} />}
            <span className="hidden sm:inline">Attach</span>
          </button>
          <input ref={fileRef} type="file" accept=".pdf,.png,.jpg,.jpeg,.tiff,.txt,.docx,.xlsx" className="hidden" onChange={handleFileUpload} />
          <span className="hidden items-center gap-1.5 px-2 text-[10px] font-medium text-muted md:flex"><ShieldCheck size={12} className="text-teal" /> processed on-device</span>
        </div>
        <button type="button" onClick={handleSend} disabled={loading || !text.trim()} className="send-button" aria-label="Run task">
          {loading ? <LoaderCircle size={16} className="animate-spin" /> : <ArrowUp size={17} strokeWidth={2.4} />}
        </button>
      </div>
    </div>
  );
}
