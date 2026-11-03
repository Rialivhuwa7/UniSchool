import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ok, type HomeworkView } from '@unischool/api';
import { api } from '../../lib/api';
import { Badge, Card, Checkbox, Empty, Page, QueryView, formatDate } from '../../components/ui';
import { ChildSelect, useAction, useLearners } from './shared';

/** Homework set for the children. Ticking it off lets the teacher see it's done. */
export function Homework() {
  const learners = useLearners();
  const [learnerId, setLearnerId] = useState<number | undefined>();
  const homework = useQuery({
    queryKey: ['parent', 'homework', learnerId ?? 'all'],
    queryFn: () => ok(api.GET('/api/homework', { params: { query: { learnerId } } })),
  });

  return (
    <Page
      title="Homework"
      intro="Tick off homework once it's done."
      actions={<ChildSelect learners={learners.data ?? []} value={learnerId} onChange={setLearnerId} allLabel="All children" />}
    >
      <QueryView query={homework}>
        {(list) => {
          const todo = list.filter((h) => !h.done).sort((a, b) => a.dueDate.localeCompare(b.dueDate));
          const done = list.filter((h) => h.done);
          return (
            <>
              <Card title={`To do (${todo.length})`}>
                {todo.length === 0 ? <Empty>All caught up.</Empty> : <HomeworkList items={todo} />}
              </Card>
              {done.length > 0 && (
                <Card title={`Done (${done.length})`}>
                  <HomeworkList items={done} />
                </Card>
              )}
            </>
          );
        }}
      </QueryView>
    </Page>
  );
}

function HomeworkList({ items }: { items: HomeworkView[] }) {
  return (
    <ul className="divide-y divide-line">
      {items.map((h) => (
        <HomeworkItem key={`${h.id}-${h.learnerId}`} hw={h} />
      ))}
    </ul>
  );
}

function HomeworkItem({ hw }: { hw: HomeworkView }) {
  const done = useAction(
    (value: boolean) => ok(api.PUT('/api/homework/{id}/done', { params: { path: { id: hw.id } }, body: { learnerId: hw.learnerId, done: value } })),
    (_, value) => (value ? `Well done, ${hw.learnerName}!` : 'Marked as not done.'),
  );
  const overdue = !hw.done && hw.dueDate < new Date().toLocaleDateString('en-CA');

  return (
    <li className="flex flex-wrap items-start justify-between gap-3 py-3">
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2 text-sm text-ink-3">
          <Badge tone="blue">{hw.learnerName}</Badge>
          <span>
            {hw.subject} · {hw.by}
          </span>
          <Badge tone={overdue ? 'bad' : 'neutral'}>Due {formatDate(hw.dueDate)}</Badge>
        </div>
        <p className="mt-1 font-semibold">{hw.title}</p>
        {hw.body && <p className="text-ink-2">{hw.body}</p>}
        {hw.attachment && <p className="mt-1 text-sm text-ink-3">Attached: {hw.attachment}</p>}
      </div>
      <Checkbox label="Done" checked={hw.done} disabled={done.isPending} onChange={(e) => done.mutate(e.target.checked)} />
    </li>
  );
}
