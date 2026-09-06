import { useEffect, useMemo, useRef, useState } from "react";
import { FileImage, FileSpreadsheet, FileText, HardDrive, RefreshCw, Search, Trash2, Upload, X } from "lucide-react";
import { Badge, Page, Stat } from "../components/Ui";
import { deleteDocument, getRagStatus, ingestFile, listDocuments } from "../api/client";
import { useAuth } from "../auth/useAuth";

const SUPPORTED_FORMATS = ".pdf,.png,.jpg,.jpeg,.tiff,.bmp,.txt,.docx,.xlsx,.xlsm";

export default function Documents() {
  const { hasRole } = useAuth();
  const [documents, setDocuments] = useState([]);
  const [query, setQuery] = useState("");
  const [format, setFormat] = useState("ALL");
  const [ragStatus, setRagStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState("");
  const fileRef = useRef(null);
  const canDelete = hasRole("ROLE_MODERATOR", "ROLE_ADMIN");

  const visible = useMemo(() => documents.filter((item) => {
    const matchesQuery = item.name.toLowerCase().includes(query.toLowerCase());
    const matchesFormat = format === "ALL" || item.type === format;
    return matchesQuery && matchesFormat;
  }), [documents, query, format]);

  const refresh = async () => {
    setLoading(true);
    try {
      const [items, status] = await Promise.all([listDocuments(), getRagStatus()]);
      setDocuments(Array.isArray(items) ? items : []);
      setRagStatus(status);
    } catch {
      setRagStatus(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    Promise.all([listDocuments(), getRagStatus()])
      .then(([items, status]) => {
        setDocuments(Array.isArray(items) ? items : []);
        setRagStatus(status);
      })
      .catch(() => setRagStatus(null))
      .finally(() => setLoading(false));
  }, []);

  const addFiles = async (event) => {
    const files = Array.from(event.target.files || []);
    event.target.value = "";
    if (!files.length) return;

    const additions = files.map((file) => ({
      id: `pending-${file.name}-${file.lastModified}`,
      name: file.name,
      type: file.name.split(".").pop()?.toUpperCase() || "FILE",
      size: file.size,
      pages: 0,
      addedAt: new Date().toISOString(),
      status: "Processing",
      chunks: 0,
    }));
    setDocuments((current) => [...additions, ...current.filter((item) => !files.some((file) => file.name === item.name))]);

    const failures = [];
    await Promise.all(files.map(async (file) => {
      try {
        const result = await ingestFile(file);
        setDocuments((current) => current.map((item) => item.name === file.name ? {
          ...item,
          status: "Indexed",
          chunks: result.chunks_stored,
          replaced: result.replaced_existing,
        } : item));
      } catch (error) {
        const detail = error?.response?.data?.detail || error?.message || "Document indexing failed";
        failures.push({
          ...additions.find((item) => item.name === file.name),
          status: "Failed",
          error: detail,
        });
        setDocuments((current) => current.map((item) => item.name === file.name ? { ...item, status: "Failed", error: detail } : item));
      }
    }));
    try {
      const [items, status] = await Promise.all([listDocuments(), getRagStatus()]);
      setDocuments([...failures, ...(Array.isArray(items) ? items : [])]);
      setRagStatus(status);
    } catch {
      setRagStatus(null);
    }
  };

  const remove = async (item) => {
    if (!window.confirm(`Remove ${item.name} and all of its indexed chunks?`)) return;
    setDeleting(item.name);
    try {
      await deleteDocument(item.name);
      await refresh();
    } finally {
      setDeleting("");
    }
  };

  return (
    <Page title="Documents" eyebrow="Workspace / Local files" model="Local OCR + RAG"
      actions={<><button className="secondary-button" onClick={refresh} disabled={loading}><RefreshCw size={14} className={loading ? "animate-spin" : ""} /> Refresh</button><button className="primary-button" onClick={() => fileRef.current?.click()}><Upload size={14} /> Import</button><input ref={fileRef} className="hidden" type="file" multiple accept={SUPPORTED_FORMATS} onChange={addFiles} /></>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat label="Documents" value={documents.length} detail="Persistent local knowledge sources" icon={FileText} />
        <Stat label="Vector chunks" value={ragStatus?.total_chunks ?? "—"} detail={ragStatus ? "Local embeddings available" : "RAG service unavailable"} icon={FileImage} tone="teal" />
        <Stat label="Storage node" value={ragStatus ? "Online" : "Offline"} detail={ragStatus?.collection_name || "Local Chroma persistent store"} icon={HardDrive} tone="blue" />
      </div>

      <section className="panel overflow-hidden">
        <div className="panel-header flex-wrap">
          <div><h2 className="panel-title">Document library</h2><p className="panel-subtitle">PDF, image, text, Word, and Excel sources available to local agents</p></div>
          <div className="flex w-full gap-2 sm:w-auto">
            <select className="secondary-button w-28 bg-panel-2" value={format} onChange={(event) => setFormat(event.target.value)}><option value="ALL">All formats</option>{[...new Set(documents.map((item) => item.type))].sort().map((type) => <option key={type}>{type}</option>)}</select>
            <div className="search-field min-w-0 flex-1 sm:w-64"><Search size={14} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search documents" />{query && <button onClick={() => setQuery("")} aria-label="Clear search"><X size={13} /></button>}</div>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="data-table min-w-[760px]">
            <thead><tr><th>Document</th><th>Type</th><th>Pages / sheets</th><th>Added</th><th>Status</th><th /></tr></thead>
            <tbody>{visible.map((item) => { const Icon = iconFor(item); return (
              <tr key={item.id || item.name}>
                <td><div className="flex items-center gap-3"><div className="icon-box icon-box-accent"><Icon size={15} /></div><div><p className="font-medium text-text">{item.name}</p><p className="mt-0.5 font-mono text-[9px] text-faint">{formatBytes(item.size)} · {item.chunks || 0} chunks</p></div></div></td>
                <td>{item.type}</td><td>{item.pages || "—"}</td><td>{formatDate(item.addedAt)}</td>
                <td><div><Badge tone={item.status === "Processing" ? "accent" : item.status === "Failed" ? "danger" : "teal"} dot>{item.status}</Badge>{item.replaced && <p className="mt-1 text-[10px] text-muted">Previous index replaced</p>}{item.error && <p className="mt-1 max-w-xs text-[10px] leading-4 text-danger">{item.error}</p>}</div></td>
                <td>{canDelete && item.status !== "Processing" && <button type="button" className="icon-button" onClick={() => remove(item)} disabled={deleting === item.name} title="Delete indexed document"><Trash2 size={14} /></button>}</td>
              </tr>
            ); })}</tbody>
          </table>
          {loading && !documents.length && <div className="py-12 text-center text-xs text-muted">Loading the local document library…</div>}
          {!loading && !visible.length && <div className="py-12 text-center text-xs text-muted">{query || format !== "ALL" ? "No documents match the current filters." : "Import a document to add it to the local knowledge base."}</div>}
        </div>
      </section>
    </Page>
  );
}

function iconFor(item) {
  if (["XLSX", "XLSM"].includes(item.type)) return FileSpreadsheet;
  if (["PNG", "JPG", "JPEG", "TIFF", "BMP"].includes(item.type)) return FileImage;
  return FileText;
}

function formatBytes(bytes) {
  if (!bytes) return "Unknown size";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
  if (!value) return "Previously indexed";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Previously indexed" : date.toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}
