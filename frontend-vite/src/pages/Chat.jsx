import { useEffect, useRef, useState } from "react";
import {
  ArrowRight,
  Bot,
  BrainCircuit,
  Check,
  CircleDashed,
  Code2,
  FileScan,
  LockKeyhole,
  Network,
  ScanSearch,
  ShieldCheck,
  Sparkles,
  Wrench,
} from "lucide-react";
import { StatusBar } from "../components/StatusBar";
import { ChatMessage } from "../components/ChatMessage";
import { ChatInput } from "../components/ChatInput";
import { Badge } from "../components/Ui";
import { getChatHistory, getConversationId, listArtifacts, pingAgent, streamMessage } from "../api/client";

const workflows = [
  {
    icon: FileScan,
    title: "Inspect a scanned report",
    text: "Extract findings and draft an approval note",
    prompt: "Read the attached inspection report, identify critical findings, and draft an approval note as a Word document.",
  },
  {
    icon: Code2,
    title: "Build & verify code",
    text: "Create an internal tool and test it in a sandbox",
    prompt: "Create a Python utility to validate equipment inspection CSV files and run tests in the local sandbox.",
  },
  {
    icon: ScanSearch,
    title: "Search plant knowledge",
    text: "Answer from SOPs with traceable citations",
    prompt: "Find the shutdown procedure in our indexed SOPs and summarise the required approval chain with citations.",
  },
];

export default function Chat() {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState(true);
  const [model, setModel] = useState("Auto route");
  const bottomRef = useRef(null);

  useEffect(() => {
    pingAgent().then(() => setConnected(true)).catch(() => setConnected(false));

    const conversationId = getConversationId();
    Promise.all([
      getChatHistory(conversationId),
      listArtifacts(conversationId).catch(() => []),
    ])
      .then(([history, artifacts]) => {
        if (getConversationId() !== conversationId || !Array.isArray(history)) return;
        setMessages(restoreHistoryArtifacts(history, Array.isArray(artifacts) ? artifacts : []));
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    const reset = () => {
      setMessages([]);
      setModel("Auto route");
    };
    window.addEventListener("nexus:new-conversation", reset);
    return () => window.removeEventListener("nexus:new-conversation", reset);
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  const updateAssistant = (id, updater) => {
    setMessages((current) => current.map((message) => message.id === id ? updater(message) : message));
  };

  const handleSend = async (text) => {
    const userId = window.crypto.randomUUID();
    const assistantId = window.crypto.randomUUID();
    setMessages((current) => [
      ...current,
      { id: userId, role: "user", text },
      { id: assistantId, role: "assistant", text: "", trace: [] },
    ]);
    setLoading(true);
    try {
      const result = await streamMessage(text, {
        conversationId: getConversationId(),
        onEvent: (event) => {
          if (event.type === "ROUTER") {
            setModel(event.model || "Auto route");
            updateAssistant(assistantId, (message) => ({
              ...message,
              model_used: event.model,
              trace: [...message.trace, { type: event.type, label: event.detail }],
            }));
          }
          if (event.type === "TEXT") {
            updateAssistant(assistantId, (message) => ({ ...message, text: message.text + (event.content || "") }));
          }
          if (event.type === "TOOL_START" || event.type === "TOOL_COMPLETE") {
            updateAssistant(assistantId, (message) => ({
              ...message,
              trace: [...message.trace, { type: event.type, label: event.toolName || "Local tool", detail: event.detail }],
            }));
          }
          if (event.artifact) {
            updateAssistant(assistantId, (message) => ({
              ...message,
              artifact: event.artifact,
              artifacts: appendArtifact(message.artifacts, event.artifact),
            }));
          }
          if (event.type === "ERROR") {
            updateAssistant(assistantId, (message) => ({
              ...message,
              error: true,
              text: message.text || `Agent error: ${event.detail || "Execution failed"}`,
            }));
          }
        },
      });
      setConnected(true);
      updateAssistant(assistantId, (message) => ({
        ...message,
        text: message.text || result.text || "The agent completed without returning text.",
        model_used: message.model_used || result.model_used,
        artifact: message.artifact || result.artifact,
        artifacts: result.artifact ? appendArtifact(message.artifacts, result.artifact) : message.artifacts,
      }));
    } catch (error) {
      setConnected(false);
      updateAssistant(assistantId, (message) => ({
        ...message,
        error: true,
        text: `The local agent service could not complete this request. ${error.message}`,
      }));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex h-full min-w-0 flex-col">
      <StatusBar connected={connected} model={model} title={messages.length ? "Active agent task" : "Agent workbench"} />
      <div className="min-h-0 flex-1 overflow-y-auto">
        {messages.length === 0 ? <WorkbenchHome onPrompt={handleSend} connected={connected} /> : (
          <div className="mx-auto grid min-h-full max-w-[1220px] grid-cols-1 gap-5 px-5 py-6 xl:grid-cols-[minmax(0,1fr)_280px]">
            <section className="flex min-h-[calc(100vh-122px)] flex-col">
              <div className="flex-1 space-y-6 pb-6">
                {messages.map((message) => <ChatMessage key={message.id} message={message} />)}
                {loading && <Thinking />}
                <div ref={bottomRef} />
              </div>
              <div className="sticky bottom-0 bg-base/95 pt-2 pb-1"><ChatInput onSend={handleSend} loading={loading} compact /></div>
            </section>
            <TaskRail loading={loading} model={model} />
          </div>
        )}
      </div>
    </div>
  );
}

function restoreHistoryArtifacts(history, artifacts) {
  const restored = history
    .filter((message) => message.messageType === "USER" || message.messageType === "ASSISTANT")
    .map((message, index) => ({
      id: `history-${message.metadata?.JdbcChatMemoryRepository_message_timestamp || index}`,
      role: message.messageType === "USER" ? "user" : "assistant",
      text: message.text || "",
      trace: [],
      timestamp: parseTimestamp(message.metadata?.JdbcChatMemoryRepository_message_timestamp),
      artifacts: [],
    }));

  const assistantIndexes = restored
    .map((message, index) => message.role === "assistant" ? index : -1)
    .filter((index) => index >= 0);

  [...artifacts]
    .sort((left, right) => parseTimestamp(left.createdAt) - parseTimestamp(right.createdAt))
    .forEach((artifact) => {
      const createdAt = parseTimestamp(artifact.createdAt);
      const targetIndex = assistantIndexes.find((index) => {
        const assistantTime = restored[index].timestamp;
        return Number.isFinite(createdAt) && Number.isFinite(assistantTime) && assistantTime >= createdAt - 2000;
      }) ?? assistantIndexes.at(-1);

      if (targetIndex !== undefined) {
        restored[targetIndex].artifacts = appendArtifact(restored[targetIndex].artifacts, artifact);
        restored[targetIndex].artifact = artifact;
      }
    });

  return restored;
}

function appendArtifact(artifacts = [], artifact) {
  if (!artifact) return artifacts;
  const key = `${artifact.conversationId || ""}:${artifact.path || artifact.id}`;
  return [...artifacts.filter((item) => `${item.conversationId || ""}:${item.path || item.id}` !== key), artifact];
}

function parseTimestamp(value) {
  const timestamp = value ? Date.parse(value) : Number.NaN;
  return Number.isFinite(timestamp) ? timestamp : Number.NaN;
}

function WorkbenchHome({ onPrompt, connected }) {
  return (
    <main className="mx-auto grid min-h-full w-full max-w-[1220px] grid-cols-1 gap-6 px-5 py-8 xl:grid-cols-[minmax(0,1fr)_280px] xl:py-12">
      <section className="flex min-w-0 flex-col justify-center">
        <div className="mb-8 max-w-2xl">
          <div className="mb-5 flex items-center gap-2"><Badge tone="teal" dot>Local-only workspace</Badge><span className="text-[10px] text-muted">Loopback endpoints enforced</span></div>
          <h2 className="font-display text-[clamp(28px,4vw,45px)] font-semibold leading-[1.08] tracking-[-0.04em] text-white">
            Industrial intelligence.<br /><span className="text-muted">Under your control.</span>
          </h2>
          <p className="mt-4 max-w-xl text-sm leading-6 text-muted">Plan complex work, understand confidential documents, run code, and produce real deliverables with open-weight models hosted entirely on-premise.</p>
        </div>

        <ChatInput onSend={onPrompt} loading={false} />

        <div className="mt-5 grid grid-cols-1 gap-2.5 md:grid-cols-3">
          {workflows.map((item) => {
            const Icon = item.icon;
            return (
              <button key={item.title} onClick={() => onPrompt(item.prompt)} className="workflow-card">
                <div className="icon-box icon-box-accent"><Icon size={16} /></div>
                <div className="min-w-0 text-left"><p className="text-xs font-semibold text-text">{item.title}</p><p className="mt-1 text-[10.5px] leading-4 text-muted">{item.text}</p></div>
                <ArrowRight size={14} className="ml-auto shrink-0 text-faint" />
              </button>
            );
          })}
        </div>
      </section>

      <aside className="space-y-4 xl:self-center">
        <div className="panel overflow-hidden">
          <div className="panel-header"><div><p className="eyebrow">System state</p><h3 className="panel-title mt-1">Sovereignty monitor</h3></div><ShieldCheck size={18} className="text-teal" /></div>
          <div className="space-y-4 p-4">
            <StatusRow icon={connected ? Network : CircleDashed} label="Local inference" value={connected ? "Connected" : "Standby"} ok={connected} />
            <StatusRow icon={LockKeyhole} label="External egress" value="Policy blocked" ok />
            <StatusRow icon={BrainCircuit} label="Model router" value="Automatic" ok />
            <div className="rounded-lg border border-line bg-base px-3 py-2.5 font-mono text-[9px] leading-5 text-muted">
              <div className="flex justify-between"><span>INSTANCE</span><span className="text-text">MRPL-WBX-01</span></div>
              <div className="flex justify-between"><span>AGENT</span><span className={connected ? "text-teal" : "text-accent"}>{connected ? "ONLINE" : "OFFLINE"}</span></div>
              <div className="flex justify-between"><span>AUDIT</span><span className="text-text">Security page</span></div>
            </div>
          </div>
        </div>
        <div className="panel p-4">
          <div className="mb-3 flex items-center justify-between"><p className="eyebrow">Ready tools</p><Wrench size={14} className="text-muted" /></div>
          <div className="flex flex-wrap gap-1.5">{["RAG", "File I/O", "Artifacts", "SSE"].map((tool) => <Badge key={tool}>{tool}</Badge>)}</div>
        </div>
      </aside>
    </main>
  );
}

function StatusRow({ icon: Icon, label, value, ok }) {
  return <div className="flex items-center gap-3"><div className={`icon-box ${ok ? "icon-box-teal" : "icon-box-accent"}`}><Icon size={15} /></div><div className="min-w-0 flex-1"><p className="text-[11px] font-medium text-text">{label}</p><p className={`mt-0.5 text-[10px] ${ok ? "text-teal" : "text-accent"}`}>{value}</p></div>{ok && <Check size={13} className="text-teal" />}</div>;
}

function Thinking() {
  return <div className="message"><div className="message-avatar message-avatar-agent"><Bot size={15} /></div><div><p className="mb-2 text-[11px] font-semibold text-white">Nexus Agent</p><div className="flex items-center gap-2 text-xs text-muted"><Sparkles size={14} className="animate-pulse-soft text-accent" /> Planning and selecting tools…</div></div></div>;
}

function TaskRail({ loading, model }) {
  const steps = [
    { label: "Understand request", state: "done" },
    { label: "Select model & tools", state: loading ? "active" : "done" },
    { label: "Execute locally", state: loading ? "waiting" : "done" },
    { label: "Verify output", state: loading ? "waiting" : "done" },
  ];
  return (
    <aside className="hidden xl:block">
      <div className="panel sticky top-0 overflow-hidden">
        <div className="panel-header"><div><p className="eyebrow">Live trace</p><h3 className="panel-title mt-1">Agent execution</h3></div><Bot size={17} className="text-accent" /></div>
        <div className="p-4">
          <p className="mb-3 text-[10px] font-semibold uppercase tracking-[.12em] text-faint">Plan</p>
          <div className="space-y-1">
            {steps.map((step, index) => <div key={step.label} className="flex items-center gap-2.5 py-2"><span className={`step-dot step-${step.state}`}>{step.state === "done" ? <Check size={9} /> : index + 1}</span><span className={`text-[11px] ${step.state === "waiting" ? "text-faint" : "text-text"}`}>{step.label}</span></div>)}
          </div>
          <div className="my-4 border-t border-line" />
          <p className="mb-2 text-[10px] font-semibold uppercase tracking-[.12em] text-faint">Router decision</p>
          <div className="rounded-lg border border-accent/15 bg-accent/[.05] p-3"><div className="mb-1.5 flex items-center gap-2 text-[11px] font-semibold text-accent"><BrainCircuit size={14} />{model}</div><p className="text-[10px] leading-4 text-muted">Selected for reasoning quality and task modality.</p></div>
          <div className="mt-4 flex items-center gap-2 text-[9.5px] text-teal"><LockKeyhole size={12} /> Local endpoint policy enforced</div>
        </div>
      </div>
    </aside>
  );
}
