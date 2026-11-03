import { useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { formatRand, isValidAmount, ok } from '@unischool/api';
import { api } from '../../lib/api';
import { Button, Card, Field, Input, Money, Page, QueryView, Stat, Stats } from '../../components/ui';
import { ActivityTable } from './Children';
import { ChildSelect, parseAmount, useAction, useLearners } from './shared';

const QUICK = [100, 200, 500];

/** The family wallet: add money, send some to a child, and see where it went. */
export function Wallet() {
  const wallet = useQuery({ queryKey: ['parent', 'wallet'], queryFn: () => ok(api.GET('/api/wallet')) });
  const activity = useQuery({ queryKey: ['parent', 'wallet', 'activity'], queryFn: () => ok(api.GET('/api/wallet/activity')) });
  const learners = useLearners();

  return (
    <Page title="Wallet" intro="Money you add here can go to your children, the tuckshop, school payments and the shop.">
      <Stats>
        <Stat label="Family wallet" value={wallet.data ? <Money value={wallet.data.balance} /> : '…'} />
        {learners.data?.map((l) => <Stat key={l.id} label={`${l.firstName}'s wallet`} value={<Money value={l.walletBalance} />} />)}
      </Stats>

      <div className="grid gap-6 md:grid-cols-2">
        <TopUp />
        <Card title="Send to a child">
          <QueryView query={learners}>{(kids) => <MoveToChild learnerIds={kids.map((k) => k.id)} />}</QueryView>
        </Card>
      </div>

      <Card title="Activity">
        <QueryView query={activity}>{(rows) => <ActivityTable rows={rows} showWho empty="Nothing in or out yet." />}</QueryView>
      </Card>
    </Page>
  );
}

function TopUp() {
  const [amount, setAmount] = useState('');
  const value = parseAmount(amount);
  const valid = value !== null && isValidAmount(value);
  const topUp = useAction(
    (a: number) => ok(api.POST('/api/wallet/top-up', { body: { amount: a } })),
    (_, a) => `${formatRand(a)} added to your wallet.`,
  );

  function submit(e: FormEvent) {
    e.preventDefault();
    if (valid) topUp.mutate(value, { onSuccess: () => setAmount('') });
  }

  return (
    <Card title="Add money">
      <form onSubmit={submit} className="space-y-3">
        <div className="flex flex-wrap gap-2">
          {QUICK.map((q) => (
            <Button key={q} type="button" size="sm" variant="secondary" onClick={() => setAmount(String(q))}>
              {formatRand(q)}
            </Button>
          ))}
        </div>
        <Field label="Amount (R)" hint="Paid by card or instant EFT." error={amount && !valid ? 'Enter an amount like 150 or 150.50.' : undefined}>
          {(id) => <Input id={id} type="number" min={1} step="0.01" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />}
        </Field>
        <Button type="submit" busy={topUp.isPending} disabled={!valid}>
          Add {valid ? formatRand(value) : 'money'}
        </Button>
      </form>
    </Card>
  );
}

function MoveToChild({ learnerIds }: { learnerIds: number[] }) {
  const learners = useLearners();
  const [learnerId, setLearnerId] = useState<number | undefined>(learnerIds[0]);
  const [amount, setAmount] = useState('');
  const value = parseAmount(amount);
  const valid = value !== null && isValidAmount(value);
  const child = learners.data?.find((l) => l.id === learnerId);
  const move = useAction(
    (v: { id: number; amount: number }) => ok(api.POST('/api/wallet/move/{learnerId}', { params: { path: { learnerId: v.id } }, body: { amount: v.amount } })),
    (_, v) => `${formatRand(v.amount)} sent to ${learners.data?.find((l) => l.id === v.id)?.firstName ?? 'your child'}.`,
  );

  function submit(e: FormEvent) {
    e.preventDefault();
    if (valid && learnerId) move.mutate({ id: learnerId, amount: value }, { onSuccess: () => setAmount('') });
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <Field label="Child">{(id) => <ChildSelect id={id} learners={learners.data ?? []} value={learnerId} onChange={setLearnerId} />}</Field>
      <Field label="Amount (R)" hint={child && <>They have <Money value={child.walletBalance} /> now.</>} error={amount && !valid ? 'Enter an amount like 50 or 50.50.' : undefined}>
        {(id) => <Input id={id} type="number" min={1} step="0.01" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />}
      </Field>
      <Button type="submit" busy={move.isPending} disabled={!valid || !learnerId}>
        Send {child ? `to ${child.firstName}` : ''}
      </Button>
    </form>
  );
}
