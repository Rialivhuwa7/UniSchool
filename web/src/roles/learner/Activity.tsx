import { useQuery } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api } from '../../lib/api';
import { useMe } from '../../lib/auth';
import { Card, Empty, Money, Page, QueryView, Table, cx, formatDateTime } from '../../components/ui';

/** Money in and out of the learner's card: tuckshop, pocket money, savings. */
export function Activity() {
  const learnerId = useMe().activeRole.learnerId;
  const activity = useQuery({
    queryKey: ['learner', 'activity', learnerId],
    queryFn: () => ok(api.GET('/api/learners/{id}/activity', { params: { path: { id: learnerId } } })),
    enabled: learnerId != null,
  });

  return (
    <Page title="My card activity" intro="Your last 30 card payments and top-ups.">
      {learnerId == null ? (
        <Empty>Your account isn't linked to a learner card yet.</Empty>
      ) : (
        <Card>
          <QueryView query={activity}>
            {(rows) => (
              <Table
                rows={rows}
                rowKey={(t) => t.id}
                empty="Nothing on your card yet."
                columns={[
                  { header: 'When', className: 'whitespace-nowrap text-ink-2', cell: (t) => formatDateTime(t.at) },
                  { header: 'What', cell: (t) => t.description },
                  {
                    header: 'Amount',
                    className: 'text-right',
                    cell: (t) => (t.amount === 0 ? <span className="text-ink-3">—</span> : <Money value={t.amount} className={cx('font-semibold', t.amount > 0 && 'text-ok')} />),
                  },
                ]}
              />
            )}
          </QueryView>
        </Card>
      )}
    </Page>
  );
}
