import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api } from '../../lib/api';
import { Card, Checkbox, Empty, Input, Money, Page, QueryView, Stat, Stats, formatDate } from '../../components/ui';
import { PreOrderHandout } from './Handout';

/** Today's pre-orders at the counter, with a running total of sales. */
export function Today() {
  const today = useQuery({ queryKey: ['pos', 'today'], queryFn: () => ok(api.GET('/api/pos/today')), refetchInterval: 30_000 });
  const [filter, setFilter] = useState('');
  const [waitingOnly, setWaitingOnly] = useState(true);

  return (
    <QueryView query={today}>
      {(t) => {
        const orders = (t.orders ?? [])
          .filter((o) => !waitingOnly || o.status !== 'COLLECTED')
          .filter((o) => o.learnerName.toLowerCase().includes(filter.trim().toLowerCase()));
        return (
          <Page title="Pre-orders" intro={`For ${formatDate(t.serviceDay)}. These are already paid: just hand them over.`}>
            <Stats>
              <Stat label="Pre-orders" value={t.preOrders} />
              <Stat label="Handed over in full" value={t.collectedInFull} tone={t.collectedInFull === t.preOrders && t.preOrders > 0 ? 'ok' : undefined} />
              <Stat label="Still to hand over" value={t.preOrders - t.collectedInFull} />
              <Stat label="Sales today" value={<Money value={t.salesToday} />} hint={`${t.tillSalesToday} at the till`} />
            </Stats>
            <div className="flex flex-wrap items-center gap-4">
              <Input className="max-w-sm py-3 text-lg" placeholder="Find a name" aria-label="Find a name" value={filter} onChange={(e) => setFilter(e.target.value)} />
              <Checkbox label="Hide handed over" checked={waitingOnly} onChange={(e) => setWaitingOnly(e.target.checked)} />
            </div>
            {orders.length === 0 ? (
              <Empty>{t.preOrders === 0 ? 'No pre-orders today.' : filter.trim() ? 'No waiting pre-order for that name.' : 'Everything has been handed over.'}</Empty>
            ) : (
              <div className="grid gap-4 lg:grid-cols-2">
                {orders.map((o) => (
                  <Card key={o.id}>
                    <PreOrderHandout order={o} />
                  </Card>
                ))}
              </div>
            )}
          </Page>
        );
      }}
    </QueryView>
  );
}
