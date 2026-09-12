import { useEffect, useRef, useState } from "react";
import {
  ArrowRight,
  Code2,
  FileScan,
  LockKeyhole,
  ScanSearch,
} from "lucide-react";
import { StatusBar } from "../components/StatusBar";
import { ChatMessage } from "../components/ChatMessage";
import { ChatInput } from "../components/ChatInput";
import { ExecutionRail } from "../components/ExecutionRail";
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
  const [taskRun, setTaskRun] = useState(() => loadTaskRun(getConversationId()));
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
      setTaskRun(emptyTaskRun());
    };
    window.addEventListener("nexus:new-conversation", reset);
    return () => window.removeEventListener("nexus:new-conversation", reset);
  }, []);

  useEffect(() => {
    window.sessionStorage.setItem(taskRunKey(getConversationId()), JSON.stringify(taskRun));
  }, [taskRun]);

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
    setTaskRun({
      phase: "routing",
      startedAt: Date.now(),
      finishedAt: null,
      detail: "Understanding the request and applying local policy",
      model: null,
      modelReason: null,
      activeTool: null,
      completedTools: [],
    });
    setLoading(true);
    try {
      const result = await streamMessage(text, {
        conversationId: getConversationId(),
        onEvent: (event) => {
          setTaskRun((current) => applyTaskEvent(current, event));
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
    <div className="flex h-full min-w-0 flex-col bg-base">
      <StatusBar connected={connected} model={model} title={messages.length ? "Active agent task" : "Agent workbench"} />
      <div className="flex min-h-0 flex-1">
        <section className="flex min-w-0 flex-1 flex-col">
          {messages.length === 0 ? <WorkbenchHome onPrompt={handleSend} connected={connected} /> : <>
            <main className="min-h-0 flex-1 overflow-y-auto px-4 sm:px-7">
              <div className="mx-auto flex max-w-[820px] flex-col py-6 sm:py-8">
                {messages.map((message) => <ChatMessage key={message.id} message={message} />)}
                <div ref={bottomRef} />
              </div>
            </main>
            <div className="shrink-0 border-t border-edge bg-base/90 px-4 pb-4 pt-3 backdrop-blur-xl sm:px-7 sm:pb-5">
              <div className="mx-auto max-w-[820px]"><ChatInput onSend={handleSend} loading={loading} compact /><p className="mt-2 text-center text-[9px] text-ink3">Nexus can make mistakes. Verify engineering decisions before operational use.</p></div>
            </div>
          </>}
        </section>
        <ExecutionRail run={taskRun} model={model} connected={connected} />
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
    <main className="relative min-h-0 flex-1 overflow-y-auto px-4 py-8 sm:px-8">
      <div className="nexus-grid pointer-events-none absolute inset-0 opacity-50" />
      <div className="relative mx-auto flex min-h-full max-w-[920px] flex-col justify-center py-4">
        <div className="mb-7 flex items-center gap-2 text-[10px] text-ink3"><span className="inline-flex items-center gap-1.5 rounded-full border border-ok/25 bg-ok/[.055] px-2.5 py-1 font-mono text-ok"><span className={`h-1.5 w-1.5 rounded-full ${connected ? "bg-ok" : "bg-danger"}`} />{connected ? "Air-gapped workspace" : "Local agent offline"}</span><span className="hidden sm:inline">Nothing leaves this network</span></div>
        <h2 className="max-w-[720px] font-display text-[38px] font-semibold leading-[1.04] tracking-[-.035em] text-ink sm:text-[52px]">Industrial intelligence.<span className="block text-ink3">Under your control.</span></h2>
        <p className="mt-6 max-w-[680px] text-[14px] leading-6 text-ink2 sm:text-[15px] sm:leading-7">Plan complex work, understand confidential documents, run code, and produce real deliverables with open-weight models hosted entirely on-premise.</p>
        <div className="mt-8 max-w-[820px]"><ChatInput onSend={onPrompt} loading={false} /></div>
        <div className="mt-5 grid max-w-[820px] grid-cols-1 gap-2.5 md:grid-cols-3">
          {workflows.map((item) => {
            const Icon = item.icon;
            return (
              <button key={item.title} onClick={() => onPrompt(item.prompt)} className="group flex min-h-[108px] items-start gap-3 rounded-xl border border-edge bg-surface/80 p-3.5 text-left transition hover:-translate-y-0.5 hover:border-amber/30 hover:bg-surface2">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-amber/25 bg-amber/[.07] text-amber"><Icon size={16} /></span>
                <span className="min-w-0 flex-1"><span className="block text-[11px] font-semibold text-ink">{item.title}</span><span className="mt-1 block text-[9px] leading-4 text-ink3">{item.text}</span></span>
                <ArrowRight size={14} className="mt-1 shrink-0 text-ink3 transition group-hover:translate-x-0.5 group-hover:text-amber" />
              </button>
            );
          })}
        </div>
        <div className="mt-5 flex max-w-[820px] items-center gap-2 text-[9px] text-ink3"><LockKeyhole size={11} className="text-ok" />OCR · Vision · RAG · Python · DOCX · XLSX · PPTX</div>
      </div>
    </main>
  );
}

function emptyTaskRun() {
  return { phase: "idle", startedAt: null, finishedAt: null, detail: null, model: null, modelReason: null, activeTool: null, completedTools: [] };
}

function applyTaskEvent(run, event) {
  const current = run?.phase ? run : emptyTaskRun();
  if (event.type === "ROUTER") return { ...current, phase: "executing", model: event.model, modelReason: event.detail, detail: "Model selected; generating the local execution plan" };
  if (event.type === "TOOL_START") return { ...current, phase: "executing", activeTool: event.toolName, detail: event.detail || `Running ${friendlyTool(event.toolName)}` };
  if (event.type === "TOOL_COMPLETE") return { ...current, phase: "verifying", activeTool: null, detail: event.detail || "Local tool completed; verifying its output", completedTools: [...new Set([...current.completedTools, event.toolName].filter(Boolean))] };
  if (event.type === "ARTIFACT_CREATED") return { ...current, phase: "verifying", detail: event.detail || "Artifact created; checking delivery metadata" };
  if (event.type === "TEXT") return { ...current, phase: "verifying", detail: current.completedTools.length ? "Preparing the verified result" : "Composing the local response" };
  if (event.type === "DONE") return { ...current, phase: "done", finishedAt: Date.now(), activeTool: null, detail: "Execution completed and output verified" };
  if (event.type === "ERROR") return { ...current, phase: "error", finishedAt: Date.now(), activeTool: null, detail: event.detail || "Execution failed" };
  return current;
}

function friendlyTool(toolName) {
  return (toolName || "tool").replaceAll("_", " ");
}

function taskRunKey(conversationId) {
  return `nexus.taskRun.${conversationId}`;
}

function loadTaskRun(conversationId) {
  try {
    const stored = JSON.parse(window.sessionStorage.getItem(taskRunKey(conversationId)) || "null");
    if (!stored) return emptyTaskRun();
    if (!stored.finishedAt && stored.startedAt) return { ...stored, phase: "error", finishedAt: Date.now(), detail: "The previous task was interrupted before completion" };
    return stored;
  } catch {
    return emptyTaskRun();
  }
}
