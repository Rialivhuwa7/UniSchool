import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ALL_GRADES, CAMPUS_LABEL, gradesFor, ok, STATUS_LABEL, TEAMS, type Campus, type PublishStatus, type Schemas } from '@unischool/api';
import { api } from '../../lib/api';
import { useToast } from '../toast';
import { Badge, Button, Checkbox, ErrorAlert, Field, formatDate, Input, Progress, Select, Table, Textarea } from '../ui';

/*
 * Notices shared by the staff roles: teachers, the principal and the school office all post
 * through POST /api/staff/notices. A teacher's notice may wait for the principal's approval.
 */

export const STATUS_TONE: Record<PublishStatus, 'ok' | 'warn' | 'bad'> = {
  PUBLISHED: 'ok',
  PENDING: 'warn',
  CHANGES_REQUESTED: 'bad',
};

export function StatusBadge({ status }: { status: PublishStatus }) {
  return <Badge tone={STATUS_TONE[status]}>{STATUS_LABEL[status]}</Badge>;
}

/** Form for a new notice. `campuses` limits the choice (a Prep teacher only posts to Prep). */
export function NoticeComposer({ campuses = ['BOTH', 'PREP', 'COLLEGE'], onPosted }: { campuses?: Campus[]; onPosted?: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [campus, setCampus] = useState<Campus>(campuses[0]);
  const [grades, setGrades] = useState<string[]>([ALL_GRADES]);
  const [teams, setTeams] = useState<string[]>([]);
  const [tag, setTag] = useState('Notice');
  const [requiresAck, setRequiresAck] = useState(false);
  const [rsvpEnabled, setRsvpEnabled] = useState(false);

  const post = useMutation({
    mutationFn: () =>
      ok(
        api.POST('/api/staff/notices', {
          body: { title: title.trim(), body: body.trim(), campus, grades, teams, tag, attachmentName: '', requiresAck, rsvpEnabled, linkedCollectionId: null as unknown as number },
        }),
      ),
    onSuccess: (res) => {
      toast(res.message);
      setTitle('');
      setBody('');
      queryClient.invalidateQueries({ queryKey: ['staff-notices'] });
      queryClient.invalidateQueries({ queryKey: ['principal'] });
      onPosted?.();
    },
  });

  const toggle = (list: string[], value: string) => (list.includes(value) ? list.filter((v) => v !== value) : [...list, value]);

  function submit(e: FormEvent) {
    e.preventDefault();
    post.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <ErrorAlert error={post.error} />
      <div className="grid gap-4 md:grid-cols-[1fr_180px]">
        <Field label="Title">{(id) => <Input id={id} required maxLength={140} value={title} onChange={(e) => setTitle(e.target.value)} />}</Field>
        <Field label="Label" hint="Shown on the notice, e.g. Sport, Event">
          {(id) => <Input id={id} value={tag} onChange={(e) => setTag(e.target.value)} />}
        </Field>
      </div>
      <Field label="Message">{(id) => <Textarea id={id} required maxLength={4000} value={body} onChange={(e) => setBody(e.target.value)} />}</Field>

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
              {campuses.map((c) => (
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
              gradesFor(campus).map((g) => <Checkbox key={g} label={g.replace('Grade ', 'Gr ')} checked={grades.includes(g)} onChange={() => setGrades(toggle(grades, g))} />)}
          </div>
        </fieldset>
      </div>

      <fieldset>
        <legend className="mb-1 text-sm font-semibold">Or sports teams</legend>
        <div className="flex flex-wrap gap-x-4 gap-y-1">
          {TEAMS.map((t) => (
            <Checkbox key={t} label={t} checked={teams.includes(t)} onChange={() => setTeams(toggle(teams, t))} />
          ))}
        </div>
      </fieldset>

      <div className="flex flex-wrap gap-x-6 gap-y-2">
        <Checkbox label="Parents must acknowledge" checked={requiresAck} onChange={(e) => setRequiresAck(e.target.checked)} />
        <Checkbox label="Ask parents to RSVP" checked={rsvpEnabled} onChange={(e) => setRsvpEnabled(e.target.checked)} />
      </div>

      <Button type="submit" busy={post.isPending} disabled={grades.length === 0 && teams.length === 0}>
        Post notice
      </Button>
    </form>
  );
}

/** Notices with how many guardians have read and acknowledged them. */
export function NoticeStatsTable({ rows, empty }: { rows: Schemas['NoticeStats'][]; empty?: string }) {
  return (
    <Table
      rows={rows}
      rowKey={(n) => n.id}
      empty={empty ?? 'No notices yet.'}
      columns={[
        {
          header: 'Notice',
          cell: (n) => (
            <div>
              <p className="font-semibold">{n.title}</p>
              <p className="text-ink-3">
                {n.from} · {n.audience}
              </p>
              {n.reviewComment && <p className="mt-1 text-bad">Principal: {n.reviewComment}</p>}
            </div>
          ),
        },
        { header: 'Status', cell: (n) => <StatusBadge status={n.status} /> },
        {
          header: 'Read',
          className: 'w-40',
          cell: (n) => (
            <div className="space-y-1">
              <Progress value={n.readPercent} tone={n.readPercent >= 80 ? 'ok' : n.readPercent < 50 ? 'warn' : 'blue'} />
              <p className="text-ink-3">
                {n.reads} of {n.guardians} · {n.readPercent}%
              </p>
            </div>
          ),
        },
        {
          header: 'Acknowledged',
          className: 'w-32',
          cell: (n) => (n.requiresAck ? `${n.acks} · ${n.ackPercent}%` : <span className="text-ink-3">Not needed</span>),
        },
        { header: 'Date', cell: (n) => formatDate(n.publishedAt ?? n.createdAt) },
      ]}
    />
  );
}
