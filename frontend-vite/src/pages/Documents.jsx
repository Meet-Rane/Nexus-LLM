import { useEffect, useMemo, useRef, useState } from "react";
import { FileImage, FileText, Filter, HardDrive, Search, Upload, X } from "lucide-react";
import { Badge, Page, Stat, TableActions } from "../components/Ui";
import { getRagStatus, ingestFile } from "../api/client";

export default function Documents() {
  const [documents, setDocuments] = useState([]);
  const [query, setQuery] = useState("");
  const [ragStatus, setRagStatus] = useState(null);
  const fileRef = useRef(null);
  const visible = useMemo(() => documents.filter((item) => item.name.toLowerCase().includes(query.toLowerCase())), [documents, query]);

  useEffect(() => {
    getRagStatus().then(setRagStatus).catch(() => setRagStatus(null));
  }, []);

  const addFiles = async (event) => {
    const files = Array.from(event.target.files || []);
    const additions = files.map((file) => ({
      name: file.name,
      type: file.name.split(".").pop()?.toUpperCase() || "FILE",
      size: `${(file.size / (1024 * 1024)).toFixed(1)} MB`,
      pages: "—",
      added: "Just now",
      status: "Processing",
      icon: file.type.startsWith("image/") ? FileImage : FileText,
    }));
    setDocuments((current) => [...additions, ...current]);
    event.target.value = "";
    await Promise.all(files.map(async (file) => {
      try {
        await ingestFile(file);
        setDocuments((current) => current.map((item) => item.name === file.name ? { ...item, status: "Indexed" } : item));
        getRagStatus().then(setRagStatus).catch(() => {});
      } catch (error) {
        const detail = error?.response?.data?.detail || error?.message || "Document indexing failed";
        setDocuments((current) => current.map((item) => item.name === file.name ? { ...item, status: "Failed", error: detail } : item));
      }
    }));
  };

  return (
    <Page title="Documents" eyebrow="Workspace / Local files" model="Local OCR + RAG"
      actions={<><button className="secondary-button hidden sm:inline-flex"><Filter size={14} /> Filter</button><button className="primary-button" onClick={() => fileRef.current?.click()}><Upload size={14} /> Import</button><input ref={fileRef} className="hidden" type="file" multiple accept=".pdf,.png,.jpg,.jpeg,.tiff,.bmp,.txt" onChange={addFiles} /></>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat label="This session" value={documents.length} detail="Files imported from this browser" icon={FileText} />
        <Stat label="Vector chunks" value={ragStatus?.total_chunks ?? "—"} detail={ragStatus ? "Local embeddings available" : "RAG service unavailable"} icon={FileImage} tone="teal" />
        <Stat label="Storage node" value={ragStatus ? "Online" : "Offline"} detail={ragStatus?.collection_name || "Local Chroma persistent store"} icon={HardDrive} tone="blue" />
      </div>

      <section className="panel overflow-hidden">
        <div className="panel-header flex-wrap">
          <div><h2 className="panel-title">Document library</h2><p className="panel-subtitle">Confidential source files available to local agents</p></div>
          <div className="search-field w-full sm:w-64"><Search size={14} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search documents" />{query && <button onClick={() => setQuery("")} aria-label="Clear search"><X size={13} /></button>}</div>
        </div>
        <div className="overflow-x-auto">
          <table className="data-table min-w-[720px]">
            <thead><tr><th>Document</th><th>Type</th><th>Pages</th><th>Added</th><th>Status</th><th /></tr></thead>
            <tbody>{visible.map((item) => { const Icon = item.icon; return (
              <tr key={item.name}>
                <td><div className="flex items-center gap-3"><div className="icon-box icon-box-accent"><Icon size={15} /></div><div><p className="font-medium text-text">{item.name}</p><p className="mt-0.5 font-mono text-[9px] text-faint">{item.size}</p></div></div></td>
                <td>{item.type}</td><td>{item.pages}</td><td>{item.added}</td>
                <td><div><Badge tone={item.status === "Processing" ? "accent" : item.status === "Failed" ? "danger" : "teal"} dot>{item.status}</Badge>{item.error && <p className="mt-1 max-w-xs text-[10px] leading-4 text-danger">{item.error}</p>}</div></td><td><TableActions /></td>
              </tr>
            ); })}</tbody>
          </table>
          {!visible.length && <div className="py-12 text-center text-xs text-muted">{query ? `No imported documents match “${query}”.` : "Import a document to add it to the local knowledge base."}</div>}
        </div>
      </section>
    </Page>
  );
}
