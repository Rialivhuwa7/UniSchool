import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CAMPUS_LABEL, formatRand, ok, type Schemas } from '@unischool/api';
import { api } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, Checkbox, ErrorAlert, Input, Page, QueryView, Table } from '../../components/ui';

type ListStatus = Schemas['ListStatus'];
type Row = { key: number; name: string; quantity: string; unitPrice: string };

const STATUS_TONE: Record<string, 'ok' | 'warn' | 'neutral'> = { Published: 'ok', Draft: 'warn' };

let nextKey = 1;
const blankRow = (): Row => ({ key: nextKey++, name: '', quantity: '1', unitPrice: '' });

/** Stationery lists per grade: what's published and how many families ordered, with an editor. */
export function Lists() {
  const [editing, setEditing] = useState<ListStatus | null>(null);
  const lists = useQuery({ queryKey: ['office', 'lists'], queryFn: () => ok(api.GET('/api/school/lists')) });

  return (
    <Page title="Stationery lists" intro="Parents order their child's grade list in the shop once it's published.">
      {editing && <ListEditor key={`${editing.campus}-${editing.grade}`} list={editing} onDone={() => setEditing(null)} />}
      <QueryView query={lists}>
        {(rows) => (
          <div className="grid gap-6 lg:grid-cols-2">
            {(['PREP', 'COLLEGE'] as const).map((campus) => (
              <Card key={campus} title={CAMPUS_LABEL[campus]}>
                <Table
                  rows={rows.filter((l) => l.campus === campus)}
                  rowKey={(l) => l.grade}
                  columns={[
                    { header: 'Grade', cell: (l) => <span className="font-semibold">{l.grade}</span> },
                    { header: 'Status', cell: (l) => <Badge tone={STATUS_TONE[l.status] ?? 'neutral'}>{l.status}</Badge> },
                    { header: 'Items', cell: (l) => l.items || <span className="text-ink-3">–</span> },
                    { header: 'Orders', cell: (l) => l.ordersPlaced },
                    {
                      header: '',
                      cell: (l) => (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => {
                            setEditing(l);
                            window.scrollTo({ top: 0, behavior: 'smooth' });
                          }}
                        >
                          {l.items > 0 ? 'Edit' : 'Start list'}
                        </Button>
                      ),
                    },
                  ]}
                />
              </Card>
            ))}
          </div>
        )}
      </QueryView>
    </Page>
  );
}

/** Loads a grade's current items, then opens them in the editor. */
function ListEditor({ list, onDone }: { list: ListStatus; onDone: () => void }) {
  const detail = useQuery({
    queryKey: ['office', 'lists', list.campus, list.grade],
    queryFn: () => ok(api.GET('/api/school/lists/{campus}/{grade}', { params: { path: { campus: list.campus, grade: list.grade } } })),
    // Always start from what's saved now, not a copy from an earlier visit.
    staleTime: 0,
    gcTime: 0,
  });
  return (
    <Card title={`${list.grade}, ${CAMPUS_LABEL[list.campus]}`}>
      <QueryView query={detail} label="Loading the list">
        {(d) => (
          <ListForm
            list={list}
            items={(d.items ?? []).map((i) => ({ key: nextKey++, name: i.name, quantity: String(i.quantity ?? 1), unitPrice: Number(i.unitPrice).toFixed(2) }))}
            onDone={onDone}
          />
        )}
      </QueryView>
    </Card>
  );
}

/** Edit a grade's items. Saving stores exactly the rows shown. */
function ListForm({ list, items, onDone }: { list: ListStatus; items: Row[]; onDone: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [rows, setRows] = useState<Row[]>(() => (items.length > 0 ? [...items, blankRow()] : [blankRow(), blankRow(), blankRow()]));
  const [publish, setPublish] = useState(list.status !== 'Draft');

  const filled = rows.filter((r) => r.name.trim());
  const valid = filled.length > 0 && filled.every((r) => Number(r.quantity) >= 1 && Number(r.quantity) <= 100 && r.unitPrice !== '' && Number(r.unitPrice) >= 0);
  const total = filled.reduce((sum, r) => sum + Number(r.quantity) * Number(r.unitPrice || 0), 0);

  const save = useMutation({
    mutationFn: () =>
      ok(
        api.PUT('/api/school/lists/{campus}/{grade}', {
          params: { path: { campus: list.campus, grade: list.grade } },
          body: { publish, items: filled.map((r) => ({ name: r.name.trim(), quantity: Number(r.quantity), unitPrice: Number(r.unitPrice) })) },
        }),
      ),
    onSuccess: (s) => {
      toast(`${s.grade} list saved: ${s.items} ${s.items === 1 ? 'item' : 'items'}, ${s.status.toLowerCase()}.`);
      queryClient.invalidateQueries({ queryKey: ['office', 'lists'] });
      onDone();
    },
  });

  const update = (key: number, patch: Partial<Row>) => setRows(rows.map((r) => (r.key === key ? { ...r, ...patch } : r)));

  function submit(e: FormEvent) {
    e.preventDefault();
    save.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <ErrorAlert error={save.error} />

      <div className="space-y-2">
        <div className="hidden grid-cols-[1fr_90px_130px_40px] gap-2 text-sm font-semibold text-ink-2 md:grid">
          <span>Item</span>
          <span>How many</span>
          <span>Price each (R)</span>
        </div>
        {rows.map((r, i) => (
          <div key={r.key} className="grid grid-cols-[1fr_90px_130px_40px] gap-2">
            <Input aria-label={`Item ${i + 1}`} placeholder="e.g. HB pencils, box of 12" value={r.name} onChange={(e) => update(r.key, { name: e.target.value })} />
            <Input aria-label={`How many of item ${i + 1}`} type="number" min={1} max={100} value={r.quantity} onChange={(e) => update(r.key, { quantity: e.target.value })} />
            <Input
              aria-label={`Price of item ${i + 1}`}
              type="number"
              min={0}
              step="0.01"
              inputMode="decimal"
              value={r.unitPrice}
              onChange={(e) => update(r.key, { unitPrice: e.target.value })}
            />
            <button
              type="button"
              aria-label={`Remove item ${i + 1}`}
              onClick={() => setRows(rows.filter((x) => x.key !== r.key))}
              disabled={rows.length === 1}
              className="rounded-full text-ink-3 hover:bg-surface-2 hover:text-bad disabled:opacity-30"
            >
              ×
            </button>
          </div>
        ))}
        <Button size="sm" variant="ghost" onClick={() => setRows([...rows, blankRow()])}>
          Add item
        </Button>
      </div>

      <p className="text-ink-2">
        {filled.length} {filled.length === 1 ? 'item' : 'items'} · list total <b className="text-ink">{formatRand(total)}</b>
      </p>

      <Checkbox label="Publish now, so parents can order it" checked={publish} onChange={(e) => setPublish(e.target.checked)} />

      <div className="flex flex-wrap gap-2">
        <Button type="submit" busy={save.isPending} disabled={!valid}>
          {publish ? 'Save and publish' : 'Save as draft'}
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  );
}
