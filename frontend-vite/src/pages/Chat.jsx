import React, { useEffect, useRef, useState } from "react";
import { StatusBar } from "../components/StatusBar";
import { ChatMessage } from "../components/ChatMessage";
import { ChatInput } from "../components/ChatInput";
import { sendMessage, pingAgent } from "../api/client";

export default function Chat() {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState(true);
  const [model, setModel] = useState("—");

  const bottomRef = useRef(null);

  useEffect(() => {
    pingAgent()
      .then(() => setConnected(true))
      .catch(() => setConnected(false));
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({
      behavior: "smooth",
    });
  }, [messages, loading]);

  const handleSend = async (text) => {
    const userMsg = {
      role: "user",
      text,
    };

    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);

    try {
      const reply = await sendMessage(
        text,
        messages.map(({ role, text }) => ({
          role,
          text,
        })),
      );

      setConnected(true);

      if (reply.model_used) {
        setModel(reply.model_used);
      }

      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          text: reply.text,
          model_used: reply.model_used,
          download_url: reply.download_url,
        },
      ]);
    } catch (err) {
      setConnected(false);

      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          text: "The workbench couldn't be reached. Confirm the local agent is running, then try again.",
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex h-full flex-col">
      <StatusBar connected={connected} model={model} />

      <main className="flex-1 overflow-y-auto px-4 py-6 sm:px-8">
        <div className="mx-auto flex max-w-3xl flex-col gap-4">
          {messages.length === 0 ? (
            <EmptyState />
          ) : (
            messages.map((message, index) => (
              <ChatMessage key={index} message={message} />
            ))
          )}

          {loading && <TypingIndicator />}

          <div ref={bottomRef} />
        </div>
      </main>

      <div className="mx-auto w-full max-w-3xl">
        <ChatInput onSend={handleSend} loading={loading} />
      </div>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-24 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-xl border border-edge bg-surface2 font-display text-xl text-amber">
        ⌁
      </div>

      <h2 className="font-display text-lg font-semibold text-ink">
        Sovereign AI Workbench
      </h2>

      <p className="max-w-sm text-[13px] leading-relaxed text-ink2">
        Ask a question or attach a document to begin a local AI task.
      </p>
    </div>
  );
}

function TypingIndicator() {
  return (
    <div className="flex justify-start">
      <div className="flex items-center gap-1.5 rounded-2xl rounded-tl-sm border border-edge bg-surface2 px-4 py-3">
        {[0, 1, 2].map((i) => (
          <span
            key={i}
            className="h-1.5 w-1.5 animate-pulseDot rounded-full bg-ink3"
            style={{
              animationDelay: `${i * 0.15}s`,
            }}
          />
        ))}
      </div>
    </div>
  );
}
