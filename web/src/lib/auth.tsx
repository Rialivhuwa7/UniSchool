import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { ok, type MeResponse, type Role, type TokenResponse } from '@unischool/api';
import { api, queryClient, tokenStore } from './api';

interface Auth {
  /** The signed-in account, or null when signed out. */
  me: MeResponse | null;
  /** True while a stored token is being checked on first load. */
  loading: boolean;
  login: (username: string, password: string) => Promise<MeResponse>;
  switchRole: (role: Role) => Promise<MeResponse>;
  logout: () => void;
}

const AuthContext = createContext<Auth | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(() => tokenStore.get() !== null);

  const logout = useCallback(() => {
    tokenStore.clear();
    queryClient.clear();
    setMe(null);
  }, []);

  useEffect(() => tokenStore.onUnauthorized(logout), [logout]);

  // A token from an earlier visit: ask the backend who it belongs to.
  useEffect(() => {
    if (!tokenStore.get()) return;
    ok(api.GET('/api/auth/me'))
      .then(setMe)
      .catch(() => logout())
      .finally(() => setLoading(false));
  }, [logout]);

  const accept = useCallback((res: TokenResponse) => {
    tokenStore.set(res.token);
    // Every screen's data depends on the active role, so start from a clean cache.
    queryClient.clear();
    setMe(res.me);
    return res.me;
  }, []);

  const login = useCallback(
    async (username: string, password: string) => accept(await ok(api.POST('/api/auth/login', { body: { username, password } }))),
    [accept],
  );

  const switchRole = useCallback(
    async (role: Role) => accept(await ok(api.POST('/api/auth/switch-role', { body: { role } }))),
    [accept],
  );

  const value = useMemo(() => ({ me, loading, login, switchRole, logout }), [me, loading, login, switchRole, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): Auth {
  const auth = useContext(AuthContext);
  if (!auth) throw new Error('useAuth must be used inside <AuthProvider>');
  return auth;
}

/** The signed-in account. Only use inside the signed-in part of the app. */
export function useMe(): MeResponse {
  const { me } = useAuth();
  if (!me) throw new Error('useMe used while signed out');
  return me;
}
