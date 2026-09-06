import { useEffect, useState } from "react";
import { Activity, AlertTriangle, Ban, Check, FileLock2, Fingerprint, LockKeyhole, Network, RadioTower, RefreshCw, RotateCcw, Server, ShieldCheck, WifiOff } from "lucide-react";
import { Badge, Page, Stat } from "../components/Ui";
import { getNetworkAudit, getOsNetworkMonitor, getRagStatus, getSystemStatus, pingAgent, resetOsNetworkMonitor } from "../api/client";

export default function Security() {
  const [checks, setChecks] = useState(null);
  const [lastChecked, setLastChecked] = useState("—");
  const [resetting, setResetting] = useState(false);

  const refresh = () => {
    loadProof().then((next) => {
      setChecks(next);
      setLastChecked(formatClock(new Date()));
    });
  };

  useEffect(() => {
    let active = true;
    loadProof().then((next) => {
      if (!active) return;
      setChecks(next);
      setLastChecked(formatClock(new Date()));
    });

    const timer = window.setInterval(() => {
      getOsNetworkMonitor()
        .then((osMonitor) => {
          if (!active) return;
          setChecks((current) => current ? { ...current, osMonitor } : current);
          setLastChecked(formatClock(new Date()));
        })
        .catch(() => {});
    }, 3000);

    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  const resetCapture = async () => {
    setResetting(true);
    try {
      const osMonitor = await resetOsNetworkMonitor();
      setChecks((current) => current ? { ...current, osMonitor } : current);
      setLastChecked(formatClock(new Date()));
    } finally {
      setResetting(false);
    }
  };

  const externalEvents = checks?.audit.filter((event) => !event.loopback).length || 0;
  const osMonitor = checks?.osMonitor;
  const nexusExternal = osMonitor?.observedNexusExternalConnections || 0;
  const monitorActive = Boolean(osMonitor?.enabled && osMonitor?.supported && osMonitor?.running);
  const localServices = (checks?.agentOnline ? 1 : 0) + (checks?.ragOnline ? 1 : 0);
  const policyVerified = Boolean(checks?.system?.localRuntimeConfigured && checks?.system?.localOnlyPolicyEnforced);
  const proofVerified = policyVerified && monitorActive && nexusExternal === 0;
  const bannerTone = proofVerified ? "teal" : nexusExternal ? "danger" : "accent";

  return (
    <Page title="Sovereignty & audit" eyebrow="Governance / Security posture" model="OS + application audit"
      actions={<button onClick={refresh} className="secondary-button"><RefreshCw size={14} /> Refresh proof</button>}>
      <div className={`mb-5 rounded-xl border p-4 sm:flex sm:items-center sm:justify-between sm:gap-5 sm:p-5 ${bannerClass(bannerTone)}`}>
        <div className="flex items-start gap-3"><div className={`icon-box h-10 w-10 icon-box-${bannerTone}`}>{proofVerified ? <ShieldCheck size={20} /> : <AlertTriangle size={20} />}</div><div><div className="flex flex-wrap items-center gap-2"><h2 className="text-[14px] font-semibold text-white">{proofVerified ? "Nexus OS egress evidence is clean" : nexusExternal ? "External Nexus connections were observed" : "Boundary needs attention"}</h2><Badge tone={bannerTone} dot>{proofVerified ? "Verified" : nexusExternal ? "Review" : "Incomplete"}</Badge></div><p className="mt-1.5 max-w-3xl text-[11px] leading-5 text-muted">{proofVerified ? "The Windows TCP table is monitoring the whole machine and has recorded no external connection owned by a tracked Nexus process. Unrelated host traffic remains visible below for full transparency." : nexusExternal ? "A tracked Nexus process reached a non-loopback endpoint. Review its process, PID, and destination below before continuing the sovereign demo." : "Start the backend on Windows and confirm the OS monitor, local-only policy, and local services are available."}</p></div></div>
        <div className="mt-4 shrink-0 text-left sm:mt-0 sm:text-right"><p className="eyebrow">Last sampled</p><p className={`mt-1 font-mono text-[10px] ${proofVerified ? "text-teal" : nexusExternal ? "text-danger" : "text-accent"}`}>{lastChecked}</p></div>
      </div>

      <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Nexus external evidence" value={osMonitor ? nexusExternal : "—"} detail="Tracked processes since reset" icon={WifiOff} tone={nexusExternal ? "danger" : "teal"} />
        <Stat label="Nexus active TCP" value={osMonitor?.currentNexusConnections ?? "—"} detail={`${osMonitor?.currentNexusLoopbackConnections ?? 0} loopback · ${osMonitor?.currentNexusExternalConnections ?? 0} external`} icon={RadioTower} tone="blue" />
        <Stat label="Host external TCP" value={osMonitor?.currentExternalConnections ?? "—"} detail={`${osMonitor?.currentConnections ?? 0} active across Windows`} icon={Network} tone="accent" />
        <Stat label="Monitor samples" value={osMonitor?.sampleCount ?? "—"} detail={`${osMonitor?.trackedNexusProcesses ?? 0} tracked · ${localServices} / 2 services`} icon={Activity} tone="accent" />
      </div>

      <section className="panel mb-5 overflow-hidden">
        <div className="panel-header flex-wrap"><div><div className="flex items-center gap-2"><h2 className="panel-title">OS-wide TCP evidence</h2><span className={`h-1.5 w-1.5 rounded-full ${monitorActive ? "animate-pulse-soft bg-teal" : "bg-danger"}`} /></div><p className="panel-subtitle">Windows connection table sampled every two seconds across all processes</p></div><div className="flex items-center gap-2"><Badge tone={monitorActive ? "teal" : "danger"} dot>{monitorActive ? "CAPTURING" : "UNAVAILABLE"}</Badge><button type="button" onClick={resetCapture} disabled={!monitorActive || resetting} className="secondary-button"><RotateCcw size={13} className={resetting ? "animate-spin" : ""} /> Reset capture</button></div></div>
        {osMonitor?.error && <div className="border-b border-danger/20 bg-danger/[.05] px-4 py-3 text-[10.5px] text-danger">Monitor error: {osMonitor.error}</div>}
        <div className="overflow-x-auto"><table className="data-table min-w-[1000px]"><thead><tr><th>Last seen</th><th>Process</th><th>Scope</th><th>PID</th><th>Local endpoint</th><th>Remote endpoint</th><th>State</th><th>Samples</th><th>Verdict</th></tr></thead><tbody>{osMonitor?.connections?.map((connection) => <tr key={`${connection.pid}-${connection.localAddress}-${connection.remoteAddress}`}><td className="font-mono text-[9.5px]!">{formatTime(connection.lastSeen)}</td><td className="text-text!">{connection.processName}</td><td><Badge tone={connection.nexusProcess ? "blue" : "neutral"}>{connection.nexusProcess ? "NEXUS" : "HOST"}</Badge></td><td className="font-mono text-[9.5px]!">{connection.pid}</td><td className="font-mono text-[9.5px]!">{connection.localAddress}</td><td className="font-mono text-[9.5px]!">{connection.remoteAddress}</td><td>{connection.state}</td><td className="font-mono text-[9.5px]!">{connection.samples}</td><td><Badge tone={connection.loopback ? "teal" : "danger"}>{connection.loopback ? <Check size={10} /> : <Ban size={10} />}{connection.loopback ? "LOOPBACK" : "EXTERNAL"}</Badge></td></tr>)}</tbody></table></div>
        {osMonitor && !osMonitor.connections?.length && <div className="p-8 text-center text-xs text-muted">No active TCP connections have been observed since this capture began.</div>}
        {!osMonitor && <div className="p-8 text-center text-xs text-muted">Loading the Windows network monitor…</div>}
        <div className="flex items-center gap-2 border-t border-line bg-base/35 px-4 py-3 text-[10px] text-muted"><RadioTower size={13} className="text-accent" />Connection-state evidence only: no payload inspection. Very short TCP connections between samples and UDP traffic require Windows Firewall, WFP, or packet-capture evidence.</div>
      </section>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <section className="panel overflow-hidden">
          <div className="panel-header"><div><div className="flex items-center gap-2"><h2 className="panel-title">Application egress audit</h2><span className="h-1.5 w-1.5 animate-pulse-soft rounded-full bg-teal" /></div><p className="panel-subtitle">Destinations recorded for Spring model and RAG operations</p></div><Network size={16} className="text-teal" /></div>
          <div className="overflow-x-auto"><table className="data-table min-w-[680px]"><thead><tr><th>Time</th><th>Target</th><th>Destination</th><th>Operation</th><th>Verdict</th></tr></thead><tbody>{checks?.audit.map((event, index) => <tr key={`${event.timestamp}-${index}`}><td className="font-mono text-[9.5px]!">{formatTime(event.timestamp)}</td><td className="text-text!">{event.target}</td><td className="font-mono text-[9.5px]!">{event.endpoint}</td><td>{event.operation}</td><td><Badge tone={event.loopback ? "teal" : "danger"}>{event.loopback ? <Check size={10} /> : <Ban size={10} />}{event.loopback ? "LOOPBACK" : "EXTERNAL"}</Badge></td></tr>)}</tbody></table></div>
          {checks && checks.audit.length === 0 && <div className="border-t border-line p-8 text-center text-xs text-muted">Send a chat or grounded retrieval request to populate the audit.</div>}
          <div className="flex items-center gap-2 border-t border-line bg-base/35 px-4 py-3 text-[10px] text-muted"><Activity size={13} className="text-teal" />Spring instrumentation and the independent OS connection table provide two separate evidence layers.</div>
        </section>

        <aside className="space-y-4">
          <section className="panel overflow-hidden"><div className="panel-header"><div><p className="eyebrow">Control plane</p><h2 className="panel-title mt-1">Boundary controls</h2></div><LockKeyhole size={17} className="text-teal" /></div><div className="divide-y divide-line"><Control icon={Network} title="Local-only startup" value={checks?.system?.localOnlyPolicyEnforced ? "ENFORCED" : "UNKNOWN"} ok={checks?.system?.localOnlyPolicyEnforced} /><Control icon={FileLock2} title="Sandbox network" value={checks?.system?.sandboxNetworkDisabled ? "NONE" : "UNKNOWN"} ok={checks?.system?.sandboxNetworkDisabled} /><Control icon={Server} title="Docker runtime" value={checks?.system?.dockerAvailable ? "AVAILABLE" : "MISSING"} ok={checks?.system?.dockerAvailable} /><Control icon={Fingerprint} title="Tesseract OCR" value={checks?.system?.tesseractAvailable ? "AVAILABLE" : "MISSING"} ok={checks?.system?.tesseractAvailable} /><Control icon={RadioTower} title="OS TCP capture" value={monitorActive ? "ACTIVE" : "NEEDED"} ok={monitorActive} /></div></section>
          <section className="terminal overflow-hidden"><div className="terminal-bar"><span className="terminal-dot" /><span className="terminal-dot" /><span className="terminal-dot" /><span className="ml-2 text-[8px] uppercase tracking-widest text-faint">policy verification</span></div><div className="p-3.5"><p className={policyVerified ? "text-teal" : "text-accent"}>{policyVerified ? "✓ loopback endpoints enforced" : "! endpoint policy unavailable"}</p><p className={checks?.system?.sandboxNetworkDisabled ? "text-teal" : "text-accent"}>{checks?.system?.sandboxNetworkDisabled ? "✓ sandbox network: none" : "! sandbox policy unavailable"}</p><p className={externalEvents === 0 ? "text-teal" : "text-danger"}>{externalEvents === 0 ? "✓ application external: 0" : `! application external: ${externalEvents}`}</p><p className={!monitorActive ? "text-accent" : nexusExternal === 0 ? "text-teal" : "text-danger"}>{!monitorActive ? "! OS TCP monitor unavailable" : nexusExternal === 0 ? "✓ Nexus external TCP: 0 observed" : `! Nexus external TCP: ${nexusExternal} observed`}</p></div></section>
        </aside>
      </div>
    </Page>
  );
}

async function loadProof() {
  const [system, agent, rag, audit, osMonitor] = await Promise.allSettled([
    getSystemStatus(),
    pingAgent(),
    getRagStatus(),
    getNetworkAudit(),
    getOsNetworkMonitor(),
  ]);
  return {
    system: system.status === "fulfilled" ? system.value : null,
    agentOnline: agent.status === "fulfilled",
    ragOnline: rag.status === "fulfilled",
    audit: audit.status === "fulfilled" ? audit.value : [],
    osMonitor: osMonitor.status === "fulfilled" ? osMonitor.value : null,
  };
}

function bannerClass(tone) {
  if (tone === "teal") return "border-teal/25 bg-teal/[.055]";
  if (tone === "danger") return "border-danger/25 bg-danger/[.055]";
  return "border-accent/25 bg-accent/[.055]";
}

function formatClock(date) {
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
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
