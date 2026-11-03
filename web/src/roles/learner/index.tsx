import type { RoleSection } from '../types';
import { Home } from './Home';
import { Homework } from './Homework';
import { Tuckshop } from './Tuckshop';
import { Activity } from './Activity';

export const learner: RoleSection = {
  nav: [
    { path: '', label: 'Home' },
    { path: 'homework', label: 'Homework' },
    { path: 'tuckshop', label: 'Tuckshop' },
    { path: 'activity', label: 'Activity' },
  ],
  routes: [
    { index: true, element: <Home /> },
    { path: 'homework', element: <Homework /> },
    { path: 'tuckshop', element: <Tuckshop /> },
    { path: 'activity', element: <Activity /> },
  ],
};
