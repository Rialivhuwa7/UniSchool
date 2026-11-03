import { createBrowserRouter, Navigate, Outlet, type RouteObject } from 'react-router';
import type { Role } from '@unischool/api';
import { useAuth } from './lib/auth';
import { homeOf, ROLE_BASE, SECTIONS } from './roles';
import { Layout } from './components/Layout';
import { Spinner } from './components/ui';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Give } from './pages/Give';
import { NotFound } from './pages/NotFound';

/** Signed-in area: waits for the stored token check, sends signed-out visitors to /login. */
function RequireAuth() {
  const { me, loading } = useAuth();
  if (loading) return <Spinner label="Signing you in" />;
  if (!me) return <Navigate to="/login" replace />;
  return <Outlet />;
}

/** A role's screens only show while that role is active; otherwise go to the active role's home. */
function RequireRole({ role }: { role: Role }) {
  const { me } = useAuth();
  if (me && me.activeRole.role !== role) return <Navigate to={homeOf(me.activeRole.role)} replace />;
  return <Outlet />;
}

function Home() {
  const { me } = useAuth();
  return <Navigate to={me ? homeOf(me.activeRole.role) : '/login'} replace />;
}

const roleRoutes: RouteObject[] = (Object.keys(SECTIONS) as Role[]).map((role) => ({
  path: ROLE_BASE[role],
  element: <RequireRole role={role} />,
  children: SECTIONS[role].routes,
}));

export const router = createBrowserRouter([
  { path: '/login', element: <Login /> },
  { path: '/register', element: <Register /> },
  // Family and friends open a parent's share link to add to a savings pot; no sign-in needed.
  { path: '/give/:token', element: <Give /> },
  {
    element: <RequireAuth />,
    children: [
      { index: true, element: <Home /> },
      { element: <Layout />, children: [...roleRoutes, { path: '*', element: <NotFound /> }] },
    ],
  },
]);
