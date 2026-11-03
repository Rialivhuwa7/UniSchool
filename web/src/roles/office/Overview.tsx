import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api } from '../../lib/api';
import { Card, Empty, Money, Page, QueryView, Stat, Stats, formatDateTime } from '../../components/ui';
import { CollectionsTable } from './shared';

/** The office's home: money in, fee plans, collections and the latest payments. */
export function Overview() {
  const overview = useQuery({ queryKey: ['office', 'overview'], queryFn: () => ok(api.GET('/api/school/overview')) });
  const learners = useQuery({ queryKey: ['office', 'learners', '', '', ''], queryFn: () => ok(api.GET('/api/school/learners')) });

  return (
    <Page title="School office" intro="Money coming in and what still needs following up.">
      {learners.data?.length === 0 && <SetupSteps />}
      <QueryView query={overview}>
        {(o) => (
          <>
            <Stats>
              <Stat label="Collected this year" value={<Money value={o.collected} />} />
              <Stat
                label="Families with a fee plan"
                value={`${o.feePlanPercent}%`}
                hint={o.learnersTotal === 0 ? 'No fees set for next year yet' : `${o.learnersWithFeePlan} of ${o.learnersTotal} learners`}
                tone={o.learnersTotal === 0 ? undefined : o.feePlanPercent < 80 ? 'warn' : 'ok'}
              />
              <Stat label="UniSchool give-back this year" value={<Money value={o.giveBackThisYear} />} />
              <Stat label="Collections" value={(o.openCollections ?? []).length} />
            </Stats>

            <Card
              title="Collections"
              actions={
                <Link to="collections" className="text-sm font-semibold text-blue-ink hover:underline">
                  Manage
                </Link>
              }
            >
              <CollectionsTable rows={o.openCollections ?? []} compact />
            </Card>

            <Card title="Latest payments">
              {(o.latestPayments ?? []).length === 0 ? (
                <Empty>No payments yet. Payments for fees, collections and the shop show here as parents make them.</Empty>
              ) : (
                <ul className="divide-y divide-line">
                  {o.latestPayments.map((p, i) => (
                    <li key={i} className="flex items-start justify-between gap-4 py-3 first:pt-0 last:pb-0">
                      <div>
                        <p className="font-semibold">{p.description}</p>
                        <p className="text-sm text-ink-3">
                          {p.payer ? `${p.payer} · ` : ''}
                          {formatDateTime(p.at)}
                        </p>
                      </div>
                      <Money value={p.amount} className="font-semibold text-ok" />
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </>
        )}
      </QueryView>
    </Page>
  );
}

const STEPS = [
  { to: 'learners', label: 'Enrol your learners', hint: 'Parents ask for access with their child’s student number.' },
  { to: 'access', label: 'Approve parents and staff', hint: 'Check who people are before they see learners’ information.' },
  { to: 'fees', label: 'Set next year’s fees', hint: 'One annual fee per grade. Families then choose how to pay.' },
  { to: 'uniform', label: 'Stock the uniform shop', hint: 'Items, prices, sizes in stock and sport kit.' },
  { to: 'lists', label: 'Upload stationery lists', hint: 'Parents order their child’s grade list in the shop.' },
  { to: 'groups', label: 'Start parent groups', hint: 'Class and team chats for parents.' },
];

/** Shown while no learners are enrolled: the order to set a new school up in. */
function SetupSteps() {
  return (
    <Card title="Setting up your school">
      <ol className="space-y-3">
        {STEPS.map((s, i) => (
          <li key={s.to} className="flex gap-3">
            <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-blue-soft text-sm font-bold text-blue-ink">{i + 1}</span>
            <div>
              <Link to={s.to} className="font-semibold text-blue-ink hover:underline">
                {s.label}
              </Link>
              <p className="text-sm text-ink-3">{s.hint}</p>
            </div>
          </li>
        ))}
      </ol>
    </Card>
  );
}
