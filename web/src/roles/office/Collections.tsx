import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ALL_GRADES, CAMPUS_LABEL, gradesFor, isValidAmount, KIND_LABEL, ok, type Campus, type CollectionKind } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Button, Card, Checkbox, ErrorAlert, Field, Input, Page, QueryView, Select } from '../../components/ui';
import { CollectionsTable } from './shared';

const AMOUNT_LABEL: Record<CollectionKind, string> = {
  PAYMENT: 'Amount per learner (R)',
  TICKET: 'Price per ticket (R)',
  DONATION: 'Suggested amount (R)',
};

/** Every collection with who has paid, a reminder per collection, a CSV download and a form for a new one. */
export function Collections() {
  const [adding, setAdding] = useState(false);
  const collections = useQuery({ queryKey: ['office', 'collections'], queryFn: () => ok(api.GET('/api/school/collections')) });

  return (
    <Page
      title="Collections"
      intro="Camps, tours, tickets and donations parents pay for in the app."
      actions={
        <>
          <DownloadButton />
          {!adding && <Button onClick={() => setAdding(true)}>New collection</Button>}
        </>
      }
    >
      {adding && (
        <Card title="New collection">
          <NewCollectionForm onDone={() => setAdding(false)} />
        </Card>
      )}
      <Card>
        <QueryView query={collections}>{(rows) => <CollectionsTable rows={rows} />}</QueryView>
      </Card>
    </Page>
  );
}

/** Downloads every collection as a spreadsheet-friendly CSV file. */
function DownloadButton() {
  const toast = useToast();
  const download = useMutation({
    mutationFn: () => ok(api.GET('/api/school/collections/export', { parseAs: 'text' })),
    onSuccess: (csv) => {
      const url = URL.createObjectURL(new Blob([csv ?? ''], { type: 'text/csv' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = 'collections.csv';
      a.click();
      URL.revokeObjectURL(url);
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });
  return (
    <Button variant="secondary" busy={download.isPending} onClick={() => download.mutate()}>
      Download CSV
    </Button>
  );
}

function NewCollectionForm({ onDone }: { onDone: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [kind, setKind] = useState<CollectionKind>('PAYMENT');
  const [campus, setCampus] = useState<Campus>('BOTH');
  const [grades, setGrades] = useState<string[]>([ALL_GRADES]);
  const [dueDate, setDueDate] = useState('');
  const [instalments, setInstalments] = useState('1');

  const value = Number(amount);
  const amountOk = isValidAmount(value) && value >= 1;
  const count = kind === 'PAYMENT' ? Number(instalments) : 1;

  const create = useMutation({
    mutationFn: () =>
      ok(
        api.POST('/api/school/collections', {
          body: { title: title.trim(), amount: value, kind, campus, grades, dueDate: (dueDate || null) as unknown as string, instalments: count },
        }),
      ),
    onSuccess: (c) => {
      toast(`${c.title} is live. ${c.eligible} learners can pay it in the app.`);
      queryClient.invalidateQueries({ queryKey: ['office'] });
      onDone();
    },
  });

  const toggle = (g: string) => setGrades(grades.includes(g) ? grades.filter((x) => x !== g) : [...grades, g]);

  function submit(e: FormEvent) {
    e.preventDefault();
    create.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <ErrorAlert error={create.error} />
      <div className="grid gap-4 md:grid-cols-[1fr_180px]">
        <Field label="Title" hint="What parents see, e.g. Grade 6 camp, Drakensberg">
          {(id) => <Input id={id} required maxLength={140} value={title} onChange={(e) => setTitle(e.target.value)} />}
        </Field>
        <Field label="Type">
          {(id) => (
            <Select id={id} value={kind} onChange={(e) => setKind(e.target.value as CollectionKind)}>
              {(Object.keys(KIND_LABEL) as CollectionKind[]).map((k) => (
                <option key={k} value={k}>
                  {KIND_LABEL[k]}
                </option>
              ))}
            </Select>
          )}
        </Field>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Field label={AMOUNT_LABEL[kind]} error={amount && !amountOk ? 'Enter an amount of at least R1.00.' : undefined}>
          {(id) => <Input id={id} required type="number" min={1} step="0.01" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />}
        </Field>
        <Field label="Due date" hint="Leave empty if there's no deadline">
          {(id) => <Input id={id} type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />}
        </Field>
        {kind === 'PAYMENT' && (
          <Field label="Instalments" hint={count > 1 && amountOk ? `${count} payments of about R${(value / count).toFixed(2)}` : 'Parents can pay in parts'}>
            {(id) => (
              <Select id={id} value={instalments} onChange={(e) => setInstalments(e.target.value)}>
                {Array.from({ length: 12 }, (_, i) => i + 1).map((n) => (
                  <option key={n} value={n}>
                    {n === 1 ? 'Once-off' : `${n} instalments`}
                  </option>
                ))}
              </Select>
            )}
          </Field>
        )}
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Field label="School">
          {(id) => (
            <Select
              id={id}
              value={campus}
              onChange={(e) => {
                setCampus(e.target.value as Campus);
                setGrades([ALL_GRADES]);
              }}
            >
              {(['BOTH', 'PREP', 'COLLEGE'] as Campus[]).map((c) => (
                <option key={c} value={c}>
                  {CAMPUS_LABEL[c]}
                </option>
              ))}
            </Select>
          )}
        </Field>
        <fieldset className="md:col-span-2">
          <legend className="mb-1 text-sm font-semibold">Grades</legend>
          <div className="flex flex-wrap gap-x-4 gap-y-1">
            <Checkbox label="All grades" checked={grades.includes(ALL_GRADES)} onChange={() => setGrades(grades.includes(ALL_GRADES) ? [] : [ALL_GRADES])} />
            {!grades.includes(ALL_GRADES) &&
              gradesFor(campus).map((g) => <Checkbox key={g} label={g.replace('Grade ', 'Gr ')} checked={grades.includes(g)} onChange={() => toggle(g)} />)}
          </div>
        </fieldset>
      </div>

      <div className="flex flex-wrap gap-2">
        <Button type="submit" busy={create.isPending} disabled={!title.trim() || !amountOk || grades.length === 0}>
          Create collection
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  );
}
