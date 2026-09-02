import { useMemo, useRef, useState } from "react";
import { FileImage, FileSpreadsheet, FileText, Filter, HardDrive, Search, Upload, X } from "lucide-react";
import { Badge, Page, Stat, TableActions } from "../components/Ui";
import { ingestFile } from "../api/client";

const seedDocuments = [
  { name: "UT_Inspection_Report_R-401.pdf", type: "PDF", size: "8.4 MB", pages: 42, added: "Today, 09:42", status: "Indexed", icon: FileText },
  { name: "P&ID_CDU_Rev-12.png", type: "Drawing", size: "14.2 MB", pages: 1, added: "Today, 09:18", status: "Vision ready", icon: FileImage },
  { name: "Vendor_Technical_Bid.xlsx", type: "Workbook", size: "2.1 MB", pages: 8, added: "Yesterday", status: "Indexed", icon: FileSpreadsheet },
  { name: "Shutdown_SOP_2026.pdf", type: "PDF", size: "3.7 MB", pages: 28, added: "28 Aug 2026", status: "Indexed", icon: FileText },
  { name: "Corrosion_Loop_Study.pdf", type: "PDF", size: "11.9 MB", pages: 76, added: "26 Aug 2026", status: "Indexed", icon: FileText },
];

export default function Documents() {
  const [documents, setDocuments] = useState(seedDocuments);
  const [query, setQuery] = useState("");
  const fileRef = useRef(null);
  const visible = useMemo(() => documents.filter((item) => item.name.toLowerCase().includes(query.toLowerCase())), [documents, query]);

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
      } catch {
        setDocuments((current) => current.map((item) => item.name === file.name ? { ...item, status: "RAG offline" } : item));
      }
    }));
  };

  return (
    <Page title="Documents" eyebrow="Workspace / Local files" model="Vision + OCR"
      actions={<><button className="secondary-button hidden sm:inline-flex"><Filter size={14} /> Filter</button><button className="primary-button" onClick={() => fileRef.current?.click()}><Upload size={14} /> Import</button><input ref={fileRef} className="hidden" type="file" multiple onChange={addFiles} /></>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Stat label="Local documents" value={documents.length} detail="41.6 MB encrypted storage" icon={FileText} />
        <Stat label="Pages indexed" value="154" detail="OCR and embeddings complete" icon={FileImage} tone="teal" />
        <Stat label="Storage node" value="Online" detail="nexus-data-01 · AES-256" icon={HardDrive} tone="blue" />
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
                <td><Badge tone={item.status === "Processing" ? "accent" : item.status === "RAG offline" ? "danger" : "teal"} dot>{item.status}</Badge></td><td><TableActions /></td>
              </tr>
            ); })}</tbody>
          </table>
          {!visible.length && <div className="py-12 text-center text-xs text-muted">No documents match “{query}”.</div>}
        </div>
      </section>
    </Page>
  );
}
