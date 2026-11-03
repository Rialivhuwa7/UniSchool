import type { RoleSection } from '../types';
import { Till } from './Till';
import { Today } from './Today';
import { Menu } from './Menu';

export const tuckshop: RoleSection = {
  nav: [
    { path: '', label: 'Till' },
    { path: 'pre-orders', label: 'Pre-orders' },
    { path: 'menu', label: 'Menu' },
  ],
  routes: [
    { index: true, element: <Till /> },
    { path: 'pre-orders', element: <Today /> },
    { path: 'menu', element: <Menu /> },
  ],
};
