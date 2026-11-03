import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { leftToday, ok, TUCK_MODE, type LearnerView, type TuckMode, type TxView } from '@unischool/api';
import { api } from '../../lib/api';
import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Empty,
  Field,
  Input,
  Money,
  Page,
  Progress,
  QueryView,
  Stat,
  Stats,
  Table,
  cx,
  formatDateTime,
} from '../../components/ui';
import { parseAmount, useAction, useLearners, useMenu } from './shared';
import { ChildSignIn } from './ChildSignIn';

/** Overview of each child, linking to their card settings. */
export function Children() {
  const learners = useLearners();
  return (
    <Page title="Children" intro="Set how each child can spend at school.">
      <QueryView query={learners}>
        {(kids) =>
          kids.length === 0 ? (
            <Empty>No children are linked to your account yet.</Empty>
          ) : (
            <div className="grid gap-4 md:grid-cols-2">
              {kids.map((l) => (
                <Link key={l.id} to={String(l.id)} className="block rounded-2xl border border-line bg-surface p-5 hover:border-blue">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h2 className="text-lg font-bold">
                        {l.firstName} {l.lastName}
                      </h2>
                      <p className="text-sm text-ink-3">
                        {l.grade} · {l.school} · {l.house} house
                      </p>
                    </div>
                    {l.frozen && <Badge tone="bad">Frozen</Badge>}
                  </div>
                  <p className="mt-3 text-sm text-ink-2">
                    <Money value={l.walletBalance} /> in wallet · <Money value={leftToday(l)} /> of <Money value={l.dailyLimit} /> left today
                  </p>
                  <p className="mt-1 text-sm text-ink-2">Tuckshop: {TUCK_MODE[l.tuckMode].label}</p>
                </Link>
              ))}
            </div>
          )
        }
      </QueryView>
    </Page>
  );
}

/** One child: balance, savings goal, card controls and recent activity. */
export function ChildDetail() {
  const id = Number(useParams().id);
  const learner = useQuery({ queryKey: ['parent', 'learner', id], queryFn: () => ok(api.GET('/api/learners/{id}', { params: { path: { id } } })) });
  const activity = useQuery({ queryKey: ['parent', 'activity', id], queryFn: () => ok(api.GET('/api/learners/{id}/activity', { params: { path: { id } } })) });
  const name = learner.data?.firstName;

  return (
    <Page
      title={name ?? 'Child'}
      intro={learner.data && `${learner.data.grade} · ${learner.data.school}`}
      actions={
        <Link to=".." relative="path" className="text-sm font-semibold text-blue-ink hover:underline">
          All children
        </Link>
      }
    >
      <QueryView query={learner}>
        {(l) => (
          <>
            <Stats>
              <Stat label="Wallet" value={<Money value={l.walletBalance} />} hint={<Link to="../../wallet" relative="path" className="text-blue-ink hover:underline">Send money</Link>} />
              <Stat label="Spent today" value={<Money value={l.spentToday} />} />
              <Stat label="Left today" value={<Money value={leftToday(l)} />} />
              {l.goal && (
                <div className="space-y-2 rounded-2xl border border-line bg-surface p-4">
                  <p className="text-sm text-ink-2">Saving for: {l.goal.name}</p>
                  <p className="text-2xl font-extrabold">
                    <Money value={l.goal.saved} />
                  </p>
                  <Progress value={l.goal.percent} tone="ok" />
                  <p className="text-sm text-ink-3">
                    {l.goal.percent}% of <Money value={l.goal.target} />
                  </p>
                </div>
              )}
            </Stats>
            <Controls key={l.id} learner={l} />
            <ChildSignIn learner={l} />
            {Object.keys(l.sizes ?? {}).length > 0 && (
              <Card title="Sizes and sport">
                <div className="flex flex-wrap gap-2">
                  {Object.entries(l.sizes).map(([item, size]) => (
                    <Badge key={item}>
                      {item}: {size}
                    </Badge>
                  ))}
                  {l.sports.map((s) => (
                    <Badge key={s} tone="blue">
                      {s}
                    </Badge>
                  ))}
                </div>
              </Card>
            )}
          </>
        )}
      </QueryView>

      <Card title="Recent activity">
        <QueryView query={activity}>{(rows) => <ActivityTable rows={rows} empty={`No spending or top-ups for ${name ?? 'this child'} yet.`} />}</QueryView>
      </Card>
    </Page>
  );
}

export function ActivityTable({ rows, empty, showWho }: { rows: TxView[]; empty: string; showWho?: boolean }) {
  return (
    <Table
      rows={rows}
      rowKey={(t) => t.id}
      empty={empty}
      columns={[
        { header: 'When', cell: (t) => <span className="whitespace-nowrap text-ink-2">{formatDateTime(t.at)}</span> },
        ...(showWho ? [{ header: 'Who', cell: (t: TxView) => t.who }] : []),
        { header: 'What', cell: (t) => t.description },
        {
          header: 'Amount',
          className: 'text-right',
          cell: (t) => <Money value={t.amount} className={cx('font-semibold', t.amount > 0 && 'text-ok')} />,
        },
      ]}
    />
  );
}

const MODES: TuckMode[] = ['PREORDER_ONLY', 'APPROVED_ITEMS', 'FREE_CHOICE'];

function Controls({ learner }: { learner: LearnerView }) {
  const queryClient = useQueryClient();
  const menu = useMenu();
  const [dailyLimit, setDailyLimit] = useState(String(learner.dailyLimit));
  const [frozen, setFrozen] = useState(learner.frozen);
  const [tuckMode, setTuckMode] = useState<TuckMode>(learner.tuckMode);
  const [blocked, setBlocked] = useState<string[]>(learner.blockedCategories ?? []);
  const [allowed, setAllowed] = useState<string[]>(learner.allowedItems ?? []);

  const limit = parseAmount(dailyLimit);
  const limitOk = limit !== null && limit >= 0 && limit <= 1000 && Math.round(limit * 100) === limit * 100;

  const save = useAction(
    () =>
      ok(
        api.PATCH('/api/learners/{id}/controls', {
          params: { path: { id: learner.id } },
          body: { dailyLimit: limit ?? 0, frozen, tuckMode, blockedCategories: blocked, allowedItems: allowed },
        }),
      ),
    (l) => {
      queryClient.setQueryData(['parent', 'learner', l.id], l);
      return `${l.firstName}'s card settings are saved.`;
    },
  );

  const toggle = (list: string[], set: (v: string[]) => void, value: string) =>
    set(list.includes(value) ? list.filter((v) => v !== value) : [...list, value]);
  const categories = [...new Set((menu.data ?? []).map((m) => m.category))];

  return (
    <Card title="Card settings">
      <div className="space-y-5">
        {frozen && <Alert tone="warn">{learner.firstName}'s card is frozen. Nothing can be bought with it until you unfreeze it.</Alert>}
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Daily spending limit (R)" hint="What they can spend at school each day, from R0 to R1 000." error={dailyLimit && !limitOk ? 'Enter an amount from R0 to R1 000.' : undefined}>
            {(id) => <Input id={id} type="number" min={0} max={1000} step="0.01" inputMode="decimal" value={dailyLimit} onChange={(e) => setDailyLimit(e.target.value)} />}
          </Field>
          <div className="flex items-end pb-2">
            <Checkbox label="Freeze the card (lost or misused)" checked={frozen} onChange={(e) => setFrozen(e.target.checked)} />
          </div>
        </div>

        <fieldset className="space-y-2">
          <legend className="mb-1 text-sm font-semibold">At the tuckshop</legend>
          {MODES.map((m) => (
            <label key={m} className={cx('flex cursor-pointer gap-3 rounded-xl border p-3', tuckMode === m ? 'border-blue bg-blue-soft' : 'border-line')}>
              <input type="radio" name={`mode-${learner.id}`} className="mt-1 accent-blue" checked={tuckMode === m} onChange={() => setTuckMode(m)} />
              <span>
                <span className="block font-semibold">{TUCK_MODE[m].label}</span>
                <span className="text-sm text-ink-2">{TUCK_MODE[m].help}</span>
              </span>
            </label>
          ))}
        </fieldset>

        {tuckMode === 'APPROVED_ITEMS' && (
          <fieldset>
            <legend className="mb-2 text-sm font-semibold">Items {learner.firstName} may buy</legend>
            <QueryView query={menu}>
              {(items) => (
                <div className="grid gap-2 sm:grid-cols-2 md:grid-cols-3">
                  {items.map((i) => (
                    <Checkbox
                      key={i.id}
                      label={
                        <>
                          {i.name} <span className="text-ink-3">· <Money value={i.price} /></span>
                        </>
                      }
                      checked={allowed.includes(i.name)}
                      onChange={() => toggle(allowed, setAllowed, i.name)}
                    />
                  ))}
                </div>
              )}
            </QueryView>
          </fieldset>
        )}

        {tuckMode === 'FREE_CHOICE' && (
          <fieldset>
            <legend className="mb-2 text-sm font-semibold">Never allow</legend>
            <div className="flex flex-wrap gap-x-6 gap-y-2">
              {categories.map((c) => (
                <Checkbox key={c} label={c} checked={blocked.includes(c)} onChange={() => toggle(blocked, setBlocked, c)} />
              ))}
            </div>
          </fieldset>
        )}

        <Button busy={save.isPending} disabled={!limitOk} onClick={() => save.mutate(undefined)}>
          Save settings
        </Button>
      </div>
    </Card>
  );
}
