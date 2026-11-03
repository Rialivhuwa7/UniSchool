import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CAMPUS_LABEL, gradesFor, isValidAmount, ok, percent, PLAN_LABEL, type FeePlan, type Schemas } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Badge, Button, Card, Empty, ErrorAlert, Field, Input, Money, Page, Progress, QueryView, Select, Stat, Stats, Table } from '../../components/ui';
import { GradeOptions, SchoolOptions, type School } from './shared';

type GradeFees = Schemas['GradeFees'];

/** Next year's fee per grade, which payment plan families chose, and a nudge for those who haven't. */
export function Fees() {
  const toast = useToast();
  const fees = useQuery({ queryKey: ['office', 'fees'], queryFn: () => ok(api.GET('/api/school/fees')) });
  const remind = useMutation({
    mutationFn: () => ok(api.POST('/api/school/fees/remind')),
    onSuccess: (res) => toast(res.message),
    onError: (e) => toast(errorMessage(e), 'bad'),
  });

  return (
    <Page title="School fees" intro="Set next year's fee for each grade, then see how families have chosen to pay.">
      <QueryView query={fees}>
        {(f) => {
          const rows = Object.entries(f.byPlan ?? {}).map(([plan, count]) => ({ plan, count }));
          const withPlan = f.total - f.withoutPlan;
          return (
            <>
              <Stats>
                <Stat label={`Learners for ${f.year}`} value={f.total} />
                <Stat label="With a plan" value={withPlan} hint={`${percent(withPlan, f.total)}%`} tone={withPlan > 0 ? 'ok' : undefined} />
                <Stat label="No plan yet" value={f.withoutPlan} tone={f.withoutPlan > 0 ? 'warn' : undefined} />
              </Stats>

              {f.withoutPlan > 0 && (
                <Card>
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <p className="text-ink-2">
                      {f.withoutPlan === 1 ? '1 family hasn’t' : `${f.withoutPlan} families haven’t`} chosen a {f.year} fee plan yet.
                    </p>
                    <Button busy={remind.isPending} onClick={() => remind.mutate()}>
                      Send a reminder
                    </Button>
                  </div>
                </Card>
              )}

              <FeeSchedule />

              <Card title={`Plans chosen for ${f.year}`}>
                {f.total === 0 ? (
                  <Empty>No {f.year} fees set yet. Once a grade's fee is set above, families choose how to pay in the app.</Empty>
                ) : (
                  <Table
                    rows={rows}
                    rowKey={(r) => r.plan}
                    columns={[
                      { header: 'Plan', cell: (r) => <span className="font-semibold">{PLAN_LABEL[r.plan as FeePlan] ?? r.plan}</span> },
                      { header: 'Learners', cell: (r) => r.count },
                      {
                        header: 'Share',
                        className: 'w-48',
                        cell: (r) => (
                          <div className="space-y-1">
                            <Progress value={percent(r.count, f.total)} />
                            <p className="text-ink-3">{percent(r.count, f.total)}%</p>
                          </div>
                        ),
                      },
                    ]}
                  />
                )}
              </Card>
            </>
          );
        }}
      </QueryView>
    </Page>
  );
}

/** Next year's annual fee per grade, with a form to set it. Grades where some learners have no fee yet stand out. */
function FeeSchedule() {
  const [picked, setPicked] = useState<GradeFees | null>(null);
  const schedule = useQuery({ queryKey: ['office', 'fees', 'schedule'], queryFn: () => ok(api.GET('/api/school/fees/schedule')) });

  return (
    <QueryView query={schedule}>
      {(s) => {
        const grades = s.grades ?? [];
        return (
          <Card title={`${s.year} fees by grade`}>
            <div className="space-y-5">
              <SetFeeForm key={picked ? `${picked.campus}-${picked.grade}` : 'blank'} year={s.year} initial={picked} />
              {grades.length === 0 ? (
                <Empty>
                  No learners enrolled yet, so there are no grades to set fees for.{' '}
                  <Link to="../learners" className="font-semibold text-blue-ink hover:underline">
                    Enrol learners
                  </Link>{' '}
                  first.
                </Empty>
              ) : (
                <Table
                  rows={grades}
                  rowKey={(g) => `${g.campus}-${g.grade}`}
                  columns={[
                    {
                      header: 'Grade',
                      cell: (g) => (
                        <div>
                          <p className="font-semibold">{g.grade}</p>
                          <p className="text-ink-3">{CAMPUS_LABEL[g.campus]}</p>
                        </div>
                      ),
                    },
                    {
                      header: 'Annual fee',
                      cell: (g) => (g.annualFee == null ? <span className="text-ink-3">Not set</span> : <Money value={g.annualFee} className="font-semibold" />),
                    },
                    {
                      header: 'Learners with a fee',
                      cell: (g) => (
                        <span className="inline-flex flex-wrap items-center gap-2">
                          <span className="tabular-nums">
                            {g.loaded} of {g.learners}
                          </span>
                          {g.loaded < g.learners && <Badge tone="warn">{g.learners - g.loaded} without a fee</Badge>}
                        </span>
                      ),
                    },
                    {
                      header: '',
                      className: 'text-right',
                      cell: (g) => (
                        <Button
                          size="sm"
                          variant={g.loaded < g.learners ? 'primary' : 'ghost'}
                          onClick={() => {
                            setPicked(g);
                            window.scrollTo({ top: 0, behavior: 'smooth' });
                          }}
                        >
                          Set fee
                        </Button>
                      ),
                    },
                  ]}
                />
              )}
            </div>
          </Card>
        );
      }}
    </QueryView>
  );
}

/** Sets the annual fee for every learner in one grade. */
function SetFeeForm({ year, initial }: { year: number; initial: GradeFees | null }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [campus, setCampus] = useState<School>(initial?.campus === 'COLLEGE' ? 'COLLEGE' : 'PREP');
  const [grade, setGrade] = useState(initial?.grade ?? gradesFor('PREP')[0]);
  const [fee, setFee] = useState(initial?.annualFee != null ? String(initial.annualFee) : '');

  const value = Number(fee);
  const feeOk = isValidAmount(value) && value >= 1 && value <= 1_000_000;

  const load = useMutation({
    mutationFn: () => ok(api.POST('/api/school/fees/load', { body: { campus, grade, annualFee: value } })),
    onSuccess: (res) => {
      toast(res.message);
      setFee('');
      queryClient.invalidateQueries({ queryKey: ['office'] });
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    load.mutate();
  }

  return (
    <form onSubmit={submit} className="space-y-3">
      <ErrorAlert error={load.error} />
      <div className="grid items-start gap-4 md:grid-cols-[1fr_160px_200px_auto]">
        <Field label="School">
          {(id) => (
            <Select
              id={id}
              value={campus}
              onChange={(e) => {
                const c = e.target.value as School;
                setCampus(c);
                if (!gradesFor(c).includes(grade)) setGrade(gradesFor(c)[0]);
              }}
            >
              <SchoolOptions />
            </Select>
          )}
        </Field>
        <Field label="Grade">
          {(id) => (
            <Select id={id} value={grade} onChange={(e) => setGrade(e.target.value)}>
              <GradeOptions campus={campus} />
            </Select>
          )}
        </Field>
        <Field label={`${year} annual fee (R)`} error={fee && !feeOk ? 'Enter an amount from R1.00 to R1 000 000.' : undefined}>
          {(id) => <Input id={id} required type="number" min={1} step="0.01" inputMode="decimal" value={fee} onChange={(e) => setFee(e.target.value)} />}
        </Field>
        <Button type="submit" className="md:mt-6" busy={load.isPending} disabled={!feeOk}>
          Set fee
        </Button>
      </div>
      <p className="text-sm text-ink-3">The fee applies to every learner in the grade. Families who already chose a payment plan keep the fee they agreed to.</p>
    </form>
  );
}
