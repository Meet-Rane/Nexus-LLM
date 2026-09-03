import { useEffect, useState } from "react";
import { Activity, AlertTriangle, Ban, Check, FileLock2, Fingerprint, LockKeyhole, Network, RefreshCw, Server, ShieldCheck, WifiOff } from "lucide-react";
import { Badge, Page, Stat } from "../components/Ui";
import { getNetworkAudit, getRagStatus, getSystemStatus, pingAgent } from "../api/client";

export default function Security() {
  const [checks, setChecks] = useState(null);
  const [lastChecked, setLastChecked] = useState("—");

  const refresh = () => {
    Promise.allSettled([getSystemStatus(), pingAgent(), getRagStatus(), getNetworkAudit()]).then(([system, agent, rag, audit]) => {
      setChecks({
        system: system.status === "fulfilled" ? system.value : null,
        agentOnline: agent.status === "fulfilled",
        ragOnline: rag.status === "fulfilled",
        audit: audit.status === "fulfilled" ? audit.value : [],
      });
      setLastChecked(new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }));
    });
  };

  useEffect(() => {
    Promise.allSettled([getSystemStatus(), pingAgent(), getRagStatus(), getNetworkAudit()]).then(([system, agent, rag, audit]) => {
      setChecks({
        system: system.status === "fulfilled" ? system.value : null,
        agentOnline: agent.status === "fulfilled",
        ragOnline: rag.status === "fulfilled",
        audit: audit.status === "fulfilled" ? audit.value : [],
      });
      setLastChecked(new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }));
    });
  }, []);

  const externalEvents = checks?.audit.filter((event) => !event.loopback).length || 0;
  const localServices = (checks?.agentOnline ? 1 : 0) + (checks?.ragOnline ? 1 : 0);
  const dependencies = (checks?.system?.dockerAvailable ? 1 : 0) + (checks?.system?.tesseractAvailable ? 1 : 0);
  const policyVerified = Boolean(checks?.system?.localRuntimeConfigured && checks?.system?.localOnlyPolicyEnforced);

  return (
    <Page title="Sovereignty & audit" eyebrow="Governance / Security posture" model="Local-only policy"
      actions={<button onClick={refresh} className="secondary-button"><RefreshCw size={14} /> Refresh proof</button>}>
      <div className={`mb-5 rounded-xl border p-4 sm:flex sm:items-center sm:justify-between sm:gap-5 sm:p-5 ${policyVerified ? "border-teal/25 bg-teal/[.055]" : "border-accent/25 bg-accent/[.055]"}`}>
        <div className="flex items-start gap-3"><div className={`icon-box h-10 w-10 ${policyVerified ? "icon-box-teal" : "icon-box-accent"}`}>{policyVerified ? <ShieldCheck size={20} /> : <AlertTriangle size={20} />}</div><div><div className="flex flex-wrap items-center gap-2"><h2 className="text-[14px] font-semibold text-white">{policyVerified ? "Application boundary verified" : "Boundary needs attention"}</h2><Badge tone={policyVerified ? "teal" : "accent"} dot>{policyVerified ? "Enforced" : "Review"}</Badge></div><p className="mt-1.5 max-w-2xl text-[11px] leading-5 text-muted">Spring rejects non-loopback model and RAG endpoints, records application destinations, and disables sandbox networking. Machine-wide packet capture remains a separate deployment requirement.</p></div></div>
        <div className="mt-4 shrink-0 text-left sm:mt-0 sm:text-right"><p className="eyebrow">Last checked</p><p className={`mt-1 font-mono text-[10px] ${policyVerified ? "text-teal" : "text-accent"}`}>{lastChecked}</p></div>
      </div>

      <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="External audit events" value={externalEvents} detail="Recorded by Spring" icon={WifiOff} tone={externalEvents ? "accent" : "teal"} />
        <Stat label="Local services" value={`${localServices} / 2`} detail="Spring agent and RAG" icon={Server} tone="blue" />
        <Stat label="Host dependencies" value={`${dependencies} / 2`} detail="Docker and Tesseract" icon={Fingerprint} tone={dependencies === 2 ? "teal" : "accent"} />
        <Stat label="Audit events" value={checks?.audit.length ?? "—"} detail="Router, model and RAG" icon={Activity} tone="teal" />
      </div>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <section className="panel overflow-hidden">
          <div className="panel-header"><div><div className="flex items-center gap-2"><h2 className="panel-title">Application egress audit</h2><span className="h-1.5 w-1.5 animate-pulse-soft rounded-full bg-teal" /></div><p className="panel-subtitle">Destinations recorded for Spring model and RAG operations</p></div><Network size={16} className="text-teal" /></div>
          <div className="overflow-x-auto"><table className="data-table min-w-[680px]"><thead><tr><th>Time</th><th>Target</th><th>Destination</th><th>Operation</th><th>Verdict</th></tr></thead><tbody>{checks?.audit.map((event, index) => <tr key={`${event.timestamp}-${index}`}><td className="font-mono text-[9.5px]!">{formatTime(event.timestamp)}</td><td className="text-text!">{event.target}</td><td className="font-mono text-[9.5px]!">{event.endpoint}</td><td>{event.operation}</td><td><Badge tone={event.loopback ? "teal" : "danger"}>{event.loopback ? <Check size={10} /> : <Ban size={10} />}{event.loopback ? "LOOPBACK" : "EXTERNAL"}</Badge></td></tr>)}</tbody></table></div>
          {checks && checks.audit.length === 0 && <div className="border-t border-line p-8 text-center text-xs text-muted">Send a chat or grounded retrieval request to populate the audit.</div>}
          <div className="flex items-center gap-2 border-t border-line bg-base/35 px-4 py-3 text-[10px] text-muted"><Activity size={13} className="text-teal" />Application audit only; use an OS firewall or packet monitor for final venue evidence.</div>
        </section>

        <aside className="space-y-4">
          <section className="panel overflow-hidden"><div className="panel-header"><div><p className="eyebrow">Control plane</p><h2 className="panel-title mt-1">Boundary controls</h2></div><LockKeyhole size={17} className="text-teal" /></div><div className="divide-y divide-line"><Control icon={Network} title="Local-only startup" value={checks?.system?.localOnlyPolicyEnforced ? "ENFORCED" : "UNKNOWN"} ok={checks?.system?.localOnlyPolicyEnforced} /><Control icon={FileLock2} title="Sandbox network" value={checks?.system?.sandboxNetworkDisabled ? "NONE" : "UNKNOWN"} ok={checks?.system?.sandboxNetworkDisabled} /><Control icon={Server} title="Docker runtime" value={checks?.system?.dockerAvailable ? "AVAILABLE" : "MISSING"} ok={checks?.system?.dockerAvailable} /><Control icon={Fingerprint} title="Tesseract OCR" value={checks?.system?.tesseractAvailable ? "AVAILABLE" : "MISSING"} ok={checks?.system?.tesseractAvailable} /><Control icon={WifiOff} title="OS egress capture" value={checks?.system?.egressMonitorEnabled ? "ACTIVE" : "NEEDED"} ok={checks?.system?.egressMonitorEnabled} /></div></section>
          <section className="terminal overflow-hidden"><div className="terminal-bar"><span className="terminal-dot" /><span className="terminal-dot" /><span className="terminal-dot" /><span className="ml-2 text-[8px] uppercase tracking-widest text-faint">policy verification</span></div><div className="p-3.5"><p className={policyVerified ? "text-teal" : "text-accent"}>{policyVerified ? "✓ loopback endpoints enforced" : "! endpoint policy unavailable"}</p><p className={checks?.system?.sandboxNetworkDisabled ? "text-teal" : "text-accent"}>{checks?.system?.sandboxNetworkDisabled ? "✓ sandbox network: none" : "! sandbox policy unavailable"}</p><p className={externalEvents === 0 ? "text-teal" : "text-danger"}>{externalEvents === 0 ? "✓ no external audit events" : `! ${externalEvents} external events`}</p><p className="mt-1 text-accent">! OS-wide monitor still required</p></div></section>
        </aside>
      </div>
    </Page>
  );
}

function formatTime(timestamp) {
  try {
    return new Date(timestamp).toLocaleTimeString([], { hour12: false });
  } catch {
    return timestamp;
  }
}

function Control({ icon: Icon, title, value, ok }) {
  return <div className="flex items-center gap-3 p-4"><div className={`icon-box ${ok ? "icon-box-teal" : "icon-box-accent"}`}><Icon size={15} /></div><span className="flex-1 text-[11px] text-muted">{title}</span><span className={`font-mono text-[9.5px] ${ok ? "text-teal" : "text-accent"}`}>{value}</span></div>;
}
