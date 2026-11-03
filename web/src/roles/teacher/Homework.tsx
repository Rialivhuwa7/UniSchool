import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ok } from '@unischool/api';
import { api } from '../../lib/api';
import { useMe } from '../../lib/auth';
import { useToast } from '../../components/toast';
import { StatusBadge } from '../../components/staff/notices';
import { Button, Card, ErrorAlert, Field, Input, Page, Progress, QueryView, Table, Textarea, formatDate } from '../../components/ui';
import { ResubmitCard } from './Resubmit';

const SUBJECTS = ['Mathematics', 'English', 'Afrikaans', 'isiZulu', 'Natural Sciences', 'Social Sciences', 'Life Skills', 'Technology', 'Creative Arts'];

/** "2026-10-28" for a date in the browser's time zone. */
function isoDay(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Set homework for the teacher's class and see how many have finished it. */
export function Homework() {
  const me = useMe();
  const mine = useQuery({ queryKey: ['teacher', 'homework'], queryFn: () => ok(api.GET('/api/teacher/homework')) });

  return (
    <Page title="Homework" intro={`Homework goes to ${me.activeRole.grade ?? 'your class'} learners and their parents.`}>
      <Card title="Set homework">
        <HomeworkForm />
      </Card>
      <QueryView query={mine}>
        {(rows) => {
          const sentBack = rows.filter((h) => h.status === 'CHANGES_REQUESTED');
          return (
            <>
              {sentBack.length > 0 && (
                <div className="space-y-4">
                  {sentBack.map((h) => (
                    <ResubmitCard key={h.id} kind="homework" id={h.id} title={h.title} body={h.body ?? ''} comment={h.reviewComment} />
                  ))}
                </div>
              )}
              <Card title="Your homework">
                <Table
                  rows={rows}
                  rowKey={(h) => h.id}
                  empty="You haven't set any homework yet."
                  columns={[
                    {
                      header: 'Homework',
                      cell: (h) => (
                        <div>
                          <p className="font-semibold">{h.title}</p>
                          <p className="text-ink-3">
                            {h.subject} · {h.grade}
                            {h.attachment ? ` · ${h.attachment}` : ''}
                          </p>
                          {h.reviewComment && h.status !== 'PUBLISHED' && <p className="mt-1 text-bad">Principal: {h.reviewComment}</p>}
                        </div>
                      ),
                    },
                    { header: 'Status', cell: (h) => <StatusBadge status={h.status} /> },
                    { header: 'Due', cell: (h) => formatDate(h.dueDate) },
                    {
                      header: 'Done',
                      className: 'w-40',
                      cell: (h) =>
                        h.status === 'PUBLISHED' ? (
                          <div className="space-y-1">
                            <Progress value={h.percentDone} tone={h.percentDone >= 80 ? 'ok' : 'blue'} />
                            <p className="text-ink-3">
                              {h.done} of {h.total}
                            </p>
                          </div>
                        ) : (
                          <span className="text-ink-3">Not sent yet</span>
                        ),
                    },
                  ]}
                />
              </Card>
            </>
          );
        }}
      </QueryView>
    </Page>
  );
}

function HomeworkForm() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const [subject, setSubject] = useState('');
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [dueDate, setDueDate] = useState(isoDay(tomorrow));
  const [attachmentName, setAttachmentName] = useState('');

  const post = useMutation({
    mutationFn: () =>
      ok(
        api.POST('/api/teacher/homework', {
          body: { subject: subject.trim(), title: title.trim(), body: body.trim(), dueDate, attachmentName: attachmentName.trim() || undefined },
        }),
      ),
    onSuccess: (res) => {
      toast(res.message);
      setTitle('');
      setBody('');
      setAttachmentName('');
      queryClient.invalidateQueries({ queryKey: ['teacher'] });
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    post.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <ErrorAlert error={post.error} />
      <div className="grid gap-4 md:grid-cols-[220px_1fr_180px]">
        <Field label="Subject">
          {(id) => (
            <>
              <Input id={id} required list="hw-subjects" value={subject} onChange={(e) => setSubject(e.target.value)} />
              <datalist id="hw-subjects">
                {SUBJECTS.map((s) => (
                  <option key={s} value={s} />
                ))}
              </datalist>
            </>
          )}
        </Field>
        <Field label="What to do">{(id) => <Input id={id} required maxLength={200} value={title} onChange={(e) => setTitle(e.target.value)} />}</Field>
        <Field label="Due">{(id) => <Input id={id} type="date" required min={isoDay(new Date())} value={dueDate} onChange={(e) => setDueDate(e.target.value)} />}</Field>
      </div>
      <Field label="Instructions" hint="Optional. Leave blank if you explained it in class.">
        {(id) => <Textarea id={id} rows={3} maxLength={2000} value={body} onChange={(e) => setBody(e.target.value)} />}
      </Field>
      <Field label="Worksheet name" hint="Optional, e.g. Fractions worksheet 3.pdf">
        {(id) => <Input id={id} value={attachmentName} onChange={(e) => setAttachmentName(e.target.value)} />}
      </Field>
      <Button type="submit" busy={post.isPending}>
        Set homework
      </Button>
    </form>
  );
}
