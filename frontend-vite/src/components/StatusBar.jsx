import React from "react";

/**
 * A header styled like an equipment nameplate — the signature element of this
 * design. Reinforces that this is a sovereign, on-premise, air-gapped agent,
 * not a generic hosted chatbot.
 */
export function StatusBar({ connected, model = "—", instanceId = "MRPL-WBX-01" }) {
  return (
    <header className="flex items-center justify-between border-b border-edge bg-surface px-5 py-3">
      <div className="flex items-center gap-3">
        <div className="flex h-8 w-8 items-center justify-center rounded-md border border-edge bg-surface2 font-display text-sm font-semibold text-amber">
          ⌁
        </div>
        <div className="leading-tight">
          <div className="font-display text-sm font-semibold tracking-wide text-ink">
            Agentic AI Workbench
          </div>
          <div className="font-mono text-[11px] text-ink3">{instanceId}</div>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="hidden items-center gap-1.5 rounded-full border border-edge bg-surface2 px-2.5 py-1 font-mono text-[11px] text-ink2 sm:flex">
          <span>model</span>
          <span className="text-ink">{model}</span>
        </div>

        <div
          className={`flex items-center gap-2 rounded-full border px-3 py-1 font-mono text-[11px] ${
            connected
              ? "border-ok/40 bg-ok/10 text-ok"
              : "border-danger/40 bg-danger/10 text-danger"
          }`}
        >
          <span className="relative flex h-2 w-2">
            <span
              className={`absolute inline-flex h-full w-full rounded-full ${
                connected ? "bg-ok" : "bg-danger"
              } animate-pulseDot`}
            />
          </span>
          {connected ? "ON-PREM · CONNECTED" : "OFFLINE"}
        </div>
      </div>
    </header>
  );
}
