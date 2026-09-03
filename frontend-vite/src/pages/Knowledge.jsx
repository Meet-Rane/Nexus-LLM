import { useEffect, useState } from "react";
import { BookOpenCheck, Database, FolderKanban, Network, Search, Settings2 } from "lucide-react";
import { Badge, Page, Stat } from "../components/Ui";
import { getRagStatus, retrieveKnowledge } from "../api/client";

export default function Knowledge() {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getRagStatus().then(setStatus).catch(() => setStatus(null));
  }, []);

  const search = async (event) => {
    event.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    try {
      setResult({ ok: true, ...(await retrieveKnowledge(query.trim(), 5)) });
    } catch (error) {
      setResult({ ok: false, message: error.response?.data?.detail || error.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Page title="Knowledge base" eyebrow="Workspace / Grounding" model="all-MiniLM-L6-v2">
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat label="Collections" value={status ? "1" : "—"} detail={status?.collection_name || "RAG service unavailable"} icon={Database} />
        <Stat label="Vector chunks" value={status?.total_chunks ?? "—"} detail="Local Chroma index" icon={Network} tone="teal" />
        <Stat label="Retrieval" value={status ? "Ready" : "Offline"} detail="Traceable source metadata" icon={BookOpenCheck} tone="blue" />
      </div>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <section className="panel overflow-hidden">
          <div className="panel-header"><div><h2 className="panel-title">Retrieved passages</h2><p className="panel-subtitle">Matches returned directly by the local vector store</p></div><FolderKanban size={16} className="text-muted" /></div>
          <div className="divide-y divide-line">
            {!result && <div className="p-8 text-center text-xs text-muted">Run a local query to inspect grounded passages and their source files.</div>}
            {result?.ok && result.chunks.length === 0 && <div className="p-8 text-center text-xs text-muted">No indexed passages were found.</div>}
            {result?.ok && result.chunks.map((chunk, index) => (
              <article key={`${result.sources[index]}-${index}`} className="p-4 sm:p-5">
                <div className="mb-3 flex items-center justify-between gap-3"><Badge tone="teal">Match {index + 1}</Badge><span className="truncate font-mono text-[9px] text-muted">{result.sources[index]}</span></div>
                <p className="whitespace-pre-wrap text-[11px] leading-5 text-text">{chunk}</p>
              </article>
            ))}
            {result && !result.ok && <div className="m-4 rounded-lg border border-danger/20 bg-danger/[.06] p-3 text-xs text-danger">Retrieval failed: {result.message}</div>}
          </div>
        </section>

        <aside className="space-y-4">
          <section className="panel p-4">
            <div className="mb-3"><p className="eyebrow">Retrieval test</p><h2 className="panel-title mt-1">Query your local index</h2></div>
            <form onSubmit={search}>
              <div className="search-field"><Search size={14} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Try: isolation permit" /></div>
              <button disabled={loading || !query.trim()} className="primary-button mt-3 w-full">{loading ? "Searching locally…" : "Run local search"}</button>
            </form>
          </section>
          <section className="panel p-4">
            <div className="flex items-center justify-between"><p className="eyebrow">Index configuration</p><Settings2 size={14} className="text-muted" /></div>
            <div className="mt-4 space-y-3"><Config label="Embedding model" value="all-MiniLM-L6-v2" /><Config label="Vector store" value="ChromaDB" /><Config label="Chunk strategy" value="300 words · 50 overlap" /><Config label="External connectors" value="Disabled" accent /></div>
          </section>
        </aside>
      </div>
    </Page>
  );
}

function Config({ label, value, accent }) {
  return <div className="flex items-center justify-between gap-4 border-b border-line pb-3 last:border-0 last:pb-0"><span className="text-[10.5px] text-muted">{label}</span><span className={`font-mono text-[9.5px] ${accent ? "text-teal" : "text-text"}`}>{value}</span></div>;
}
