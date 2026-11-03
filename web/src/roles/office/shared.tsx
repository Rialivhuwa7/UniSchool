import { useMutation } from '@tanstack/react-query';
import { ALL_GRADES, CAMPUS_LABEL, gradesFor, KIND_LABEL, ok, type Campus, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Money, Progress, Table, formatDate } from '../../components/ui';

/** The two schools. Learners, fees, uniform items and teachers belong to one of them, never both. */
export const SCHOOLS = ['PREP', 'COLLEGE'] as const;
export type School = (typeof SCHOOLS)[number];

/** <option>s for a school select. */
export function SchoolOptions() {
  return SCHOOLS.map((c) => (
    <option key={c} value={c}>
      {CAMPUS_LABEL[c]}
    </option>
  ));
}

/** <option>s for the grades at a school. */
export function GradeOptions({ campus }: { campus: Campus }) {
  return gradesFor(campus).map((g) => (
    <option key={g} value={g}>
      {g}
    </option>
  ));
}

/** Who something is for: "Highveld Ridge Prep · Grade 4" or "Both schools · All grades". */
export function audienceLabel(campus: Campus, grades: string[] | null | undefined): string {
  const list = grades ?? [];
  const who = list.length === 0 || list.includes(ALL_GRADES) ? 'All grades' : list.join(', ');
  return `${CAMPUS_LABEL[campus]} · ${who}`;
}

/** Sends a reminder to families who haven't paid a collection yet. */
function RemindButton({ id }: { id: number }) {
  const toast = useToast();
  const remind = useMutation({
    mutationFn: () => ok(api.POST('/api/school/collections/{id}/remind', { params: { path: { id } } })),
    onSuccess: (res) => toast(res.message),
    onError: (e) => toast(errorMessage(e), 'bad'),
  });
  return (
    <Button size="sm" variant="secondary" busy={remind.isPending} onClick={() => remind.mutate()}>
      Remind
    </Button>
  );
}

/** Collections with how many families have paid. `compact` drops the money columns for the overview. */
export function CollectionsTable({ rows, compact }: { rows: Schemas['CollectionStats'][]; compact?: boolean }) {
  return (
    <Table
      rows={rows}
      rowKey={(c) => c.id}
      empty="No collections yet."
      columns={[
        {
          header: 'Collection',
          cell: (c) => (
            <div>
              <p className="font-semibold">{c.title}</p>
              <p className="text-ink-3">{audienceLabel(c.campus, c.grades)}</p>
            </div>
          ),
        },
        { header: 'Type', cell: (c) => <Badge tone="blue">{KIND_LABEL[c.kind]}</Badge> },
        ...(compact
          ? []
          : [
              {
                header: 'Amount',
                cell: (c: Schemas['CollectionStats']) => (
                  <div>
                    <Money value={c.amount} />
                    {c.instalments > 1 && <p className="text-ink-3">in {c.instalments} instalments</p>}
                  </div>
                ),
              },
            ]),
        { header: 'Due', cell: (c) => (c.dueDate ? formatDate(c.dueDate) : <span className="text-ink-3">Open</span>) },
        {
          header: 'Paid',
          className: 'w-40',
          cell: (c) => (
            <div className="space-y-1">
              <Progress value={c.percentPaid} tone={c.percentPaid >= 80 ? 'ok' : 'blue'} />
              <p className="text-ink-3">
                {c.paid} of {c.eligible} · {c.percentPaid}%
              </p>
            </div>
          ),
        },
        ...(compact
          ? []
          : [
              { header: 'Collected', cell: (c: Schemas['CollectionStats']) => <Money value={c.collected} className="font-semibold" /> },
              { header: '', cell: (c: Schemas['CollectionStats']) => <RemindButton id={c.id} /> },
            ]),
      ]}
    />
  );
}
