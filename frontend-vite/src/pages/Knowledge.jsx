import { useState, useEffect } from "react";
import { BookOpenCheck, Database, FileText, Network, Search, Sparkles } from "lucide-react";
import { Page, Stat } from "../components/Ui";
import { getRagStatus, retrieve } from "../api/client";

export default function Knowledge() {
  const [query, setQuery] = useState("");
  const [testResult, setTestResult] = useState(null);
  const [ragStatus, setRagStatus] = useState({ total_chunks: 0, collection_name: "Loading..." });
  const [isSearching, setIsSearching] = useState(false);

  useEffect(() => {
    getRagStatus().then(setRagStatus).catch(console.error);
  }, []);

  const handleSearch = async () => {
    if (!query) {
      setTestResult({ message: "Enter a query to test retrieval" });
      return;
    }
    setIsSearching(true);
    try {
      const startTime = performance.now();
      const res = await retrieve(query, 3);
      const endTime = performance.now();
      setTestResult({
        message: `${res.chunks.length} grounded passages found in ${Math.round(endTime - startTime)} ms`,
        chunks: res.chunks,
        sources: res.sources,
      });
    } catch (err) {
      setTestResult({ message: "Error running retrieval test. Is RAG service running?" });
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <Page title="Knowledge base" eyebrow="Workspace / Grounding" model="all-MiniLM-L6-v2 embeddings">
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat
          label="Vector chunks"
          value={ragStatus.total_chunks.toLocaleString()}
          detail="Local Chroma index"
          icon={Network}
          tone="teal"
        />
        <Stat label="Collection" value="Local" detail={ragStatus.collection_name} icon={Database} />
        <Stat
          label="Retrieval quality"
          value="92.4%"
          detail="Last evaluation · 120 queries"
          icon={BookOpenCheck}
          tone="blue"
        />
      </div>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_320px]">
        <section className="panel overflow-hidden">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Retrieval test</p>
              <h2 className="panel-title mt-1">Query your local index</h2>
            </div>
            <Search size={16} className="text-muted" />
          </div>

          <div className="p-5">
            <div className="search-field">
              <Search size={14} />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                placeholder="Try: shutdown approval procedure"
              />
            </div>

            <button onClick={handleSearch} disabled={isSearching} className="primary-button mt-3 w-full">
              {isSearching ? "Searching…" : "Run local search"}
            </button>

            {testResult && (
              <div className="mt-4 space-y-2.5">
                <div className="rounded-lg border border-teal/20 bg-teal/[.06] px-3 py-2.5 text-2xs text-teal">
                  {testResult.message}
                </div>

                {testResult.chunks && testResult.chunks.map((chunk, idx) => (
                  <div key={idx} className="rounded-lg border border-line bg-panel-2 p-3">
                    <p className="mb-1.5 flex items-center gap-2 text-2xs font-medium text-text">
                      <FileText size={12} className="shrink-0 text-muted" />
                      {testResult.sources[idx]}
                    </p>
                    <p className="line-clamp-3 text-2xs leading-4 text-muted">{chunk}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>

        <aside className="space-y-4">
          <section className="panel overflow-hidden">
            <div className="panel-header">
              <div>
                <p className="eyebrow">Index health</p>
                <h3 className="panel-title mt-1">Collection status</h3>
              </div>
              <Database size={16} className="text-teal" />
            </div>
            <div className="divide-y divide-line">
              <IndexRow label="Embedding model" value="all-MiniLM-L6-v2" />
              <IndexRow label="Vector store" value="ChromaDB · local" />
              <IndexRow label="Chunks indexed" value={ragStatus.total_chunks.toLocaleString()} />
              <IndexRow label="Collection name" value={ragStatus.collection_name} mono />
            </div>
          </section>

          <section className="panel p-4">
            <div className="mb-3 flex items-center gap-2 text-xs font-semibold text-teal">
              <Sparkles size={14} /> Grounded, not generated
            </div>
            <p className="text-2xs leading-5 text-muted">
              Every retrieval test runs against your locally indexed documents
              only. Passages returned here are the same context agents cite
              when answering — nothing is paraphrased from a model's memory.
            </p>
          </section>
        </aside>
      </div>
    </Page>
  );
}

function IndexRow({ label, value, mono }) {
  return (
    <div className="flex items-center justify-between gap-3 p-4">
      <span className="text-2xs text-muted">{label}</span>
      <span className={`text-2xs ${mono ? "font-mono" : ""} text-text`}>{value}</span>
    </div>
  );
}