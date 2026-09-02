import { useState } from "react";
import { BrainCircuit, Check, ChevronRight, Code2, Cpu, Eye, Gauge, HardDrive, Plus, Route, ScanText, Settings2, Sparkles } from "lucide-react";
import { Badge, MiniBar, Page, Stat } from "../components/Ui";

const models = [
  { id: "qwen-vl", name: "Qwen2.5-VL", size: "7B", format: "Q4_K_M", role: "Vision & document", description: "Scans, drawings, photographs and document understanding", icon: Eye, vram: 7.8, load: 68, tone: "accent" },
  { id: "qwen-code", name: "Qwen3-Coder", size: "14B", format: "Q4_K_M", role: "Code specialist", description: "Internal tools, debugging, tests and engineering scripts", icon: Code2, vram: 10.6, load: 0, tone: "blue" },
  { id: "deepseek", name: "DeepSeek-R1", size: "14B", format: "Q5_K_M", role: "Deep reasoning", description: "Calculations, technical analysis and multi-step planning", icon: BrainCircuit, vram: 12.4, load: 32, tone: "teal" },
];

export default function Models() {
  const [defaultModel, setDefaultModel] = useState("deepseek");
  const [autoRoute, setAutoRoute] = useState(true);
  return (
    <Page title="Model registry" eyebrow="Orchestration / Open-weight models" model={autoRoute ? "Auto routing on" : "Manual routing"}
      actions={<button className="primary-button"><Plus size={14} /> Add local model</button>}>
      <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3"><Stat label="Models available" value="03" detail="All weights stored locally" icon={Cpu} /><Stat label="GPU memory" value="17.2 GB" detail="of 24 GB currently allocated" icon={Gauge} tone="teal" /><Stat label="Model storage" value="31.8 GB" detail="Encrypted NVMe volume" icon={HardDrive} tone="blue" /></div>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1fr)_340px]">
        <section className="space-y-3">
          {models.map((item) => { const Icon = item.icon; const selected = defaultModel === item.id; return (
            <article key={item.id} className={`panel p-4 transition sm:p-5 ${selected ? "border-accent/25" : "hover:border-line-bright"}`}>
              <div className="flex items-start gap-4"><div className={`icon-box icon-box-${item.tone} h-10 w-10`}><Icon size={19} /></div><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="text-[14px] font-semibold text-white">{item.name} <span className="font-normal text-muted">{item.size}</span></h2><Badge tone={item.load ? "teal" : "neutral"} dot>{item.load ? "Loaded" : "Standby"}</Badge>{selected && <Badge tone="accent">Default</Badge>}</div><p className="mt-1 text-[11px] text-muted">{item.description}</p>
                <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4"><ModelMeta label="Specialty" value={item.role} /><ModelMeta label="Quantization" value={item.format} /><ModelMeta label="VRAM" value={`${item.vram} GB`} /><div><p className="mb-1.5 text-[9px] uppercase tracking-wider text-faint">GPU load · {item.load}%</p><MiniBar value={item.load} tone={item.tone} /></div></div></div>
                <button onClick={() => setDefaultModel(item.id)} className={`icon-button ${selected ? "border-accent/30! text-accent!" : ""}`} aria-label={`Set ${item.name} as default`}>{selected ? <Check size={15} /> : <ChevronRight size={15} />}</button>
              </div>
            </article>
          ); })}
        </section>

        <aside className="space-y-4">
          <section className="panel overflow-hidden"><div className="panel-header"><div><p className="eyebrow">Smart orchestration</p><h2 className="panel-title mt-1">Model router</h2></div><button onClick={() => setAutoRoute(!autoRoute)} className={`toggle ${autoRoute ? "toggle-on" : ""}`} aria-label="Toggle model routing"><span /></button></div><div className="p-4"><p className="mb-4 text-[10.5px] leading-5 text-muted">Classifies each request locally and chooses the best model by capability, VRAM and latency.</p><div className="space-y-2"><RouteRule icon={Eye} task="Scans & images" model="Qwen2.5-VL" /><RouteRule icon={Code2} task="Code & testing" model="Qwen3-Coder" /><RouteRule icon={Sparkles} task="Reasoning" model="DeepSeek-R1" /><RouteRule icon={ScanText} task="Short summaries" model="Qwen2.5-VL" /></div><button className="secondary-button mt-4 w-full"><Settings2 size={14} /> Edit routing policy</button></div></section>
          <section className="panel p-4"><div className="mb-3 flex items-center gap-2 text-xs font-semibold text-teal"><Route size={14} /> Backend agnostic</div><p className="text-[10.5px] leading-5 text-muted">OpenAI-compatible local endpoints let new open-weight models be registered without redesigning the workbench.</p></section>
        </aside>
      </div>
    </Page>
  );
}

function ModelMeta({ label, value }) { return <div><p className="text-[9px] uppercase tracking-wider text-faint">{label}</p><p className="mt-1 font-mono text-[9.5px] text-text">{value}</p></div>; }
function RouteRule({ icon: Icon, task, model }) { return <div className="flex items-center gap-2.5 rounded-lg border border-line bg-base/50 p-2.5"><Icon size={13} className="text-muted" /><span className="flex-1 text-[10.5px] text-muted">{task}</span><span className="font-mono text-[9px] text-text">{model}</span></div>; }
