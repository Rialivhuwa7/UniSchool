import { useQuery } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api } from '../../lib/api';
import { useMe } from '../../lib/auth';
import { NoticeComposer, NoticeStatsTable } from '../../components/staff/notices';
import { Card, Page, QueryView } from '../../components/ui';
import { ResubmitCard } from './Resubmit';

/** Post a notice to the teacher's own school, fix ones the principal sent back, and see who has read them. */
export function Notices() {
  const me = useMe();
  const mine = useQuery({ queryKey: ['staff-notices', 'mine'], queryFn: () => ok(api.GET('/api/staff/notices/mine')) });
  const campus = me.activeRole.campus;

  return (
    <Page title="Notices" intro="Depending on the principal's settings, your notice may wait for approval before parents see it.">
      <Card title="New notice">
        <NoticeComposer campuses={campus ? [campus] : undefined} />
      </Card>
      <QueryView query={mine}>
        {(rows) => (
          <>
            {rows
              .filter((n) => n.status === 'CHANGES_REQUESTED')
              .map((n) => (
                <ResubmitCard key={n.id} kind="notice" id={n.id} title={n.title} body={n.body ?? ''} comment={n.reviewComment} />
              ))}
            <Card title="Your notices">
              <NoticeStatsTable rows={rows} empty="You haven't posted any notices yet." />
            </Card>
          </>
        )}
      </QueryView>
    </Page>
  );
}
