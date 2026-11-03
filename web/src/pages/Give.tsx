import { useState, type FormEvent } from 'react';
import { useParams } from 'react-router';
import { useMutation, useQuery } from '@tanstack/react-query';
import { formatRand, ok } from '@unischool/api';
import { api } from '../lib/api';
import { Alert, Button, ErrorAlert, Field, Input, QueryView } from '../components/ui';

/**
 * Public page behind a parent's share link (/give/<token>). Family and friends add to the
 * children's back-to-school pot without an account; they see nothing else about the family.
 */
export function Give() {
  const { token = '' } = useParams();
  const page = useQuery({ queryKey: ['give', token], queryFn: () => ok(api.GET('/api/give/{token}', { params: { path: { token } } })) });
  const [fromName, setFromName] = useState('');
  const [amount, setAmount] = useState('');

  const give = useMutation({
    mutationFn: () => ok(api.POST('/api/give/{token}', { params: { path: { token } }, body: { fromName: fromName.trim(), amount: Number(amount) } })),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    give.mutate();
  }

  return (
    <div className="grid min-h-screen place-items-center px-5 py-10">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <b className="text-4xl font-extrabold tracking-tight">
            Uni<i className="not-italic text-blue">School</i>
          </b>
        </div>
        <div className="space-y-4 rounded-2xl border border-line bg-surface p-6">
          <QueryView query={page}>
            {(p) =>
              give.isSuccess ? (
                <Alert tone="ok">
                  Thank you, {fromName.trim()}! {formatRand(Number(amount))} is on its way to {p.forFamily}.
                </Alert>
              ) : (
                <form onSubmit={submit} className="space-y-4">
                  <h1 className="text-xl font-bold">A gift for {p.forFamily}</h1>
                  <p className="text-ink-2">{p.message}</p>
                  <ErrorAlert error={give.error} />
                  <Field label="Your name" hint="The family sees this with your gift.">
                    {(id) => <Input id={id} required maxLength={60} value={fromName} onChange={(e) => setFromName(e.target.value)} />}
                  </Field>
                  <Field label="Amount (R)" hint="Between R10 and R20 000.">
                    {(id) => (
                      <Input id={id} type="number" required min={10} max={20000} step="0.01" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
                    )}
                  </Field>
                  <Button type="submit" busy={give.isPending} className="w-full">
                    Give {amount ? formatRand(Number(amount)) : ''}
                  </Button>
                </form>
              )
            }
          </QueryView>
        </div>
      </div>
    </div>
  );
}
