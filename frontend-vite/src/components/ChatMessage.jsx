import React from "react";
import { AGENT_URL } from "../api/client";

export function ChatMessage({ message }) {
  const isUser = message.role === "user";

  return (
    <div
      className={`flex animate-fadeInUp ${isUser ? "justify-end" : "justify-start"}`}
    >
      <div className={`flex max-w-[75%] gap-2.5 ${isUser ? "flex-row-reverse" : "flex-row"}`}>
        {/* Role marker */}
        <div
          className={`mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-md font-mono text-[10px] font-medium ${
            isUser
              ? "bg-steel/20 text-steel"
              : "border border-edge bg-surface2 text-amber"
          }`}
        >
          {isUser ? "YOU" : "AI"}
        </div>

        <div
          className={`rounded-2xl px-4 py-3 text-[14px] leading-relaxed shadow-sm ${
            isUser
              ? "rounded-tr-sm bg-steel text-white"
              : "rounded-tl-sm border border-edge bg-surface2 text-ink"
          }`}
        >
          <p className="whitespace-pre-wrap break-words">{message.text}</p>

          {/* Model badge — assistant messages only */}
          {!isUser && message.model_used && (
            <span className="mt-2.5 inline-flex items-center rounded-full border border-edge bg-base px-2 py-0.5 font-mono text-[10px] text-ink3">
              {message.model_used}
            </span>
          )}

          {/* Download link — if the agent produced a file */}
          {!isUser && message.download_url && (
            <div className="mt-3">
              <a
                href={`${AGENT_URL}${message.download_url}`}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 rounded-lg bg-ok px-3.5 py-1.5 text-[13px] font-semibold text-white transition hover:brightness-110"
              >
                ⬇ Download document
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
