import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';
import { useId } from 'react';
import type { UseQueryResult } from '@tanstack/react-query';
import { formatRand } from '@unischool/api';
import { errorMessage } from '../lib/api';

/*
 * The building blocks every screen uses. Colours come from the prototype's tokens in index.css
 * (bg-surface, text-ink-2, border-line, bg-blue, text-ok, bg-warn-soft ...).
 */

export function cx(...classes: (string | false | null | undefined)[]) {
  return classes.filter(Boolean).join(' ');
}

/** A screen: heading, optional intro line and actions, then content. */
export function Page({ title, intro, actions, children }: { title: string; intro?: ReactNode; actions?: ReactNode; children: ReactNode }) {
  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl font-extrabold">{title}</h1>
          {intro && <p className="mt-1 text-ink-2">{intro}</p>}
        </div>
        {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
      </header>
      {children}
    </div>
  );
}

export function Card({ title, actions, children, className }: { title?: ReactNode; actions?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={cx('rounded-2xl border border-line bg-surface p-5', className)}>
      {(title || actions) && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          {title && <h2 className="text-lg font-bold">{title}</h2>}
          {actions}
        </div>
      )}
      {children}
    </section>
  );
}

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';
const VARIANTS: Record<Variant, string> = {
  primary: 'bg-blue text-on-blue hover:opacity-90',
  secondary: 'border border-line bg-surface text-ink hover:bg-surface-2',
  ghost: 'text-blue-ink hover:bg-surface-2',
  danger: 'bg-bad text-white hover:opacity-90',
};

export function Button({
  variant = 'primary',
  size = 'md',
  busy,
  className,
  children,
  disabled,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; size?: 'sm' | 'md'; busy?: boolean }) {
  return (
    <button
      type="button"
      {...rest}
      disabled={disabled || busy}
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-full font-semibold transition disabled:cursor-not-allowed disabled:opacity-50',
        size === 'sm' ? 'px-3 py-1.5 text-sm' : 'px-5 py-2.5',
        VARIANTS[variant],
        className,
      )}
    >
      {busy && <span className="size-4 animate-spin rounded-full border-2 border-current border-t-transparent" aria-hidden />}
      {children}
    </button>
  );
}

const control = 'w-full rounded-xl border border-line bg-surface px-3 py-2 text-ink placeholder:text-ink-3 focus:border-blue focus:outline-none';

/** Label + control + hint/error. Pass the control as a render function to get the id wired up. */
export function Field({ label, hint, error, children }: { label: string; hint?: ReactNode; error?: string; children: (id: string) => ReactNode }) {
  const id = useId();
  return (
    <div className="space-y-1">
      <label htmlFor={id} className="block text-sm font-semibold">
        {label}
      </label>
      {children(id)}
      {error ? <p className="text-sm text-bad">{error}</p> : hint ? <p className="text-sm text-ink-3">{hint}</p> : null}
    </div>
  );
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cx(control, props.className)} />;
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={cx(control, props.className)} />;
}

export function Textarea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea rows={4} {...props} className={cx(control, props.className)} />;
}

export function Checkbox({ label, ...rest }: InputHTMLAttributes<HTMLInputElement> & { label: ReactNode }) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2">
      <input type="checkbox" {...rest} className="size-4 accent-blue" />
      <span>{label}</span>
    </label>
  );
}

type Tone = 'neutral' | 'blue' | 'ok' | 'warn' | 'bad';
const TONES: Record<Tone, string> = {
  neutral: 'bg-surface-2 text-ink-2',
  blue: 'bg-blue-soft text-blue-ink',
  ok: 'bg-ok-soft text-ok',
  warn: 'bg-warn-soft text-warn',
  bad: 'bg-bad-soft text-bad',
};

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return <span className={cx('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold', TONES[tone])}>{children}</span>;
}

/** A headline number with a label, for dashboards. */
export function Stat({ label, value, hint, tone }: { label: string; value: ReactNode; hint?: ReactNode; tone?: Tone }) {
  return (
    <div className="rounded-2xl border border-line bg-surface p-4">
      <p className="text-sm text-ink-2">{label}</p>
      <p className={cx('mt-1 text-2xl font-extrabold tabular-nums', tone === 'ok' && 'text-ok', tone === 'warn' && 'text-warn', tone === 'bad' && 'text-bad')}>
        {value}
      </p>
      {hint && <p className="mt-1 text-sm text-ink-3">{hint}</p>}
    </div>
  );
}

export function Stats({ children }: { children: ReactNode }) {
  return <div className="grid grid-cols-2 gap-3 md:grid-cols-4">{children}</div>;
}

export function Money({ value, className }: { value: number | null | undefined; className?: string }) {
  return <span className={cx('tabular-nums', className)}>{formatRand(value)}</span>;
}

/** A thin progress bar for percentages (read rates, homework done, savings goals). */
export function Progress({ value, tone = 'blue' }: { value: number; tone?: 'blue' | 'ok' | 'warn' }) {
  const fill = tone === 'ok' ? 'bg-ok' : tone === 'warn' ? 'bg-warn' : 'bg-blue';
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-surface-2" role="progressbar" aria-valuenow={value} aria-valuemin={0} aria-valuemax={100}>
      <div className={cx('h-full rounded-full', fill)} style={{ width: `${Math.max(0, Math.min(100, value))}%` }} />
    </div>
  );
}

/** A simple data table. Columns render from each row. */
export function Table<T>({
  rows,
  columns,
  rowKey,
  empty = 'Nothing here yet.',
}: {
  rows: T[];
  columns: { header: string; cell: (row: T) => ReactNode; className?: string }[];
  rowKey: (row: T) => string | number;
  empty?: string;
}) {
  if (rows.length === 0) return <Empty>{empty}</Empty>;
  return (
    <div className="-mx-5 overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="border-b border-line text-ink-2">
            {columns.map((c) => (
              <th key={c.header} className={cx('px-5 py-2 font-semibold', c.className)}>
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)} className="border-b border-line last:border-0">
              {columns.map((c) => (
                <td key={c.header} className={cx('px-5 py-3 align-top', c.className)}>
                  {c.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return <p className="rounded-xl bg-surface-2 px-4 py-6 text-center text-ink-2">{children}</p>;
}

export function Spinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="flex items-center gap-3 py-8 text-ink-2" role="status">
      <span className="size-5 animate-spin rounded-full border-2 border-blue border-t-transparent" aria-hidden />
      {label}…
    </div>
  );
}

/** Inline message box. Use for errors from the API and for confirmations. */
export function Alert({ tone = 'bad', children }: { tone?: 'ok' | 'warn' | 'bad' | 'blue'; children: ReactNode }) {
  return (
    <div role={tone === 'bad' ? 'alert' : 'status'} className={cx('rounded-xl px-4 py-3 text-sm font-medium', TONES[tone])}>
      {children}
    </div>
  );
}

export function ErrorAlert({ error }: { error: unknown }) {
  return error ? <Alert>{errorMessage(error)}</Alert> : null;
}

/**
 * Renders a query's three states: spinner while loading, an error box on failure,
 * otherwise the children with the data.
 *
 *   <QueryView query={useQuery(...)}>{(wallet) => <Money value={wallet.balance} />}</QueryView>
 */
export function QueryView<T>({ query, children, label }: { query: UseQueryResult<T>; children: (data: T) => ReactNode; label?: string }) {
  if (query.isPending) return <Spinner label={label} />;
  if (query.isError) return <ErrorAlert error={query.error} />;
  return <>{children(query.data)}</>;
}

/** Date like "27 Oct 2026". Accepts ISO dates and date-times. */
export function formatDate(value: string | null | undefined): string {
  if (!value) return '';
  return new Date(value.length === 10 ? `${value}T00:00:00` : value).toLocaleDateString('en-ZA', { day: 'numeric', month: 'short', year: 'numeric' });
}

/** Date and time like "27 Oct, 09:30". */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '';
  return new Date(value).toLocaleString('en-ZA', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}
