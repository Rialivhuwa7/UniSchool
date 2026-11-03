import type { Campus, CollectionKind, DayOfWeek, FeePlan, LearnerView, ProductCategory, PublishStatus, Role, TuckMode } from './types';

/*
 * Business rules shared by the web and mobile apps. These give people instant feedback before
 * a request is sent; the backend still enforces every rule and has the final say.
 */

/** Rands the way the backend writes them: "R1 234.50", "-R20.00". Mirrors Money.format in Java. */
export function formatRand(value: number | null | undefined): string {
  const v = Math.round((value ?? 0) * 100) / 100;
  const sign = v < 0 ? '-' : '';
  const [whole, cents] = Math.abs(v).toFixed(2).split('.');
  return `${sign}R${whole.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')}.${cents}`;
}

/** Whole percentage of part in whole, clamped to 0..100. Mirrors Money.pct in Java. */
export function percent(part: number, whole: number): number {
  if (whole <= 0) return 0;
  return Math.max(0, Math.min(100, Math.round((part * 100) / whole)));
}

export const ROLE_LABEL: Record<Role, string> = {
  PARENT: 'Parent',
  LEARNER: 'Learner',
  TEACHER: 'Teacher',
  PRINCIPAL: 'Principal',
  SCHOOL_OFFICE: 'School office',
  TUCKSHOP: 'Tuckshop',
};

export const CAMPUS_LABEL: Record<Campus, string> = {
  PREP: 'Highveld Ridge Prep',
  COLLEGE: 'Highveld Ridge College',
  BOTH: 'Both schools',
};

export const TUCK_MODE: Record<TuckMode, { label: string; help: string }> = {
  PREORDER_ONLY: { label: 'Pre-orders only', help: 'They collect what you pre-order. Anything else is declined.' },
  APPROVED_ITEMS: { label: 'Approved items', help: 'Pre-orders plus items you approve, up to the daily limit.' },
  FREE_CHOICE: { label: 'Free choice', help: 'Anything within the daily limit, except blocked categories.' },
};

export const STATUS_LABEL: Record<PublishStatus, string> = {
  PENDING: 'Waiting for approval',
  CHANGES_REQUESTED: 'Changes requested',
  PUBLISHED: 'Published',
};

export const CATEGORY_LABEL: Record<ProductCategory, string> = {
  SUPPLY_LIST: 'Stationery list',
  UNIFORM: 'Uniform',
  SPORT: 'Sport',
  PRELOVED: 'Pre-loved',
  LABELS: 'Name labels',
};

export const KIND_LABEL: Record<CollectionKind, string> = {
  PAYMENT: 'Payment',
  TICKET: 'Tickets',
  DONATION: 'Donation',
};

export const PLAN_LABEL: Record<FeePlan, string> = {
  ANNUAL: 'Annual',
  TERMLY: 'Termly',
  MONTHLY_10: '10 months',
  MONTHLY_12: '12 months',
  FINANCED: 'Financed',
};

/** Audience value meaning every grade. Mirrors Audience.ALL_GRADES in Java. */
export const ALL_GRADES = 'All';

/** Grades at each school, as the backend stores them ("Grade R", "Grade 4"). */
export const GRADES: Record<Exclude<Campus, 'BOTH'>, string[]> = {
  PREP: ['Grade R', ...[1, 2, 3, 4, 5, 6, 7].map((g) => `Grade ${g}`)],
  COLLEGE: [8, 9, 10, 11, 12].map((g) => `Grade ${g}`),
};

export function gradesFor(campus: Campus): string[] {
  return campus === 'BOTH' ? [...GRADES.PREP, ...GRADES.COLLEGE] : GRADES[campus];
}

/** Sports teams notices and collections can be aimed at. */
export const TEAMS = ['Athletics', 'Cricket', 'Hockey', 'Netball', 'Rugby', 'Soccer', 'Swimming', 'Tennis'];

export const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export function dayLabel(day: DayOfWeek): string {
  return day.charAt(0) + day.slice(1).toLowerCase();
}

/** What's still allowed today at the till. Mirrors Learner.remainingToday. */
export function leftToday(learner: Pick<LearnerView, 'dailyLimit' | 'spentToday'>): number {
  return Math.max(0, learner.dailyLimit - learner.spentToday);
}

/**
 * Why a till purchase would be declined, or null if it would go through. Same order of checks
 * as PosService.charge: frozen card, pre-order-only, approved items, blocked categories,
 * daily limit, wallet balance.
 */
export function tillDeclineReason(
  learner: Pick<LearnerView, 'firstName' | 'frozen' | 'tuckMode' | 'allowedItems' | 'blockedCategories' | 'dailyLimit' | 'spentToday' | 'walletBalance'>,
  items: { name: string; category: string; price: number }[],
): string | null {
  const total = items.reduce((sum, i) => sum + i.price, 0);
  const name = learner.firstName;
  if (learner.frozen) return `${name}'s card is frozen by a parent.`;
  if (learner.tuckMode === 'PREORDER_ONLY') return `${name} can only collect what their parent pre-orders.`;
  if (learner.tuckMode === 'APPROVED_ITEMS') {
    const na = items.find((i) => !learner.allowedItems.includes(i.name));
    if (na) return `${na.name} isn't on ${name}'s approved list.`;
  }
  if (learner.tuckMode === 'FREE_CHOICE') {
    const bad = items.find((i) => learner.blockedCategories.includes(i.category));
    if (bad) return `${bad.category} are blocked by ${name}'s parent.`;
  }
  if (learner.spentToday + total > learner.dailyLimit) return `Over the daily limit: ${formatRand(leftToday(learner))} left today.`;
  if (learner.walletBalance < total) return `Only ${formatRand(learner.walletBalance)} in the wallet.`;
  return null;
}

/** Positive amount with at most two decimals, as the backend's @Positive money fields expect. */
export function isValidAmount(value: number): boolean {
  return Number.isFinite(value) && value > 0 && Math.round(value * 100) === value * 100;
}
