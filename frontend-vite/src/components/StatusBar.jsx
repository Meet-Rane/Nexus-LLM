import { Activity, ChevronDown, ShieldCheck } from "lucide-react";

export function StatusBar({ connected = true, model = "Auto route", title = "Agent workbench", eyebrow = "Workspace / Active task", children }) {
  const routeLabel = model === "auto-route" ? "Auto route" : model;
  return (
    <header className="flex h-[88px] shrink-0 items-center justify-between border-b border-edge bg-base/80 px-4 backdrop-blur-xl sm:px-7">
      <div className="min-w-0">
        <p className="hidden text-[9px] font-bold uppercase tracking-[.2em] text-ink3 sm:block">{eyebrow}</p>
        <h1 className="mt-1 truncate font-display text-[18px] font-semibold tracking-tight text-ink sm:text-[20px]">{title}</h1>
      </div>
      <div className="flex shrink-0 items-center gap-2.5">
        {children}
        <button type="button" className="hidden h-11 max-w-[230px] items-center gap-2 rounded-xl border border-edge bg-surface px-3.5 text-[12px] text-ink2 shadow-sm transition hover:border-edge2 md:flex">
          <Activity size={15} className="shrink-0 text-amber" /><span className="truncate text-ink">{routeLabel}</span><ChevronDown size={13} className="shrink-0 text-ink3" />
        </button>
        <div className={`flex h-11 items-center gap-2 rounded-xl border px-3 font-mono text-[9px] font-semibold uppercase tracking-[.08em] ${connected ? "border-ok/30 bg-ok/[.06] text-ok" : "border-danger/35 bg-danger/[.07] text-danger"}`}>
          <ShieldCheck size={14} /><span className="hidden sm:inline">{connected ? "Local · Secure" : "Agent offline"}</span><span className="sm:hidden">{connected ? "Local" : "Offline"}</span>
        </div>
      </div>
    </header>
  );
}
