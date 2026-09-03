import { useEffect, useState } from "react";
import { BrainCircuit, Check, Code2, Cpu, HardDrive, RefreshCw, Route, Settings2 } from "lucide-react";
import { Badge, Page, Stat } from "../components/Ui";
import { getSystemStatus } from "../api/client";

export default function Models() {
  const [status, setStatus] = useState(null);
  const [offline, setOffline] = useState(false);

  const refresh = () => {
    setOffline(false);
    getSystemStatus().then(setStatus).catch(() => setOffline(true));
  };

  useEffect(() => {
    getSystemStatus().then(setStatus).catch(() => setOffline(true));
  }, []);

  return (
    <Page title="Model registry" eyebrow="Orchestration / Open-weight models" model="Automatic routing"
      actions={<button onClick={refresh} className="secondary-button"><RefreshCw size={14} /> Refresh</button>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat label="Configured routes" value={status?.modelRoutes.length ?? "—"} detail="Coding and general workloads" icon={Route} />
        <Stat label="Provider" value={status?.provider || "—"} detail="Open-weight local inference" icon={Cpu} tone="teal" />
        <Stat label="Runtime endpoint" value={status?.localRuntimeConfigured ? "Loopback" : "—"} detail={status?.ollamaBaseUrl || "Backend unavailable"} icon={HardDrive} tone="blue" />
      </div>

      {offline && <div className="mb-5 rounded-lg border border-danger/20 bg-danger/[.06] p-3 text-xs text-danger">Model configuration is unavailable because the Spring backend is offline.</div>}

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <section className="space-y-3">
          {status?.modelRoutes.map((route, index) => { const Icon = index === 0 ? Code2 : BrainCircuit; return (
            <article key={route.taskType} className="panel p-4 sm:p-5">
              <div className="flex items-start gap-4"><div className={`icon-box ${index === 0 ? "icon-box-blue" : "icon-box-teal"} h-10 w-10`}><Icon size={19} /></div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="text-[14px] font-semibold text-white">{route.taskType}</h2><Badge tone="teal" dot>Configured</Badge></div><p className="mt-2 text-[11px] text-muted">Requests classified for this route are sent to the model below.</p><div className="mt-4 rounded-lg border border-line bg-base/50 px-3 py-2.5 font-mono text-[10px] text-text">{route.model}</div></div><Check size={16} className="mt-1 text-teal" /></div>
            </article>
          ); })}
          {!status && !offline && <div className="panel p-8 text-center text-xs text-muted">Loading configured model routes…</div>}
        </section>

        <aside className="space-y-4">
          <section className="panel overflow-hidden"><div className="panel-header"><div><p className="eyebrow">Smart orchestration</p><h2 className="panel-title mt-1">Model router</h2></div><Route size={17} className="text-accent" /></div><div className="p-4"><p className="mb-4 text-[10.5px] leading-5 text-muted">Classifies each request locally and chooses the configured coding or general route.</p><RouteRule icon={Code2} task="Code, scripts and debugging" model={status?.modelRoutes?.[0]?.model || "—"} /><RouteRule icon={BrainCircuit} task="Documents and general work" model={status?.modelRoutes?.[1]?.model || "—"} /><div className="mt-4 flex items-center gap-2 text-[9.5px] text-teal"><Settings2 size={12} /> Environment-variable configurable</div></div></section>
          <section className="panel p-4"><div className="mb-3 flex items-center gap-2 text-xs font-semibold text-teal"><Cpu size={14} /> Backend agnostic</div><p className="text-[10.5px] leading-5 text-muted">Set separate local model names with AI_CODING_MODEL and AI_GENERAL_MODEL without changing the frontend.</p></section>
        </aside>
      </div>
    </Page>
  );
}

function RouteRule({ icon: Icon, task, model }) {
  return <div className="mb-2 flex items-center gap-2.5 rounded-lg border border-line bg-base/50 p-2.5"><Icon size={13} className="text-muted" /><span className="min-w-0 flex-1 text-[10.5px] text-muted">{task}</span><span className="max-w-[130px] truncate font-mono text-[9px] text-text">{model}</span></div>;
}
