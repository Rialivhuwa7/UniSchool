import { useState, type FormEvent } from 'react';
import { Link, NavLink, useNavigate, useSearchParams } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { CATEGORY_LABEL, formatRand, isValidAmount, ok, type CatalogEntry, type LearnerView, type ProductCategory, type Schemas } from '@unischool/api';
import { api } from '../../lib/api';
import { Alert, Badge, Button, Card, Checkbox, Empty, Field, Input, Money, Page, QueryView, Select, Table, cx, formatDateTime } from '../../components/ui';
import { ChildSelect, parseAmount, useAction, useLearners } from './shared';

type Delivery = Schemas['CheckoutRequest']['delivery'];
type PayMethod = Schemas['CheckoutRequest']['paymentMethod'];

const CATEGORIES: ProductCategory[] = ['SUPPLY_LIST', 'UNIFORM', 'SPORT', 'PRELOVED', 'LABELS'];
const PAY_LABEL: Record<PayMethod, string> = { WALLET: 'Family wallet', SAVINGS: 'Back-to-school savings', CARD: 'Card', PAY_IN_4: 'Pay in 4' };
const DELIVERY_LABEL: Record<Delivery, string> = { TO_SCHOOL: 'Delivered to school', TO_HOME: 'Delivered home' };

function useCart() {
  return useQuery({ queryKey: ['parent', 'cart'], queryFn: () => ok(api.GET('/api/shop/cart')) });
}

/** Browse / Cart (n) / Orders links shown on every shop screen. */
function ShopNav() {
  const cart = useCart();
  const link = ({ isActive }: { isActive: boolean }) =>
    cx('rounded-full px-4 py-1.5 text-sm font-semibold', isActive ? 'bg-ink text-paper' : 'border border-line bg-surface hover:bg-surface-2');
  return (
    <nav className="flex flex-wrap gap-2" aria-label="Shop">
      <NavLink to="/parent/shop" end className={link}>
        Browse
      </NavLink>
      <NavLink to="/parent/shop/cart" className={link}>
        Cart{cart.data ? ` (${cart.data.count})` : ''}
      </NavLink>
      <NavLink to="/parent/shop/orders" className={link}>
        Orders
      </NavLink>
    </nav>
  );
}

/** Uniform, sport kit, stationery lists, labels and pre-loved items, chosen for one child. */
export function Shop() {
  const learners = useLearners();
  const [params, setParams] = useSearchParams();
  const category = (CATEGORIES.find((c) => c === params.get('category')) ?? 'SUPPLY_LIST') as ProductCategory;
  const kids = learners.data ?? [];
  const learnerId = Number(params.get('learner')) || kids[0]?.id;
  const learner = kids.find((l) => l.id === learnerId);

  const update = (key: string, value: string) =>
    setParams((p) => {
      p.set(key, value);
      return p;
    });

  return (
    <Page title="School shop" intro="Everything on the list, in the right sizes. Part of every order goes back to the school." actions={<ShopNav />}>
      <div className="flex flex-wrap items-center gap-3">
        <span className="text-sm font-semibold">Shopping for</span>
        <ChildSelect learners={kids} value={learnerId} onChange={(id) => id && update('learner', String(id))} />
        <div className="flex flex-wrap gap-1">
          {CATEGORIES.map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => update('category', c)}
              className={cx('rounded-full px-3 py-1.5 text-sm font-semibold', c === category ? 'bg-blue-soft text-blue-ink' : 'text-ink-2 hover:bg-surface-2')}
            >
              {CATEGORY_LABEL[c]}
            </button>
          ))}
        </div>
      </div>

      {!learner ? (
        <QueryView query={learners}>{() => <Empty>No children linked yet.</Empty>}</QueryView>
      ) : category === 'SUPPLY_LIST' ? (
        <SupplyList key={learner.id} learner={learner} />
      ) : (
        <>
          {category === 'PRELOVED' && <SellForm key={learner.id} learner={learner} />}
          <Catalog learner={learner} category={category} />
        </>
      )}
    </Page>
  );
}

function Catalog({ learner, category }: { learner: LearnerView; category: ProductCategory }) {
  const catalog = useQuery({
    queryKey: ['parent', 'catalog', learner.id, category],
    queryFn: () => ok(api.GET('/api/shop/catalog', { params: { query: { learnerId: learner.id, category } } })),
  });
  return (
    <QueryView query={catalog}>
      {(items) =>
        items.length === 0 ? (
          <Empty>Nothing here for {learner.firstName} right now.</Empty>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((item) => (
              <Product key={item.key} item={item} learner={learner} />
            ))}
          </div>
        )
      }
    </QueryView>
  );
}

function Product({ item, learner }: { item: CatalogEntry; learner: LearnerView }) {
  const add = useAction(() => ok(api.POST('/api/shop/cart', { body: { learnerId: learner.id, key: item.key } })), `${item.name} is in your cart.`);
  const unlist = useAction(
    () => ok(api.DELETE('/api/shop/preloved/{id}', { params: { path: { id: Number(item.key.slice(2)) } } })),
    `${item.name} is no longer listed.`,
  );

  return (
    <div className="flex flex-col rounded-2xl border border-line bg-surface p-4">
      <p className="font-bold">{item.name}</p>
      <p className="mt-1 flex-1 text-sm text-ink-2">{item.description}</p>
      <div className="mt-2 flex flex-wrap gap-1">
        {(item.badges ?? []).map((b) => (
          <Badge key={b} tone={b.startsWith('Fits') || b.startsWith('Save') ? 'ok' : 'neutral'}>
            {b}
          </Badge>
        ))}
      </div>
      <div className="mt-3 flex items-end justify-between gap-2">
        <p>
          <span className="text-lg font-extrabold">
            <Money value={item.price} />
          </span>
          {item.perMonth && <span className="text-sm text-ink-3"> a month</span>}
          {item.priceNew != null && (
            <span className="block text-sm text-ink-3">
              New: <Money value={item.priceNew} className="line-through" />
            </span>
          )}
        </p>
        {item.mine ? (
          <Button size="sm" variant="secondary" busy={unlist.isPending} onClick={() => unlist.mutate(undefined)}>
            Remove listing
          </Button>
        ) : item.inCart ? (
          <Badge tone="ok">In cart</Badge>
        ) : (
          <Button size="sm" busy={add.isPending} onClick={() => add.mutate(undefined)}>
            Add to cart
          </Button>
        )}
      </div>
    </div>
  );
}

function SupplyList({ learner }: { learner: LearnerView }) {
  const list = useQuery({
    queryKey: ['parent', 'supply-list', learner.id],
    queryFn: () => ok(api.GET('/api/shop/supply-list', { params: { query: { learnerId: learner.id } } })),
  });
  const [have, setHave] = useState<number[]>([]);
  const add = useAction(
    () => ok(api.POST('/api/shop/cart/supply-list', { body: { learnerId: learner.id, alreadyHaveItemIds: have } })),
    `${learner.firstName}'s stationery list is in your cart.`,
  );

  return (
    <Card title={`${learner.firstName}'s ${learner.grade} stationery list`}>
      <QueryView query={list}>
        {(l) =>
          !l.published ? (
            <Empty>The school hasn't published the {l.grade} list yet.</Empty>
          ) : (
            <div className="space-y-4">
              <p className="text-ink-2">Untick anything you already have. Buying the whole list saves {l.fullListDiscountPercent}%.</p>
              <Table
                rows={l.items}
                rowKey={(i) => i.itemId}
                columns={[
                  {
                    header: 'Need it',
                    className: 'w-24',
                    cell: (i) => (
                      <Checkbox
                        label={<span className="sr-only">Need {i.name}</span>}
                        checked={!have.includes(i.itemId)}
                        onChange={(e) => setHave((h) => (e.target.checked ? h.filter((x) => x !== i.itemId) : [...h, i.itemId]))}
                      />
                    ),
                  },
                  { header: 'Item', cell: (i) => <span className={cx(have.includes(i.itemId) && 'text-ink-3 line-through')}>{i.name}</span> },
                  { header: 'Qty', cell: (i) => i.quantity },
                  { header: 'Price', className: 'text-right', cell: (i) => <Money value={i.lineTotal} /> },
                ]}
              />
              <div className="flex flex-wrap items-center gap-3">
                {l.inCart ? (
                  <Badge tone="ok">In your cart</Badge>
                ) : (
                  <Button busy={add.isPending} disabled={have.length === l.items.length} onClick={() => add.mutate(undefined)}>
                    Add to cart
                  </Button>
                )}
                <span className="text-sm text-ink-2">
                  Whole list <Money value={l.fullListTotal} />
                </span>
              </div>
            </div>
          )
        }
      </QueryView>
    </Card>
  );
}

function SellForm({ learner }: { learner: LearnerView }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [priceNew, setPriceNew] = useState('');
  const [sizeType, setSizeType] = useState('');
  const [size, setSize] = useState('');
  const [sport, setSport] = useState('');
  const priceValue = parseAmount(price);
  const newValue = parseAmount(priceNew);
  const priceOk = priceValue !== null && isValidAmount(priceValue) && priceValue >= 10;
  const newOk = priceNew === '' || (newValue !== null && isValidAmount(newValue));

  const sell = useAction(
    () =>
      ok(
        api.POST('/api/shop/preloved', {
          body: {
            learnerId: learner.id,
            name: name.trim(),
            price: priceValue ?? 0,
            ...(newValue !== null && { priceNew: newValue }),
            ...(sizeType && { sizeType }),
            ...(size.trim() && { size: size.trim() }),
            ...(sport && { sport }),
          },
        }),
      ),
    (r) => `${r.name} is listed for other ${learner.school} families.`,
  );

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!priceOk || !newOk || !name.trim()) return;
    sell.mutate(undefined, {
      onSuccess: () => {
        setOpen(false);
        setName('');
        setPrice('');
        setPriceNew('');
        setSize('');
      },
    });
  }

  if (!open) {
    return (
      <Alert tone="blue">
        <span className="flex flex-wrap items-center justify-between gap-2">
          Outgrown something? Sell it to another family at {learner.school}.
          <Button size="sm" onClick={() => setOpen(true)}>
            Sell an item
          </Button>
        </span>
      </Alert>
    );
  }

  return (
    <Card title="Sell a pre-loved item">
      <form onSubmit={submit} className="space-y-4">
        <Field label="What is it?">{(id) => <Input id={id} required maxLength={120} value={name} onChange={(e) => setName(e.target.value)} placeholder="Grey school jersey" />}</Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Your price (R)" error={price && !priceOk ? 'Set a price of at least R10.' : undefined}>
            {(id) => <Input id={id} type="number" min={10} step="0.01" inputMode="decimal" required value={price} onChange={(e) => setPrice(e.target.value)} />}
          </Field>
          <Field label="Price new (R)" hint="Optional. Shows buyers what they save." error={!newOk ? 'Enter an amount like 250.' : undefined}>
            {(id) => <Input id={id} type="number" min={1} step="0.01" inputMode="decimal" value={priceNew} onChange={(e) => setPriceNew(e.target.value)} />}
          </Field>
          <Field label="Size of" hint="Lets us show it to children it will fit.">
            {(id) => (
              <Select id={id} value={sizeType} onChange={(e) => setSizeType(e.target.value)}>
                <option value="">Not sized</option>
                {Object.keys(learner.sizes ?? {}).map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </Select>
            )}
          </Field>
          <Field label="Size">{(id) => <Input id={id} value={size} onChange={(e) => setSize(e.target.value)} placeholder="9-10" />}</Field>
          {learner.sports.length > 0 && (
            <Field label="Sport">
              {(id) => (
                <Select id={id} value={sport} onChange={(e) => setSport(e.target.value)}>
                  <option value="">Not sport kit</option>
                  {learner.sports.map((s) => (
                    <option key={s}>{s}</option>
                  ))}
                </Select>
              )}
            </Field>
          )}
        </div>
        <div className="flex gap-2">
          <Button type="submit" busy={sell.isPending} disabled={!priceOk || !newOk || !name.trim()}>
            List it
          </Button>
          <Button variant="ghost" onClick={() => setOpen(false)}>
            Cancel
          </Button>
        </div>
      </form>
    </Card>
  );
}

export function Cart() {
  const cart = useCart();
  const navigate = useNavigate();
  const [delivery, setDelivery] = useState<Delivery>('TO_SCHOOL');
  const [method, setMethod] = useState<PayMethod>('WALLET');
  const remove = useAction((itemId: number) => ok(api.DELETE('/api/shop/cart/{itemId}', { params: { path: { itemId } } })), 'Removed from your cart.');
  const checkout = useAction(
    () => ok(api.POST('/api/shop/checkout', { body: { delivery, paymentMethod: method } })),
    (o) => `Order ${o.orderNumber} placed. Thank you!`,
  );

  return (
    <Page title="Your cart" actions={<ShopNav />}>
      <QueryView query={cart}>
        {(c) => {
          if (c.items.length === 0)
            return (
              <Empty>
                Your cart is empty. <Link to="/parent/shop" className="font-semibold text-blue-ink hover:underline">Browse the shop</Link>
              </Empty>
            );
          const fee = delivery === 'TO_HOME' && c.subtotal < c.freeHomeDeliveryFrom ? c.homeDeliveryFee : 0;
          return (
            <>
              <Card>
                <Table
                  rows={c.items}
                  rowKey={(i) => i.id}
                  columns={[
                    {
                      header: 'Item',
                      cell: (i) => (
                        <div>
                          <p className="font-semibold">{i.name}</p>
                          <p className="text-ink-3">
                            {CATEGORY_LABEL[i.category]}
                            {i.size && ` · size ${i.size}`}
                            {i.note && ` · ${i.note}`}
                          </p>
                        </div>
                      ),
                    },
                    { header: 'For', cell: (i) => i.learnerName },
                    { header: 'Qty', cell: (i) => i.quantity },
                    { header: 'Price', className: 'text-right', cell: (i) => <Money value={i.lineTotal} /> },
                    {
                      header: '',
                      className: 'text-right',
                      cell: (i) => (
                        <Button size="sm" variant="ghost" busy={remove.isPending && remove.variables === i.id} onClick={() => remove.mutate(i.id)}>
                          Remove
                        </Button>
                      ),
                    },
                  ]}
                />
              </Card>
              <Card title="Checkout">
                <div className="space-y-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <Field label="Delivery" hint={`Home delivery is ${formatRand(c.homeDeliveryFee)}, free from ${formatRand(c.freeHomeDeliveryFrom)}.`}>
                      {(id) => (
                        <Select id={id} value={delivery} onChange={(e) => setDelivery(e.target.value as Delivery)}>
                          <option value="TO_SCHOOL">To school, in your child's bag (free)</option>
                          <option value="TO_HOME">To your home</option>
                        </Select>
                      )}
                    </Field>
                    <Field label="Pay with">
                      {(id) => (
                        <Select id={id} value={method} onChange={(e) => setMethod(e.target.value as PayMethod)}>
                          {(Object.keys(PAY_LABEL) as PayMethod[]).map((m) => (
                            <option key={m} value={m}>
                              {PAY_LABEL[m]}
                            </option>
                          ))}
                        </Select>
                      )}
                    </Field>
                  </div>
                  <dl className="space-y-1 text-sm">
                    <div className="flex justify-between">
                      <dt className="text-ink-2">Items</dt>
                      <dd><Money value={c.subtotal} /></dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-ink-2">Delivery</dt>
                      <dd>{fee ? <Money value={fee} /> : 'Free'}</dd>
                    </div>
                    <div className="flex justify-between text-base font-bold">
                      <dt>Total</dt>
                      <dd><Money value={c.subtotal + fee} /></dd>
                    </div>
                  </dl>
                  <Button busy={checkout.isPending} onClick={() => checkout.mutate(undefined, { onSuccess: () => navigate('/parent/shop/orders') })}>
                    Place order · {formatRand(c.subtotal + fee)}
                  </Button>
                </div>
              </Card>
            </>
          );
        }}
      </QueryView>
    </Page>
  );
}

export function Orders() {
  const orders = useQuery({ queryKey: ['parent', 'orders'], queryFn: () => ok(api.GET('/api/shop/orders')) });
  return (
    <Page title="Your orders" actions={<ShopNav />}>
      <QueryView query={orders}>
        {(list) =>
          list.length === 0 ? (
            <Empty>No orders yet.</Empty>
          ) : (
            <div className="space-y-4">
              {list.map((o) => (
                <Card
                  key={o.id}
                  title={`Order ${o.orderNumber}`}
                  actions={<span className="text-sm text-ink-3">{formatDateTime(o.placedAt)}</span>}
                >
                  <ul className="space-y-1 text-sm">
                    {o.lines.map((l, i) => (
                      <li key={i} className="flex justify-between gap-3">
                        <span>
                          {l.quantity > 1 && `${l.quantity} × `}
                          {l.name} <span className="text-ink-3">for {l.learnerName}</span>
                        </span>
                        <Money value={l.unitPrice * l.quantity} />
                      </li>
                    ))}
                  </ul>
                  <div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-line pt-3 text-sm">
                    <span className="text-ink-2">
                      {DELIVERY_LABEL[o.delivery]} · {PAY_LABEL[o.paymentMethod]} · <Money value={o.schoolGiveBack} /> back to the school
                    </span>
                    <span className="font-bold">
                      Total <Money value={o.total} />
                    </span>
                  </div>
                </Card>
              ))}
            </div>
          )
        }
      </QueryView>
    </Page>
  );
}
