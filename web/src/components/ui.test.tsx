import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { UseQueryResult } from '@tanstack/react-query';
import { ApiError } from '@unischool/api';
import { Money, QueryView, formatDate } from './ui';

const query = <T,>(state: Partial<UseQueryResult<T>>) => state as UseQueryResult<T>;

describe('QueryView', () => {
  it('shows a spinner while loading', () => {
    render(<QueryView query={query<string>({ isPending: true })}>{(d) => d}</QueryView>);
    expect(screen.getByRole('status')).toHaveTextContent('Loading');
  });

  it("shows the backend's message on failure", () => {
    const error = new ApiError(422, 'Your wallet has R120.00');
    render(<QueryView query={query<string>({ isPending: false, isError: true, error })}>{(d) => d}</QueryView>);
    expect(screen.getByRole('alert')).toHaveTextContent('Your wallet has R120.00');
  });

  it('renders the data', () => {
    render(<QueryView query={query({ isPending: false, isError: false, data: 'Ready' })}>{(d) => <p>{d}</p>}</QueryView>);
    expect(screen.getByText('Ready')).toBeInTheDocument();
  });
});

describe('formatting', () => {
  it('writes rands like the backend', () => {
    render(<Money value={1234.5} />);
    expect(screen.getByText('R1 234.50')).toBeInTheDocument();
  });

  it('formats ISO dates for South Africa', () => {
    expect(formatDate('2026-10-28')).toMatch(/28 Oct 2026/);
  });
});
