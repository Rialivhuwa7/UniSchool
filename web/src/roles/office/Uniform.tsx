import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query';
import { CAMPUS_LABEL, formatRand, isValidAmount, ok, TEAMS, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, ErrorAlert, Field, Input, Money, Page, QueryView, Select, Stat, Stats, Table } from '../../components/ui';
import { SchoolOptions, type School } from './shared';

type ItemKind = Schemas['UniformItemRequest']['kind'];
type OfferType = Schemas['SportOfferRequest']['type'];

const KIND: Record<ItemKind, string> = { GENERIC: 'Generic', CRESTED: 'Crested' };
const OFFER_TYPE: Record<OfferType, string> = { BUY: 'Buy', RENT: 'Rent' };
/** Size charts parents fill in for their children, so the shop can suggest the right size. */
const SIZE_TYPES = ['Shirt', 'Shorts', 'Trousers', 'Shoes', 'Blazer', 'Jersey', 'One size'];

const ITEMS_KEY = ['office', 'uniform', 'items'];
const SPORT_KEY = ['office', 'sport'];

/** The uniform shop's items, prices and stock, plus sport kit parents can buy or rent. */
export function Uniform() {
  const uniform = useQuery({ queryKey: ['office', 'uniform'], queryFn: () => ok(api.GET('/api/school/uniform')) });
  const items = useQuery({ queryKey: ITEMS_KEY, queryFn: () => ok(api.GET('/api/school/uniform/items')) });

  return (
    <Page title="Uniform & sport" intro="What the uniform shop sells, what's in stock, and sport kit to buy or rent.">
      <QueryView query={uniform}>
        {(u) => (
          <Stats>
            <Stat label="Sales this year" value={<Money value={u.salesThisYear} />} />
            <Stat label="Sizes running low" value={u.lowSizes} tone={u.lowSizes > 0 ? 'warn' : 'ok'} />
          </Stats>
        )}
      </QueryView>

      <UniformItems />

      <Card title="Stock">
        <div className="space-y-5">
          <QueryView query={items}>{(list) => <AddStockForm itemNames={[...new Set(list.map((i) => i.name))]} />}</QueryView>
          <QueryView query={uniform}>
            {(u) => (
              <Table
                rows={u.stock ?? []}
                rowKey={(s) => s.id}
                empty="No stock recorded yet."
                columns={[
                  { header: 'Item', cell: (s) => <span className="font-semibold">{s.item}</span> },
                  { header: 'Size', cell: (s) => s.size },
                  {
                    header: 'In stock',
                    cell: (s) => (
                      <span className="inline-flex items-center gap-2">
                        <QuantityCell line={s} />
                        {s.low && <Badge tone="warn">Low</Badge>}
                      </span>
                    ),
                  },
                  { header: '', className: 'text-right', cell: (s) => <RestockCell line={s} /> },
                ]}
              />
            )}
          </QueryView>
        </div>
      </Card>

      <SportKit />
    </Page>
  );
}

/** Items the shop sells, with their prices. */
function UniformItems() {
  const [adding, setAdding] = useState(false);
  const items = useQuery({ queryKey: ITEMS_KEY, queryFn: () => ok(api.GET('/api/school/uniform/items')) });

  return (
    <Card title="Uniform items" actions={!adding && <Button size="sm" onClick={() => setAdding(true)}>Add an item</Button>}>
      <div className="space-y-5">
        {adding && <UniformItemForm onDone={() => setAdding(false)} />}
        <QueryView query={items}>
          {(rows) => (
            <Table
              rows={rows}
              rowKey={(i) => i.id}
              empty="No uniform items yet. Add the first one so parents can buy it in the shop."
              columns={[
                {
                  header: 'Item',
                  cell: (i) => (
                    <div>
                      <p className="font-semibold">{i.name}</p>
                      <p className="text-ink-3">{CAMPUS_LABEL[i.campus]}</p>
                    </div>
                  ),
                },
                { header: 'Type', cell: (i) => <Badge tone={i.kind === 'CRESTED' ? 'blue' : 'neutral'}>{KIND[i.kind]}</Badge> },
                { header: 'Sized as', cell: (i) => i.sizeType || <span className="text-ink-3">–</span> },
                { header: 'Supplier', cell: (i) => i.supplier || <span className="text-ink-3">–</span> },
                {
                  header: 'Price',
                  cell: (i) => (
                    <PriceCell
                      label={i.name}
                      price={i.price}
                      queryKey={ITEMS_KEY}
                      save={(price) => ok(api.PATCH('/api/school/uniform/items/{id}', { params: { path: { id: i.id } }, body: { price } }))}
                    />
                  ),
                },
              ]}
            />
          )}
        </QueryView>
      </div>
    </Card>
  );
}

function UniformItemForm({ onDone }: { onDone: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [campus, setCampus] = useState<School>('PREP');
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [kind, setKind] = useState<ItemKind>('GENERIC');
  const [sizeType, setSizeType] = useState(SIZE_TYPES[0]);
  const [supplier, setSupplier] = useState('');

  const value = Number(price);
  const priceOk = isValidAmount(value);

  const add = useMutation({
    mutationFn: () =>
      ok(api.POST('/api/school/uniform/items', { body: { campus, name: name.trim(), price: value, kind, sizeType, supplier: supplier.trim() || undefined } })),
    onSuccess: (i) => {
      toast(`${i.name} added at ${formatRand(i.price)}.`);
      queryClient.invalidateQueries({ queryKey: ITEMS_KEY });
      onDone();
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    add.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-xl bg-surface-2 p-4">
      <ErrorAlert error={add.error} />
      <div className="grid gap-4 md:grid-cols-[1fr_200px_160px]">
        <Field label="Item" hint="e.g. Grey shorts, or School blazer with crest">
          {(id) => <Input id={id} required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />}
        </Field>
        <Field label="School">
          {(id) => (
            <Select id={id} value={campus} onChange={(e) => setCampus(e.target.value as School)}>
              <SchoolOptions />
            </Select>
          )}
        </Field>
        <Field label="Price (R)" error={price && !priceOk ? 'Enter an amount like 149.00.' : undefined}>
          {(id) => <Input id={id} required type="number" min={0.01} step="0.01" inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} />}
        </Field>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        <Field label="Type" hint="Crested items carry the school badge">
          {(id) => (
            <Select id={id} value={kind} onChange={(e) => setKind(e.target.value as ItemKind)}>
              {(Object.keys(KIND) as ItemKind[]).map((k) => (
                <option key={k} value={k}>
                  {KIND[k]}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Sized as" hint="Matches the sizes parents enter for their child">
          {(id) => (
            <Select id={id} value={sizeType} onChange={(e) => setSizeType(e.target.value)}>
              {SIZE_TYPES.map((s) => (
                <option key={s}>{s}</option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Supplier" hint="Optional">
          {(id) => <Input id={id} maxLength={60} value={supplier} onChange={(e) => setSupplier(e.target.value)} />}
        </Field>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button type="submit" busy={add.isPending} disabled={!name.trim() || !priceOk}>
          Add item
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  );
}

/** A new stock line: one item in one size. */
function AddStockForm({ itemNames }: { itemNames: string[] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [itemName, setItemName] = useState('');
  const [size, setSize] = useState('');
  const [quantity, setQuantity] = useState('');

  const qty = Number(quantity);
  const qtyOk = quantity !== '' && Number.isInteger(qty) && qty >= 0 && qty <= 100_000;

  const add = useMutation({
    mutationFn: () => ok(api.POST('/api/school/uniform/stock', { body: { itemName, size: size.trim(), quantity: qty } })),
    onSuccess: () => {
      toast(`${itemName}, size ${size.trim()}: ${qty} in stock.`);
      setSize('');
      setQuantity('');
      queryClient.invalidateQueries({ queryKey: ['office', 'uniform'] });
    },
  });

  if (itemNames.length === 0) return <p className="text-ink-2">Add uniform items above first, then record how many of each size you have.</p>;

  function submit(e: FormEvent) {
    e.preventDefault();
    add.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <ErrorAlert error={add.error} />
      <div className="grid items-start gap-4 md:grid-cols-[1fr_140px_140px_auto]">
        <Field label="Item">
          {(id) => (
            <Select id={id} required value={itemName} onChange={(e) => setItemName(e.target.value)}>
              <option value="">Choose an item</option>
              {itemNames.map((n) => (
                <option key={n}>{n}</option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Size">{(id) => <Input id={id} required maxLength={20} placeholder="e.g. 9-10" value={size} onChange={(e) => setSize(e.target.value)} />}</Field>
        <Field label="How many">
          {(id) => <Input id={id} required type="number" min={0} max={100000} step={1} value={quantity} onChange={(e) => setQuantity(e.target.value)} />}
        </Field>
        <Button type="submit" className="md:mt-6" busy={add.isPending} disabled={!itemName || !size.trim() || !qtyOk}>
          Add stock line
        </Button>
      </div>
    </form>
  );
}

/** Sport kit parents can buy or rent, by sport. */
function SportKit() {
  const [adding, setAdding] = useState(false);
  const offers = useQuery({ queryKey: SPORT_KEY, queryFn: () => ok(api.GET('/api/school/sport')) });

  return (
    <Card title="Sport kit" actions={!adding && <Button size="sm" onClick={() => setAdding(true)}>Add sport kit</Button>}>
      <div className="space-y-5">
        {adding && <SportOfferForm onDone={() => setAdding(false)} />}
        <QueryView query={offers}>
          {(rows) => (
            <Table
              rows={rows}
              rowKey={(o) => o.id}
              empty="No sport kit yet. Add bundles to buy or equipment to rent."
              columns={[
                { header: 'Sport', cell: (o) => <span className="font-semibold">{o.sport}</span> },
                { header: 'Kit', cell: (o) => o.name },
                { header: 'Buy or rent', cell: (o) => <Badge tone={o.type === 'RENT' ? 'blue' : 'neutral'}>{OFFER_TYPE[o.type]}</Badge> },
                {
                  header: 'Price',
                  cell: (o) => (
                    <PriceCell
                      label={o.name}
                      price={o.price}
                      queryKey={SPORT_KEY}
                      save={(price) => ok(api.PATCH('/api/school/sport/{id}', { params: { path: { id: o.id } }, body: { price } }))}
                    />
                  ),
                },
              ]}
            />
          )}
        </QueryView>
      </div>
    </Card>
  );
}

function SportOfferForm({ onDone }: { onDone: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [sport, setSport] = useState(TEAMS[0]);
  const [type, setType] = useState<OfferType>('BUY');
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');

  const value = Number(price);
  const priceOk = isValidAmount(value);

  const add = useMutation({
    mutationFn: () => ok(api.POST('/api/school/sport', { body: { sport, type, name: name.trim(), price: value } })),
    onSuccess: (o) => {
      toast(`${o.name} added at ${formatRand(o.price)}.`);
      queryClient.invalidateQueries({ queryKey: SPORT_KEY });
      onDone();
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    add.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-xl bg-surface-2 p-4">
      <ErrorAlert error={add.error} />
      <div className="grid gap-4 md:grid-cols-[180px_140px_1fr_160px]">
        <Field label="Sport">
          {(id) => (
            <Select id={id} value={sport} onChange={(e) => setSport(e.target.value)}>
              {TEAMS.map((t) => (
                <option key={t}>{t}</option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Buy or rent">
          {(id) => (
            <Select id={id} value={type} onChange={(e) => setType(e.target.value as OfferType)}>
              {(Object.keys(OFFER_TYPE) as OfferType[]).map((t) => (
                <option key={t} value={t}>
                  {OFFER_TYPE[t]}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Kit" hint="e.g. Hockey bundle: stick, shin guards, mouthguard">
          {(id) => <Input id={id} required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />}
        </Field>
        <Field label={type === 'RENT' ? 'Rental price (R)' : 'Price (R)'} error={price && !priceOk ? 'Enter an amount like 85.00.' : undefined}>
          {(id) => <Input id={id} required type="number" min={0.01} step="0.01" inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} />}
        </Field>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button type="submit" busy={add.isPending} disabled={!name.trim() || !priceOk}>
          Add sport kit
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  );
}

/** A price with a Change button that turns it into a small form. */
function PriceCell({ label, price, queryKey, save }: { label: string; price: number; queryKey: QueryKey; save: (price: number) => Promise<unknown> }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');
  const value = Number(draft);

  const change = useMutation({
    mutationFn: () => save(value),
    onSuccess: () => {
      toast(`${label} now costs ${formatRand(value)}.`);
      queryClient.invalidateQueries({ queryKey });
      setEditing(false);
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  if (!editing) {
    return (
      <span className="inline-flex items-center gap-2">
        <Money value={price} className="font-semibold" />
        <Button
          size="sm"
          variant="ghost"
          onClick={() => {
            setDraft(String(price ?? ''));
            setEditing(true);
          }}
        >
          Change
        </Button>
      </span>
    );
  }
  return (
    <form
      className="flex items-center gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        change.mutate();
      }}
    >
      <Input aria-label={`New price for ${label}`} className="w-28" type="number" min={0.01} step="0.01" inputMode="decimal" autoFocus value={draft} onChange={(e) => setDraft(e.target.value)} />
      <Button size="sm" type="submit" busy={change.isPending} disabled={!isValidAmount(value)}>
        Save
      </Button>
      <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>
        Cancel
      </Button>
    </form>
  );
}

/** How many are on the shelf, with a Change button for a stock count. */
function QuantityCell({ line }: { line: Schemas['StockLine'] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');
  const qty = Number(draft);
  const qtyOk = draft !== '' && Number.isInteger(qty) && qty >= 0 && qty <= 100_000;

  const change = useMutation({
    mutationFn: () => ok(api.PUT('/api/school/uniform/stock/{id}', { params: { path: { id: line.id } }, body: { quantity: qty } })),
    onSuccess: () => {
      toast(`${line.item}, size ${line.size}: ${qty} in stock.`);
      queryClient.invalidateQueries({ queryKey: ['office', 'uniform'] });
      setEditing(false);
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  if (!editing) {
    return (
      <>
        <span className="tabular-nums">{line.quantity}</span>
        <Button
          size="sm"
          variant="ghost"
          onClick={() => {
            setDraft(String(line.quantity));
            setEditing(true);
          }}
        >
          Change
        </Button>
      </>
    );
  }
  return (
    <form
      className="flex items-center gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        change.mutate();
      }}
    >
      <Input aria-label={`How many ${line.item} in size ${line.size}`} className="w-24" type="number" min={0} max={100000} step={1} autoFocus value={draft} onChange={(e) => setDraft(e.target.value)} />
      <Button size="sm" type="submit" busy={change.isPending} disabled={!qtyOk}>
        Save
      </Button>
      <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>
        Cancel
      </Button>
    </form>
  );
}

function RestockCell({ line }: { line: Schemas['StockLine'] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const restock = useMutation({
    mutationFn: () => ok(api.POST('/api/school/uniform/stock/{id}/restock', { params: { path: { id: line.id } } })),
    onSuccess: (res) => {
      toast(res.message);
      queryClient.invalidateQueries({ queryKey: ['office', 'uniform'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });
  if (line.restockRequested) return <Badge tone="blue">Restock requested</Badge>;
  return (
    <Button size="sm" variant={line.low ? 'primary' : 'ghost'} busy={restock.isPending} onClick={() => restock.mutate()}>
      Request restock
    </Button>
  );
}
