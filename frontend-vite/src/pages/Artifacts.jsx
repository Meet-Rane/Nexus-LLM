import { useEffect, useMemo, useState } from "react";
import { Check, Code2, Download, Eye, FileOutput, FileSpreadsheet, FileText, Presentation, Search, SlidersHorizontal } from "lucide-react";
import { Badge, Page, Stat } from "../components/Ui";
import { downloadArtifact, getArtifactContent, listArtifacts } from "../api/client";

export default function Artifacts() {
  const [artifacts, setArtifacts] = useState([]);
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState(null);
  const [content, setContent] = useState(null);
  const [state, setState] = useState("loading");
  const visible = useMemo(() => artifacts.filter((item) => item.fileName.toLowerCase().includes(query.toLowerCase())), [artifacts, query]);
  const totalBytes = artifacts.reduce((sum, item) => sum + (item.fileSize || 0), 0);

  const refresh = async () => {
    setState("loading");
    try {
      setArtifacts(await listArtifacts());
      setState("ready");
    } catch {
      setState("offline");
    }
  };

  useEffect(() => {
    listArtifacts().then((items) => {
      setArtifacts(items);
      setState("ready");
    }).catch(() => setState("offline"));
  }, []);

  const preview = async (artifact) => {
    setSelected(artifact);
    setContent(null);
    try {
      setContent(await getArtifactContent(artifact.path, artifact.conversationId));
    } catch {
      setContent({ content: "Preview is unavailable for this binary file. Use Download instead." });
    }
  };

  return (
    <Page title="Artifacts" eyebrow="Workspace / Agent deliverables" model="Local artifact store"
      actions={<><button className="secondary-button"><SlidersHorizontal size={14} /> Filter</button><button onClick={refresh} className="primary-button">Refresh</button></>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3"><Stat label="Deliverables" value={artifacts.length} detail="Current conversation" icon={FileOutput} /><Stat label="Integrity scope" value={artifacts.length ? "Local" : "—"} detail="Conversation-isolated paths" icon={Check} tone="teal" /><Stat label="Local storage" value={formatBytes(totalBytes)} detail="Generated workspace output" icon={Download} tone="blue" /></div>

      {state === "offline" && <div className="mb-5 rounded-lg border border-danger/20 bg-danger/[.06] p-3 text-xs text-danger">The Spring backend is unavailable on port 8090.</div>}

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_330px]">
        <section className="panel overflow-hidden">
          <div className="panel-header flex-wrap"><div><h2 className="panel-title">Generated files</h2><p className="panel-subtitle">Real outputs produced by the active local conversation</p></div><div className="search-field w-full sm:w-56"><Search size={14} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search artifacts" /></div></div>
          <div className="overflow-x-auto">
            <table className="data-table min-w-[700px]"><thead><tr><th>Artifact</th><th>Updated</th><th>Type</th><th>Scope</th></tr></thead><tbody>{visible.map((item) => { const Icon = iconFor(item); return <tr key={item.id} onClick={() => preview(item)} className="cursor-pointer"><td><div className="flex items-center gap-3"><div className="icon-box icon-box-accent"><Icon size={15} /></div><div><p className="font-medium text-text">{item.fileName}</p><p className="mt-0.5 font-mono text-[9px] text-faint">{formatBytes(item.fileSize)}</p></div></div></td><td>{item.updatedAt}</td><td>{item.artifactType}</td><td><Badge tone="teal"><Check size={10} />Local</Badge></td></tr>; })}</tbody></table>
            {state === "loading" && <div className="py-12 text-center text-xs text-muted">Loading local artifacts…</div>}
            {state === "ready" && !visible.length && <div className="py-12 text-center text-xs text-muted">No artifacts exist for this conversation yet.</div>}
          </div>
        </section>

        <aside className="panel self-start overflow-hidden xl:sticky xl:top-0">
          <div className="panel-header"><div><p className="eyebrow">Quick preview</p><h2 className="panel-title mt-1">{selected?.fileName || "Select an artifact"}</h2></div><Eye size={16} className="text-muted" /></div>
          <div className="p-4">
            {content ? <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-lg border border-line bg-base p-3 font-mono text-[10px] leading-5 text-muted">{content.content}</pre> : <div className="artifact-preview flex items-center justify-center text-xs text-muted">Choose a generated file to preview it.</div>}
            {selected && <><div className="mt-4 space-y-2"><Detail label="Format" value={selected.artifactType} /><Detail label="Size" value={formatBytes(selected.fileSize)} /><Detail label="Classification" value="LOCAL" accent /></div><button type="button" onClick={() => downloadArtifact(selected.path, selected.conversationId, selected.fileName)} className="primary-button mt-4 w-full"><Download size={13} /> Download</button></>}
          </div>
        </aside>
      </div>
    </Page>
  );
}

function formatBytes(bytes) {
  if (!bytes) return "0 B";
  if (bytes < 1024) return `${bytes} B`;
  return `${(bytes / 1024).toFixed(1)} KB`;
}

function iconFor(artifact) {
  const value = `${artifact.fileName} ${artifact.artifactType}`.toLowerCase();
  if (value.includes("xlsx") || value.includes("spreadsheet")) return FileSpreadsheet;
  if (value.includes("pptx") || value.includes("presentation")) return Presentation;
  if (value.includes(".py") || value.includes("code")) return Code2;
  return FileText;
}

function Detail({ label, value, accent }) {
  return <div className="flex justify-between gap-3 text-[10.5px]"><span className="text-muted">{label}</span><span className={accent ? "font-mono text-accent" : "text-text"}>{value}</span></div>;
}
