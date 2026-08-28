import { useState } from "react";
import { Routes, Route } from "react-router-dom";

import Sidebar from "./components/Sidebar";

import Chat from "./pages/Chat";
import Documents from "./pages/Documents";
import Knowledge from "./pages/Knowledge";
import Agents from "./pages/Agents";
import Models from "./pages/Models";
import Artifacts from "./pages/Artifacts";
import Security from "./pages/Security";

export default function App() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden bg-base">
      <Sidebar collapsed={collapsed} setCollapsed={setCollapsed} />

      <main className="min-w-0 flex-1 overflow-hidden">
        <Routes>
          <Route path="/" element={<Chat />} />
          <Route path="/documents" element={<Documents />} />
          <Route path="/knowledge" element={<Knowledge />} />
          <Route path="/agents" element={<Agents />} />
          <Route path="/models" element={<Models />} />
          <Route path="/artifacts" element={<Artifacts />} />
          <Route path="/security" element={<Security />} />
        </Routes>
      </main>
    </div>
  );
}
