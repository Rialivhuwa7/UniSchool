import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ok, type HomeworkView } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Card, Checkbox, Empty, Page, QueryView, cx, formatDate } from '../../components/ui';

/** "2026-10-28" for today in the browser's time zone, to compare with due dates. */
function today() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** The learner's homework. They tick it off when it's done; their teacher and parents see the tick. */
export function Homework() {
  const homework = useQuery({ queryKey: ['learner', 'homework'], queryFn: () => ok(api.GET('/api/homework')) });

  return (
    <Page title="Homework" intro="Tick it off when you're done. Your teacher and parents can see it.">
      <QueryView query={homework}>
        {(items) => {
          if (items.length === 0) return <Empty>No homework right now.</Empty>;
          const todo = items.filter((h) => !h.done);
          const done = items.filter((h) => h.done);
          return (
            <>
              <Card title={`To do (${todo.length})`}>
                {todo.length === 0 ? <p className="text-ink-2">All done. Well done!</p> : <HomeworkList items={todo} />}
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
        <HomeworkRow key={`${h.id}-${h.learnerId}`} item={h} />
      ))}
    </ul>
  );
}

function HomeworkRow({ item }: { item: HomeworkView }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const overdue = !item.done && item.dueDate < today();

  const toggle = useMutation({
    mutationFn: (done: boolean) => ok(api.PUT('/api/homework/{id}/done', { params: { path: { id: item.id } }, body: { learnerId: item.learnerId, done } })),
    onSuccess: (saved) => {
      queryClient.setQueryData(['learner', 'homework'], (old: HomeworkView[] | undefined) =>
        old?.map((h) => (h.id === saved.id && h.learnerId === saved.learnerId ? saved : h)),
      );
      if (saved.done) toast(`${saved.title}: marked done.`);
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  return (
    <li className="flex flex-wrap items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
      <div className={cx('min-w-0 flex-1', item.done && 'opacity-60')}>
        <div className="flex flex-wrap items-center gap-2">
          <Badge tone="blue">{item.subject}</Badge>
          <span className={cx('text-sm', overdue ? 'font-semibold text-bad' : 'text-ink-3')}>
            {overdue ? 'Overdue · ' : 'Due '}
            {formatDate(item.dueDate)}
          </span>
        </div>
        <p className={cx('mt-1 font-semibold', item.done && 'line-through')}>{item.title}</p>
        {item.body && <p className="mt-1 whitespace-pre-line text-sm text-ink-2">{item.body}</p>}
        <p className="mt-1 text-sm text-ink-3">
          {item.by}
          {item.attachment ? ` · ${item.attachment}` : ''}
        </p>
      </div>
      <Checkbox label="Done" checked={item.done} disabled={toggle.isPending} onChange={(e) => toggle.mutate(e.target.checked)} />
    </li>
  );
}
