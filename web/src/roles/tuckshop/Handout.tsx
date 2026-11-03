import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ok, type PreOrderView } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Money, cx } from '../../components/ui';

export const ORDER_STATUS: Record<PreOrderView['status'], { label: string; tone: 'blue' | 'warn' | 'ok' }> = {
  WAITING: { label: 'Waiting', tone: 'blue' },
  PARTIAL: { label: 'Partly handed over', tone: 'warn' },
  COLLECTED: { label: 'Handed over', tone: 'ok' },
};

/**
 * One pre-order at the counter. Tap items to pick some, or hand over everything still waiting.
 * Pre-orders are already paid, so nothing is charged here.
 */
export function PreOrderHandout({ order, showName = true }: { order: PreOrderView; showName?: boolean }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [picked, setPicked] = useState<number[]>([]);
  const waiting = order.lines.filter((l) => !l.collected);

  const handout = useMutation({
    mutationFn: (lineIndexes: number[]) => ok(api.POST('/api/pos/pre-orders/{orderId}/handout', { params: { path: { orderId: order.id } }, body: { lineIndexes } })),
    onSuccess: (res) => {
      toast(res.message, res.ok ? 'ok' : 'bad');
      setPicked([]);
      queryClient.invalidateQueries({ queryKey: ['pos'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  const toggle = (i: number) => setPicked(picked.includes(i) ? picked.filter((p) => p !== i) : [...picked, i]);

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        {showName && <p className="text-xl font-extrabold">{order.learnerName}</p>}
        <div className="flex items-center gap-2">
          <Badge tone={ORDER_STATUS[order.status].tone}>{ORDER_STATUS[order.status].label}</Badge>
          <span className="text-sm text-ink-3">
            Paid · <Money value={order.total} />
          </span>
        </div>
      </div>
      <div className="grid gap-2 sm:grid-cols-2">
        {order.lines.map((l) => (
          <button
            key={l.index}
            type="button"
            disabled={l.collected || handout.isPending}
            aria-pressed={picked.includes(l.index)}
            onClick={() => toggle(l.index)}
            className={cx(
              'flex min-h-14 items-center justify-between gap-2 rounded-xl border-2 px-4 py-3 text-left text-lg font-semibold transition',
              l.collected ? 'border-line bg-surface-2 text-ink-3 line-through' : picked.includes(l.index) ? 'border-blue bg-blue-soft text-blue-ink' : 'border-line bg-surface hover:border-blue',
            )}
          >
            <span>
              {l.quantity} × {l.name}
            </span>
            {l.collected && <span className="text-sm no-underline">{l.collectedAt?.slice(0, 5)}</span>}
          </button>
        ))}
      </div>
      {waiting.length > 0 && (
        <div className="flex flex-wrap gap-2">
          <Button className="px-6 py-3 text-lg" busy={handout.isPending && handout.variables.length === 0} onClick={() => handout.mutate([])}>
            Hand over all ({waiting.length})
          </Button>
          {picked.length > 0 && picked.length < waiting.length && (
            <Button className="px-6 py-3 text-lg" variant="secondary" busy={handout.isPending && handout.variables.length > 0} onClick={() => handout.mutate(picked)}>
              Hand over picked ({picked.length})
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
