import { Bot, Check, Download, UserRound } from "lucide-react";
import { getArtifactDownloadUrl } from "../api/client";
import { Badge } from "./Ui";

export function ChatMessage({ message }) {
  const isUser = message.role === "user";
  const downloadUrl = message.artifact
    ? getArtifactDownloadUrl(message.artifact.path, message.artifact.conversationId)
    : null;
  return (
    <article className={`message ${isUser ? "message-user" : ""}`}>
      <div className={`message-avatar ${isUser ? "message-avatar-user" : "message-avatar-agent"}`}>
        {isUser ? <UserRound size={14} /> : <Bot size={15} />}
      </div>
      <div className="min-w-0 flex-1">
        <div className="mb-1.5 flex items-center gap-2">
          <span className="text-[11px] font-semibold text-white">{isUser ? "You" : "Nexus Agent"}</span>
          {!isUser && message.model_used && <Badge tone="accent">{message.model_used}</Badge>}
        </div>
        <div className={`message-body ${isUser ? "message-body-user" : ""}`}>
          {!isUser && message.trace?.length > 0 && (
            <div className="mb-3 space-y-1.5 border-b border-line pb-3">
              {message.trace.map((event, index) => (
                <div key={`${event.type}-${index}`} className="flex items-start gap-2 font-mono text-[10px] text-muted">
                  <span className={event.type === "TOOL_COMPLETE" ? "text-teal" : "text-accent"}>{event.type === "TOOL_COMPLETE" ? "✓" : "›"}</span>
                  <span><strong className="font-medium text-text">{event.label}</strong>{event.detail ? ` · ${event.detail}` : ""}</span>
                </div>
              ))}
            </div>
          )}
          <p className="whitespace-pre-wrap break-words">{message.text || "Waiting for the local agent…"}</p>
          {!isUser && downloadUrl && (
            <a href={downloadUrl} className="artifact-link">
              <span className="icon-box icon-box-teal h-8 w-8"><Download size={15} /></span>
              <span><strong>{message.artifact.fileName}</strong><small>Generated locally · ready to download</small></span>
              <Check size={14} className="ml-auto text-teal" />
            </a>
          )}
        </div>
      </div>
    </article>
  );
}
