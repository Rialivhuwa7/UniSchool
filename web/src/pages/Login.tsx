import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router';
import { useAuth } from '../lib/auth';
import { homeOf } from '../roles';
import { Button, ErrorAlert, Field, Input } from '../components/ui';

export function Login() {
  const { me, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  if (me) return <Navigate to={homeOf(me.activeRole.role)} replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const signedIn = await login(username.trim(), password);
      navigate(homeOf(signedIn.activeRole.role), { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grid min-h-screen place-items-center px-5 py-10">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <b className="text-4xl font-extrabold tracking-tight">
            Uni<i className="not-italic text-blue">School</i>
          </b>
          <p className="mt-1 text-ink-2">One app for everything school.</p>
        </div>

        <form onSubmit={submit} className="space-y-4 rounded-2xl border border-line bg-surface p-6">
          <h1 className="text-xl font-bold">Sign in</h1>
          <ErrorAlert error={error} />
          <Field label="Username">
            {(id) => <Input id={id} autoComplete="username" required value={username} onChange={(e) => setUsername(e.target.value)} />}
          </Field>
          <Field label="Password">
            {(id) => <Input id={id} type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />}
          </Field>
          <Button type="submit" busy={busy} className="w-full">
            Sign in
          </Button>
          <p className="text-center text-sm text-ink-2">
            New to UniSchool?{' '}
            <Link to="/register" className="font-semibold text-blue-ink hover:underline">
              Ask for access
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
