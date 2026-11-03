import { useDeferredValue, useState, type FormEvent } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CAMPUS_LABEL, gradesFor, ok, TEAMS, type Campus, type Schemas } from '@unischool/api';
import { api } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, Checkbox, ErrorAlert, Field, Input, Page, QueryView, Select, Table } from '../../components/ui';
import { GradeOptions, SchoolOptions, type School } from './shared';

type Learner = Schemas['EnrolledLearner'];

/** The enrolment record: every learner, a search, and forms to enrol or correct a learner. */
export function Learners() {
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<Learner | null>(null);
  const [q, setQ] = useState('');
  const [campus, setCampus] = useState<School | ''>('');
  const [grade, setGrade] = useState('');
  const search = useDeferredValue(q.trim());
  const filtered = Boolean(search || campus || grade);

  const learners = useQuery({
    queryKey: ['office', 'learners', search, campus, grade],
    queryFn: () =>
      ok(api.GET('/api/school/learners', { params: { query: { q: search || undefined, campus: campus || undefined, grade: grade || undefined } } })),
    placeholderData: keepPreviousData,
  });

  const open = (l: Learner | null) => {
    setAdding(!l);
    setEditing(l);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  const close = () => {
    setAdding(false);
    setEditing(null);
  };

  return (
    <Page
      title="Learners"
      intro="The enrolment record. Parents ask for access with their child's student number, so enrol a learner before their parents sign up."
      actions={!adding && !editing && <Button onClick={() => open(null)}>Enrol a learner</Button>}
    >
      {(adding || editing) && (
        <Card title={editing ? `Edit ${editing.firstName} ${editing.lastName}` : 'Enrol a learner'}>
          <LearnerForm key={editing?.id ?? 'new'} learner={editing} onDone={close} />
        </Card>
      )}

      <Card>
        <div className="mb-4 grid gap-3 md:grid-cols-[1fr_220px_160px]">
          <Input aria-label="Search learners" type="search" placeholder="Search by name or student number" value={q} onChange={(e) => setQ(e.target.value)} />
          <Select
            aria-label="School"
            value={campus}
            onChange={(e) => {
              setCampus(e.target.value as School | '');
              setGrade('');
            }}
          >
            <option value="">Both schools</option>
            <SchoolOptions />
          </Select>
          <Select aria-label="Grade" value={grade} onChange={(e) => setGrade(e.target.value)}>
            <option value="">All grades</option>
            <GradeOptions campus={campus || 'BOTH'} />
          </Select>
        </div>
        <QueryView query={learners}>
          {(rows) => (
            <Table
              rows={rows}
              rowKey={(l) => l.id}
              empty={filtered ? 'No learners match your search.' : 'No learners yet. Enrol the first one above.'}
              columns={[
                { header: 'Student number', cell: (l) => <span className="font-mono">{l.studentNumber}</span> },
                {
                  header: 'Learner',
                  cell: (l) => (
                    <div>
                      <p className="font-semibold">
                        {l.firstName} {l.lastName}
                      </p>
                      <p className="text-ink-3">
                        {l.grade} · {CAMPUS_LABEL[l.campus]}
                      </p>
                    </div>
                  ),
                },
                { header: 'House', cell: (l) => l.house || <span className="text-ink-3">–</span> },
                { header: 'Sport', cell: (l) => ((l.sports ?? []).length ? [...l.sports].sort().join(', ') : <span className="text-ink-3">–</span>) },
                {
                  header: 'Parents',
                  cell: (l) => (l.guardians > 0 ? l.guardians : <Badge tone="warn">None yet</Badge>),
                },
                { header: 'Sign-in', cell: (l) => (l.hasSignIn ? <Badge tone="ok">Has sign-in</Badge> : <span className="text-ink-3">No</span>) },
                {
                  header: '',
                  className: 'text-right',
                  cell: (l) => (
                    <Button size="sm" variant="ghost" onClick={() => open(l)}>
                      Edit
                    </Button>
                  ),
                },
              ]}
            />
          )}
        </QueryView>
      </Card>
    </Page>
  );
}

/** Enrols a new learner, or corrects one. The student number can't change once enrolled. */
function LearnerForm({ learner, onDone }: { learner: Learner | null; onDone: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [studentNumber, setStudentNumber] = useState('');
  const [firstName, setFirstName] = useState(learner?.firstName ?? '');
  const [lastName, setLastName] = useState(learner?.lastName ?? '');
  const [campus, setCampus] = useState<School>(learner?.campus === 'COLLEGE' ? 'COLLEGE' : 'PREP');
  const [grade, setGrade] = useState(learner?.grade ?? gradesFor('PREP')[0]);
  const [house, setHouse] = useState(learner?.house ?? '');
  const [sports, setSports] = useState<string[]>(learner?.sports ?? []);

  // Keep any sport already on the record, even if it isn't one of the usual teams.
  const sportChoices = [...TEAMS, ...sports.filter((s) => !TEAMS.includes(s))];
  const toggle = (s: string) => setSports(sports.includes(s) ? sports.filter((x) => x !== s) : [...sports, s]);

  const save = useMutation({
    mutationFn: () => {
      const details = { firstName: firstName.trim(), lastName: lastName.trim(), grade, campus, house: house.trim() || undefined, sports };
      return learner
        ? ok(api.PUT('/api/school/learners/{id}', { params: { path: { id: learner.id } }, body: details }))
        : ok(api.POST('/api/school/learners', { body: { ...details, studentNumber: studentNumber.trim() } }));
    },
    onSuccess: (l) => {
      toast(learner ? `${l.firstName} ${l.lastName} updated.` : `${l.firstName} ${l.lastName} enrolled with student number ${l.studentNumber}.`);
      queryClient.invalidateQueries({ queryKey: ['office'] });
      onDone();
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    save.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <ErrorAlert error={save.error} />
      <div className="grid gap-4 md:grid-cols-3">
        {learner ? (
          <Field label="Student number" hint="Can't be changed">
            {(id) => <Input id={id} value={learner.studentNumber} disabled />}
          </Field>
        ) : (
          <Field label="Student number" hint="Parents use this to ask for access">
            {(id) => <Input id={id} required maxLength={20} value={studentNumber} onChange={(e) => setStudentNumber(e.target.value)} />}
          </Field>
        )}
        <Field label="First name">{(id) => <Input id={id} required maxLength={60} value={firstName} onChange={(e) => setFirstName(e.target.value)} />}</Field>
        <Field label="Surname">{(id) => <Input id={id} required maxLength={60} value={lastName} onChange={(e) => setLastName(e.target.value)} />}</Field>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Field label="School">
          {(id) => (
            <Select
              id={id}
              value={campus}
              onChange={(e) => {
                const c = e.target.value as School;
                setCampus(c);
                if (!gradesFor(c).includes(grade)) setGrade(gradesFor(c)[0]);
              }}
            >
              <SchoolOptions />
            </Select>
          )}
        </Field>
        <Field label="Grade">
          {(id) => (
            <Select id={id} value={grade} onChange={(e) => setGrade(e.target.value)}>
              <GradeOptions campus={campus as Campus} />
            </Select>
          )}
        </Field>
        <Field label="House" hint="Optional">
          {(id) => <Input id={id} maxLength={40} value={house} onChange={(e) => setHouse(e.target.value)} />}
        </Field>
      </div>

      <fieldset>
        <legend className="mb-1 text-sm font-semibold">Sport</legend>
        <div className="flex flex-wrap gap-x-4 gap-y-1">
          {sportChoices.map((s) => (
            <Checkbox key={s} label={s} checked={sports.includes(s)} onChange={() => toggle(s)} />
          ))}
        </div>
      </fieldset>

      <div className="flex flex-wrap gap-2">
        <Button type="submit" busy={save.isPending} disabled={!firstName.trim() || !lastName.trim() || (!learner && !studentNumber.trim())}>
          {learner ? 'Save changes' : 'Enrol learner'}
        </Button>
        <Button variant="ghost" onClick={onDone}>
          Cancel
        </Button>
      </div>
    </form>
  );
}
