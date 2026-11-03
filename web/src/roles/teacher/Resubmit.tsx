import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Alert, Badge, Button, Card, Field, Input, Textarea } from '../../components/ui';

/** A notice or homework the principal sent back: shows their comment and lets the teacher fix and resubmit. */
export function ResubmitCard({ kind, id, title, body, comment }: { kind: 'notice' | 'homework'; id: number; title: string; body: string; comment: string | null }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [editing, setEditing] = useState(false);
  const [newTitle, setNewTitle] = useState(title);
  const [newBody, setNewBody] = useState(body);

  const resubmit = useMutation({
    mutationFn: () => {
      const req = { params: { path: { id } }, body: { title: newTitle.trim(), body: newBody.trim() } };
      return ok(kind === 'notice' ? api.PUT('/api/staff/notices/{id}', req) : api.PUT('/api/teacher/homework/{id}', req));
    },
    onSuccess: (res) => {
      toast(res.message);
      setEditing(false);
      queryClient.invalidateQueries({ queryKey: kind === 'notice' ? ['staff-notices'] : ['teacher', 'homework'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  return (
    <Card>
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <Badge tone="bad">Changes requested</Badge>
        <span className="text-sm text-ink-3">{kind === 'notice' ? 'Notice' : 'Homework'}</span>
      </div>
      {comment && <Alert tone="warn">Principal: {comment}</Alert>}
      {editing ? (
        <div className="mt-4 space-y-3">
          <Field label="Title">{(fid) => <Input id={fid} value={newTitle} maxLength={200} onChange={(e) => setNewTitle(e.target.value)} />}</Field>
          <Field label="Message">{(fid) => <Textarea id={fid} value={newBody} onChange={(e) => setNewBody(e.target.value)} />}</Field>
          <div className="flex flex-wrap gap-2">
            <Button busy={resubmit.isPending} disabled={!newTitle.trim() || !newBody.trim()} onClick={() => resubmit.mutate()}>
              Resubmit
            </Button>
            <Button variant="ghost" onClick={() => setEditing(false)}>
              Cancel
            </Button>
          </div>
        </div>
      ) : (
        <div className="mt-4">
          <h3 className="text-lg font-bold">{title}</h3>
          <p className="mt-1 whitespace-pre-line text-ink-2">{body}</p>
          <Button className="mt-4" variant="secondary" onClick={() => setEditing(true)}>
            Fix and resubmit
          </Button>
        </div>
      )}
    </Card>
  );
}
