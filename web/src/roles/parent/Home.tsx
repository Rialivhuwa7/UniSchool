import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { leftToday, ok, TUCK_MODE } from '@unischool/api';
import { api } from '../../lib/api';
import { Badge, Card, Money, Page, Progress, QueryView, Stat, Stats } from '../../components/ui';
import { useLearners } from './shared';

/** Where each back-to-school checklist item is sorted out. */
const AREA_LINK: Record<string, string> = {
  fees: 'payments',
  'supply-list': 'shop?category=SUPPLY_LIST',
  uniform: 'shop?category=UNIFORM',
  sport: 'shop?category=SPORT',
  savings: 'savings',
};

export function Home() {
  const home = useQuery({ queryKey: ['parent', 'home'], queryFn: () => ok(api.GET('/api/home')) });
  const learners = useLearners();
  const name = home.data?.greetingName;

  return (
    <Page title={name ? `Hello, ${name}` : 'Hello'} intro="Here's what's happening with your family today.">
      <QueryView query={home}>
        {(h) => (
          <>
            <Stats>
              <Stat label="Family wallet" value={<Money value={h.walletBalance} />} hint={<Link to="wallet" className="text-blue-ink hover:underline">Add money</Link>} />
              <Stat
                label="Unread notices"
                value={h.unreadNotices}
                tone={h.unreadNotices > 0 ? 'warn' : undefined}
                hint={<Link to="notices" className="text-blue-ink hover:underline">Read them</Link>}
              />
              <Stat label="Ready for next year" value={`${h.readyCount} of ${h.readiness.length}`} tone={h.readyCount === h.readiness.length ? 'ok' : undefined} />
            </Stats>

            <Card title="Back to school checklist">
              <div className="mb-4">
                <Progress value={h.readiness.length ? (h.readyCount * 100) / h.readiness.length : 0} tone="ok" />
              </div>
              <ul className="divide-y divide-line">
                {h.readiness.map((r) => (
                  <li key={`${r.area}-${r.learnerId ?? ''}-${r.label}`} className="flex flex-wrap items-center justify-between gap-2 py-2.5">
                    <span className="flex items-center gap-2">
                      <Badge tone={r.done ? 'ok' : 'neutral'}>{r.done ? 'Done' : 'To do'}</Badge>
                      {r.label}
                    </span>
                    {!r.done && AREA_LINK[r.area] && (
                      <Link
                        to={AREA_LINK[r.area] + (r.learnerId && r.area === 'supply-list' ? `&learner=${r.learnerId}` : '')}
                        className="text-sm font-semibold text-blue-ink hover:underline"
                      >
                        Sort it out
                      </Link>
                    )}
                  </li>
                ))}
              </ul>
            </Card>
          </>
        )}
      </QueryView>

      <QueryView query={learners}>
        {(kids) => (
          <div className="grid gap-4 md:grid-cols-2">
            {kids.map((l) => (
              <Card
                key={l.id}
                title={`${l.firstName} ${l.lastName}`}
                actions={
                  <Link to={`children/${l.id}`} className="text-sm font-semibold text-blue-ink hover:underline">
                    Card settings
                  </Link>
                }
              >
                <p className="-mt-3 mb-4 text-sm text-ink-3">
                  {l.grade} · {l.school}
                </p>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <p className="text-ink-2">Wallet</p>
                    <p className="text-lg font-bold">
                      <Money value={l.walletBalance} />
                    </p>
                  </div>
                  <div>
                    <p className="text-ink-2">Left to spend today</p>
                    <p className="text-lg font-bold">
                      <Money value={leftToday(l)} />
                    </p>
                  </div>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Badge tone="blue">{TUCK_MODE[l.tuckMode].label}</Badge>
                  {l.frozen && <Badge tone="bad">Card frozen</Badge>}
                </div>
              </Card>
            ))}
          </div>
        )}
      </QueryView>
    </Page>
  );
}
