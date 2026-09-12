import { CheckCircle2, ChevronDown, Download, FileOutput, Sparkles } from "lucide-react";
import ReactMarkdown from "react-markdown";
import rehypeKatex from "rehype-katex";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import "katex/dist/katex.min.css";
import { downloadArtifact } from "../api/client";

export function ChatMessage({ message }) {
  const isUser = message.role === "user";
  const artifacts = message.artifacts?.length
    ? message.artifacts
    : message.artifact ? [message.artifact] : [];

  if (isUser) {
    return (
      <article className="flex animate-fadeInUp justify-end py-2">
        <div className="max-w-[88%] rounded-2xl rounded-tr-md border border-edge bg-surface3 px-4 py-3 text-[14px] leading-6 text-ink shadow-sm sm:max-w-[78%]">
          <p className="whitespace-pre-wrap break-words">{message.text}</p>
        </div>
      </article>
    );
  }

  return (
    <article className="animate-fadeInUp py-3">
      <div className="flex items-center gap-2.5">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-amber/30 bg-amber/10 text-amber">
          <Sparkles size={15} />
        </div>
        <div className="flex min-w-0 items-center gap-2">
          <span className="text-[12px] font-semibold text-ink">Nexus Agent</span>
          {message.model_used && <span className="max-w-[190px] truncate rounded-full border border-amber/25 bg-amber/[0.07] px-2 py-0.5 font-mono text-[9px] text-amber">{message.model_used}</span>}
        </div>
      </div>

      <div className={`ml-10 mt-2 border-l pl-4 sm:pl-5 ${message.error ? "border-danger/40" : "border-edge"}`}>
        {message.trace?.length > 0 && (
          <details className="group mb-3 rounded-xl border border-edge bg-surface/60 open:bg-surface">
            <summary className="flex cursor-pointer list-none items-center gap-2 px-3 py-2 text-[10px] text-ink3">
              <CheckCircle2 size={13} className="text-ok" />
              <span>{message.trace.length} local execution event{message.trace.length === 1 ? "" : "s"}</span>
              <ChevronDown size={12} className="ml-auto transition group-open:rotate-180" />
            </summary>
            <div className="space-y-2 border-t border-edge px-3 py-2.5">
              {message.trace.map((event, index) => (
                <div key={`${event.type}-${index}`} className="grid grid-cols-[10px_minmax(0,1fr)] gap-2 font-mono text-[9px] leading-4 text-ink3">
                  <span className={event.type === "TOOL_COMPLETE" ? "text-ok" : "text-amber"}>{event.type === "TOOL_COMPLETE" ? "✓" : "›"}</span>
                  <span><strong className="font-medium text-ink2">{event.label || "Local route"}</strong>{event.detail ? ` · ${event.detail}` : ""}</span>
                </div>
              ))}
            </div>
          </details>
        )}

        {message.text ? (
          <div className="message-markdown text-[14px] leading-6 text-ink2">
            <ReactMarkdown remarkPlugins={[remarkGfm, remarkMath]} rehypePlugins={[rehypeKatex]}>
              {normalizeMathDelimiters(message.text)}
            </ReactMarkdown>
          </div>
        ) : (
          <div className="flex items-center gap-2 py-1 text-[12px] text-ink3">
            <span className="relative h-1 w-8 overflow-hidden rounded-full bg-edge"><span className="absolute h-full w-3 animate-thinking rounded-full bg-amber" /></span>
            Planning and selecting local tools…
          </div>
        )}

        {artifacts.map((artifact) => (
          <div key={`${artifact.conversationId || ""}:${artifact.path || artifact.id}`} className="mt-4 flex flex-wrap items-center gap-3 rounded-xl border border-edge bg-surface p-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg border border-amber/25 bg-amber/10 text-amber"><FileOutput size={16} /></div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-[12px] font-medium text-ink">{artifact.fileName || artifact.path?.split(/[\\/]/).pop() || "Generated artifact"}</p>
              <p className="mt-0.5 text-[9px] text-ink3">Generated, verified and stored locally</p>
            </div>
            <button type="button" onClick={() => downloadArtifact(artifact.path, artifact.conversationId, artifact.fileName)} className="inline-flex h-8 items-center gap-1.5 rounded-lg bg-amber px-3 text-[10px] font-semibold text-white transition hover:bg-[#827bff]">
              <Download size={13} /> Download
            </button>
          </div>
        ))}
      </div>
    </article>
  );
}

function normalizeMathDelimiters(value) {
  return String(value)
    .replace(/\\+\[([\s\S]*?)\\+\]/g, (_, expression) => `\n\n$$\n${expression.trim()}\n$$\n\n`)
    .replace(/\\+\(([\s\S]*?)\\+\)/g, (_, expression) => `$${expression.trim()}$`);
}
