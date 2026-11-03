import { useQuery } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api } from '../../lib/api';
import { Badge, Card, Money, Page, QueryView, Stat, Stats, Table, formatDate } from '../../components/ui';

/** Money paid over to the school's bank account in the last 60 days, by day and source. */
export function Payouts() {
  const payouts = useQuery({ queryKey: ['office', 'payouts'], queryFn: () => ok(api.GET('/api/school/payouts')) });

  return (
    <Page title="Payouts" intro="What UniSchool pays into the school's bank account. Last 60 days.">
      <QueryView query={payouts}>
        {(rows) => {
          const pending = rows.filter((p) => p.status !== 'Settled').reduce((sum, p) => sum + p.amount, 0);
          const settled = rows.filter((p) => p.status === 'Settled').reduce((sum, p) => sum + p.amount, 0);
          return (
            <>
              <Stats>
                <Stat label="Paid out" value={<Money value={settled} />} tone="ok" />
                <Stat label="On its way" value={<Money value={pending} />} hint="Settles the next working day" />
              </Stats>
              <Card>
                <Table
                  rows={rows}
                  rowKey={(p) => `${p.date}-${p.source}`}
                  empty="No payouts in the last 60 days."
                  columns={[
                    { header: 'Date', cell: (p) => formatDate(p.date) },
                    { header: 'From', cell: (p) => p.source },
                    { header: 'Status', cell: (p) => <Badge tone={p.status === 'Settled' ? 'ok' : 'warn'}>{p.status}</Badge> },
                    { header: 'Amount', className: 'text-right', cell: (p) => <Money value={p.amount} className="font-semibold" /> },
                  ]}
                />
              </Card>
            </>
          );
        }}
      </QueryView>
    </Page>
  );
}
