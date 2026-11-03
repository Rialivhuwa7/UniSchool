import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { isValidAmount, ok, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, ErrorAlert, Field, Input, Page, QueryView, Table } from '../../components/ui';

type Item = Schemas['MenuItemAdminView'];

/** Categories the menu already uses, plus the ones parents can block. Staff can type a new one. */
const SUGGESTED = ['Hot food', 'Healthy', 'Drinks', 'Fizzy drinks', 'Snacks', 'Sweets'];

/**
 * The tuckshop's own menu. Items are never deleted, because pre-orders and parents' approved
 * lists use their names; taking an item off the menu hides it from parents and the till.
 */
export function Menu() {
  const menu = useQuery({ queryKey: ['tuckshop', 'menu-admin'], queryFn: () => ok(api.GET('/api/pos/menu')) });
  return (
    <Page title="Menu" intro="What parents can pre-order and what the till sells. Prices change straight away.">
      <Card title="Add an item">
        <AddItem categories={[...new Set([...SUGGESTED, ...(menu.data ?? []).map((i) => i.category)])]} />
      </Card>
      <Card title="On the menu">
        <QueryView query={menu}>
          {(items) => (
            <Table
              rows={items}
              rowKey={(i) => i.id}
              empty="The menu is empty. Add the first item above."
              columns={[
                { header: 'Item', cell: (i) => <span className="font-semibold">{i.name}</span> },
                { header: 'Category', cell: (i) => i.category },
                { header: 'Price', className: 'w-56', cell: (i) => <PriceEditor key={`${i.id}-${i.price}`} item={i} /> },
                { header: 'Status', cell: (i) => (i.available ? <Badge tone="ok">On the menu</Badge> : <Badge>Off the menu</Badge>) },
                { header: '', className: 'text-right', cell: (i) => <AvailabilityButton item={i} /> },
              ]}
            />
          )}
        </QueryView>
      </Card>
    </Page>
  );
}

/** Saves a change to one item and refreshes the menu everywhere (this screen, the till, parents). */
function useUpdate(item: Item) {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation({
    mutationFn: (body: Schemas['MenuItemUpdate']) => ok(api.PATCH('/api/pos/menu/{id}', { params: { path: { id: item.id } }, body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tuckshop'] }),
    onError: (e) => toast(errorMessage(e), 'bad'),
  });
}

function AddItem({ categories }: { categories: string[] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [category, setCategory] = useState('Hot food');

  const add = useMutation({
    mutationFn: () => ok(api.POST('/api/pos/menu', { body: { name: name.trim(), price: Number(price), category: category.trim() } })),
    onSuccess: (item) => {
      toast(`${item.name} is on the menu.`);
      setName('');
      setPrice('');
      queryClient.invalidateQueries({ queryKey: ['tuckshop'] });
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    add.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <ErrorAlert error={add.error} />
      <div className="grid gap-4 md:grid-cols-[2fr_1fr_1fr_auto] md:items-end">
        <Field label="Name">{(id) => <Input id={id} required maxLength={60} value={name} onChange={(e) => setName(e.target.value)} />}</Field>
        <Field label="Category">
          {(id) => (
            <>
              <Input id={id} required maxLength={40} list="menu-categories" value={category} onChange={(e) => setCategory(e.target.value)} />
              <datalist id="menu-categories">
                {categories.map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
            </>
          )}
        </Field>
        <Field label="Price (R)">
          {(id) => <Input id={id} type="number" required min="0.01" max="1000" step="0.01" inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value)} />}
        </Field>
        <Button type="submit" busy={add.isPending} disabled={!isValidAmount(Number(price))}>
          Add
        </Button>
      </div>
      <p className="text-sm text-ink-3">Parents can block Fizzy drinks, Sweets and Snacks, so use those category names for those items.</p>
    </form>
  );
}

function PriceEditor({ item }: { item: Item }) {
  const [price, setPrice] = useState(String(item.price));
  const update = useUpdate(item);
  const changed = Number(price) !== item.price;
  return (
    <div className="flex items-center gap-2">
      <span className="text-ink-3">R</span>
      <div className="w-24">
        <Input aria-label={`Price of ${item.name}`} type="number" min="0.01" max="1000" step="0.01" className="py-1" value={price} onChange={(e) => setPrice(e.target.value)} />
      </div>
      {changed && (
        <Button size="sm" busy={update.isPending} disabled={!isValidAmount(Number(price))} onClick={() => update.mutate({ price: Number(price) } as Schemas['MenuItemUpdate'])}>
          Save
        </Button>
      )}
    </div>
  );
}

function AvailabilityButton({ item }: { item: Item }) {
  const update = useUpdate(item);
  return (
    <Button size="sm" variant="secondary" busy={update.isPending} onClick={() => update.mutate({ available: !item.available } as Schemas['MenuItemUpdate'])}>
      {item.available ? 'Take off the menu' : 'Put back on'}
    </Button>
  );
}
