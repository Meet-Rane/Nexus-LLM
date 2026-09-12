import { useEffect, useState } from "react";
import { Bot, Check, Circle, Cpu, FileCheck2, LockKeyhole, Search, Wrench, X } from "lucide-react";

function Step({ icon: Icon, label, state, number }) {
  const complete = state === "complete";
  const active = state === "active";
  const failed = state === "error";
  return (
    <div className={`flex items-center gap-3 ${state === "pending" ? "text-ink3" : "text-ink2"}`}>
      <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border font-mono text-[9px] ${complete ? "border-ok/30 bg-ok/10 text-ok" : failed ? "border-danger/35 bg-danger/10 text-danger" : active ? "border-amber/35 bg-amber/10 text-amber" : "border-edge bg-base text-ink3"}`}>
        {complete ? <Check size={12} /> : failed ? <X size={12} /> : active ? <Icon size={11} /> : number}
      </span>
      <span className={`min-w-0 truncate text-[11px] ${active ? "font-medium text-ink" : ""}`}>{label}</span>
      {active && <span className="ml-auto h-1.5 w-1.5 shrink-0 animate-pulseDot rounded-full bg-amber" />}
    </div>
  );
}

export function ExecutionRail({ run, model, connected }) {
  const [clock, setClock] = useState(run.startedAt || run.finishedAt || 0);
  useEffect(() => {
    if (!run.startedAt || run.finishedAt) return undefined;
    const timer = window.setInterval(() => setClock(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [run.startedAt, run.finishedAt]);

  const done = run.phase === "done";
  const failed = run.phase === "error";
  const routed = ["executing", "verifying", "done", "error"].includes(run.phase);
  const activeTool = run.activeTool ? `Execute ${friendlyTool(run.activeTool)}` : "Execute locally";
  const steps = [
    { number: 1, icon: Search, label: "Understand request", state: run.phase === "idle" ? "pending" : "complete" },
    { number: 2, icon: Cpu, label: "Select model & tools", state: run.phase === "routing" ? "active" : routed ? "complete" : "pending" },
    { number: 3, icon: Wrench, label: activeTool, state: run.phase === "executing" ? "active" : (["verifying", "done"].includes(run.phase) ? "complete" : failed && routed ? "error" : "pending") },
    { number: 4, icon: FileCheck2, label: "Verify output", state: run.phase === "verifying" ? "active" : done ? "complete" : failed ? "error" : "pending" },
  ];
  const elapsed = run.startedAt ? Math.max(0, Math.round(((run.finishedAt || clock) - run.startedAt) / 1000)) : 0;

  return (
    <aside className="hidden w-[300px] shrink-0 overflow-y-auto border-l border-edge bg-surface/55 px-4 py-5 xl:block">
      <section className="overflow-hidden rounded-2xl border border-edge bg-surface">
        <div className="flex items-center justify-between border-b border-edge px-4 py-4"><div><p className="text-[9px] font-bold uppercase tracking-[.18em] text-ink3">Live trace</p><h2 className="mt-1.5 text-[13px] font-semibold text-ink">Agent execution</h2></div><Bot size={17} className={run.startedAt && !run.finishedAt ? "animate-pulseDot text-amber" : "text-ink3"} /></div>
        <div className="p-4">
          <p className="mb-4 text-[9px] font-bold uppercase tracking-[.17em] text-ink3">Plan</p>
          <div className="space-y-4">{steps.map((step) => <Step key={step.number} {...step} />)}</div>

          {run.detail && <div className={`mt-5 rounded-xl border p-3 text-[9px] leading-4 ${failed ? "border-danger/25 bg-danger/[.05] text-danger" : "border-edge bg-base text-ink3"}`}><p className="mb-1 font-semibold text-ink">Latest activity</p>{run.detail}</div>}
          {run.startedAt && <div className="mt-3 flex items-center justify-between font-mono text-[9px] text-ink3"><span>{run.completedTools.length} local tool{run.completedTools.length === 1 ? "" : "s"}</span><span>{elapsed}s elapsed</span></div>}

          <div className="my-5 h-px bg-edge" />
          <p className="mb-3 text-[9px] font-bold uppercase tracking-[.17em] text-ink3">Router decision</p>
          <div className="rounded-xl border border-amber/20 bg-amber/[.055] p-3"><div className="flex items-center gap-2 text-amber"><Cpu size={14} /><span className="truncate font-mono text-[10px] font-semibold">{run.model || model}</span></div><p className="mt-2 text-[9px] leading-4 text-ink3">{run.modelReason || "Selected locally for task modality and reasoning needs."}</p></div>

          {run.completedTools.length > 0 && <><div className="my-5 h-px bg-edge" /><p className="mb-3 text-[9px] font-bold uppercase tracking-[.17em] text-ink3">Completed locally</p><div className="space-y-2">{run.completedTools.slice(-5).map((tool) => <div key={tool} className="grid grid-cols-[8px_minmax(0,1fr)] gap-2 text-[9px] leading-4 text-ink3"><Circle size={6} fill="currentColor" className="mt-1 text-ok" /><p className="truncate">{friendlyTool(tool)}</p></div>)}</div></>}

          <div className={`mt-5 flex items-center gap-2 rounded-lg border px-3 py-2.5 text-[9px] ${connected ? "border-ok/15 bg-ok/[.045] text-ok" : "border-danger/20 bg-danger/[.045] text-danger"}`}><LockKeyhole size={12} /><span>{connected ? "Local endpoint policy enforced" : "Waiting for local agent"}</span></div>
        </div>
      </section>
    </aside>
  );
}

function friendlyTool(toolName) {
  return (toolName || "tool").replaceAll("_", " ");
}
