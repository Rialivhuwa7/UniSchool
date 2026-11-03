import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api } from '../../lib/api';
import { NoticeStatsTable, StatusBadge } from '../../components/staff/notices';
import { Badge, Card, Money, Page, Progress, QueryView, Stat, Stats, Table, formatDate } from '../../components/ui';

export function Dashboard() {
  const dashboard = useQuery({ queryKey: ['principal', 'dashboard'], queryFn: () => ok(api.GET('/api/principal/dashboard')) });

  return (
    <Page title="Good morning" intro="How the school is doing today.">
      <QueryView query={dashboard}>
        {(d) => (
          <>
            <Stats>
              <Stat label="Learners with a fee plan" value={`${d.feePlanPercent}%`} tone={d.feePlanPercent < 80 ? 'warn' : 'ok'} />
              <Stat label="Collected this year" value={<Money value={d.collected} />} />
              <Stat label="Notices read" value={`${d.noticeReadPercent}%`} tone={d.noticeReadPercent < 50 ? 'warn' : undefined} />
              <Stat label="Homework done" value={`${d.homeworkDonePercent}%`} />
            </Stats>

            <div className="grid gap-6 lg:grid-cols-2">
              <Card
                title={`Waiting for you (${d.waitingForApproval.length})`}
                actions={
                  d.waitingForApproval.length > 0 && (
                    <Link to="approvals" className="text-sm font-semibold text-blue-ink hover:underline">
                      Review
                    </Link>
                  )
                }
              >
                {d.waitingForApproval.length === 0 ? (
                  <p className="text-ink-2">Nothing to approve.</p>
                ) : (
                  <ul className="space-y-3">
                    {d.waitingForApproval.map((a) => (
                      <li key={`${a.type}-${a.id}`}>
                        <p className="font-semibold">{a.title}</p>
                        <p className="text-sm text-ink-3">
                          {a.type === 'NOTICE' ? 'Notice' : 'Homework'} · {a.from} · {a.audience}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>

              <Card title="Needs attention">
                {d.attention.length === 0 ? (
                  <p className="text-ink-2">All clear.</p>
                ) : (
                  <ul className="space-y-2">
                    {d.attention.map((a, i) => (
                      <li key={i} className="flex items-start gap-2">
                        <Badge tone={a.level === 'follow-up' ? 'warn' : 'neutral'}>{a.area}</Badge>
                        <span>{a.message}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>
            </div>

            <Card title="Tuckshop today">
              <Stats>
                <Stat label="Pre-orders" value={d.tuckshopPreOrdersToday} />
                <Stat label="Collected" value={d.tuckshopCollectedToday} />
                <Stat label="Sales" value={<Money value={d.tuckshopSalesToday} />} />
              </Stats>
            </Card>

            <Card title="Homework">
              <Table
                rows={d.homework}
                rowKey={(h) => h.id}
                columns={[
                  {
                    header: 'Homework',
                    cell: (h) => (
                      <div>
                        <p className="font-semibold">{h.title}</p>
                        <p className="text-ink-3">
                          {h.subject} · {h.grade}, {h.school} · {h.by}
                        </p>
                      </div>
                    ),
                  },
                  { header: 'Status', cell: (h) => <StatusBadge status={h.status} /> },
                  { header: 'Due', cell: (h) => formatDate(h.dueDate) },
                  {
                    header: 'Done',
                    className: 'w-40',
                    cell: (h) => (
                      <div className="space-y-1">
                        <Progress value={h.percentDone} />
                        <p className="text-ink-3">
                          {h.done} of {h.total}
                        </p>
                      </div>
                    ),
                  },
                ]}
              />
            </Card>

            <Card title="Notices">
              <NoticeStatsTable rows={d.notices} />
            </Card>
          </>
        )}
      </QueryView>
    </Page>
  );
}
