import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CAMPUS_LABEL, gradesFor, ok, TEAMS, type Campus, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, Empty, ErrorAlert, Field, Input, Page, QueryView, Select, Table, formatDateTime } from '../../components/ui';

type Mode = Schemas['ModeRequest']['mode'];

const MODE_LABEL: Record<Mode, string> = {
  OPEN: 'Everyone can post',
  ANNOUNCE: 'Only the moderator posts',
};

/** Parent groups: new groups, who may post in each, and messages parents have reported. */
export function Groups() {
  const [adding, setAdding] = useState(false);
  const groups = useQuery({ queryKey: ['office', 'groups'], queryFn: () => ok(api.GET('/api/school/groups')) });
  const reported = useQuery({ queryKey: ['office', 'groups', 'reported'], queryFn: () => ok(api.GET('/api/school/groups/reported')) });

  return (
    <Page title="Parent groups" intro="Class and team chats for parents." actions={!adding && <Button onClick={() => setAdding(true)}>New parent group</Button>}>
      {adding && (
        <Card title="New parent group">
          <NewGroupForm onDone={() => setAdding(false)} />
        </Card>
      )}

      <Card title="Reported messages">
        <QueryView query={reported}>
          {(rows) =>
            rows.length === 0 ? (
              <Empty>No messages have been reported.</Empty>
            ) : (
              <ul className="divide-y divide-line">
                {rows.map((m) => (
                  <ReportedRow key={m.id} message={m} />
                ))}
              </ul>
            )
          }
        </QueryView>
      </Card>

      <Card title="Groups">
        <QueryView query={groups}>
          {(rows) => (
            <Table
              rows={rows}
              rowKey={(g) => g.id}
              empty="No parent groups yet. Start one for a grade or a team above."
              columns={[
                {
                  header: 'Group',
                  cell: (g) => (
                    <div>
                      <p className="font-semibold">{g.name}</p>
                      <p className="text-ink-3">
                        {CAMPUS_LABEL[g.campus]} · {g.moderator}
                      </p>
                    </div>
                  ),
                },
                { header: 'Members', cell: (g) => g.members },
                { header: 'Reported', cell: (g) => (g.reportedMessages > 0 ? <Badge tone="bad">{g.reportedMessages}</Badge> : <span className="text-ink-3">None</span>) },
                { header: 'Who can post', className: 'w-60', cell: (g) => <ModeSelect group={g} /> },
              ]}
            />
          )}
        </QueryView>
      </Card>
    </Page>
  );
}

/** A group for a grade, a team, or a team within a grade. */
function NewGroupForm({ onDone }: { onDone: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState('');
  const [campus, setCampus] = useState<Campus>('PREP');
  const [grade, setGrade] = useState('');
  const [team, setTeam] = useState('');
  const [moderator, setModerator] = useState('');
  const [mode, setMode] = useState<Mode>('OPEN');

  const create = useMutation({
    mutationFn: () =>
      ok(
        api.POST('/api/school/groups', {
          body: { name: name.trim(), campus, grade: grade || undefined, team: team || undefined, moderatorLabel: moderator.trim() || undefined, mode },
        }),
      ),
    onSuccess: (g) => {
      toast(`${g.name} created.`);
      queryClient.invalidateQueries({ queryKey: ['office', 'groups'] });
      onDone();
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    create.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <ErrorAlert error={create.error} />
      <div className="grid gap-4 md:grid-cols-[1fr_240px]">
        <Field label="Name" hint="e.g. Grade 4 parents, or U13 Hockey parents">
          {(id) => <Input id={id} required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />}
        </Field>
        <Field label="School">
          {(id) => (
            <Select
              id={id}
              value={campus}
              onChange={(e) => {
                setCampus(e.target.value as Campus);
                setGrade('');
              }}
            >
              {(['PREP', 'COLLEGE', 'BOTH'] as Campus[]).map((c) => (
                <option key={c} value={c}>
                  {CAMPUS_LABEL[c]}
                </option>
              ))}
            </Select>
          )}
        </Field>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Grade" hint="Choose a grade, a team, or both">
          {(id) => (
            <Select id={id} value={grade} onChange={(e) => setGrade(e.target.value)}>
              <option value="">Any grade</option>
              {gradesFor(campus).map((g) => (
                <option key={g}>{g}</option>
              ))}
            </Select>
          )}
        </Field>
        <Field label="Team">
          {(id) => (
            <Select id={id} value={team} onChange={(e) => setTeam(e.target.value)}>
              <option value="">No team</option>
              {TEAMS.map((t) => (
                <option key={t}>{t}</option>
              ))}
            </Select>
          )}
        </Field>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Moderator" hint="Who runs the group, e.g. Mrs Naidoo (class teacher). Leave empty for School office.">
          {(id) => <Input id={id} maxLength={80} value={moderator} onChange={(e) => setModerator(e.target.value)} />}
        </Field>
        <Field label="Who can post">
          {(id) => (
            <Select id={id} value={mode} onChange={(e) => setMode(e.target.value as Mode)}>
              {(Object.keys(MODE_LABEL) as Mode[]).map((m) => (
                <option key={m} value={m}>
                  {MODE_LABEL[m]}
                </option>
              ))}
            </Select>
          )}
        </Field>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button type="submit" busy={create.isPending} disabled={!name.trim() || (!grade && !team)}>
          Create group
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  );
}

function ModeSelect({ group }: { group: Schemas['GroupAdminView'] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const change = useMutation({
    mutationFn: (mode: Mode) => ok(api.PUT('/api/school/groups/{id}/mode', { params: { path: { id: group.id } }, body: { mode } })),
    onSuccess: (g) => {
      toast(`${g.name}: ${MODE_LABEL[g.mode].toLowerCase()}.`);
      queryClient.invalidateQueries({ queryKey: ['office', 'groups'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });
  return (
    <Select aria-label={`Who can post in ${group.name}`} value={group.mode} disabled={change.isPending} onChange={(e) => change.mutate(e.target.value as Mode)}>
      {(Object.keys(MODE_LABEL) as Mode[]).map((m) => (
        <option key={m} value={m}>
          {MODE_LABEL[m]}
        </option>
      ))}
    </Select>
  );
}

function ReportedRow({ message: m }: { message: Schemas['ReportedMessage'] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const hide = useMutation({
    mutationFn: () => ok(api.POST('/api/school/groups/messages/{messageId}/hide', { params: { path: { messageId: m.id } } })),
    onSuccess: () => {
      toast('Message hidden.');
      queryClient.invalidateQueries({ queryKey: ['office', 'groups'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });
  return (
    <li className="flex flex-wrap items-start justify-between gap-4 py-4 first:pt-0 last:pb-0">
      <div className="space-y-1">
        <p className="text-sm text-ink-3">
          {m.groupName} · {m.author} · {formatDateTime(m.sentAt)}
        </p>
        <p className="whitespace-pre-line">{m.body}</p>
        <Badge tone="bad">Reported {m.reports === 1 ? 'once' : `${m.reports} times`}</Badge>
      </div>
      <Button size="sm" variant="danger" busy={hide.isPending} onClick={() => hide.mutate()}>
        Hide message
      </Button>
    </li>
  );
}
