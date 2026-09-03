import { Activity, ChevronDown, LockKeyhole, ShieldCheck } from "lucide-react";

export function StatusBar({ connected = true, model = "Auto route", title = "Agent workbench", eyebrow = "Workspace / Active task", children }) {
  return (
    <header className="topbar">
      <div className="min-w-0">
        <p className="mb-1 truncate text-[10px] font-semibold uppercase tracking-[0.15em] text-muted">{eyebrow}</p>
        <h1 className="truncate font-display text-lg font-semibold tracking-tight text-white">{title}</h1>
      </div>
      <div className="flex items-center gap-2.5">
        {children}
        <button className="hidden h-9 items-center gap-2 rounded-lg border border-line bg-panel px-3 text-xs text-text transition hover:border-line-bright md:flex">
          <Activity size={14} className="text-accent" />
          <span>{model}</span>
          <ChevronDown size={13} className="text-muted" />
        </button>
        <div className={`status-pill ${connected ? "status-online" : "status-offline"}`}>
          {connected ? <ShieldCheck size={13} /> : <LockKeyhole size={13} />}
          <span className="hidden sm:inline">{connected ? "LOCAL · SECURE" : "AGENT OFFLINE"}</span>
          <span className="sm:hidden">{connected ? "LOCAL" : "OFFLINE"}</span>
        </div>
      </div>
    </header>
  );
}
