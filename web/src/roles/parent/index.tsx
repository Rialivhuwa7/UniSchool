import type { RoleSection } from '../types';
import { GroupsPage } from '../../components/groups';
import { Home } from './Home';
import { ChildDetail, Children } from './Children';
import { Wallet } from './Wallet';
import { Tuckshop } from './Tuckshop';
import { Notices } from './Notices';
import { Homework } from './Homework';
import { Payments } from './Payments';
import { Cart, Orders, Shop } from './Shop';
import { Savings } from './Savings';

export const parent: RoleSection = {
  nav: [
    { path: '', label: 'Home' },
    { path: 'children', label: 'Children' },
    { path: 'wallet', label: 'Wallet' },
    { path: 'tuckshop', label: 'Tuckshop' },
    { path: 'notices', label: 'Notices' },
    { path: 'homework', label: 'Homework' },
    { path: 'payments', label: 'Payments' },
    { path: 'shop', label: 'Shop' },
    { path: 'savings', label: 'Savings' },
    { path: 'groups', label: 'Groups' },
  ],
  routes: [
    { index: true, element: <Home /> },
    { path: 'children', element: <Children /> },
    { path: 'children/:id', element: <ChildDetail /> },
    { path: 'wallet', element: <Wallet /> },
    { path: 'tuckshop', element: <Tuckshop /> },
    { path: 'notices', element: <Notices /> },
    { path: 'homework', element: <Homework /> },
    { path: 'payments', element: <Payments /> },
    { path: 'shop', element: <Shop /> },
    { path: 'shop/cart', element: <Cart /> },
    { path: 'shop/orders', element: <Orders /> },
    { path: 'savings', element: <Savings /> },
    { path: 'groups', element: <GroupsPage /> },
  ],
};
