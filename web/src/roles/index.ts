import type { Role } from '@unischool/api';
import type { RoleSection } from './types';
import { parent } from './parent';
import { learner } from './learner';
import { teacher } from './teacher';
import { principal } from './principal';
import { office } from './office';
import { tuckshop } from './tuckshop';

/** URL prefix for each role, e.g. the school office lives under /office. */
export const ROLE_BASE: Record<Role, string> = {
  PARENT: 'parent',
  LEARNER: 'learner',
  TEACHER: 'teacher',
  PRINCIPAL: 'principal',
  SCHOOL_OFFICE: 'office',
  TUCKSHOP: 'tuckshop',
};

export const SECTIONS: Record<Role, RoleSection> = {
  PARENT: parent,
  LEARNER: learner,
  TEACHER: teacher,
  PRINCIPAL: principal,
  SCHOOL_OFFICE: office,
  TUCKSHOP: tuckshop,
};

export function homeOf(role: Role): string {
  return `/${ROLE_BASE[role]}`;
}
