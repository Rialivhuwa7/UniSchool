import type { components } from './schema';

/** Every request and response shape the backend publishes, by its Java record name. */
export type Schemas = components['schemas'];

export type Role = Schemas['RoleView']['role'];
export type Campus = Schemas['RoleView']['campus'];
export type TuckMode = Schemas['LearnerView']['tuckMode'];
export type PublishStatus = Schemas['PostResult']['status'];
export type FeePlan = Schemas['FeeView']['plan'];
export type ProductCategory = Schemas['CatalogEntry']['category'];
export type CollectionKind = Schemas['CollectionStats']['kind'];
export type DayOfWeek = Schemas['PocketMoneyView']['dayOfWeek'];

export type MeResponse = Schemas['MeResponse'];
export type RoleView = Schemas['RoleView'];
export type TokenResponse = Schemas['TokenResponse'];
export type LearnerView = Schemas['LearnerView'];
export type TxView = Schemas['TxView'];
export type NoticeView = Schemas['NoticeView'];
export type HomeworkView = Schemas['HomeworkView'];
export type PreOrderView = Schemas['PreOrderView'];
export type CatalogEntry = Schemas['CatalogEntry'];
export type CartView = Schemas['CartView'];
export type OrderView = Schemas['OrderView'];
