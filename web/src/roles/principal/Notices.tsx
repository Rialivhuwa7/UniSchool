import { useQuery } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api } from '../../lib/api';
import { NoticeComposer, NoticeStatsTable } from '../../components/staff/notices';
import { Card, Page, QueryView } from '../../components/ui';

/** Post a notice from the principal's office and see how every published notice is landing. */
export function Notices() {
  const all = useQuery({ queryKey: ['staff-notices', 'all'], queryFn: () => ok(api.GET('/api/staff/notices')) });
  return (
    <Page title="Notices" intro="Your notices go out straight away, signed “Principal's office”.">
      <Card title="New notice">
        <NoticeComposer />
      </Card>
      <Card title="All published notices">
        <QueryView query={all}>{(rows) => <NoticeStatsTable rows={rows} />}</QueryView>
      </Card>
    </Page>
  );
}
