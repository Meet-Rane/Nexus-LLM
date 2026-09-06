import { useEffect, useMemo, useState } from "react";
import { getCurrentUser, getStoredAuth, login, signup, storeAuth } from "../api/client";
import { AuthContext } from "./auth-store";

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => getStoredAuth());
  const [checking, setChecking] = useState(Boolean(getStoredAuth()?.accessToken));

  useEffect(() => {
    const cached = getStoredAuth();
    if (!cached?.accessToken) return undefined;
    getCurrentUser()
      .then((user) => {
        const next = { ...cached, ...user };
        storeAuth(next);
        setSession(next);
      })
      .catch(() => {
        storeAuth(null);
        setSession(null);
      })
      .finally(() => setChecking(false));
    return undefined;
  }, []);

  useEffect(() => {
    const clear = () => setSession(null);
    window.addEventListener("nexus:unauthorized", clear);
    return () => window.removeEventListener("nexus:unauthorized", clear);
  }, []);

  const value = useMemo(() => ({
    user: session,
    checking,
    hasRole: (...roles) => roles.some((role) => session?.roles?.includes(role)),
    signIn: async (credentials) => {
      const result = await login(credentials);
      const next = {
        accessToken: result.accessToken,
        tokenType: result.tokenType,
        id: result.id,
        username: result.username,
        email: result.email,
        roles: result.roles || [],
      };
      storeAuth(next);
      setSession(next);
      return next;
    },
    register: async (account) => {
      const result = await signup(account);
      const next = await login({ username: account.username, password: account.password });
      const normalized = {
        accessToken: next.accessToken,
        tokenType: next.tokenType,
        id: next.id,
        username: next.username,
        email: next.email,
        roles: next.roles || [],
      };
      storeAuth(normalized);
      setSession(normalized);
      return result;
    },
    signOut: () => {
      storeAuth(null);
      setSession(null);
    },
  }), [session, checking]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
