import { useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { DAYS, dayLabel, formatRand, isValidAmount, ok, percent, type DayOfWeek, type Schemas } from '@unischool/api';
import { api } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Alert, Badge, Button, Card, Checkbox, Empty, Field, Input, Money, Page, Progress, QueryView, Select, Stat, Stats, Table, formatDate } from '../../components/ui';
import { ChildSelect, parseAmount, useAction, useLearners } from './shared';

/** The back-to-school savings pot, the family share link, and weekly pocket money. */
export function Savings() {
  const pot = useQuery({ queryKey: ['parent', 'savings'], queryFn: () => ok(api.GET('/api/savings')) });

  return (
    <Page title="Savings" intro="Put a little away each month so January isn't a shock.">
      <QueryView query={pot}>
        {(p) => (
          <>
            <Stats>
              <Stat label="In the pot" value={<Money value={p.balance} />} hint={`Goal ${formatRand(p.goal)}`} />
              <Stat label="Monthly debit" value={<Money value={p.monthlyDebit} />} hint={`${p.debitsBeforeOpening} more before ${formatDate(p.opensOn)}`} />
              <Stat label={`By ${formatDate(p.opensOn)}`} value={<Money value={p.projected} />} tone={p.onTrack ? 'ok' : 'warn'} hint={p.onTrack ? 'On track' : `Short by ${formatRand(p.shortBy)}`} />
            </Stats>
            <Card>
              <Progress value={percent(p.balance, p.goal)} tone={p.onTrack ? 'ok' : 'warn'} />
              <p className="mt-2 text-sm text-ink-2">
                {percent(p.balance, p.goal)}% of your <Money value={p.goal} /> goal. The pot opens on {formatDate(p.opensOn)} for fees, uniform and stationery.
              </p>
            </Card>
            <div className="grid gap-6 md:grid-cols-2">
              <AddNow />
              <Monthly current={p.monthlyDebit} shortBy={p.onTrack ? 0 : p.shortBy} debits={p.debitsBeforeOpening} />
            </div>
            <ShareLink link={p.shareLink} />
            <Card title="Contributions">
              <Table
                rows={p.contributions ?? []}
                rowKey={(c) => `${c.on}-${c.from}-${c.amount}`}
                empty="Nothing added yet."
                columns={[
                  { header: 'Date', cell: (c) => formatDate(c.on) },
                  { header: 'From', cell: (c) => c.from },
                  { header: 'Amount', className: 'text-right', cell: (c) => <Money value={c.amount} className="font-semibold text-ok" /> },
                ]}
              />
            </Card>
          </>
        )}
      </QueryView>
      <PocketMoney />
    </Page>
  );
}

function AddNow() {
  const [amount, setAmount] = useState('');
  const value = parseAmount(amount);
  const valid = value !== null && isValidAmount(value);
  const add = useAction((a: number) => ok(api.POST('/api/savings/add', { body: { amount: a } })), (_, a) => `${formatRand(a)} moved from your wallet to savings.`);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (valid) add.mutate(value, { onSuccess: () => setAmount('') });
  }

  return (
    <Card title="Add now">
      <form onSubmit={submit} className="space-y-3">
        <Field label="Amount (R)" hint="Comes out of your family wallet." error={amount && !valid ? 'Enter an amount like 200.' : undefined}>
          {(id) => <Input id={id} type="number" min={1} step="0.01" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />}
        </Field>
        <Button type="submit" busy={add.isPending} disabled={!valid}>
          Add to savings
        </Button>
      </form>
    </Card>
  );
}

function Monthly({ current, shortBy, debits }: { current: number; shortBy: number; debits: number }) {
  const [amount, setAmount] = useState(String(current));
  const value = parseAmount(amount);
  const valid = value !== null && isValidAmount(value) && value >= 100 && value <= 20000;
  const suggested = debits > 0 && shortBy > 0 ? Math.ceil((current + shortBy / debits) / 10) * 10 : null;
  const save = useAction((m: number) => ok(api.PUT('/api/savings/monthly', { body: { monthlyDebit: m } })), (_, m) => `Monthly debit set to ${formatRand(m)}.`);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (valid) save.mutate(value);
  }

  return (
    <Card title="Monthly debit order">
      <form onSubmit={submit} className="space-y-3">
        <Field
          label="Each month (R)"
          hint={suggested ? `About ${formatRand(suggested)} a month would get you there.` : 'Between R100 and R20 000.'}
          error={amount && !valid ? 'Choose between R100 and R20 000.' : undefined}
        >
          {(id) => <Input id={id} type="number" min={100} max={20000} step="0.01" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />}
        </Field>
        <Button type="submit" busy={save.isPending} disabled={!valid || value === current}>
          Save
        </Button>
      </form>
    </Card>
  );
}

function ShareLink({ link }: { link: string | null }) {
  const toast = useToast();
  // The API gives its own path (/api/give/<token>); family members open the web page /give/<token>.
  const token = link?.split('/').pop();
  if (!token) return null;
  const url = `${window.location.origin}/give/${token}`;

  async function copy() {
    try {
      await navigator.clipboard.writeText(url);
      toast('Link copied. Share it with family on WhatsApp or SMS.');
    } catch {
      toast("Couldn't copy. Select the link and copy it yourself.", 'bad');
    }
  }

  return (
    <Card title="Let family help">
      <p className="mb-3 text-ink-2">Grandparents, aunts and uncles can add to the pot with this link. They don't need an account and won't see anything else.</p>
      <div className="flex flex-wrap gap-2">
        <Input readOnly value={url} onFocus={(e) => e.target.select()} className="min-w-0 flex-1 font-mono text-sm" aria-label="Share link" />
        <Button variant="secondary" onClick={copy}>
          Copy link
        </Button>
      </div>
    </Card>
  );
}

/** Weekly pocket money from the family wallet into each child's wallet. */
function PocketMoney() {
  const list = useQuery({ queryKey: ['parent', 'pocket-money'], queryFn: () => ok(api.GET('/api/pocket-money')) });
  const learners = useLearners();

  return (
    <Card title="Pocket money">
      <p className="mb-4 text-ink-2">Paid automatically from your wallet into your child's wallet every week.</p>
      <QueryView query={list}>
        {(rows) => {
          const without = (learners.data ?? []).filter((l) => !rows.some((r) => r.learnerId === l.id));
          return (
            <div className="space-y-4">
              {rows.length === 0 ? <Empty>No pocket money set up yet.</Empty> : rows.map((r) => <PocketMoneyRow key={r.id} row={r} />)}
              {without.length > 0 && <NewPocketMoney learners={without} />}
            </div>
          );
        }}
      </QueryView>
    </Card>
  );
}

function PocketMoneyRow({ row }: { row: Schemas['PocketMoneyView'] }) {
  const [amount, setAmount] = useState(String(row.amount));
  const [day, setDay] = useState<DayOfWeek>(row.dayOfWeek);
  const [enabled, setEnabled] = useState(row.enabled);
  const value = parseAmount(amount);
  const valid = value !== null && isValidAmount(value) && value <= 2000;
  const changed = value !== row.amount || day !== row.dayOfWeek || enabled !== row.enabled;
  const save = useAction(
    () => ok(api.PATCH('/api/pocket-money/{id}', { params: { path: { id: row.id } }, body: { amount: value ?? 0, dayOfWeek: day, enabled } })),
    (r) => (r.enabled ? `${r.learnerName} gets ${formatRand(r.amount)} every ${dayLabel(r.dayOfWeek)}.` : `${r.learnerName}'s pocket money is paused.`),
  );

  return (
    <div className="flex flex-wrap items-end gap-3 rounded-xl border border-line p-3">
      <p className="w-24 pb-2 font-semibold">
        {row.learnerName} {!row.enabled && <Badge>Paused</Badge>}
      </p>
      <Field label="Amount (R)" error={amount && !valid ? 'R1 to R2 000.' : undefined}>
        {(id) => <Input id={id} type="number" min={1} max={2000} step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} className="w-32" />}
      </Field>
      <Field label="Every">{(id) => <DaySelect id={id} value={day} onChange={setDay} />}</Field>
      <div className="pb-2">
        <Checkbox label="On" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
      </div>
      <Button size="sm" busy={save.isPending} disabled={!valid || !changed} onClick={() => save.mutate(undefined)} className="mb-1">
        Save
      </Button>
    </div>
  );
}

function NewPocketMoney({ learners }: { learners: Schemas['LearnerView'][] }) {
  const [learnerId, setLearnerId] = useState<number | undefined>(learners[0]?.id);
  const [amount, setAmount] = useState('');
  const [day, setDay] = useState<DayOfWeek>('MONDAY');
  const value = parseAmount(amount);
  const valid = value !== null && isValidAmount(value) && value <= 2000;
  const add = useAction(
    () => ok(api.POST('/api/pocket-money', { body: { learnerId: learnerId ?? 0, amount: value ?? 0, dayOfWeek: day, enabled: true } })),
    (r) => `${r.learnerName} gets ${formatRand(r.amount)} every ${dayLabel(r.dayOfWeek)}.`,
  );

  function submit(e: FormEvent) {
    e.preventDefault();
    if (valid && learnerId) add.mutate(undefined);
  }

  return (
    <form onSubmit={submit} className="flex flex-wrap items-end gap-3 rounded-xl bg-surface-2 p-3">
      <Field label="Set up for">{(id) => <ChildSelect id={id} learners={learners} value={learnerId} onChange={setLearnerId} />}</Field>
      <Field label="Amount (R)">
        {(id) => <Input id={id} type="number" min={1} max={2000} step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} className="w-32" />}
      </Field>
      <Field label="Every">{(id) => <DaySelect id={id} value={day} onChange={setDay} />}</Field>
      <Button type="submit" size="sm" busy={add.isPending} disabled={!valid || !learnerId} className="mb-1">
        Start pocket money
      </Button>
      {amount && !valid && <Alert tone="warn">Choose an amount from R1 to R2 000.</Alert>}
    </form>
  );
}

function DaySelect({ id, value, onChange }: { id: string; value: DayOfWeek; onChange: (d: DayOfWeek) => void }) {
  return (
    <Select id={id} value={value} onChange={(e) => onChange(e.target.value as DayOfWeek)} className="max-w-60">
      {DAYS.map((d) => (
        <option key={d} value={d}>
          {dayLabel(d)}
        </option>
      ))}
    </Select>
  );
}
