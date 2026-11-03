import { useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { formatRand, isValidAmount, KIND_LABEL, ok, PLAN_LABEL, type FeePlan, type Schemas } from '@unischool/api';
import { api } from '../../lib/api';
import { Alert, Badge, Button, Card, Empty, Field, Input, Money, Page, QueryView, Select, Table, cx, formatDate } from '../../components/ui';
import { parseAmount, useAction } from './shared';

type Collection = Schemas['CollectionForChild'];
type Method = 'WALLET' | 'CARD';

/** Trips, events, tickets and donations; then next year's school fee plans. */
export function Payments() {
  const collections = useQuery({ queryKey: ['parent', 'collections'], queryFn: () => ok(api.GET('/api/payments/collections')) });
  const tickets = useQuery({ queryKey: ['parent', 'tickets'], queryFn: () => ok(api.GET('/api/payments/tickets')) });
  const fees = useQuery({ queryKey: ['parent', 'fees'], queryFn: () => ok(api.GET('/api/fees')) });
  const [paying, setPaying] = useState<Collection | null>(null);

  return (
    <Page title="Payments" intro="School trips, events and fees in one place.">
      <Card title="School payments">
        <QueryView query={collections}>
          {(rows) => (
            <>
              <Table
                rows={rows}
                rowKey={(c) => `${c.collectionId}-${c.learnerId}`}
                empty="Nothing to pay right now."
                columns={[
                  {
                    header: 'For',
                    cell: (c) => (
                      <div>
                        <p className="font-semibold">{c.title}</p>
                        <p className="text-ink-3">
                          {c.learnerName} · {KIND_LABEL[c.kind]}
                          {c.instalments > 1 && ` · up to ${c.instalments} instalments`}
                        </p>
                      </div>
                    ),
                  },
                  { header: 'Due', cell: (c) => (c.dueDate ? formatDate(c.dueDate) : '—') },
                  {
                    header: 'Amount',
                    cell: (c) => (
                      <div>
                        <Money value={c.amount} />
                        {c.kind === 'TICKET' && <span className="text-ink-3"> each</span>}
                        {c.kind === 'PAYMENT' && c.paid > 0 && (
                          <p className="text-ink-3">
                            <Money value={c.paid} /> paid, <Money value={c.owing} /> to go
                          </p>
                        )}
                        {c.kind === 'TICKET' && c.ticketsBought > 0 && <p className="text-ink-3">{c.ticketsBought} bought</p>}
                      </div>
                    ),
                  },
                  { header: 'Status', cell: (c) => <Badge tone={statusTone(c)}>{c.status}</Badge> },
                  {
                    header: '',
                    className: 'text-right',
                    cell: (c) =>
                      c.kind === 'PAYMENT' && c.owing <= 0 ? null : (
                        <Button size="sm" variant={paying?.collectionId === c.collectionId && paying.learnerId === c.learnerId ? 'primary' : 'secondary'} onClick={() => setPaying(c)}>
                          {c.kind === 'TICKET' ? 'Buy' : c.kind === 'DONATION' ? 'Give' : 'Pay'}
                        </Button>
                      ),
                  },
                ]}
              />
              {paying && <PayForm key={`${paying.collectionId}-${paying.learnerId}`} item={paying} onDone={() => setPaying(null)} />}
            </>
          )}
        </QueryView>
      </Card>

      <Card title="Your tickets">
        <QueryView query={tickets}>
          {(list) =>
            list.length === 0 ? (
              <Empty>No tickets yet.</Empty>
            ) : (
              <ul className="grid gap-2 sm:grid-cols-2 md:grid-cols-3">
                {list.map((t) => (
                  <li key={t.code} className="rounded-xl border border-dashed border-line p-3">
                    <p className="font-mono text-lg font-bold">{t.code}</p>
                    <p className="text-sm text-ink-2">{t.title}</p>
                  </li>
                ))}
              </ul>
            )
          }
        </QueryView>
      </Card>

      <QueryView query={fees}>
        {(list) => (
          <>
            {list.map((f) => (
              <FeeCard key={f.learnerId} fee={f} />
            ))}
          </>
        )}
      </QueryView>
    </Page>
  );
}

function statusTone(c: Collection): 'ok' | 'warn' | 'neutral' {
  if (c.kind === 'PAYMENT') return c.owing <= 0 ? 'ok' : c.paid > 0 ? 'warn' : 'neutral';
  return c.paid > 0 || c.ticketsBought > 0 ? 'ok' : 'neutral';
}

function PayForm({ item, onDone }: { item: Collection; onDone: () => void }) {
  const [part, setPart] = useState<'FULL' | 'INSTALMENT'>('FULL');
  const [quantity, setQuantity] = useState(1);
  const [donation, setDonation] = useState(String(item.amount));
  const [method, setMethod] = useState<Method>('WALLET');

  const instalment = Math.min(Math.round((item.amount / Math.max(1, item.instalments)) * 100) / 100, item.owing);
  const donationValue = parseAmount(donation);
  const donationOk = donationValue !== null && isValidAmount(donationValue) && donationValue >= 10;
  const total =
    item.kind === 'TICKET' ? item.amount * quantity : item.kind === 'DONATION' ? (donationValue ?? 0) : part === 'INSTALMENT' ? instalment : item.owing;

  const pay = useAction(
    () =>
      ok(
        api.POST('/api/payments/collections/{id}/pay', {
          params: { path: { id: item.collectionId } },
          body: {
            learnerId: item.learnerId,
            method,
            ...(item.kind === 'PAYMENT' && { part }),
            ...(item.kind === 'TICKET' && { quantity }),
            ...(item.kind === 'DONATION' && { amount: donationValue ?? 0 }),
          },
        }),
      ),
    (r) => (r.ticketCodes?.length ? `${r.message} Ticket codes: ${r.ticketCodes.join(', ')}.` : r.message),
  );

  return (
    <div className="mt-4 space-y-4 rounded-xl border border-blue p-4">
      <h3 className="font-bold">
        {item.title} for {item.learnerName}
      </h3>
      {item.kind === 'PAYMENT' && item.instalments > 1 && (
        <div className="flex flex-wrap gap-2">
          <Choice active={part === 'FULL'} onClick={() => setPart('FULL')}>
            Pay it all · {formatRand(item.owing)}
          </Choice>
          <Choice active={part === 'INSTALMENT'} onClick={() => setPart('INSTALMENT')}>
            One instalment · {formatRand(instalment)}
          </Choice>
        </div>
      )}
      <div className="flex flex-wrap items-end gap-4">
        {item.kind === 'TICKET' && (
          <Field label="Tickets">
            {(id) => (
              <Select id={id} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} className="max-w-60">
                {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => (
                  <option key={n} value={n}>
                    {n}
                  </option>
                ))}
              </Select>
            )}
          </Field>
        )}
        {item.kind === 'DONATION' && (
          <Field label="Amount (R)" error={donation && !donationOk ? 'Give R10 or more.' : undefined}>
            {(id) => <Input id={id} type="number" min={10} step="0.01" inputMode="decimal" value={donation} onChange={(e) => setDonation(e.target.value)} />}
          </Field>
        )}
        <Field label="Pay with">
          {(id) => (
            <Select id={id} value={method} onChange={(e) => setMethod(e.target.value as Method)} className="max-w-60">
              <option value="WALLET">Family wallet</option>
              <option value="CARD">Card</option>
            </Select>
          )}
        </Field>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button busy={pay.isPending} disabled={item.kind === 'DONATION' && !donationOk} onClick={() => pay.mutate(undefined, { onSuccess: onDone })}>
          Pay {formatRand(total)}
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </div>
  );
}

function Choice({ active, onClick, children }: { active: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cx('rounded-full border px-4 py-2 text-sm font-semibold', active ? 'border-blue bg-blue-soft text-blue-ink' : 'border-line hover:bg-surface-2')}
    >
      {children}
    </button>
  );
}

function FeeCard({ fee: f }: { fee: Schemas['FeeView'] }) {
  const choose = useAction(
    (plan: FeePlan) => ok(api.PUT('/api/fees/{learnerId}/plan', { params: { path: { learnerId: f.learnerId } }, body: { plan } })),
    (r) => `${r.learnerName}'s ${r.year} fees: ${r.planTitle ?? PLAN_LABEL[r.plan]}.`,
  );

  return (
    <Card title={`${f.year} school fees: ${f.learnerName}`} actions={<span className="text-sm text-ink-2">{f.school}</span>}>
      <div className="space-y-4">
        {f.plan ? (
          <Alert tone="ok">
            You chose {f.planTitle ?? PLAN_LABEL[f.plan]}: {f.payments} payment{f.payments === 1 ? '' : 's'} of {formatRand(f.eachPayment)}, starting{' '}
            {formatDate(f.firstPayment)}.
          </Alert>
        ) : (
          <Alert tone="warn">Choose how you'd like to pay {f.learnerName}'s fees of {formatRand(f.annualFee)} for the year.</Alert>
        )}
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {(f.options ?? []).map((o) => {
            const current = o.plan === f.plan;
            return (
              <div key={o.plan} className={cx('flex flex-col rounded-xl border p-4', current ? 'border-blue bg-blue-soft' : 'border-line')}>
                <p className="font-bold">{o.title}</p>
                <p className="mt-1 flex-1 text-sm text-ink-2">{o.description}</p>
                <p className="mt-3 text-lg font-extrabold">
                  {o.payments > 1 && `${o.payments} × `}
                  <Money value={o.eachPayment} />
                </p>
                <p className="text-sm text-ink-3">
                  Total <Money value={o.totalCost} />
                </p>
                <div className="mt-3">
                  {current ? (
                    <Badge tone="ok">Your plan</Badge>
                  ) : (
                    <Button size="sm" variant="secondary" busy={choose.isPending && choose.variables === o.plan} onClick={() => choose.mutate(o.plan)}>
                      Choose this
                    </Button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </Card>
  );
}
