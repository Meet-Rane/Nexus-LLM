import { useState } from "react";
import { Activity, Ban, Check, Clock3, Download, FileLock2, Fingerprint, LockKeyhole, Network, RefreshCw, Server, ShieldCheck, WifiOff } from "lucide-react";
import { Badge, Page, Stat } from "../components/Ui";

const logLines = [
  { time: "09:47:22.184", process: "nexus-agent", destination: "127.0.0.1:11434", protocol: "TCP", result: "LOCAL" },
  { time: "09:47:21.906", process: "nexus-rag", destination: "127.0.0.1:8001", protocol: "HTTP", result: "LOCAL" },
  { time: "09:47:19.331", process: "docx-worker", destination: "filesystem://artifacts", protocol: "IPC", result: "LOCAL" },
  { time: "09:47:18.402", process: "vision-worker", destination: "127.0.0.1:11434", protocol: "TCP", result: "LOCAL" },
  { time: "09:47:16.821", process: "nexus-agent", destination: "0.0.0.0/0", protocol: "EGRESS", result: "BLOCKED" },
];

export default function Security() {
  const [monitoring, setMonitoring] = useState(true);
  const [lastChecked, setLastChecked] = useState("Just now");
  const refresh = () => setLastChecked(new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }));
  return (
    <Page title="Sovereignty & audit" eyebrow="Governance / Security posture" model="Egress policy enforced"
      actions={<button onClick={refresh} className="secondary-button"><RefreshCw size={14} /> Refresh proof</button>}>
      <div className="mb-5 rounded-xl border border-teal/25 bg-teal/[.055] p-4 sm:flex sm:items-center sm:justify-between sm:gap-5 sm:p-5"><div className="flex items-start gap-3"><div className="icon-box icon-box-teal h-10 w-10"><ShieldCheck size={20} /></div><div><div className="flex flex-wrap items-center gap-2"><h2 className="text-[14px] font-semibold text-white">Sovereign boundary verified</h2><Badge tone="teal" dot>Protected</Badge></div><p className="mt-1.5 max-w-2xl text-[11px] leading-5 text-muted">Model inference, OCR, retrieval, code execution and artifact generation are confined to this on-premise host. Default-deny firewall policy blocks external egress.</p></div></div><div className="mt-4 shrink-0 text-left sm:mt-0 sm:text-right"><p className="eyebrow">Last attested</p><p className="mt-1 font-mono text-[10px] text-teal">{lastChecked}</p></div></div>

      <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-4"><Stat label="External requests" value="0" detail="Current session" icon={WifiOff} tone="teal" /><Stat label="Blocked attempts" value="1" detail="Policy test event" icon={Ban} tone="accent" /><Stat label="Local services" value="6 / 6" detail="Healthy and isolated" icon={Server} tone="blue" /><Stat label="Audit coverage" value="100%" detail="Tools and artifacts" icon={Fingerprint} tone="teal" /></div>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <section className="panel overflow-hidden"><div className="panel-header"><div><div className="flex items-center gap-2"><h2 className="panel-title">Live network monitor</h2><span className={`h-1.5 w-1.5 rounded-full ${monitoring ? "animate-pulse-soft bg-teal" : "bg-faint"}`} /></div><p className="panel-subtitle">Process-level connections from the Nexus runtime</p></div><button onClick={() => setMonitoring(!monitoring)} className="ghost-button">{monitoring ? "Pause" : "Resume"}</button></div>
          <div className="overflow-x-auto"><table className="data-table min-w-[680px]"><thead><tr><th>Time</th><th>Process</th><th>Destination</th><th>Protocol</th><th>Verdict</th></tr></thead><tbody>{logLines.map((line) => <tr key={`${line.time}-${line.process}`}><td className="font-mono text-[9.5px]!">{line.time}</td><td className="text-text!">{line.process}</td><td className="font-mono text-[9.5px]!">{line.destination}</td><td>{line.protocol}</td><td><Badge tone={line.result === "LOCAL" ? "teal" : "accent"}>{line.result === "LOCAL" ? <Check size={10} /> : <Ban size={10} />}{line.result}</Badge></td></tr>)}</tbody></table></div>
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line bg-base/35 px-4 py-3"><div className="flex items-center gap-2 text-[10px] text-muted"><Activity size={13} className="text-teal" />Capturing interfaces: loopback, ethernet0</div><button className="ghost-button h-7"><Download size={12} /> Export audit log</button></div>
        </section>

        <aside className="space-y-4">
          <section className="panel overflow-hidden"><div className="panel-header"><div><p className="eyebrow">Control plane</p><h2 className="panel-title mt-1">Boundary controls</h2></div><LockKeyhole size={17} className="text-teal" /></div><div className="divide-y divide-line"><Control icon={Network} title="Network egress" value="Default deny" /><Control icon={FileLock2} title="Data at rest" value="AES-256" /><Control icon={Fingerprint} title="Access control" value="Local RBAC" /><Control icon={Clock3} title="Audit retention" value="365 days" /></div></section>
          <section className="terminal overflow-hidden"><div className="terminal-bar"><span className="terminal-dot" /><span className="terminal-dot" /><span className="terminal-dot" /><span className="ml-2 text-[8px] uppercase tracking-widest text-faint">policy verification</span></div><div className="p-3.5"><p><span className="text-faint">$</span> nexus-audit verify --egress</p><p className="text-teal">✓ outbound policy: DENY</p><p className="text-teal">✓ DNS resolution: DISABLED</p><p className="text-teal">✓ cloud endpoints: UNREACHABLE</p><p className="mt-1 text-text">Attestation passed in 142 ms</p></div></section>
        </aside>
      </div>
    </Page>
  );
}
function Control({ icon: Icon, title, value }) { return <div className="flex items-center gap-3 p-4"><div className="icon-box icon-box-teal"><Icon size={15} /></div><span className="flex-1 text-[11px] text-muted">{title}</span><span className="font-mono text-[9.5px] text-text">{value}</span></div>; }
