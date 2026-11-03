import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { formatRand, ok, type PreOrderView, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Alert, Badge, Button, Card, Checkbox, Empty, Money, Page, QueryView } from '../../components/ui';

type MenuItem = Schemas['MenuItemView'];
type Day = Schemas['DayView'];

/** TUCK_MODE's help is written for parents; this is the same rule told to the learner. */
const MODE_HELP: Record<Schemas['LearnerWeek']['mode'], string> = {
  PREORDER_ONLY: 'At the tuckshop you can only collect what was ordered here.',
  APPROVED_ITEMS: 'At the counter you can also buy items your parent approved, up to your daily limit.',
  FREE_CHOICE: 'At the counter you can also buy what you like, up to your daily limit.',
};

const ORDER_STATUS: Record<PreOrderView['status'], { label: string; tone: 'blue' | 'warn' | 'ok' }> = {
  WAITING: { label: 'Ready to collect', tone: 'blue' },
  PARTIAL: { label: 'Partly collected', tone: 'warn' },
  COLLECTED: { label: 'Collected', tone: 'ok' },
};

function dayLabel(day: Day) {
  const date = new Date(`${day.day}T00:00:00`).toLocaleDateString('en-ZA', { weekday: 'long', day: 'numeric', month: 'short' });
  return day.today ? `Today · ${date}` : date;
}

/** Pre-order tuckshop food for the next school days, paid from the learner's own card. */
export function Tuckshop() {
  const week = useQuery({ queryKey: ['learner', 'tuckshop-week'], queryFn: () => ok(api.GET('/api/tuckshop/week')) });
  const menu = useQuery({ queryKey: ['tuckshop', 'menu'], queryFn: () => ok(api.GET('/api/tuckshop/menu')) });

  return (
    <Page title="Tuckshop" intro="Order ahead and collect at break. Orders are paid from your card.">
      <QueryView query={week}>
        {(weeks) => {
          const me = weeks[0];
          if (!me) return <Empty>The tuckshop isn't set up for you yet.</Empty>;
          return (
            <>
              <Alert tone="blue">
                On your card: <Money value={me.walletBalance} />. {MODE_HELP[me.mode]}
              </Alert>
              <div className="space-y-4">
                {me.days.map((d) => (
                  <DayCard key={d.day} learnerId={me.learnerId} day={d} menu={menu.data ?? []} />
                ))}
              </div>
            </>
          );
        }}
      </QueryView>
    </Page>
  );
}

function DayCard({ learnerId, day, menu }: { learnerId: number; day: Day; menu: MenuItem[] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const order = day.order ?? null;
  const canChange = !order || order.status === 'WAITING';

  const cancel = useMutation({
    mutationFn: () => ok(api.DELETE('/api/tuckshop/learners/{learnerId}/orders/{day}', { params: { path: { learnerId, day: day.day } } })),
    onSuccess: () => {
      toast(`Order cancelled. ${formatRand(order?.total)} is back on your card.`);
      queryClient.invalidateQueries({ queryKey: ['learner'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  return (
    <Card
      title={dayLabel(day)}
      actions={order && <Badge tone={ORDER_STATUS[order.status].tone}>{ORDER_STATUS[order.status].label}</Badge>}
      className={day.today ? 'border-blue' : undefined}
    >
      {editing ? (
        <OrderEditor learnerId={learnerId} day={day} menu={menu} onDone={() => setEditing(false)} />
      ) : (
        <>
          {order ? (
            <ul className="space-y-1">
              {order.lines.map((l) => (
                <li key={l.index} className="flex justify-between gap-2">
                  <span className={l.collected ? 'text-ink-3 line-through' : undefined}>
                    {l.quantity} × {l.name}
                  </span>
                  <Money value={l.unitPrice * l.quantity} />
                </li>
              ))}
              <li className="flex justify-between gap-2 border-t border-line pt-1 font-semibold">
                <span>Total{order.payFrom === 'PARENT' ? ' (paid by your parent)' : ''}</span>
                <Money value={order.total} />
              </li>
            </ul>
          ) : (
            <p className="text-ink-2">No order yet.</p>
          )}
          {canChange && (
            <div className="mt-4 flex flex-wrap gap-2">
              <Button variant={order ? 'secondary' : 'primary'} disabled={menu.length === 0} onClick={() => setEditing(true)}>
                {order ? 'Change order' : 'Order'}
              </Button>
              {order && (
                <Button variant="ghost" busy={cancel.isPending} onClick={() => cancel.mutate()}>
                  Cancel order
                </Button>
              )}
            </div>
          )}
        </>
      )}
    </Card>
  );
}

function OrderEditor({ learnerId, day, menu, onDone }: { learnerId: number; day: Day; menu: MenuItem[]; onDone: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  // Start from the existing order, matched to the menu by name.
  const [qty, setQty] = useState<Record<number, number>>(() => {
    const start: Record<number, number> = {};
    for (const l of day.order?.lines ?? []) {
      const m = menu.find((i) => i.name === l.name);
      if (m) start[m.id] = l.quantity;
    }
    return start;
  });
  const [repeatWeekly, setRepeatWeekly] = useState(day.order?.repeatWeekly ?? false);
  const items = Object.entries(qty).filter(([, q]) => q > 0).map(([id, q]) => ({ menuItemId: Number(id), quantity: q }));
  const total = items.reduce((sum, i) => sum + (menu.find((m) => m.id === i.menuItemId)?.price ?? 0) * i.quantity, 0);

  const place = useMutation({
    mutationFn: () => ok(api.PUT('/api/tuckshop/learners/{learnerId}/orders', { params: { path: { learnerId } }, body: { day: day.day, items, repeatWeekly } })),
    onSuccess: (saved) => {
      toast(`Ordered. ${formatRand(saved.total)} paid from your card.`);
      queryClient.invalidateQueries({ queryKey: ['learner'] });
      onDone();
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  const set = (id: number, q: number) => setQty({ ...qty, [id]: Math.max(0, Math.min(5, q)) });

  return (
    <div className="space-y-4">
      {day.order?.payFrom === 'PARENT' && <Alert tone="warn">Your parent paid for this order. If you change it, they get their money back and your card pays instead.</Alert>}
      <ul className="grid gap-2 sm:grid-cols-2">
        {menu.map((m) => {
          const q = qty[m.id] ?? 0;
          return (
            <li key={m.id} className="flex items-center justify-between gap-2 rounded-xl border border-line px-3 py-2">
              <div>
                <p className="font-semibold">{m.name}</p>
                <p className="text-sm text-ink-3">
                  {m.category} · <Money value={m.price} />
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button size="sm" variant="secondary" aria-label={`One less ${m.name}`} disabled={q === 0} onClick={() => set(m.id, q - 1)}>
                  −
                </Button>
                <span className="w-5 text-center font-bold tabular-nums">{q}</span>
                <Button size="sm" variant="secondary" aria-label={`One more ${m.name}`} disabled={q >= 5} onClick={() => set(m.id, q + 1)}>
                  +
                </Button>
              </div>
            </li>
          );
        })}
      </ul>
      <Checkbox label="Repeat every week" checked={repeatWeekly} onChange={(e) => setRepeatWeekly(e.target.checked)} />
      <div className="flex flex-wrap items-center gap-2">
        <Button busy={place.isPending} disabled={items.length === 0} onClick={() => place.mutate()}>
          Pay {formatRand(total)} from my card
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </div>
  );
}
