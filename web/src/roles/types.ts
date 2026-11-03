import type { RouteObject } from 'react-router';

/**
 * One role's part of the app. Each role lives in its own folder under src/roles and exports a
 * RoleSection: its tabs in the top bar and its routes, mounted under /<base> (e.g. /office).
 */
export interface RoleSection {
  /** Tabs in the sub-navigation. `path` is relative to the role's base; '' is the role's home. */
  nav: { path: string; label: string }[];
  /** Routes relative to the role's base. Use `index: true` for the home screen. */
  routes: RouteObject[];
}
