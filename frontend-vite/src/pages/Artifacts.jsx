import { useMemo, useState } from "react";
import { Check, Code2, Download, Eye, FileOutput, FileSpreadsheet, FileText, Presentation, Search, SlidersHorizontal } from "lucide-react";
import { Badge, Page, Stat, TableActions } from "../components/Ui";

const artifacts = [
  { name: "Approval_Note_R401.docx", type: "Word document", ext: "DOCX", size: "184 KB", created: "Today, 09:47", source: "Agent run #014", icon: FileText, tone: "blue" },
  { name: "Inspection_Findings_Summary.pptx", type: "Presentation", ext: "PPTX", size: "2.8 MB", created: "Today, 09:31", source: "Agent run #013", icon: Presentation, tone: "accent" },
  { name: "Exchanger_Validation_Report.xlsx", type: "Workbook", ext: "XLSX", size: "412 KB", created: "Yesterday", source: "Agent run #012", icon: FileSpreadsheet, tone: "teal" },
  { name: "csv_validator.py", type: "Source code", ext: "PY", size: "18 KB", created: "Yesterday", source: "Agent run #011", icon: Code2, tone: "blue" },
  { name: "Vendor_Bid_Comparison.xlsx", type: "Workbook", ext: "XLSX", size: "728 KB", created: "28 Aug 2026", source: "Agent run #009", icon: FileSpreadsheet, tone: "teal" },
];

export default function Artifacts() {
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState(artifacts[0]);
  const visible = useMemo(() => artifacts.filter((item) => item.name.toLowerCase().includes(query.toLowerCase())), [query]);
  return (
    <Page title="Artifacts" eyebrow="Workspace / Agent deliverables" model="Local artifact store"
      actions={<button className="secondary-button"><SlidersHorizontal size={14} /> Filter</button>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3"><Stat label="Deliverables" value="08" detail="Created across 7 agent runs" icon={FileOutput} /><Stat label="Verified" value="08 / 08" detail="Integrity checks passed" icon={Check} tone="teal" /><Stat label="Local storage" value="6.2 MB" detail="Encrypted workspace output" icon={Download} tone="blue" /></div>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_330px]">
        <section className="panel overflow-hidden"><div className="panel-header flex-wrap"><div><h2 className="panel-title">Generated files</h2><p className="panel-subtitle">Real outputs produced and verified by local agents</p></div><div className="search-field w-full sm:w-56"><Search size={14} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search artifacts" /></div></div>
          <div className="overflow-x-auto"><table className="data-table min-w-[700px]"><thead><tr><th>Artifact</th><th>Created</th><th>Source</th><th>Integrity</th><th /></tr></thead><tbody>{visible.map((item) => { const Icon = item.icon; return <tr key={item.name} onClick={() => setSelected(item)} className="cursor-pointer"><td><div className="flex items-center gap-3"><div className={`icon-box icon-box-${item.tone}`}><Icon size={15} /></div><div><p className="font-medium text-text">{item.name}</p><p className="mt-0.5 font-mono text-[9px] text-faint">{item.type} · {item.size}</p></div></div></td><td>{item.created}</td><td>{item.source}</td><td><Badge tone="teal"><Check size={10} />Verified</Badge></td><td><TableActions /></td></tr>; })}</tbody></table></div>
        </section>

        <aside className="panel self-start overflow-hidden xl:sticky xl:top-0"><div className="panel-header"><div><p className="eyebrow">Quick preview</p><h2 className="panel-title mt-1">{selected.ext} artifact</h2></div><Eye size={16} className="text-muted" /></div><div className="p-4"><div className="artifact-preview"><div className="mx-auto w-[78%] rounded-sm bg-[#e9e7e0] p-4 shadow-xl"><div className="mb-3 h-2 w-1/3 rounded bg-[#8c8c87]" /><div className="mb-4 h-1 w-2/3 rounded bg-[#bbb9b2]" />{[92,88,96,72,90,80].map((width, i) => <div key={i} className="mb-1.5 h-[3px] rounded bg-[#c9c7c0]" style={{ width: `${width}%` }} />)}<div className="mt-4 border-l-2 border-[#c99443] bg-[#dedbd2] p-2"><div className="h-[3px] w-4/5 rounded bg-[#b5b2aa]" /></div></div></div>
          <h3 className="mt-4 truncate text-[12px] font-semibold text-white">{selected.name}</h3><div className="mt-3 space-y-2"><Detail label="Format" value={selected.ext} /><Detail label="Size" value={selected.size} /><Detail label="Created by" value={selected.source} /><Detail label="Classification" value="INTERNAL" accent /></div><div className="mt-4 grid grid-cols-2 gap-2"><button className="secondary-button"><Eye size={13} /> Open</button><button className="primary-button"><Download size={13} /> Download</button></div></div></aside>
      </div>
    </Page>
  );
}
function Detail({ label, value, accent }) { return <div className="flex justify-between gap-3 text-[10.5px]"><span className="text-muted">{label}</span><span className={accent ? "font-mono text-accent" : "text-text"}>{value}</span></div>; }
