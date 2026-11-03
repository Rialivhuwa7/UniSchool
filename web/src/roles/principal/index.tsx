import type { RoleSection } from '../types';
import { Dashboard } from './Dashboard';
import { Approvals } from './Approvals';
import { Notices } from './Notices';
import { Settings } from './Settings';

export const principal: RoleSection = {
  nav: [
    { path: '', label: 'Dashboard' },
    { path: 'approvals', label: 'Approvals' },
    { path: 'notices', label: 'Notices' },
    { path: 'settings', label: 'Settings' },
  ],
  routes: [
    { index: true, element: <Dashboard /> },
    { path: 'approvals', element: <Approvals /> },
    { path: 'notices', element: <Notices /> },
    { path: 'settings', element: <Settings /> },
  ],
};
