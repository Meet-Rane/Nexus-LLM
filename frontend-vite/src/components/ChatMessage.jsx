import { Bot, Check, Download, UserRound } from "lucide-react";
import { AGENT_URL } from "../api/client";
import { Badge } from "./Ui";

export function ChatMessage({ message }) {
  const isUser = message.role === "user";
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
          <p className="whitespace-pre-wrap break-words">{message.text}</p>
          {!isUser && message.download_url && (
            <a href={`${AGENT_URL}${message.download_url}`} target="_blank" rel="noreferrer" className="artifact-link">
              <span className="icon-box icon-box-teal h-8 w-8"><Download size={15} /></span>
              <span><strong>Generated deliverable</strong><small>Ready to download</small></span>
              <Check size={14} className="ml-auto text-teal" />
            </a>
          )}
        </div>
      </div>
    </article>
  );
}
