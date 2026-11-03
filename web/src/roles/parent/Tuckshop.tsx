import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ok, TUCK_MODE, type PreOrderView, type Schemas } from '@unischool/api';
import { api } from '../../lib/api';
import { Badge, Button, Card, Checkbox, Empty, Field, Money, Page, QueryView, Select, cx } from '../../components/ui';
import { dayAndDate, useAction, useMenu } from './shared';

type Week = Schemas['LearnerWeek'];
type Editing = { learner: Week; day: string; order: PreOrderView | null };

const STATUS: Record<PreOrderView['status'], { label: string; tone: 'blue' | 'warn' | 'ok' }> = {
  WAITING: { label: 'Waiting', tone: 'blue' },
  PARTIAL: { label: 'Partly collected', tone: 'warn' },
  COLLECTED: { label: 'Collected', tone: 'ok' },
};

/** Pre-order tuckshop meals for the coming school days, per child. */
export function Tuckshop() {
  const week = useQuery({ queryKey: ['parent', 'tuckshop', 'week'], queryFn: () => ok(api.GET('/api/tuckshop/week')) });
  const [editing, setEditing] = useState<Editing | null>(null);
  useMenu(); // load early so the editor can match an order's lines to menu items

  return (
    <Page title="Tuckshop" intro="Order lunch ahead for the next few school days. Your child collects it at break.">
      <QueryView query={week}>
        {(weeks) =>
          weeks.length === 0 ? (
            <Empty>No children to order for.</Empty>
          ) : (
            weeks.map((w) => (
              <Card
                key={w.learnerId}
                title={w.name}
                actions={
                  <div className="flex flex-wrap items-center gap-2 text-sm text-ink-2">
                    <Badge tone="blue">{TUCK_MODE[w.mode].label}</Badge>
                    <span>
                      Wallet <Money value={w.walletBalance} />
                    </span>
                  </div>
                }
              >
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
                  {w.days.map((d) => (
                    <DayCell
                      key={d.day}
                      learner={w}
                      day={d}
                      selected={editing?.learner.learnerId === w.learnerId && editing.day === d.day}
                      onEdit={() => setEditing({ learner: w, day: d.day, order: d.order ?? null })}
                    />
                  ))}
                </div>
                {editing?.learner.learnerId === w.learnerId && (
                  <OrderEditor key={editing.day} editing={editing} onDone={() => setEditing(null)} />
                )}
              </Card>
            ))
          )
        }
      </QueryView>
    </Page>
  );
}

function DayCell({ learner, day, selected, onEdit }: { learner: Week; day: Schemas['DayView']; selected: boolean; onEdit: () => void }) {
  const order = day.order;
  const cancel = useAction(
    () => ok(api.DELETE('/api/tuckshop/learners/{learnerId}/orders/{day}', { params: { path: { learnerId: learner.learnerId, day: day.day } } })),
    `${learner.name}'s order for ${dayAndDate(day.day)} is cancelled.`,
  );
  const canChange = !order || order.status === 'WAITING';

  return (
    <div className={cx('flex flex-col rounded-xl border p-3', selected ? 'border-blue bg-blue-soft' : 'border-line')}>
      <p className="font-semibold">
        {dayAndDate(day.day)} {day.today && <Badge tone="blue">Today</Badge>}
      </p>
      {order ? (
        <div className="mt-2 flex-1 space-y-1 text-sm">
          <ul>
            {order.lines.map((l) => (
              <li key={l.index} className={cx(l.collected && 'text-ink-3 line-through')}>
                {l.quantity > 1 && `${l.quantity} × `}
                {l.name}
              </li>
            ))}
          </ul>
          <p className="font-semibold">
            <Money value={order.total} />
          </p>
          <p className="text-ink-3">{order.payFrom === 'PARENT' ? 'Paid from your wallet' : `From ${learner.name}'s wallet`}</p>
          <div className="flex flex-wrap gap-1">
            <Badge tone={STATUS[order.status].tone}>{STATUS[order.status].label}</Badge>
            {order.repeatWeekly && <Badge>Every week</Badge>}
          </div>
        </div>
      ) : (
        <p className="mt-2 flex-1 text-sm text-ink-3">No order</p>
      )}
      {canChange && (
        <div className="mt-3 flex flex-wrap gap-2">
          <Button size="sm" variant={order ? 'secondary' : 'primary'} onClick={onEdit}>
            {order ? 'Change' : 'Order'}
          </Button>
          {order && (
            <Button
              size="sm"
              variant="ghost"
              busy={cancel.isPending}
              onClick={() => window.confirm(`Cancel ${learner.name}'s order? The money goes back where it came from.`) && cancel.mutate(undefined)}
            >
              Cancel
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

function OrderEditor({ editing, onDone }: { editing: Editing; onDone: () => void }) {
  const menu = useMenu();
  const { learner, day, order } = editing;
  // Start from the existing order: its lines carry names, the request needs menu ids.
  const [qty, setQty] = useState<Record<number, number>>(() => {
    const start: Record<number, number> = {};
    for (const line of order?.lines ?? []) {
      const item = menu.data?.find((m) => m.name === line.name);
      if (item) start[item.id] = line.quantity;
    }
    return start;
  });
  const [payFrom, setPayFrom] = useState<'PARENT' | 'LEARNER'>(order?.payFrom ?? 'PARENT');
  const [repeatWeekly, setRepeatWeekly] = useState(order?.repeatWeekly ?? false);

  const items = Object.entries(qty)
    .filter(([, q]) => q > 0)
    .map(([id, quantity]) => ({ menuItemId: Number(id), quantity }));
  const total = items.reduce((sum, i) => sum + (menu.data?.find((m) => m.id === i.menuItemId)?.price ?? 0) * i.quantity, 0);

  const place = useAction(
    () => ok(api.PUT('/api/tuckshop/learners/{learnerId}/orders', { params: { path: { learnerId: learner.learnerId } }, body: { day, items, payFrom, repeatWeekly } })),
    `${learner.name}'s order for ${dayAndDate(day)} is in.`,
  );

  const set = (id: number, q: number) => setQty((all) => ({ ...all, [id]: Math.max(0, Math.min(5, q)) }));

  return (
    <div className="mt-4 space-y-4 rounded-xl border border-line p-4">
      <h3 className="font-bold">
        {order ? 'Change' : 'New'} order for {learner.name}, {dayAndDate(day)}
      </h3>
      <QueryView query={menu}>
        {(items) => (
          <div className="grid gap-2 sm:grid-cols-2 md:grid-cols-3">
            {items.map((m) => (
              <div key={m.id} className="flex items-center justify-between gap-2 rounded-lg bg-surface-2 px-3 py-2">
                <span className="text-sm">
                  {m.name}
                  <span className="block text-ink-3">
                    <Money value={m.price} /> · {m.category}
                  </span>
                </span>
                <span className="flex items-center gap-1">
                  <Button size="sm" variant="secondary" aria-label={`One less ${m.name}`} disabled={!qty[m.id]} onClick={() => set(m.id, (qty[m.id] ?? 0) - 1)}>
                    −
                  </Button>
                  <span className="w-5 text-center tabular-nums">{qty[m.id] ?? 0}</span>
                  <Button size="sm" variant="secondary" aria-label={`One more ${m.name}`} disabled={(qty[m.id] ?? 0) >= 5} onClick={() => set(m.id, (qty[m.id] ?? 0) + 1)}>
                    +
                  </Button>
                </span>
              </div>
            ))}
          </div>
        )}
      </QueryView>
      <div className="flex flex-wrap items-end gap-4">
        <Field label="Pay from">
          {(id) => (
            <Select id={id} value={payFrom} onChange={(e) => setPayFrom(e.target.value as 'PARENT' | 'LEARNER')} className="max-w-60">
              <option value="PARENT">Your wallet</option>
              <option value="LEARNER">{learner.name}'s wallet</option>
            </Select>
          )}
        </Field>
        <div className="pb-2">
          <Checkbox label="Repeat every week" checked={repeatWeekly} onChange={(e) => setRepeatWeekly(e.target.checked)} />
        </div>
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <Button busy={place.isPending} disabled={items.length === 0} onClick={() => place.mutate(undefined, { onSuccess: onDone })}>
          {order ? 'Save order' : 'Place order'} · <Money value={total} />
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Close
        </Button>
      </div>
    </div>
  );
}
