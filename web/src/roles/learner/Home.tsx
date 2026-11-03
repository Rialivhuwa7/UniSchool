import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { formatRand, isValidAmount, leftToday, ok, TUCK_MODE, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, Empty, Field, Input, Money, Page, Progress, QueryView, formatDate } from '../../components/ui';

/** The learner's own card, savings goal and the latest school news. */
export function Home() {
  const card = useQuery({ queryKey: ['learner', 'card'], queryFn: () => ok(api.GET('/api/me/card')) });
  const headlines = useQuery({ queryKey: ['learner', 'headlines'], queryFn: () => ok(api.GET('/api/me/headlines')) });

  return (
    <QueryView query={card}>
      {(c) => (
        <Page title={`Hi ${c.learner.firstName}`} intro={`${c.learner.grade} · ${c.learner.school}`}>
          <div className="grid gap-6 lg:grid-cols-2">
            <CardPanel learner={c.learner} token={c.cardToken} />
            <GoalCard learner={c.learner} />
          </div>
          <Card title="School news">
            <QueryView query={headlines}>
              {(items) =>
                items.length === 0 ? (
                  <Empty>No news for you right now.</Empty>
                ) : (
                  <ul className="space-y-3">
                    {items.map((h) => (
                      <li key={h.id} className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <p className="font-semibold">{h.title}</p>
                          <p className="text-sm text-ink-3">
                            {h.from} · {formatDate(h.publishedAt)}
                          </p>
                        </div>
                        {h.tag && <Badge tone="blue">{h.tag}</Badge>}
                      </li>
                    ))}
                  </ul>
                )
              }
            </QueryView>
          </Card>
        </Page>
      )}
    </QueryView>
  );
}

/** Looks like the physical card: balance, what's left to spend today, and the code the till scans. */
function CardPanel({ learner, token }: { learner: Schemas['LearnerView']; token: string }) {
  return (
    <section className="flex flex-col justify-between gap-6 rounded-2xl bg-blue p-6 text-on-blue shadow-lg">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-sm opacity-80">UniSchool card</p>
          <p className="text-xl font-bold">
            {learner.firstName} {learner.lastName}
          </p>
        </div>
        {learner.frozen && <Badge tone="bad">Frozen</Badge>}
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <p className="text-sm opacity-80">Balance</p>
          <Money value={learner.walletBalance} className="text-3xl font-extrabold" />
        </div>
        <div>
          <p className="text-sm opacity-80">Left to spend today</p>
          <Money value={leftToday(learner)} className="text-3xl font-extrabold" />
        </div>
      </div>
      <div className="flex flex-wrap items-end justify-between gap-2 text-sm">
        <div>
          <p className="opacity-80">Card number</p>
          <p className="font-mono text-base tracking-wider">{token}</p>
        </div>
        <p className="opacity-80">Tuckshop: {TUCK_MODE[learner.tuckMode]?.label ?? learner.tuckMode}</p>
      </div>
    </section>
  );
}

function GoalCard({ learner }: { learner: Schemas['LearnerView'] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [amount, setAmount] = useState('10');
  const goal = learner.goal ?? null;
  const value = Number(amount);

  const save = useMutation({
    mutationFn: () => ok(api.POST('/api/me/goal/save', { params: { query: { amount: value } } })),
    onSuccess: (saved) => {
      toast(`Saved ${formatRand(value)} towards ${goal?.name ?? 'your goal'}.`);
      queryClient.setQueryData(['learner', 'card'], (old: Schemas['CardView'] | undefined) => (old ? { ...old, learner: saved } : old));
      queryClient.invalidateQueries({ queryKey: ['learner'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  if (!goal) {
    return (
      <Card title="Savings goal">
        <Empty>You don't have a savings goal yet. Ask a parent to set one up.</Empty>
      </Card>
    );
  }

  const error = amount !== '' && !isValidAmount(value) ? 'Enter an amount like 10 or 12.50.' : value > learner.walletBalance ? "That's more than you have on your card." : undefined;

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!error && isValidAmount(value)) save.mutate();
  }

  return (
    <Card title="Savings goal">
      <p className="text-lg font-bold">{goal.name}</p>
      <div className="mt-3 space-y-1">
        <Progress value={goal.percent} tone={goal.percent >= 100 ? 'ok' : 'blue'} />
        <p className="text-sm text-ink-2">
          <Money value={goal.saved} /> of <Money value={goal.target} /> · {goal.percent}%
        </p>
      </div>
      <form onSubmit={submit} className="mt-5 flex flex-wrap items-end gap-3">
        <div className="w-40">
          <Field label="Move from my card" error={error}>
            {(id) => <Input id={id} type="number" inputMode="decimal" min="1" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} />}
          </Field>
        </div>
        <Button type="submit" busy={save.isPending} disabled={!!error || !isValidAmount(value)}>
          Save
        </Button>
      </form>
    </Card>
  );
}
