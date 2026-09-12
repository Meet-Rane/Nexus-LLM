import { NavLink, useNavigate } from "react-router-dom";
import { startNewConversation } from "../api/client";
import {
  Bot, ChevronLeft, ChevronRight, CircleUserRound, Cpu, Database, FileOutput,
  Files, LogOut, MessageSquareText, Plus, ShieldCheck, Sparkles,
} from "lucide-react";
import { useAuth } from "../auth/useAuth";

const sections = [
  { label: "Workspace", items: [
    { label: "Workbench", icon: MessageSquareText, path: "/" },
    { label: "Documents", icon: Files, path: "/documents" },
    { label: "Knowledge", icon: Database, path: "/knowledge" },
  ] },
  { label: "Orchestration", items: [
    { label: "Agent runs", icon: Bot, path: "/agents" },
    { label: "Model registry", icon: Cpu, path: "/models" },
    { label: "Artifacts", icon: FileOutput, path: "/artifacts" },
  ] },
  { label: "Governance", items: [{ label: "Sovereignty", icon: ShieldCheck, path: "/security" }] },
];

function NexusMark({ small = false }) {
  return (
    <div className={`relative flex shrink-0 items-center justify-center rounded-xl border border-amber/35 bg-amber/[.08] text-amber shadow-[0_0_28px_rgba(117,109,246,.08)] ${small ? "h-8 w-8" : "h-11 w-11"}`} aria-hidden="true">
      <Sparkles size={small ? 15 : 19} strokeWidth={1.8} />
      <span className="absolute right-1.5 top-1.5 h-1 w-1 rounded-full bg-ok" />
    </div>
  );
}

function NavigationItem({ item, collapsed }) {
  const Icon = item.icon;
  return (
    <NavLink to={item.path} end={item.path === "/"} title={collapsed ? item.label : undefined} className={({ isActive }) => `group relative flex h-11 items-center rounded-xl transition-colors ${collapsed ? "justify-center px-2" : "gap-3 px-3"} ${isActive ? "border border-amber/25 bg-amber/[.12] text-amber" : "border border-transparent text-ink2 hover:bg-surface2 hover:text-ink"}`}>
      {({ isActive }) => <>{isActive && <span className="absolute -left-px h-5 w-0.5 rounded-r-full bg-amber" />}<Icon size={18} strokeWidth={isActive ? 2.1 : 1.7} className="shrink-0" />{!collapsed && <span className="hidden truncate text-[13px] font-medium sm:inline">{item.label}</span>}</>}
    </NavLink>
  );
}

export default function Sidebar({ collapsed, setCollapsed }) {
  const navigate = useNavigate();
  const { user, hasRole, signOut } = useAuth();
  const visibleSections = sections.map((section) => ({
    ...section,
    items: section.items.filter((item) => {
      if (item.path === "/models" || item.path === "/security") return hasRole("ROLE_ADMIN");
      if (item.path === "/agents") return hasRole("ROLE_MODERATOR", "ROLE_ADMIN");
      return true;
    }),
  })).filter((section) => section.items.length);

  const handleNewTask = () => {
    startNewConversation();
    window.dispatchEvent(new Event("nexus:new-conversation"));
    navigate("/", { state: { newTask: Date.now() } });
  };

  return (
    <aside className={`relative flex h-screen shrink-0 flex-col border-r border-edge bg-surface/95 backdrop-blur-xl transition-[width] duration-200 ${collapsed ? "w-[72px]" : "w-[72px] sm:w-[264px]"}`}>
      <div className={`flex h-[88px] items-center border-b border-edge px-4 ${collapsed ? "justify-center" : "gap-3"}`}>
        <NexusMark />
        {!collapsed && <div className="hidden min-w-0 sm:block"><h1 className="font-display text-[17px] font-bold tracking-[.14em] text-ink">NEXUS</h1><p className="mt-0.5 text-[9px] font-semibold uppercase tracking-[.22em] text-ink3">Sovereign AI</p></div>}
      </div>

      <div className="px-3 pb-4 pt-5">
        <button type="button" onClick={handleNewTask} title={collapsed ? "New agent task" : undefined} className={`flex h-12 w-full items-center rounded-xl bg-amber font-semibold text-white shadow-[0_10px_30px_rgba(117,109,246,.18)] transition hover:bg-[#827bff] active:scale-[.99] ${collapsed ? "justify-center px-2" : "gap-2.5 px-4"}`}>
          <Plus size={19} strokeWidth={2.1} />{!collapsed && <span className="hidden text-[13px] sm:inline">New agent task</span>}
        </button>
      </div>

      <nav className="min-h-0 flex-1 overflow-y-auto px-3 pb-3">
        {visibleSections.map((section) => <section key={section.label} className="mb-5">
          {!collapsed && <p className="mb-2 hidden px-3 text-[9px] font-bold uppercase tracking-[.2em] text-ink3 sm:block">{section.label}</p>}
          <div className="space-y-1">{section.items.map((item) => <NavigationItem key={item.path} item={item} collapsed={collapsed} />)}</div>
        </section>)}
      </nav>

      {!collapsed && <div className="mx-3 mb-3 hidden rounded-xl border border-ok/20 bg-ok/[.055] p-3.5 sm:block"><div className="flex items-center gap-2 text-ok"><ShieldCheck size={15} /><span className="text-[11px] font-semibold">Air-gap enforced</span></div><p className="mt-2 text-[10px] leading-4 text-ink3">Models, files and tools remain inside this deployment.</p></div>}

      <div className="border-t border-edge p-3">
        {!collapsed && <div className="flex items-center gap-3 rounded-xl px-2 py-2.5"><span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-edge2 bg-surface3 text-ink2"><CircleUserRound size={16} /></span><span className="hidden min-w-0 flex-1 text-left sm:block"><span className="block truncate text-[12px] font-semibold text-ink">{user?.username}</span><span className="block truncate font-mono text-[9px] text-ink3">{roleLabel(user?.roles)}</span></span><button type="button" title="Sign out" onClick={signOut} className="flex h-8 w-8 items-center justify-center rounded-lg text-ink3 transition hover:bg-surface2 hover:text-ink"><LogOut size={14} /></button></div>}
        <button type="button" onClick={() => setCollapsed(!collapsed)} className={`mt-1 hidden w-full items-center rounded-xl py-2 text-[11px] text-ink3 transition hover:bg-surface2 hover:text-ink sm:flex ${collapsed ? "justify-center" : "gap-3 px-3"}`} title={collapsed ? "Expand sidebar" : "Collapse sidebar"}>
          {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}{!collapsed && <span>Collapse sidebar</span>}
        </button>
      </div>
    </aside>
  );
}

export { NexusMark };

function roleLabel(roles = []) {
  if (roles.includes("ROLE_ADMIN")) return "ADMINISTRATOR";
  if (roles.includes("ROLE_MODERATOR")) return "SUPERVISOR";
  return "OPERATOR";
}
