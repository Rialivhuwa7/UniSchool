import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { formatRand, ok, TUCK_MODE, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { Alert, Badge, Button, Card, Empty, ErrorAlert, Input, Money, Page, QueryView, Spinner, cx } from '../../components/ui';
import { PreOrderHandout } from './Handout';

type TillLearner = Schemas['TillLearnerView'];
type MenuItem = Schemas['MenuItemView'];

const CARD_INPUT_ID = 'till-card';

/**
 * The counter: tap a card (the reader types the card number into the box) or search by name,
 * pick items, charge. The server checks the parent's rules and has the final say; a decline
 * charges nothing and comes back with a message to read out.
 */
export function Till() {
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [cart, setCart] = useState<number[]>([]);
  const [result, setResult] = useState<Schemas['ChargeResult'] | null>(null);

  const learner = useQuery({
    queryKey: ['pos', 'learner', selectedId],
    queryFn: () => ok(api.GET('/api/pos/learners/{id}', { params: { path: { id: selectedId! } } })),
    enabled: selectedId != null,
  });
  const menu = useQuery({ queryKey: ['tuckshop', 'menu'], queryFn: () => ok(api.GET('/api/tuckshop/menu')) });

  function choose(l: TillLearner) {
    queryClient.setQueryData(['pos', 'learner', l.id], l);
    setSelectedId(l.id);
    setCart([]);
    setResult(null);
  }

  function next() {
    setSelectedId(null);
    setCart([]);
    setResult(null);
    // Ready for the next card tap.
    document.getElementById(CARD_INPUT_ID)?.focus();
  }

  const items = menu.data ?? [];
  const byId = new Map(items.map((m) => [m.id, m]));
  const total = cart.reduce((sum, id) => sum + (byId.get(id)?.price ?? 0), 0);

  const charge = useMutation({
    mutationFn: () => ok(api.POST('/api/pos/charge', { body: { learnerId: selectedId!, menuItemIds: cart } })),
    onSuccess: (res) => {
      setResult(res);
      if (res.approved) setCart([]);
      queryClient.invalidateQueries({ queryKey: ['pos'] });
    },
  });

  function changeCart(ids: number[]) {
    setCart(ids);
    setResult(null);
    charge.reset();
  }

  return (
    <Page title="Till" intro="Tap a card or find a learner, pick the items, then charge.">
      <div className="grid gap-6 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
        <div className="space-y-6">
          <FindLearner onFound={choose} />
          {selectedId != null && (
            <Card>
              <QueryView query={learner}>{(l) => <LearnerPanel learner={l} onClear={next} />}</QueryView>
            </Card>
          )}
        </div>

        <div className="space-y-6">
          <Card title="Menu">
            <QueryView query={menu}>
              {(list) =>
                list.length === 0 ? (
                  <Empty>Nothing on the menu.</Empty>
                ) : (
                  <div className="grid grid-cols-2 gap-3 xl:grid-cols-3">
                    {list.map((m) => (
                      <MenuButton key={m.id} item={m} count={cart.filter((id) => id === m.id).length} onAdd={() => changeCart([...cart, m.id])} />
                    ))}
                  </div>
                )
              }
            </QueryView>
          </Card>

          <Card title="Sale">
            {cart.length === 0 ? (
              <p className="text-ink-2">Tap menu items to add them.</p>
            ) : (
              <ul className="space-y-2">
                {[...new Set(cart)].map((id) => {
                  const m = byId.get(id);
                  const n = cart.filter((c) => c === id).length;
                  return (
                    <li key={id} className="flex items-center justify-between gap-3 text-lg">
                      <span>
                        {n} × {m?.name}
                      </span>
                      <span className="flex items-center gap-3">
                        <Money value={(m?.price ?? 0) * n} />
                        <Button size="sm" variant="secondary" aria-label={`Remove one ${m?.name}`} onClick={() => changeCart(removeOne(cart, id))}>
                          −
                        </Button>
                      </span>
                    </li>
                  );
                })}
              </ul>
            )}
            <div className="mt-4 flex items-center justify-between border-t border-line pt-4">
              <span className="text-xl font-bold">Total</span>
              <Money value={total} className="text-3xl font-extrabold" />
            </div>
            {learner.data && cart.length > 0 && !result && <Warning learner={learner.data} total={total} />}
            <ErrorAlert error={charge.error} />
            <div className="mt-4 flex flex-wrap gap-3">
              <Button
                className="flex-1 py-4 text-xl"
                busy={charge.isPending}
                disabled={selectedId == null || cart.length === 0}
                onClick={() => charge.mutate()}
              >
                {selectedId == null ? 'Find a learner first' : `Charge ${formatRand(total)}`}
              </Button>
              {cart.length > 0 && (
                <Button variant="secondary" className="py-4 text-lg" onClick={() => changeCart([])}>
                  Clear
                </Button>
              )}
            </div>
          </Card>

          {result && <ResultBanner result={result} onNext={next} />}
        </div>
      </div>
    </Page>
  );
}

function removeOne(cart: number[], id: number) {
  const i = cart.lastIndexOf(id);
  return i < 0 ? cart : [...cart.slice(0, i), ...cart.slice(i + 1)];
}

/** Card number box (a reader or QR scanner types into it and presses Enter) plus a name search. */
function FindLearner({ onFound }: { onFound: (l: TillLearner) => void }) {
  const [token, setToken] = useState('');
  const [q, setQ] = useState('');
  const search = useQuery({
    queryKey: ['pos', 'search', q.trim()],
    queryFn: () => ok(api.GET('/api/pos/learners', { params: { query: { q: q.trim() } } })),
    enabled: q.trim().length >= 2,
  });

  const byCard = useMutation({
    mutationFn: (t: string) => ok(api.GET('/api/pos/cards/{token}', { params: { path: { token: t } } })),
    onSuccess: (l) => {
      setToken('');
      onFound(l);
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    if (token.trim()) byCard.mutate(token.trim());
  }

  return (
    <Card title="Find learner">
      <form onSubmit={submit} className="space-y-2">
        <label htmlFor={CARD_INPUT_ID} className="block text-sm font-semibold">
          Tap card or type card number
        </label>
        <div className="flex gap-2">
          <Input
            id={CARD_INPUT_ID}
            autoFocus
            autoComplete="off"
            className="py-3 font-mono text-lg"
            value={token}
            onChange={(e) => {
              setToken(e.target.value);
              byCard.reset();
            }}
          />
          <Button type="submit" className="px-6" busy={byCard.isPending} disabled={!token.trim()}>
            Go
          </Button>
        </div>
        {byCard.isError && <Alert>{errorMessage(byCard.error)}</Alert>}
      </form>

      <div className="mt-5 space-y-2">
        <label htmlFor="till-search" className="block text-sm font-semibold">
          Or search by name
        </label>
        <Input id="till-search" autoComplete="off" className="py-3 text-lg" placeholder="At least 2 letters" value={q} onChange={(e) => setQ(e.target.value)} />
        {q.trim().length >= 2 &&
          (search.isPending ? (
            <Spinner label="Searching" />
          ) : search.isError ? (
            <ErrorAlert error={search.error} />
          ) : search.data.length === 0 ? (
            <p className="text-ink-2">No learner found.</p>
          ) : (
            <ul className="space-y-2">
              {search.data.map((l) => (
                <li key={l.id}>
                  <button
                    type="button"
                    onClick={() => {
                      setQ('');
                      onFound(l);
                    }}
                    className="flex w-full items-center justify-between gap-2 rounded-xl border border-line px-4 py-3 text-left hover:border-blue hover:bg-surface-2"
                  >
                    <span>
                      <span className="block text-lg font-semibold">{l.name}</span>
                      <span className="text-sm text-ink-3">
                        {l.grade} · {l.school}
                      </span>
                    </span>
                    {l.todaysOrder && l.todaysOrder.status !== 'COLLECTED' && <Badge tone="blue">Pre-order</Badge>}
                  </button>
                </li>
              ))}
            </ul>
          ))}
      </div>
    </Card>
  );
}

function LearnerPanel({ learner, onClear }: { learner: TillLearner; onClear: () => void }) {
  const order = learner.todaysOrder ?? null;
  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="text-2xl font-extrabold">{learner.name}</p>
          <p className="text-ink-2">
            {learner.grade} · {learner.school}
          </p>
        </div>
        <Button variant="ghost" onClick={onClear}>
          Next learner
        </Button>
      </div>
      {learner.frozen && <Alert>Card frozen by a parent. Sales will be declined.</Alert>}
      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-xl bg-surface-2 p-3">
          <p className="text-sm text-ink-2">Balance</p>
          <Money value={learner.walletBalance} className="text-2xl font-extrabold" />
        </div>
        <div className="rounded-xl bg-surface-2 p-3">
          <p className="text-sm text-ink-2">Left today</p>
          <Money value={learner.leftToday} className="text-2xl font-extrabold" />
        </div>
      </div>
      <p className="text-sm">
        <span className="font-semibold">{TUCK_MODE[learner.mode]?.label}.</span> <span className="text-ink-2">{TUCK_MODE[learner.mode]?.help}</span>
      </p>
      {order && (
        <div className="border-t border-line pt-4">
          <p className="mb-2 font-bold">Today's pre-order</p>
          <PreOrderHandout order={order} showName={false} />
        </div>
      )}
    </div>
  );
}

function MenuButton({ item, count, onAdd }: { item: MenuItem; count: number; onAdd: () => void }) {
  return (
    <button
      type="button"
      onClick={onAdd}
      className={cx(
        'relative flex min-h-24 flex-col items-start justify-between rounded-2xl border-2 p-4 text-left transition active:scale-[0.98]',
        count > 0 ? 'border-blue bg-blue-soft' : 'border-line bg-surface hover:border-blue',
      )}
    >
      <span className="text-lg leading-tight font-bold">{item.name}</span>
      <span className="mt-2 flex w-full items-end justify-between gap-2">
        <Money value={item.price} className="text-lg font-semibold" />
        <span className="text-xs text-ink-3">{item.category}</span>
      </span>
      {count > 0 && <span className="absolute -top-2 -right-2 flex size-8 items-center justify-center rounded-full bg-blue font-bold text-on-blue">{count}</span>}
    </button>
  );
}

/**
 * A heads-up from what the till knows. The till view doesn't include the approved-items list or
 * blocked categories, so those are left to the server.
 */
function Warning({ learner, total }: { learner: TillLearner; total: number }) {
  const reason = learner.frozen
    ? 'the card is frozen'
    : learner.mode === 'PREORDER_ONLY'
      ? `${learner.name} may only collect pre-orders`
      : total > learner.leftToday
        ? `only ${formatRand(learner.leftToday)} left today`
        : total > learner.walletBalance
          ? `only ${formatRand(learner.walletBalance)} on the card`
          : null;
  if (!reason) return null;
  return (
    <div className="mt-4">
      <Alert tone="warn">Likely to be declined: {reason}.</Alert>
    </div>
  );
}

function ResultBanner({ result, onNext }: { result: Schemas['ChargeResult']; onNext: () => void }) {
  return (
    <section role="status" className={cx('rounded-2xl p-6', result.approved ? 'bg-ok text-white' : 'bg-bad text-white')}>
      <p className="text-4xl font-extrabold">{result.approved ? 'Approved' : 'Declined'}</p>
      <p className="mt-2 text-xl font-semibold">{result.message}</p>
      {result.approved && (
        <Button variant="secondary" className="mt-4 px-6 py-3 text-lg" onClick={onNext}>
          Next learner
        </Button>
      )}
    </section>
  );
}
