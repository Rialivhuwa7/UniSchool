import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ok, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, Empty, Field, Input, Page, QueryView, Textarea, formatDateTime } from '../../components/ui';

type Item = Schemas['ApprovalItem'];
type Mode = 'view' | 'changes' | 'edit';

/** Teachers' notices and homework waiting for the principal: approve, ask for changes, or fix and approve. */
export function Approvals() {
  const approvals = useQuery({ queryKey: ['principal', 'approvals'], queryFn: () => ok(api.GET('/api/principal/approvals')) });
  return (
    <Page title="Approvals" intro="Teachers' notices and homework only reach parents once you approve them.">
      <QueryView query={approvals}>
        {(items) =>
          items.length === 0 ? (
            <Empty>Nothing is waiting for approval.</Empty>
          ) : (
            <div className="space-y-4">
              {items.map((item) => (
                <ApprovalCard key={`${item.type}-${item.id}`} item={item} />
              ))}
            </div>
          )
        }
      </QueryView>
    </Page>
  );
}

function ApprovalCard({ item }: { item: Item }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [mode, setMode] = useState<Mode>('view');
  const [comment, setComment] = useState('');
  const [title, setTitle] = useState(item.title);
  const [body, setBody] = useState(item.body);
  const isNotice = item.type === 'NOTICE';
  const path = { params: { path: { id: item.id } } };

  const act = useMutation({
    mutationFn: async (action: 'approve' | 'changes' | 'edit') => {
      if (action === 'approve') {
        return ok(isNotice ? api.POST('/api/principal/approvals/notices/{id}/approve', path) : api.POST('/api/principal/approvals/homework/{id}/approve', path));
      }
      if (action === 'changes') {
        const req = { ...path, body: { comment: comment.trim() } };
        return ok(
          isNotice
            ? api.POST('/api/principal/approvals/notices/{id}/request-changes', req)
            : api.POST('/api/principal/approvals/homework/{id}/request-changes', req),
        );
      }
      const req = { ...path, body: { title: title.trim(), body: body.trim() } };
      return ok(isNotice ? api.PUT('/api/principal/approvals/notices/{id}', req) : api.PUT('/api/principal/approvals/homework/{id}', req));
    },
    onSuccess: (res) => {
      toast(res.message);
      queryClient.invalidateQueries({ queryKey: ['principal'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  return (
    <Card>
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <Badge tone="blue">{isNotice ? 'Notice' : 'Homework'}</Badge>
        <span className="text-sm text-ink-3">
          {item.from} · {item.audience} · sent {formatDateTime(item.submittedAt)}
        </span>
      </div>

      {mode === 'edit' ? (
        <div className="space-y-3">
          <Field label="Title">{(id) => <Input id={id} value={title} onChange={(e) => setTitle(e.target.value)} />}</Field>
          <Field label="Message">{(id) => <Textarea id={id} value={body} onChange={(e) => setBody(e.target.value)} />}</Field>
        </div>
      ) : (
        <>
          <h2 className="text-lg font-bold">{item.title}</h2>
          <p className="mt-1 whitespace-pre-line text-ink-2">{item.body}</p>
        </>
      )}

      {mode === 'changes' && (
        <div className="mt-4">
          <Field label="What should the teacher change?">
            {(id) => <Textarea id={id} rows={2} value={comment} onChange={(e) => setComment(e.target.value)} />}
          </Field>
        </div>
      )}

      <div className="mt-4 flex flex-wrap gap-2">
        {mode === 'view' && (
          <>
            <Button busy={act.isPending && act.variables === 'approve'} onClick={() => act.mutate('approve')}>
              Approve
            </Button>
            <Button variant="secondary" onClick={() => setMode('changes')}>
              Ask for changes
            </Button>
            <Button variant="ghost" onClick={() => setMode('edit')}>
              Edit and approve
            </Button>
          </>
        )}
        {mode === 'changes' && (
          <>
            <Button busy={act.isPending} disabled={!comment.trim()} onClick={() => act.mutate('changes')}>
              Send to teacher
            </Button>
            <Button variant="ghost" onClick={() => setMode('view')}>
              Cancel
            </Button>
          </>
        )}
        {mode === 'edit' && (
          <>
            <Button busy={act.isPending} disabled={!title.trim() || !body.trim()} onClick={() => act.mutate('edit')}>
              Save and approve
            </Button>
            <Button variant="ghost" onClick={() => setMode('view')}>
              Cancel
            </Button>
          </>
        )}
      </div>
    </Card>
  );
}
