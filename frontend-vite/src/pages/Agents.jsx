import { useState } from "react";
import { Bot, Check, ChevronRight, CircleDashed, Clock3, Code2, FileOutput, FileScan, ListChecks, Pause, Play, ShieldCheck, Wrench } from "lucide-react";
import { Badge, Page, Stat, TableActions } from "../components/Ui";

const history = [
  { task: "Draft approval note from inspection report", type: "Document", model: "Qwen2.5-VL", time: "2m 18s", status: "Complete" },
  { task: "Validate exchanger calculation script", type: "Code", model: "Qwen3-Coder", time: "1m 42s", status: "Complete" },
  { task: "Compare three vendor technical bids", type: "Analysis", model: "DeepSeek-R1", time: "4m 09s", status: "Complete" },
  { task: "Extract tags from P&ID drawing", type: "Vision", model: "Qwen2.5-VL", time: "3m 36s", status: "Complete" },
];

export default function Agents() {
  const [paused, setPaused] = useState(false);
  return (
    <Page title="Agent runs" eyebrow="Orchestration / Execution" model="1 active agent"
      actions={<button className="primary-button"><Play size={14} /> New run</button>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3"><Stat label="Active run" value={paused ? "Paused" : "01"} detail="Inspection analysis workflow" icon={Bot} /><Stat label="Completed today" value="07" detail="100% locally executed" icon={ListChecks} tone="teal" /><Stat label="Median runtime" value="2m 41s" detail="Across all workflow types" icon={Clock3} tone="blue" /></div>

      <section className="panel mb-5 overflow-hidden">
        <div className="panel-header flex-wrap"><div className="flex items-center gap-3"><div className="relative"><div className="icon-box icon-box-accent h-9 w-9"><Bot size={18} /></div>{!paused && <span className="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full border-2 border-panel bg-teal" />}</div><div><div className="flex items-center gap-2"><h2 className="panel-title">Inspection report → approval note</h2><Badge tone={paused ? "accent" : "teal"} dot>{paused ? "Paused" : "Running"}</Badge></div><p className="panel-subtitle font-mono">RUN-2026-0829-014 · started 09:42</p></div></div><button onClick={() => setPaused(!paused)} className="secondary-button">{paused ? <Play size={14} /> : <Pause size={14} />}{paused ? "Resume" : "Pause run"}</button></div>
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_300px]">
          <div className="p-5 sm:p-6">
            <div className="mb-5 flex items-center justify-between"><p className="eyebrow">Execution plan</p><span className="font-mono text-[9px] text-muted">4 / 5 steps</span></div>
            <div className="relative space-y-0 before:absolute before:bottom-5 before:left-[15px] before:top-5 before:w-px before:bg-line">
              <RunStep icon={FileScan} title="Read scanned inspection report" detail="OCR + vision extracted 42 pages and 6 handwritten annotations" state="done" time="18.4s" />
              <RunStep icon={ListChecks} title="Identify findings and severity" detail="Found 7 observations · 2 require immediate action" state="done" time="31.7s" />
              <RunStep icon={Wrench} title="Ground against maintenance SOP" detail="Retrieved 4 relevant passages from Plant SOPs & Manuals" state="done" time="0.9s" />
              <RunStep icon={FileOutput} title="Draft approval note" detail={paused ? "Waiting to resume" : "Generating structured DOCX with cited findings"} state={paused ? "waiting" : "active"} time="—" />
              <RunStep icon={ShieldCheck} title="Verify and save artifact" detail="Check citations, file integrity and local destination" state="waiting" time="—" />
            </div>
          </div>
          <aside className="border-t border-line bg-base/35 p-5 lg:border-l lg:border-t-0">
            <p className="eyebrow">Run context</p><div className="mt-4 space-y-3"><Info label="Primary model" value="Qwen2.5-VL-7B" /><Info label="Reasoning model" value="DeepSeek-R1-14B" /><Info label="Tools invoked" value="OCR · RAG · DOCX" /><Info label="GPU allocation" value="17.2 / 24 GB" /><Info label="Network egress" value="BLOCKED" teal /></div>
            <div className="mt-5 rounded-lg border border-line bg-panel p-3"><div className="mb-2 flex items-center gap-2 text-[10px] font-semibold text-text"><Code2 size={13} className="text-accent" /> Latest tool event</div><code className="font-mono text-[9px] leading-5 text-muted">docx.write(<span className="text-teal">"Approval_Note_R401.docx"</span>)</code></div>
          </aside>
        </div>
      </section>

      <section className="panel overflow-hidden"><div className="panel-header"><div><h2 className="panel-title">Recent runs</h2><p className="panel-subtitle">Auditable history of completed local work</p></div><button className="ghost-button">View all <ChevronRight size={14} /></button></div><div className="overflow-x-auto"><table className="data-table min-w-[700px]"><thead><tr><th>Task</th><th>Type</th><th>Model</th><th>Runtime</th><th>Status</th><th /></tr></thead><tbody>{history.map((item) => <tr key={item.task}><td className="font-medium text-text!">{item.task}</td><td>{item.type}</td><td className="font-mono text-[10px]!">{item.model}</td><td>{item.time}</td><td><Badge tone="teal"><Check size={10} />{item.status}</Badge></td><td><TableActions /></td></tr>)}</tbody></table></div></section>
    </Page>
  );
}

function RunStep({ icon: Icon, title, detail, state, time }) { return <div className="relative z-10 flex gap-4 pb-5 last:pb-0"><div className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border ${state === "done" ? "border-teal/25 bg-teal/[.08] text-teal" : state === "active" ? "border-accent/30 bg-accent/[.09] text-accent" : "border-line bg-panel-2 text-faint"}`}>{state === "done" ? <Check size={14} /> : state === "active" ? <CircleDashed size={14} className="animate-spin" /> : <Icon size={14} />}</div><div className="min-w-0 flex-1 pt-0.5"><div className="flex items-center justify-between gap-3"><p className={`text-[12px] font-semibold ${state === "waiting" ? "text-muted" : "text-text"}`}>{title}</p><span className="font-mono text-[9px] text-faint">{time}</span></div><p className="mt-1 text-[10.5px] leading-4 text-muted">{detail}</p></div></div>; }
function Info({ label, value, teal }) { return <div className="flex items-center justify-between gap-3 border-b border-line pb-3 last:border-0"><span className="text-[10.5px] text-muted">{label}</span><span className={`font-mono text-[9.5px] ${teal ? "text-teal" : "text-text"}`}>{value}</span></div>; }
