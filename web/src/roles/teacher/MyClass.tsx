import { useMutation, useQuery } from '@tanstack/react-query';
import { ok, percent, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, Page, Progress, QueryView, Stat, Stats, Table } from '../../components/ui';

type Row = Schemas['RosterRow'];

/** The teacher's class: who has done their homework and whose parents have acknowledged notices. */
export function MyClass() {
  const roster = useQuery({ queryKey: ['teacher', 'class'], queryFn: () => ok(api.GET('/api/teacher/class')) });

  return (
    <QueryView query={roster}>
      {(c) => {
        const learners = c.learners ?? [];
        const set = learners.reduce((s, r) => s + r.homeworkSet, 0);
        const done = learners.reduce((s, r) => s + r.homeworkDone, 0);
        const needed = learners.reduce((s, r) => s + r.ackNeeded, 0);
        const acked = learners.reduce((s, r) => s + r.acknowledged, 0);
        const behind = learners.filter((r) => r.homeworkDone < r.homeworkSet || r.acknowledged < r.ackNeeded).length;
        return (
          <Page title={c.className ? `${c.grade} · ${c.className}` : c.grade} intro={`${c.school}. Nudge a parent when their child falls behind.`}>
            <Stats>
              <Stat label="Learners" value={learners.length} />
              <Stat label="Homework done" value={`${percent(done, set)}%`} tone={set > 0 && percent(done, set) < 50 ? 'warn' : undefined} />
              <Stat label="Notices acknowledged" value={needed === 0 ? '—' : `${percent(acked, needed)}%`} />
              <Stat label="Need a nudge" value={behind} tone={behind > 0 ? 'warn' : 'ok'} />
            </Stats>
            <Card title="Class list">
              <Table rows={learners} rowKey={(r) => r.learnerId} empty="No learners in this class yet." columns={columns()} />
            </Card>
          </Page>
        );
      }}
    </QueryView>
  );
}

function columns() {
  return [
    { header: 'Learner', cell: (r: Row) => <span className="font-semibold">{r.name}</span> },
    {
      header: 'Homework done',
      className: 'w-48',
      cell: (r: Row) =>
        r.homeworkSet === 0 ? (
          <span className="text-ink-3">None set</span>
        ) : (
          <div className="space-y-1">
            <Progress value={percent(r.homeworkDone, r.homeworkSet)} tone={r.homeworkDone === r.homeworkSet ? 'ok' : 'blue'} />
            <p className="text-ink-3">
              {r.homeworkDone} of {r.homeworkSet}
            </p>
          </div>
        ),
    },
    {
      header: 'Notices acknowledged',
      cell: (r: Row) =>
        r.ackNeeded === 0 ? (
          <span className="text-ink-3">None needed</span>
        ) : (
          <Badge tone={r.acknowledged === r.ackNeeded ? 'ok' : 'warn'}>
            {r.acknowledged} of {r.ackNeeded}
          </Badge>
        ),
    },
    { header: '', className: 'text-right', cell: (r: Row) => <NudgeButton row={r} /> },
  ];
}

function NudgeButton({ row }: { row: Row }) {
  const toast = useToast();
  const nudge = useMutation({
    mutationFn: () => ok(api.POST('/api/teacher/class/{learnerId}/nudge', { params: { path: { learnerId: row.learnerId } } })),
    onSuccess: (res) => toast(res.message),
    onError: (e) => toast(errorMessage(e), 'bad'),
  });
  const behind = row.homeworkDone < row.homeworkSet || row.acknowledged < row.ackNeeded;
  return (
    <Button size="sm" variant={behind ? 'secondary' : 'ghost'} busy={nudge.isPending} onClick={() => nudge.mutate()}>
      Nudge parent
    </Button>
  );
}
