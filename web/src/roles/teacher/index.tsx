import { GroupsPage } from '../../components/groups';
import type { RoleSection } from '../types';
import { MyClass } from './MyClass';
import { Homework } from './Homework';
import { Notices } from './Notices';

export const teacher: RoleSection = {
  nav: [
    { path: '', label: 'My class' },
    { path: 'homework', label: 'Homework' },
    { path: 'notices', label: 'Notices' },
    { path: 'groups', label: 'Groups' },
  ],
  routes: [
    { index: true, element: <MyClass /> },
    { path: 'homework', element: <Homework /> },
    { path: 'notices', element: <Notices /> },
    { path: 'groups', element: <GroupsPage intro="Your class and team groups with parents." /> },
  ],
};
