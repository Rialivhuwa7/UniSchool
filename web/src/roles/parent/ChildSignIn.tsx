import { useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ok, type LearnerView } from '@unischool/api';
import { api } from '../../lib/api';
import { Button, Card, Field, Input, QueryView } from '../../components/ui';
import { useAction } from './shared';

/**
 * Learners don't sign up themselves; their parent makes the sign-in. With it the child sees
 * their card, savings goal, homework and tuckshop orders on their own phone.
 */
export function ChildSignIn({ learner }: { learner: LearnerView }) {
  const id = learner.id;
  const signIn = useQuery({
    queryKey: ['parent', 'sign-in', id],
    queryFn: () => ok(api.GET('/api/learners/{id}/sign-in', { params: { path: { id } } })),
  });
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const create = useAction(
    () => ok(api.POST('/api/learners/{id}/sign-in', { params: { path: { id } }, body: { username: username.trim(), password } })),
    `${learner.firstName} can sign in now.`,
  );

  function submit(e: FormEvent) {
    e.preventDefault();
    create.mutate(undefined, { onSuccess: () => setPassword('') });
  }

  return (
    <Card title={`${learner.firstName}'s own sign-in`}>
      <QueryView query={signIn}>
        {(s) =>
          s.username ? (
            <p className="text-ink-2">
              {learner.firstName} signs in as <b className="text-ink">{s.username}</b>.
            </p>
          ) : (
            <form onSubmit={submit} className="space-y-4">
              <p className="text-ink-2">
                Give {learner.firstName} their own sign-in to see their card, savings goal, homework and tuckshop orders. Your card settings
                still apply.
              </p>
              <div className="grid gap-4 md:grid-cols-[1fr_1fr_auto] md:items-end">
                <Field label="Username" hint="3 to 40 characters">
                  {(fid) => <Input id={fid} required minLength={3} maxLength={40} autoComplete="off" value={username} onChange={(e) => setUsername(e.target.value)} />}
                </Field>
                <Field label="Password" hint="At least 8 characters">
                  {(fid) => <Input id={fid} type="password" required minLength={8} autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />}
                </Field>
                <Button type="submit" busy={create.isPending}>
                  Create sign-in
                </Button>
              </div>
            </form>
          )
        }
      </QueryView>
    </Card>
  );
}
