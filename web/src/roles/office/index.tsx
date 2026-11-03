import type { RoleSection } from '../types';
import { Overview } from './Overview';
import { Learners } from './Learners';
import { Collections } from './Collections';
import { Fees } from './Fees';
import { Access } from './Access';
import { Groups } from './Groups';
import { Lists } from './Lists';
import { Uniform } from './Uniform';
import { Payouts } from './Payouts';
import { Notices } from './Notices';

export const office: RoleSection = {
  nav: [
    { path: '', label: 'Overview' },
    { path: 'learners', label: 'Learners' },
    { path: 'collections', label: 'Collections' },
    { path: 'fees', label: 'Fees' },
    { path: 'access', label: 'Access' },
    { path: 'groups', label: 'Groups' },
    { path: 'lists', label: 'Stationery' },
    { path: 'uniform', label: 'Uniform & sport' },
    { path: 'payouts', label: 'Payouts' },
    { path: 'notices', label: 'Notices' },
  ],
  routes: [
    { index: true, element: <Overview /> },
    { path: 'learners', element: <Learners /> },
    { path: 'collections', element: <Collections /> },
    { path: 'fees', element: <Fees /> },
    { path: 'access', element: <Access /> },
    { path: 'groups', element: <Groups /> },
    { path: 'lists', element: <Lists /> },
    { path: 'uniform', element: <Uniform /> },
    { path: 'payouts', element: <Payouts /> },
    { path: 'notices', element: <Notices /> },
  ],
};
