import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router';
import { ROLE_LABEL, type Role } from '@unischool/api';
import { useAuth, useMe } from '../lib/auth';
import { errorMessage } from '../lib/api';
import { homeOf, ROLE_BASE, SECTIONS } from '../roles';
import { useToast } from './toast';
import { cx } from './ui';

/** Top bar (brand, role switcher, account) and the active role's tabs, around every signed-in screen. */
export function Layout() {
  const me = useMe();
  const { switchRole, logout } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [switching, setSwitching] = useState<Role | null>(null);

  const active = me.activeRole.role;
  const roles = me.roles.filter((r) => r.verified);
  const base = `/${ROLE_BASE[active]}`;

  async function choose(role: Role) {
    if (role === active) return;
    setSwitching(role);
    try {
      await switchRole(role);
      navigate(homeOf(role));
    } catch (e) {
      toast(errorMessage(e), 'bad');
    } finally {
      setSwitching(null);
    }
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-line bg-surface">
        <div className="mx-auto flex max-w-[1120px] flex-wrap items-center gap-3 px-5 py-3">
          <NavLink to={base} className="flex items-baseline gap-2">
            <b className="text-2xl font-extrabold tracking-tight">
              Uni<i className="not-italic text-blue">School</i>
            </b>
            <small className="text-xs text-ink-3">by Unicard</small>
          </NavLink>

          {roles.length > 1 && (
            <div className="ml-auto flex max-w-full overflow-x-auto rounded-full bg-surface-2 p-1" role="group" aria-label="Switch role">
              {roles.map((r) => (
                <button
                  key={r.id}
                  type="button"
                  aria-pressed={r.role === active}
                  title={r.label}
                  disabled={switching !== null}
                  onClick={() => choose(r.role)}
                  className={cx(
                    'whitespace-nowrap rounded-full px-4 py-1.5 text-sm font-semibold',
                    r.role === active ? 'bg-surface text-ink shadow-sm' : 'text-ink-2 hover:text-ink',
                  )}
                >
                  {switching === r.role ? 'Switching…' : ROLE_LABEL[r.role]}
                </button>
              ))}
            </div>
          )}

          <div className={cx('flex items-center gap-3', roles.length <= 1 && 'ml-auto')}>
            <div className="hidden text-right sm:block">
              <p className="text-sm font-semibold">{me.fullName}</p>
              <p className="text-xs text-ink-3">{me.activeRole.label}</p>
            </div>
            <button
              type="button"
              onClick={() => {
                logout();
                navigate('/login');
              }}
              className="rounded-full border border-line px-3 py-1.5 text-sm font-semibold hover:bg-surface-2"
            >
              Sign out
            </button>
          </div>
        </div>

        <nav className="mx-auto flex max-w-[1120px] gap-1 overflow-x-auto px-3" aria-label="Sections">
          {SECTIONS[active].nav.map((item) => (
            <NavLink
              key={item.path}
              to={item.path ? `${base}/${item.path}` : base}
              end={item.path === ''}
              className={({ isActive }) =>
                cx(
                  'whitespace-nowrap border-b-2 px-3 py-2.5 text-sm font-semibold',
                  isActive ? 'border-blue text-ink' : 'border-transparent text-ink-2 hover:text-ink',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="mx-auto max-w-[1120px] px-5 py-8">
        <Outlet />
      </main>
    </div>
  );
}
