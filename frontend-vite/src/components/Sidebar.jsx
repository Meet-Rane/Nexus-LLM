import { NavLink, useNavigate } from "react-router-dom";
import {
  Bot,
  ChevronsLeft,
  ChevronsRight,
  CircleUserRound,
  Cpu,
  Database,
  FileOutput,
  Files,
  MessageSquareText,
  Plus,
  Settings,
  ShieldCheck,
} from "lucide-react";

const sections = [
  {
    label: "Workspace",
    items: [
      { label: "Workbench", icon: MessageSquareText, path: "/" },
      { label: "Documents", icon: Files, path: "/documents", count: 12 },
      { label: "Knowledge", icon: Database, path: "/knowledge" },
    ],
  },
  {
    label: "Orchestration",
    items: [
      { label: "Agent runs", icon: Bot, path: "/agents", count: 1 },
      { label: "Model registry", icon: Cpu, path: "/models" },
      { label: "Artifacts", icon: FileOutput, path: "/artifacts", count: 8 },
    ],
  },
  {
    label: "Governance",
    items: [{ label: "Sovereignty", icon: ShieldCheck, path: "/security" }],
  },
];

function NexusMark({ small = false }) {
  return (
    <div className={`nexus-mark ${small ? "h-8 w-8" : "h-9 w-9"}`} aria-hidden="true">
      <span />
      <span />
      <span />
    </div>
  );
}

function SidebarItem({ item, collapsed }) {
  const Icon = item.icon;
  return (
    <NavLink
      to={item.path}
      end={item.path === "/"}
      title={collapsed ? item.label : undefined}
      className={({ isActive }) => `nav-item ${isActive ? "nav-item-active" : ""} ${collapsed ? "justify-center" : ""}`}
    >
      <Icon size={18} strokeWidth={1.8} className="shrink-0" />
      {!collapsed && (
        <>
          <span className="min-w-0 flex-1 truncate">{item.label}</span>
          {item.count && <span className="nav-count">{item.count}</span>}
        </>
      )}
    </NavLink>
  );
}

export default function Sidebar({ collapsed, setCollapsed }) {
  const navigate = useNavigate();

  const newTask = () => {
    navigate("/", { state: { newTask: Date.now() } });
  };

  return (
    <aside className={`sidebar ${collapsed ? "sidebar-collapsed" : ""}`}>
      <div className={`flex h-[74px] items-center border-b border-line px-4 ${collapsed ? "justify-center" : "gap-3"}`}>
        <NexusMark />
        {!collapsed && (
          <div className="min-w-0">
            <div className="font-display text-[15px] font-bold tracking-[0.08em] text-white">NEXUS</div>
            <div className="text-[10px] font-medium uppercase tracking-[0.17em] text-muted">Sovereign AI</div>
          </div>
        )}
      </div>

      <div className="px-3 py-4">
        <button onClick={newTask} className={`new-task-button ${collapsed ? "justify-center px-0" : ""}`} title="Start a new task">
          <Plus size={17} strokeWidth={2.2} />
          {!collapsed && <span>New agent task</span>}
        </button>
      </div>

      <nav className="min-h-0 flex-1 overflow-y-auto px-3 pb-3">
        {sections.map((section) => (
          <section key={section.label} className="mb-5">
            {!collapsed && <p className="section-label">{section.label}</p>}
            <div className="space-y-1">
              {section.items.map((item) => <SidebarItem key={item.path} item={item} collapsed={collapsed} />)}
            </div>
          </section>
        ))}

        {!collapsed && (
          <div className="mx-1 mt-5 rounded-xl border border-teal/20 bg-teal/[0.055] p-3.5">
            <div className="mb-2 flex items-center gap-2 text-xs font-semibold text-teal">
              <ShieldCheck size={14} /> Air-gap enforced
            </div>
            <p className="text-[11px] leading-relaxed text-muted">All inference and document processing stays on this machine.</p>
            <NavLink to="/security" className="mt-3 inline-flex text-[11px] font-semibold text-text transition hover:text-white">
              View network proof →
            </NavLink>
          </div>
        )}
      </nav>

      <div className="border-t border-line p-3">
        {!collapsed && (
          <div className="mb-2 flex items-center gap-3 rounded-xl px-2 py-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-panel-2 text-muted"><CircleUserRound size={17} /></div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-semibold text-text">Local operator</p>
              <p className="truncate font-mono text-[9px] text-muted">MRPL-WBX-01</p>
            </div>
            <Settings size={15} className="text-muted" />
          </div>
        )}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className={`nav-item w-full ${collapsed ? "justify-center" : ""}`}
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? <ChevronsRight size={18} /> : <><ChevronsLeft size={18} /><span>Collapse sidebar</span></>}
        </button>
      </div>
    </aside>
  );
}

export { NexusMark };
