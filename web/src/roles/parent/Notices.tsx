import { useState } from 'react';
import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { ok, type NoticeView } from '@unischool/api';
import { api } from '../../lib/api';
import { Badge, Button, Card, Empty, Page, QueryView, cx, formatDateTime } from '../../components/ui';
import { ChildSelect, useAction, useLearners } from './shared';

/** School notices for the family: read, confirm per child, RSVP, and pay where linked. */
export function Notices() {
  const learners = useLearners();
  const [learnerId, setLearnerId] = useState<number | undefined>();
  const notices = useQuery({
    queryKey: ['parent', 'notices', learnerId ?? 'all'],
    queryFn: () => ok(api.GET('/api/notices', { params: { query: { learnerId } } })),
  });
  const readAll = useAction(
    () => ok(api.POST('/api/notices/read-all')),
    (res) => {
      const marked = (res as Record<string, number> | undefined)?.marked ?? 0;
      return marked === 0 ? 'Everything was already read.' : `${marked} notice${marked === 1 ? '' : 's'} marked as read.`;
    },
  );
  const unread = notices.data?.filter((n) => !n.read).length ?? 0;

  return (
    <Page
      title="Notices"
      intro="Messages from the school, teachers and coaches."
      actions={
        <>
          <ChildSelect learners={learners.data ?? []} value={learnerId} onChange={setLearnerId} allLabel="All children" />
          <Button variant="secondary" busy={readAll.isPending} disabled={unread === 0} onClick={() => readAll.mutate(undefined)}>
            Mark all as read
          </Button>
        </>
      }
    >
      <QueryView query={notices}>
        {(list) =>
          list.length === 0 ? (
            <Empty>No notices yet.</Empty>
          ) : (
            <div className="space-y-4">
              {list.map((n) => (
                <NoticeCard key={n.id} notice={n} />
              ))}
            </div>
          )
        }
      </QueryView>
    </Page>
  );
}

function NoticeCard({ notice: n }: { notice: NoticeView }) {
  const path = { params: { path: { id: n.id } } };
  const read = useAction(() => ok(api.POST('/api/notices/{id}/read', path)));
  const ack = useAction(
    (learnerId: number) => ok(api.POST('/api/notices/{id}/acknowledge', { ...path, body: { learnerId } })),
    (_, learnerId) => `Thanks, confirmed for ${n.children.find((c) => c.learnerId === learnerId)?.name ?? 'your child'}.`,
  );
  const rsvp = useAction(
    (attending: boolean) => ok(api.POST('/api/notices/{id}/rsvp', { ...path, body: { attending } })),
    (_, attending) => (attending ? "Thanks, the school knows you're coming." : "Thanks, the school knows you can't make it."),
  );

  return (
    <Card className={cx(!n.read && 'border-blue')}>
      <div className="mb-2 flex flex-wrap items-center gap-2">
        {!n.read && <Badge tone="blue">New</Badge>}
        {n.tag && <Badge tone={n.tag === 'Action needed' ? 'warn' : 'neutral'}>{n.tag}</Badge>}
        <span className="text-sm text-ink-3">
          {n.from} · {n.audience} · {formatDateTime(n.publishedAt)}
        </span>
      </div>
      <h2 className="text-lg font-bold">{n.title}</h2>
      <p className="mt-1 whitespace-pre-line text-ink-2">{n.body}</p>
      {n.attachment && <p className="mt-2 text-sm text-ink-2">Attached: {n.attachment}</p>}

      {n.requiresAck && (
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold">Please confirm you've read this:</span>
          {n.children.map((c) =>
            c.acknowledged ? (
              <Badge key={c.learnerId} tone="ok">
                Confirmed for {c.name}
              </Badge>
            ) : (
              <Button key={c.learnerId} size="sm" busy={ack.isPending && ack.variables === c.learnerId} onClick={() => ack.mutate(c.learnerId)}>
                Confirm for {c.name}
              </Button>
            ),
          )}
        </div>
      )}

      {n.rsvpEnabled && (
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold">Are you coming?</span>
          <Button size="sm" variant={n.myRsvp === true ? 'primary' : 'secondary'} busy={rsvp.isPending && rsvp.variables} onClick={() => rsvp.mutate(true)}>
            Yes, we'll be there
          </Button>
          <Button size="sm" variant={n.myRsvp === false ? 'primary' : 'secondary'} busy={rsvp.isPending && !rsvp.variables} onClick={() => rsvp.mutate(false)}>
            Can't make it
          </Button>
        </div>
      )}

      <div className="mt-4 flex flex-wrap gap-2">
        {n.linkedCollectionId && (
          <Link to="../payments" relative="path" className="inline-flex items-center rounded-full bg-blue px-3 py-1.5 text-sm font-semibold text-on-blue hover:opacity-90">
            Pay for this
          </Link>
        )}
        {!n.read && (
          <Button size="sm" variant="ghost" busy={read.isPending} onClick={() => read.mutate(undefined)}>
            Mark as read
          </Button>
        )}
      </div>
    </Card>
  );
}
