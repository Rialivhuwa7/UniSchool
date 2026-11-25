import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { CAMPUS_LABEL, gradesFor, ok, ROLE_LABEL, type Role, type Schemas } from '@unischool/api';
import { api } from '../lib/api';
import { Alert, Button, ErrorAlert, Field, Input, Select } from '../components/ui';
import { Brand } from '../components/Brand';

type Learner = Schemas['LearnerDetails'];
type School = 'PREP' | 'COLLEGE';

const ROLES: Role[] = ['PARENT', 'LEARNER', 'TEACHER', 'PRINCIPAL', 'SCHOOL_OFFICE', 'TUCKSHOP'];
const blankLearner = (): Learner => ({ studentNumber: '', firstName: '', lastName: '', grade: 'Grade R', campus: 'PREP' });

/**
 * Ask for access, for any role. The school office checks every request before the person can
 * sign in. Parents describe their children and learners describe themselves; anyone the school
 * hasn't added to UniSchool yet is added when the office approves.
 */
export function Register() {
  const [account, setAccount] = useState({ fullName: '', username: '', password: '', role: 'PARENT' as Role, ask: '' });
  const [children, setChildren] = useState<Learner[]>([blankLearner()]);
  const [self, setSelf] = useState<Learner>(blankLearner());
  const [teaching, setTeaching] = useState({ campus: 'PREP' as School, grade: 'Grade R', className: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [done, setDone] = useState<string | null>(null);
  const set = (key: keyof typeof account) => (e: { target: { value: string } }) => setAccount({ ...account, [key]: e.target.value });
  const isParent = account.role === 'PARENT';
  const isLearner = account.role === 'LEARNER';
  const isTeacher = account.role === 'TEACHER';

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const tidy = (l: Learner): Learner => ({ ...l, firstName: l.firstName.trim(), lastName: l.lastName.trim(), studentNumber: l.studentNumber?.trim() || null! });
    try {
      const res = await ok(
        api.POST('/api/auth/register', {
          body: {
            username: account.username.trim(),
            password: account.password,
            fullName: isLearner ? `${self.firstName.trim()} ${self.lastName.trim()}` : account.fullName.trim(),
            role: account.role,
            ask: account.ask.trim(),
            children: isParent ? children.map(tidy) : [],
            learner: isLearner ? tidy(self) : null!,
            teaching: isTeacher ? { ...teaching, className: teaching.className.trim() } : null!,
          },
        }),
      );
      setDone((res as { message?: string } | undefined)?.message ?? 'Thanks. The school will check your request.');
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grid min-h-screen place-items-center px-5 py-10">
      <div className="w-full max-w-xl space-y-6">
        <div className="text-center">
          <Brand size="lg" />
        </div>
        {done ? (
          <div className="space-y-4 rounded-2xl border border-line bg-surface p-6">
            <Alert tone="ok">{done}</Alert>
            <Link to="/login" className="font-semibold text-blue-ink hover:underline">
              Sign in
            </Link>
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-4 rounded-2xl border border-line bg-surface p-6">
            <h1 className="text-xl font-bold">Ask for access</h1>
            <p className="text-sm text-ink-2">The school office checks every request before you can sign in.</p>
            <ErrorAlert error={error} />

            <Field label="I am a">
              {(id) => (
                <Select id={id} value={account.role} onChange={set('role')}>
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {ROLE_LABEL[r]}
                    </option>
                  ))}
                </Select>
              )}
            </Field>

            {isLearner ? (
              <LearnerFields value={self} onChange={setSelf} />
            ) : (
              <Field label="Full name">{(id) => <Input id={id} required value={account.fullName} onChange={set('fullName')} />}</Field>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Username" hint="3 to 40 characters">
                {(id) => <Input id={id} required minLength={3} maxLength={40} autoComplete="username" value={account.username} onChange={set('username')} />}
              </Field>
              <Field label="Password" hint="At least 8 characters">
                {(id) => <Input id={id} type="password" required minLength={8} autoComplete="new-password" value={account.password} onChange={set('password')} />}
              </Field>
            </div>

            {isParent && (
              <fieldset className="space-y-3">
                <legend className="text-sm font-semibold">Your children at the school</legend>
                {children.map((child, i) => (
                  <div key={i} className="space-y-3 rounded-xl border border-line p-4">
                    <div className="flex items-center justify-between">
                      <p className="font-semibold">Child {i + 1}</p>
                      {children.length > 1 && (
                        <Button size="sm" variant="ghost" onClick={() => setChildren(children.filter((_, j) => j !== i))}>
                          Remove
                        </Button>
                      )}
                    </div>
                    <LearnerFields value={child} onChange={(c) => setChildren(children.map((x, j) => (j === i ? c : x)))} />
                  </div>
                ))}
                <Button variant="secondary" size="sm" onClick={() => setChildren([...children, blankLearner()])}>
                  Add another child
                </Button>
              </fieldset>
            )}

            {isTeacher && (
              <div className="grid gap-3 sm:grid-cols-3">
                <Field label="School">
                  {(id) => (
                    <Select id={id} value={teaching.campus} onChange={(e) => setTeaching({ ...teaching, campus: e.target.value as School, grade: gradesFor(e.target.value as School)[0] })}>
                      {(['PREP', 'COLLEGE'] as School[]).map((c) => (
                        <option key={c} value={c}>
                          {CAMPUS_LABEL[c]}
                        </option>
                      ))}
                    </Select>
                  )}
                </Field>
                <Field label="Grade you teach">
                  {(id) => (
                    <Select id={id} value={teaching.grade} onChange={(e) => setTeaching({ ...teaching, grade: e.target.value })}>
                      {gradesFor(teaching.campus).map((g) => (
                        <option key={g}>{g}</option>
                      ))}
                    </Select>
                  )}
                </Field>
                <Field label="Class" hint="Optional, e.g. 4A">
                  {(id) => <Input id={id} maxLength={10} value={teaching.className} onChange={(e) => setTeaching({ ...teaching, className: e.target.value })} />}
                </Field>
              </div>
            )}

            {!isParent && !isLearner && !isTeacher && (
              <Field label="What do you do at the school?" hint="For example: Grade 4 teacher, Prep">
                {(id) => <Input id={id} value={account.ask} onChange={set('ask')} />}
              </Field>
            )}

            <Button type="submit" busy={busy} className="w-full">
              Send request
            </Button>
            <p className="text-center text-sm">
              <Link to="/login" className="font-semibold text-blue-ink hover:underline">
                I already have an account
              </Link>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}

/** Name, school, grade and (optional) student number of one learner. */
function LearnerFields({ value, onChange }: { value: Learner; onChange: (l: Learner) => void }) {
  const update = (patch: Partial<Learner>) => onChange({ ...value, ...patch });
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <Field label="First name">{(id) => <Input id={id} required maxLength={60} value={value.firstName} onChange={(e) => update({ firstName: e.target.value })} />}</Field>
      <Field label="Last name">{(id) => <Input id={id} required maxLength={60} value={value.lastName} onChange={(e) => update({ lastName: e.target.value })} />}</Field>
      <Field label="School">
        {(id) => (
          <Select id={id} value={value.campus} onChange={(e) => update({ campus: e.target.value as School, grade: gradesFor(e.target.value as School)[0] })}>
            {(['PREP', 'COLLEGE'] as School[]).map((c) => (
              <option key={c} value={c}>
                {CAMPUS_LABEL[c]}
              </option>
            ))}
          </Select>
        )}
      </Field>
      <Field label="Grade">
        {(id) => (
          <Select id={id} value={value.grade} onChange={(e) => update({ grade: e.target.value })}>
            {gradesFor(value.campus).map((g) => (
              <option key={g}>{g}</option>
            ))}
          </Select>
        )}
      </Field>
      <div className="sm:col-span-2">
        <Field label="Student number" hint="Optional. It's on the school report, if you have one.">
          {(id) => <Input id={id} maxLength={20} value={value.studentNumber ?? ''} onChange={(e) => update({ studentNumber: e.target.value })} />}
        </Field>
      </div>
    </div>
  );
}
