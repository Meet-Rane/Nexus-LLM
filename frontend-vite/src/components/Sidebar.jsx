import { NavLink } from "react-router-dom";
import {
  MessageSquare,
  Files,
  Database,
  Bot,
  Cpu,
  FolderOpen,
  ShieldCheck,
  Settings,
  Plus,
  PanelLeftClose,
  PanelLeftOpen,
} from "lucide-react";

const mainItems = [
  {
    label: "Chat",
    icon: MessageSquare,
    path: "/",
  },
  {
    label: "Documents",
    icon: Files,
    path: "/documents",
  },
  {
    label: "Knowledge Base",
    icon: Database,
    path: "/knowledge",
  },
];

const aiItems = [
  {
    label: "Agents",
    icon: Bot,
    path: "/agents",
  },
  {
    label: "Models",
    icon: Cpu,
    path: "/models",
  },
];

const outputItems = [
  {
    label: "Artifacts",
    icon: FolderOpen,
    path: "/artifacts",
  },
];

const securityItems = [
  {
    label: "Security",
    icon: ShieldCheck,
    path: "/security",
  },
];

function SidebarItem({ item, collapsed }) {
  const Icon = item.icon;

  return (
    <NavLink
      to={item.path}
      end={item.path === "/"}
      title={collapsed ? item.label : undefined}
      className={({ isActive }) =>
        `group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-all duration-200 ${
          isActive
            ? "bg-amber/10 text-amber shadow-sm"
            : "text-ink2 hover:bg-surface2 hover:text-ink"
        } ${collapsed ? "justify-center" : ""}`
      }
    >
      {({ isActive }) => (
        <>
          <Icon
            size={18}
            strokeWidth={isActive ? 2.2 : 1.8}
            className="shrink-0"
          />

          {!collapsed && <span className="truncate">{item.label}</span>}
        </>
      )}
    </NavLink>
  );
}

function Section({ title, items, collapsed }) {
  return (
    <div className="mb-5">
      {!collapsed && (
        <p className="mb-2 px-3 text-[10px] font-semibold uppercase tracking-[0.14em] text-ink3">
          {title}
        </p>
      )}

      <div className="space-y-1">
        {items.map((item) => (
          <SidebarItem key={item.path} item={item} collapsed={collapsed} />
        ))}
      </div>
    </div>
  );
}

export default function Sidebar({ collapsed, setCollapsed }) {
  return (
    <aside
      className={`relative flex h-screen shrink-0 flex-col border-r border-edge bg-surface transition-all duration-300 ${
        collapsed ? "w-[72px]" : "w-[250px]"
      }`}
    >
      {/* Header */}
      <div
        className={`flex h-[72px] items-center border-b border-edge px-4 ${
          collapsed ? "justify-center" : "justify-between"
        }`}
      >
        {!collapsed && (
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl border border-edge bg-surface2">
              <span className="font-display text-lg text-amber">⌁</span>
            </div>

            <div>
              <h1 className="font-display text-sm font-semibold text-ink">
                Sovereign
              </h1>
              <p className="text-[10px] text-ink3">AI Workbench</p>
            </div>
          </div>
        )}

        {collapsed && (
          <div className="flex h-9 w-9 items-center justify-center rounded-xl border border-edge bg-surface2">
            <span className="font-display text-lg text-amber">⌁</span>
          </div>
        )}
      </div>

      {/* New Task */}
      <div className="px-3 pt-4 pb-3">
        <button
          className={`flex w-full items-center gap-2 rounded-xl border border-edge bg-surface2 px-3 py-2.5 text-sm font-medium text-ink transition hover:border-amber/40 hover:bg-amber/5 ${
            collapsed ? "justify-center" : ""
          }`}
          title={collapsed ? "New Task" : undefined}
        >
          <Plus size={17} />

          {!collapsed && <span>New Task</span>}
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-3 py-2">
        <Section title="Workspace" items={mainItems} collapsed={collapsed} />

        <Section title="AI" items={aiItems} collapsed={collapsed} />

        <Section title="Outputs" items={outputItems} collapsed={collapsed} />

        <Section title="Security" items={securityItems} collapsed={collapsed} />
      </nav>

      {/* Bottom */}
      <div className="border-t border-edge p-3">
        <button
          className={`mb-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-ink2 transition hover:bg-surface2 hover:text-ink ${
            collapsed ? "justify-center" : ""
          }`}
          title={collapsed ? "Settings" : undefined}
        >
          <Settings size={18} strokeWidth={1.8} />

          {!collapsed && <span>Settings</span>}
        </button>

        <button
          onClick={() => setCollapsed(!collapsed)}
          className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-ink3 transition hover:bg-surface2 hover:text-ink ${
            collapsed ? "justify-center" : ""
          }`}
          title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? (
            <PanelLeftOpen size={18} />
          ) : (
            <>
              <PanelLeftClose size={18} />
              <span>Collapse</span>
            </>
          )}
        </button>
      </div>
    </aside>
  );
}
