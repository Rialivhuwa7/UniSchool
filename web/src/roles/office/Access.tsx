import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ok, ROLE_LABEL, type Role, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, Empty, Field, Input, Page, QueryView, Select, Table, formatDateTime } from '../../components/ui';
import { GradeOptions, SchoolOptions, type School } from './shared';

/** People asking to join as parents or staff, and the staff who already have access. */
export function Access() {
  const requests = useQuery({ queryKey: ['office', 'access', 'requests'], queryFn: () => ok(api.GET('/api/school/access/requests')) });
  const staff = useQuery({ queryKey: ['office', 'access', 'staff'], queryFn: () => ok(api.GET('/api/school/access/staff')) });

  return (
    <Page title="Access" intro="Check who people are before they can see learners' information.">
      <Card title="Waiting for you">
        <QueryView query={requests}>
          {(rows) =>
            rows.length === 0 ? (
              <Empty>No one is waiting for access.</Empty>
            ) : (
              <ul className="divide-y divide-line">
                {rows.map((r) => (
                  <RequestRow key={r.id} request={r} />
                ))}
              </ul>
            )
          }
        </QueryView>
      </Card>

      <Card title="Staff with access">
        <QueryView query={staff}>
          {(rows) => (
            <Table
              rows={rows}
              rowKey={(s) => `${s.name}-${s.role}`}
              empty="No staff have access yet. Approve their requests above once you've checked who they are."
              columns={[
                { header: 'Name', cell: (s) => <span className="font-semibold">{s.name}</span> },
                { header: 'Role', cell: (s) => s.role },
                { header: 'Access', cell: (s) => <span className="text-ink-3">{s.since}</span> },
              ]}
            />
          )}
        </QueryView>
      </Card>
    </Page>
  );
}

/**
 * One request. Teachers need a school and grade before they can be approved, so they see what
 * class they'll have. Declining asks for a second click because it also switches the account off.
 */
function RequestRow({ request: r }: { request: Schemas['AccessRequestView'] }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [confirming, setConfirming] = useState(false);
  const isTeacher = r.requestedRole === 'TEACHER';
  const [campus, setCampus] = useState<School>('PREP');
  const [grade, setGrade] = useState('');
  const [className, setClassName] = useState('');
  const path = { params: { path: { id: r.id } } };

  const decide = useMutation({
    mutationFn: (action: 'approve' | 'decline') =>
      ok(
        action === 'decline'
          ? api.POST('/api/school/access/requests/{id}/decline', path)
          : isTeacher
            ? api.POST('/api/school/access/requests/{id}/approve', { ...path, body: { campus, grade, className: className.trim() } })
            : api.POST('/api/school/access/requests/{id}/approve', path),
      ),
    onSuccess: (res) => {
      toast(res.message);
      queryClient.invalidateQueries({ queryKey: ['office', 'access'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  return (
    <li className="flex flex-wrap items-start justify-between gap-4 py-4 first:pt-0 last:pb-0">
      <div className="space-y-1">
        <div className="flex flex-wrap items-center gap-2">
          <p className="font-semibold">{r.name}</p>
          <Badge tone="blue">{ROLE_LABEL[r.requestedRole as Role] ?? r.requestedRole}</Badge>
        </div>
        <p>{r.ask}</p>
        {(r.learners ?? []).length > 0 && (
          <ul className="list-inside list-disc text-sm text-ink-2">
            {r.learners.map((l) => (
              <li key={l}>{l}</li>
            ))}
          </ul>
        )}
        <p className="text-sm text-ink-3">
          {r.how} · @{r.username} · {formatDateTime(r.requestedAt)}
        </p>
      </div>
      {isTeacher && !confirming && (
        <fieldset className="grid w-full max-w-2xl gap-3 rounded-xl bg-surface-2 p-3 sm:grid-cols-[1fr_140px_120px]">
          <legend className="sr-only">What {r.name} teaches</legend>
          <Field label="School">
            {(id) => (
              <Select
                id={id}
                value={campus}
                onChange={(e) => {
                  setCampus(e.target.value as School);
                  setGrade('');
                }}
              >
                <SchoolOptions />
              </Select>
            )}
          </Field>
          <Field label="Grade">
            {(id) => (
              <Select id={id} value={grade} onChange={(e) => setGrade(e.target.value)}>
                <option value="">Choose</option>
                <GradeOptions campus={campus} />
              </Select>
            )}
          </Field>
          <Field label="Class" hint="Optional">
            {(id) => <Input id={id} maxLength={10} placeholder="e.g. 4A" value={className} onChange={(e) => setClassName(e.target.value)} />}
          </Field>
        </fieldset>
      )}
      <div className="flex flex-wrap gap-2">
        {confirming ? (
          <>
            <Button variant="danger" size="sm" busy={decide.isPending} onClick={() => decide.mutate('decline')}>
              Yes, decline
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setConfirming(false)}>
              Keep
            </Button>
          </>
        ) : (
          <>
            <Button
              size="sm"
              busy={decide.isPending}
              disabled={isTeacher && !grade}
              title={isTeacher && !grade ? 'Choose the grade they teach first' : undefined}
              onClick={() => decide.mutate('approve')}
            >
              Approve
            </Button>
            <Button variant="secondary" size="sm" disabled={decide.isPending} onClick={() => setConfirming(true)}>
              Decline
            </Button>
          </>
        )}
      </div>
    </li>
  );
}
