import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router';
import {
  Activity,
  BadgeCheck,
  Banknote,
  Bell,
  BookOpen,
  ChefHat,
  Circle,
  ClipboardList,
  CreditCard,
  Ellipsis,
  GraduationCap,
  House,
  KeyRound,
  LayoutDashboard,
  ListChecks,
  LogOut,
  MessagesSquare,
  PiggyBank,
  ReceiptText,
  ScanLine,
  Settings,
  Shirt,
  ShoppingBag,
  Users,
  UtensilsCrossed,
  Wallet,
  X,
  type LucideIcon,
} from 'lucide-react';
import { ROLE_LABEL, type Role } from '@unischool/api';
import { useAuth, useMe } from '../lib/auth';
import { errorMessage } from '../lib/api';
import { homeOf, ROLE_BASE, SECTIONS } from '../roles';
import { useToast } from './toast';
import { cx } from './ui';
import { Brand } from './Brand';

/** Icon for each tab on the phone's bottom bar, by its path ('' is a role's home). */
const ICONS: Record<string, LucideIcon> = {
  '': House,
  children: Users,
  learners: GraduationCap,
  wallet: Wallet,
  tuckshop: UtensilsCrossed,
  notices: Bell,
  homework: BookOpen,
  payments: CreditCard,
  shop: ShoppingBag,
  savings: PiggyBank,
  groups: MessagesSquare,
  approvals: BadgeCheck,
  settings: Settings,
  collections: ReceiptText,
  fees: Banknote,
  access: KeyRound,
  lists: ClipboardList,
  uniform: Shirt,
  payouts: Banknote,
  activity: Activity,
  'pre-orders': ListChecks,
  menu: ChefHat,
};

/** Home icons that suit some roles better than a house. */
const HOME_ICON: Partial<Record<Role, LucideIcon>> = {
  PRINCIPAL: LayoutDashboard,
  SCHOOL_OFFICE: LayoutDashboard,
  TEACHER: GraduationCap,
  TUCKSHOP: ScanLine,
};

/** Tabs that fit on a phone's bottom bar; the rest go under "More". */
const BAR_TABS = 4;

/**
 * The signed-in shell. On a computer: top bar with brand, role switcher and account, and the
 * active role's tabs beneath it. On a phone it behaves like an app: a compact top bar, the main
 * tabs in a bottom bar, and everything else (other tabs, roles, sign out) under "More".
 */
export function Layout() {
  const me = useMe();
  const { switchRole, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const [switching, setSwitching] = useState<Role | null>(null);
  const [moreOpen, setMoreOpen] = useState(false);

  const active = me.activeRole.role;
  const roles = me.roles.filter((r) => r.verified);
  const base = `/${ROLE_BASE[active]}`;
  const nav = SECTIONS[active].nav;
  const to = (path: string) => (path ? `${base}/${path}` : base);
  const iconFor = (path: string) => (path === '' ? (HOME_ICON[active] ?? House) : (ICONS[path] ?? Circle));

  // The bar shows the first tabs; when there are more, the last slot is "More".
  const barItems = nav.length <= BAR_TABS + 1 ? nav : nav.slice(0, BAR_TABS);
  const moreItems = nav.length <= BAR_TABS + 1 ? [] : nav.slice(BAR_TABS);
  const moreIsActive = moreItems.some((i) => location.pathname.startsWith(to(i.path)));

  // Close the sheet whenever the page changes.
  useEffect(() => setMoreOpen(false), [location.pathname]);

  async function choose(role: Role) {
    if (role === active) return;
    setSwitching(role);
    try {
      await switchRole(role);
      setMoreOpen(false);
      navigate(homeOf(role));
    } catch (e) {
      toast(errorMessage(e), 'bad');
    } finally {
      setSwitching(null);
    }
  }

  function signOut() {
    logout();
    navigate('/login');
  }

  const brand = (
    <NavLink to={base} className="flex items-center gap-2" aria-label="UniSchool home">
      <Brand />
      <small className="hidden self-end pb-1 text-xs text-ink-3 sm:inline">by Unicard</small>
    </NavLink>
  );

  return (
    <div className="min-h-dvh">
      <header className="sticky top-0 z-40 border-b border-line bg-surface pt-[env(safe-area-inset-top)]">
        {/* Phone: brand and who's signed in. */}
        <div className="flex items-center justify-between gap-3 px-4 py-3 md:hidden">
          {brand}
          <p className="truncate text-right text-sm font-semibold text-ink-2">{me.activeRole.label}</p>
        </div>

        {/* Computer: brand, role switcher, account. */}
        <div className="mx-auto hidden max-w-[1120px] flex-wrap items-center gap-3 px-5 py-3 md:flex">
          {brand}
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
            <div className="text-right">
              <p className="text-sm font-semibold">{me.fullName}</p>
              <p className="text-xs text-ink-3">{me.activeRole.label}</p>
            </div>
            <button type="button" onClick={signOut} className="rounded-full border border-line px-3 py-1.5 text-sm font-semibold hover:bg-surface-2">
              Sign out
            </button>
          </div>
        </div>

        <nav className="mx-auto hidden max-w-[1120px] gap-1 overflow-x-auto px-3 md:flex" aria-label="Sections">
          {nav.map((item) => (
            <NavLink
              key={item.path}
              to={to(item.path)}
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

      <main className="mx-auto max-w-[1120px] px-4 py-6 pb-[calc(6rem+env(safe-area-inset-bottom))] md:px-5 md:py-8 md:pb-8">
        <Outlet />
      </main>

      {/* Phone: bottom tab bar. */}
      <nav
        className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] md:hidden"
        aria-label="Sections"
      >
        <div className="flex">
          {barItems.map((item) => {
            const Icon = iconFor(item.path);
            return (
              <NavLink
                key={item.path}
                to={to(item.path)}
                end={item.path === ''}
                className={({ isActive }) =>
                  cx('flex min-h-14 flex-1 flex-col items-center justify-center gap-0.5 text-[11px] font-semibold', isActive ? 'text-blue' : 'text-ink-3')
                }
              >
                <Icon className="size-6" aria-hidden />
                <span className="max-w-full truncate px-1">{item.label}</span>
              </NavLink>
            );
          })}
          <button
            type="button"
            onClick={() => setMoreOpen(true)}
            aria-expanded={moreOpen}
            className={cx(
              'flex min-h-14 flex-1 flex-col items-center justify-center gap-0.5 text-[11px] font-semibold',
              moreIsActive ? 'text-blue' : 'text-ink-3',
            )}
          >
            <Ellipsis className="size-6" aria-hidden />
            More
          </button>
        </div>
      </nav>

      {moreOpen && (
        <div className="fixed inset-0 z-50 md:hidden" role="dialog" aria-modal="true" aria-label="More">
          <button type="button" className="absolute inset-0 bg-black/40" aria-label="Close" onClick={() => setMoreOpen(false)} />
          <div className="absolute inset-x-0 bottom-0 max-h-[85dvh] overflow-y-auto rounded-t-3xl bg-surface p-4 pb-[calc(1rem+env(safe-area-inset-bottom))]">
            <div className="mb-3 flex items-center justify-between">
              <div>
                <p className="font-bold">{me.fullName}</p>
                <p className="text-sm text-ink-3">{me.activeRole.label}</p>
              </div>
              <button type="button" onClick={() => setMoreOpen(false)} className="rounded-full p-2 hover:bg-surface-2" aria-label="Close">
                <X className="size-5" aria-hidden />
              </button>
            </div>

            {moreItems.length > 0 && (
              <div className="grid grid-cols-3 gap-2">
                {moreItems.map((item) => {
                  const Icon = iconFor(item.path);
                  return (
                    <NavLink
                      key={item.path}
                      to={to(item.path)}
                      className={({ isActive }) =>
                        cx(
                          'flex flex-col items-center gap-1 rounded-2xl p-3 text-center text-sm font-semibold',
                          isActive ? 'bg-blue-soft text-blue-ink' : 'bg-surface-2 text-ink',
                        )
                      }
                    >
                      <Icon className="size-6" aria-hidden />
                      {item.label}
                    </NavLink>
                  );
                })}
              </div>
            )}

            {roles.length > 1 && (
              <div className="mt-5">
                <p className="mb-2 text-sm font-semibold text-ink-2">Switch role</p>
                <div className="space-y-2" role="group" aria-label="Switch role">
                  {roles.map((r) => (
                    <button
                      key={r.id}
                      type="button"
                      aria-pressed={r.role === active}
                      disabled={switching !== null}
                      onClick={() => choose(r.role)}
                      className={cx(
                        'flex w-full items-center justify-between rounded-2xl border px-4 py-3 text-left',
                        r.role === active ? 'border-blue bg-blue-soft' : 'border-line',
                      )}
                    >
                      <span>
                        <span className="block font-semibold">{ROLE_LABEL[r.role]}</span>
                        <span className="block text-sm text-ink-3">{r.label}</span>
                      </span>
                      {switching === r.role ? <span className="text-sm text-ink-3">Switching…</span> : r.role === active && <BadgeCheck className="size-5 text-blue" aria-label="Current role" />}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <button
              type="button"
              onClick={signOut}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-2xl border border-line px-4 py-3 font-semibold text-bad"
            >
              <LogOut className="size-5" aria-hidden />
              Sign out
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
