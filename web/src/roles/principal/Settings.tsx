import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ok, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Card, Checkbox, Page, QueryView } from '../../components/ui';

export function Settings() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const settings = useQuery({ queryKey: ['principal', 'settings'], queryFn: () => ok(api.GET('/api/principal/settings')) });

  const update = useMutation({
    mutationFn: (body: Schemas['SettingsUpdate']) => ok(api.PATCH('/api/principal/settings', { body })),
    onSuccess: (saved) => {
      queryClient.setQueryData(['principal', 'settings'], saved);
      queryClient.invalidateQueries({ queryKey: ['principal', 'dashboard'] });
      toast('Settings saved.');
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  return (
    <Page title="Settings">
      <Card title="Approval before publishing">
        <QueryView query={settings}>
          {(s) => (
            <div className="space-y-3">
              <p className="text-ink-2">When this is on, teachers' posts wait for you before parents see them.</p>
              <div className="flex flex-col gap-2">
                <Checkbox
                  label="Approve teachers' notices"
                  checked={s.approveNotices}
                  disabled={update.isPending}
                  onChange={(e) => update.mutate({ ...s, approveNotices: e.target.checked })}
                />
                <Checkbox
                  label="Approve teachers' homework"
                  checked={s.approveHomework}
                  disabled={update.isPending}
                  onChange={(e) => update.mutate({ ...s, approveHomework: e.target.checked })}
                />
              </div>
            </div>
          )}
        </QueryView>
      </Card>
    </Page>
  );
}
