import { MoreHorizontal } from "lucide-react";
import { StatusBar } from "./StatusBar";

export function Page({ title, eyebrow, model, children, actions, connected = true }) {
  return (
    <div className="flex h-full min-w-0 flex-col">
      <StatusBar title={title} eyebrow={eyebrow} model={model} connected={connected}>{actions}</StatusBar>
      <main className="page-scroll"><div className="page-wrap">{children}</div></main>
    </div>
  );
}

export function Badge({ children, tone = "neutral", dot = false }) {
  return <span className={`badge badge-${tone}`}>{dot && <span className="badge-dot" />}{children}</span>;
}

export function Stat({ label, value, detail, icon: Icon, tone = "accent" }) {
  return (
    <div className="panel p-4 sm:p-5">
      <div className="mb-4 flex items-start justify-between">
        <p className="text-[11px] font-semibold uppercase tracking-[0.12em] text-muted">{label}</p>
        {Icon && <div className={`icon-box icon-box-${tone}`}><Icon size={16} /></div>}
      </div>
      <p className="font-display text-2xl font-semibold tracking-tight text-white">{value}</p>
      {detail && <p className="mt-1 text-xs text-muted">{detail}</p>}
    </div>
  );
}

export function MiniBar({ value, tone = "accent" }) {
  return <div className="h-1.5 overflow-hidden rounded-full bg-panel-3"><div className={`mini-bar mini-bar-${tone}`} style={{ width: `${value}%` }} /></div>;
}

export function TableActions() {
  return <button className="icon-button" aria-label="More actions"><MoreHorizontal size={17} /></button>;
}
