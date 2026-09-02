import { useState } from "react";
import { BookOpenCheck, Database, FolderKanban, Network, Plus, Search, Settings2 } from "lucide-react";
import { Badge, MiniBar, Page, Stat } from "../components/Ui";

const collections = [
  { name: "Plant SOPs & Manuals", description: "Operating procedures, safety manuals and maintenance guides", docs: 86, chunks: "4,218", coverage: 96, updated: "12 min ago", tone: "teal" },
  { name: "Inspection History", description: "NDT reports, thickness readings and equipment assessments", docs: 124, chunks: "8,740", coverage: 89, updated: "Today, 09:44", tone: "accent" },
  { name: "Past Correspondence", description: "Approved notes, technical clarifications and vendor exchanges", docs: 312, chunks: "12,086", coverage: 82, updated: "Yesterday", tone: "blue" },
];

export default function Knowledge() {
  const [query, setQuery] = useState("");
  const [testResult, setTestResult] = useState(null);
  const filtered = collections.filter((item) => `${item.name} ${item.description}`.toLowerCase().includes(query.toLowerCase()));
  return (
    <Page title="Knowledge base" eyebrow="Workspace / Grounding" model="bge-m3 embeddings"
      actions={<button className="primary-button"><Plus size={14} /> New collection</button>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat label="Knowledge sources" value="522" detail="Across 3 secure collections" icon={Database} />
        <Stat label="Vector chunks" value="25,044" detail="Local Chroma index" icon={Network} tone="teal" />
        <Stat label="Retrieval quality" value="92.4%" detail="Last evaluation · 120 queries" icon={BookOpenCheck} tone="blue" />
      </div>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <section className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="panel-title">Collections</h2><p className="panel-subtitle">Isolated indexes available to authorised agents</p></div><Settings2 size={16} className="text-muted" /></div>
          <div className="divide-y divide-line">
            {filtered.map((item) => (
              <article key={item.name} className="p-4 transition hover:bg-white/[.012] sm:p-5">
                <div className="flex items-start gap-3.5">
                  <div className={`icon-box icon-box-${item.tone} mt-0.5`}><FolderKanban size={16} /></div>
                  <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h3 className="text-[13px] font-semibold text-white">{item.name}</h3><Badge tone="teal" dot>Ready</Badge></div><p className="mt-1.5 text-[11px] leading-5 text-muted">{item.description}</p>
                    <div className="mt-4 grid grid-cols-3 gap-4"><Meta label="Sources" value={item.docs} /><Meta label="Chunks" value={item.chunks} /><Meta label="Updated" value={item.updated} /></div>
                    <div className="mt-3 flex items-center gap-3"><div className="flex-1"><MiniBar value={item.coverage} tone={item.tone} /></div><span className="font-mono text-[9px] text-muted">{item.coverage}% indexed</span></div>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>

        <aside className="space-y-4">
          <section className="panel p-4">
            <div className="mb-3"><p className="eyebrow">Retrieval test</p><h2 className="panel-title mt-1">Query your local index</h2></div>
            <div className="search-field"><Search size={14} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Try: shutdown approval" /></div>
            <button onClick={() => setTestResult(query ? "3 grounded passages found in 84 ms" : "Enter a query to test retrieval")} className="primary-button mt-3 w-full">Run local search</button>
            {testResult && <div className="mt-3 rounded-lg border border-teal/15 bg-teal/[.05] p-3 text-[10.5px] text-teal">{testResult}</div>}
          </section>
          <section className="panel p-4">
            <p className="eyebrow">Index configuration</p>
            <div className="mt-4 space-y-3"><Config label="Embedding model" value="bge-m3 · local" /><Config label="Vector store" value="ChromaDB" /><Config label="Chunk strategy" value="Semantic · 800 tkn" /><Config label="External connectors" value="Disabled" accent /></div>
          </section>
        </aside>
      </div>
    </Page>
  );
}

function Meta({ label, value }) { return <div><p className="text-[9px] uppercase tracking-wider text-faint">{label}</p><p className="mt-1 text-[10.5px] font-medium text-text">{value}</p></div>; }
function Config({ label, value, accent }) { return <div className="flex items-center justify-between gap-4 border-b border-line pb-3 last:border-0 last:pb-0"><span className="text-[10.5px] text-muted">{label}</span><span className={`font-mono text-[9.5px] ${accent ? "text-teal" : "text-text"}`}>{value}</span></div>; }
