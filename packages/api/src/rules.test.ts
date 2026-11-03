import { describe, expect, it } from 'vitest';
import { formatRand, isValidAmount, percent, tillDeclineReason } from './rules';

describe('formatRand', () => {
  it('matches the backend format', () => {
    expect(formatRand(1234.5)).toBe('R1 234.50');
    expect(formatRand(0)).toBe('R0.00');
    expect(formatRand(-20)).toBe('-R20.00');
    expect(formatRand(1234567.891)).toBe('R1 234 567.89');
  });
});

describe('percent', () => {
  it('clamps and handles an empty whole', () => {
    expect(percent(1, 3)).toBe(33);
    expect(percent(5, 0)).toBe(0);
    expect(percent(9, 4)).toBe(100);
  });
});

describe('tillDeclineReason', () => {
  const learner = {
    firstName: 'Lerato',
    frozen: false,
    tuckMode: 'FREE_CHOICE' as const,
    allowedItems: [],
    blockedCategories: ['Sweets'],
    dailyLimit: 50,
    spentToday: 30,
    walletBalance: 100,
  };
  const pie = { name: 'Pie', category: 'Hot food', price: 15 };

  it('allows a purchase within the rules', () => {
    expect(tillDeclineReason(learner, [pie])).toBeNull();
  });
  it('checks in the same order as the till', () => {
    expect(tillDeclineReason({ ...learner, frozen: true }, [pie])).toMatch(/frozen/);
    expect(tillDeclineReason({ ...learner, tuckMode: 'PREORDER_ONLY' }, [pie])).toMatch(/pre-orders/);
    expect(tillDeclineReason(learner, [{ name: 'Lollipop', category: 'Sweets', price: 2 }])).toMatch(/blocked/);
    expect(tillDeclineReason(learner, [pie, pie])).toMatch(/daily limit: R20.00 left/);
    expect(tillDeclineReason({ ...learner, walletBalance: 10 }, [pie])).toMatch(/Only R10.00/);
  });
});

describe('isValidAmount', () => {
  it('needs a positive amount in cents', () => {
    expect(isValidAmount(10.5)).toBe(true);
    expect(isValidAmount(0)).toBe(false);
    expect(isValidAmount(1.234)).toBe(false);
  });
});
