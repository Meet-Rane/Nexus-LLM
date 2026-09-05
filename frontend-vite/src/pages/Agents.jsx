import { useEffect, useState } from "react";
import { Bot, Code2, Database, FileOutput, ListChecks, ShieldCheck, Wrench } from "lucide-react";
import { Badge, Page, Stat } from "../components/Ui";
import { getSystemStatus } from "../api/client";

const descriptions = {
  search_knowledge_base: "Retrieve grounded passages from the local Chroma knowledge base.",
  execute_python_code: "Run generated Python inside the configured Docker sandbox.",
  create_formatted_document: "Generate a professionally formatted PDF or Word deliverable locally.",
  create_file: "Create a conversation-scoped deliverable in local artifact storage.",
  read_file: "Read an existing artifact without leaving the workstation.",
  write_file: "Update an artifact during a multi-step task.",
  list_files: "Inspect the active task's locally generated files.",
};

export default function Agents() {
  const [status, setStatus] = useState(null);
  const [offline, setOffline] = useState(false);

  useEffect(() => {
    getSystemStatus().then(setStatus).catch(() => setOffline(true));
  }, []);

  return (
    <Page title="Agent capabilities" eyebrow="Orchestration / Execution" model={status ? `${status.tools.length} local tools` : "Loading runtime"}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat label="Agent backend" value={offline ? "Offline" : status ? "Ready" : "…"} detail="Spring AI orchestration" icon={Bot} />
        <Stat label="Registered tools" value={status?.tools.length ?? "—"} detail="Available to the local model" icon={ListChecks} tone="teal" />
        <Stat label="Runtime policy" value={status?.localOnlyPolicyEnforced ? "Local only" : "—"} detail="Loopback endpoints enforced" icon={ShieldCheck} tone="blue" />
      </div>

      {offline && <div className="mb-5 rounded-lg border border-danger/20 bg-danger/[.06] p-3 text-xs text-danger">The Spring backend is unavailable on port 8090.</div>}

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_330px]">
        <section className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="panel-title">Tool registry</h2><p className="panel-subtitle">Capabilities the model can call during an agent task</p></div><Wrench size={16} className="text-accent" /></div>
          <div className="grid gap-px bg-line sm:grid-cols-2">
            {status?.tools.map((tool) => { const Icon = iconFor(tool); return (
              <article key={tool} className="bg-panel p-4 sm:p-5">
                <div className="flex items-start gap-3"><div className="icon-box icon-box-accent"><Icon size={15} /></div><div><div className="flex flex-wrap items-center gap-2"><h3 className="font-mono text-[11px] font-semibold text-text">{tool}</h3><Badge tone="teal" dot>Ready</Badge></div><p className="mt-2 text-[10.5px] leading-5 text-muted">{descriptions[tool] || "Local agent capability."}</p></div></div>
              </article>
            ); })}
            {!status && !offline && <div className="col-span-2 bg-panel p-8 text-center text-xs text-muted">Loading local tool registry…</div>}
          </div>
        </section>

        <aside className="space-y-4">
          <section className="panel overflow-hidden"><div className="panel-header"><div><p className="eyebrow">Active configuration</p><h2 className="panel-title mt-1">Agent runtime</h2></div><Bot size={17} className="text-teal" /></div><div className="p-4"><Info label="Provider" value={status?.provider || "—"} /><Info label="RAG endpoint" value={status?.ragBaseUrl || "—"} /><Info label="Artifact scope" value={status?.artifactStorage || "—"} /><Info label="Sandbox network" value={status?.sandboxNetworkDisabled ? "DISABLED" : "UNKNOWN"} teal={status?.sandboxNetworkDisabled} /></div></section>
          <section className="panel p-4"><div className="mb-3 flex items-center gap-2 text-xs font-semibold text-teal"><ShieldCheck size={14} /> Conversation isolated</div><p className="text-[10.5px] leading-5 text-muted">Chat memory and generated artifacts are keyed by the active browser-session conversation ID.</p></section>
        </aside>
      </div>
    </Page>
  );
}

function iconFor(tool) {
  if (tool.includes("knowledge")) return Database;
  if (tool.includes("python")) return Code2;
  if (tool.includes("file")) return FileOutput;
  return Wrench;
}

function Info({ label, value, teal }) {
  return <div className="flex items-center justify-between gap-3 border-b border-line py-3 first:pt-0 last:border-0 last:pb-0"><span className="text-[10.5px] text-muted">{label}</span><span className={`max-w-[180px] truncate font-mono text-[9.5px] ${teal ? "text-teal" : "text-text"}`}>{value}</span></div>;
}
