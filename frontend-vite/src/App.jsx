import { useState } from "react";
import { Routes, Route, useLocation } from "react-router-dom";

import Sidebar from "./components/Sidebar";

import Chat from "./pages/Chat";
import Documents from "./pages/Documents";
import Knowledge from "./pages/Knowledge";
import Agents from "./pages/Agents";
import Models from "./pages/Models";
import Artifacts from "./pages/Artifacts";
import Security from "./pages/Security";
import Login from "./pages/Login";
import { useAuth } from "./auth/useAuth";
import { ShieldX } from "lucide-react";

export default function App() {
  const { user, checking, hasRole } = useAuth();
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();

  if (checking) return <div className="flex h-screen items-center justify-center bg-base text-xs text-muted">Verifying local session…</div>;
  if (!user) return <Login />;

  return (
    <div className="app-shell">
      <Sidebar collapsed={collapsed} setCollapsed={setCollapsed} />

      <main className="min-w-0 flex-1 overflow-hidden">
        <Routes>
          <Route path="/" element={<Chat key={location.key} />} />
          <Route path="/documents" element={<Documents />} />
          <Route path="/knowledge" element={<Knowledge />} />
          <Route path="/agents" element={hasRole("ROLE_MODERATOR", "ROLE_ADMIN") ? <Agents /> : <AccessDenied />} />
          <Route path="/models" element={hasRole("ROLE_ADMIN") ? <Models /> : <AccessDenied />} />
          <Route path="/artifacts" element={<Artifacts />} />
          <Route path="/security" element={hasRole("ROLE_ADMIN") ? <Security /> : <AccessDenied />} />
        </Routes>
      </main>
    </div>
  );
}

function AccessDenied() {
  return <div className="flex h-full items-center justify-center p-6"><div className="panel max-w-sm p-8 text-center"><ShieldX className="mx-auto text-danger" size={28} /><h2 className="mt-4 font-display text-lg font-semibold text-white">Administrator access required</h2><p className="mt-2 text-xs leading-5 text-muted">Your local role does not allow this governance view.</p></div></div>;
}
