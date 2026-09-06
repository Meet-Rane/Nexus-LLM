import { useState } from "react";
import { KeyRound, LoaderCircle, LockKeyhole, ShieldCheck, UserPlus } from "lucide-react";
import { useAuth } from "../auth/useAuth";
import { NexusMark } from "../components/Sidebar";

export default function Login() {
  const { signIn, register } = useAuth();
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ username: "", email: "", password: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (mode === "signup") await register(form);
      else await signIn({ username: form.username, password: form.password });
    } catch (requestError) {
      setError(requestError?.response?.data?.message || requestError?.response?.data?.error || "Unable to authenticate with the local server.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-base px-5 py-10">
      <div className="grid w-full max-w-[920px] overflow-hidden rounded-2xl border border-line bg-panel shadow-2xl lg:grid-cols-[1.05fr_.95fr]">
        <section className="hidden border-r border-line bg-sidebar p-10 lg:flex lg:flex-col lg:justify-between">
          <div>
            <div className="flex items-center gap-3"><NexusMark /><div><p className="font-display text-base font-bold tracking-[.08em] text-white">NEXUS</p><p className="text-[10px] uppercase tracking-[.16em] text-muted">Sovereign AI</p></div></div>
            <h1 className="mt-16 max-w-sm font-display text-4xl font-semibold leading-tight tracking-[-.035em] text-white">Confidential industrial work stays inside your network</h1>
            <p className="mt-5 max-w-sm text-sm leading-6 text-muted">Local models, document intelligence, sandboxed tools, and native deliverables from one controlled workspace.</p>
          </div>
          <div className="space-y-3 text-xs text-muted"><TrustRow text="JWT-protected agent workspace" /><TrustRow text="Role-based governance controls" /><TrustRow text="No cloud inference required" /></div>
        </section>

        <section className="p-7 sm:p-10">
          <div className="mb-8 flex items-center gap-3 lg:hidden"><NexusMark /><p className="font-display font-bold tracking-[.08em] text-white">NEXUS</p></div>
          <div className="mb-7"><p className="eyebrow">Local identity</p><h2 className="mt-2 font-display text-2xl font-semibold text-white">{mode === "login" ? "Sign in to the workbench" : "Create a local account"}</h2><p className="mt-2 text-xs leading-5 text-muted">{mode === "signup" ? "The first account becomes the local administrator. Later accounts are operators." : "Credentials are verified only by the on-premise backend."}</p></div>
          <form onSubmit={submit} className="space-y-4">
            <Field label="Username"><input required minLength={3} value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} autoComplete="username" /></Field>
            {mode === "signup" && <Field label="Email"><input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} autoComplete="email" /></Field>}
            <Field label="Password"><input required minLength={6} type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} autoComplete={mode === "signup" ? "new-password" : "current-password"} /></Field>
            {error && <div className="rounded-lg border border-danger/20 bg-danger/[.06] px-3 py-2.5 text-xs leading-5 text-danger">{error}</div>}
            <button disabled={busy} className="primary-button h-10 w-full" type="submit">{busy ? <LoaderCircle size={15} className="animate-spin" /> : mode === "login" ? <KeyRound size={15} /> : <UserPlus size={15} />}{mode === "login" ? "Sign in" : "Create account"}</button>
          </form>
          <button type="button" className="ghost-button mt-4 w-full" onClick={() => { setMode(mode === "login" ? "signup" : "login"); setError(""); }}>{mode === "login" ? "Set up a local account" : "Back to sign in"}</button>
          <div className="mt-7 flex items-center justify-center gap-2 text-[10px] text-teal"><LockKeyhole size={12} /> Authentication remains on this machine</div>
        </section>
      </div>
    </main>
  );
}

function Field({ label, children }) {
  return <label className="block"><span className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[.12em] text-muted">{label}</span><div className="auth-field">{children}</div></label>;
}

function TrustRow({ text }) {
  return <div className="flex items-center gap-2"><ShieldCheck size={14} className="text-teal" />{text}</div>;
}
