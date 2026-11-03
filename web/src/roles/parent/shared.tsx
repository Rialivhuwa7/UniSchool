import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ok, type LearnerView } from '@unischool/api';
import { api, errorMessage } from '../../lib/api';
import { useToast } from '../../components/toast';
import { Select } from '../../components/ui';

/*
 * Bits every parent screen uses. All parent data sits under the ['parent'] query key, so an
 * action that moves money can refresh every balance on every screen in one go.
 */

export function useLearners() {
  return useQuery({ queryKey: ['parent', 'learners'], queryFn: () => ok(api.GET('/api/learners')) });
}

export function useMenu() {
  return useQuery({ queryKey: ['parent', 'menu'], queryFn: () => ok(api.GET('/api/tuckshop/menu')), staleTime: 5 * 60_000 });
}

/** useMutation with the usual toast on success or failure, then a refresh of all parent data. */
export function useAction<V, R>(mutationFn: (vars: V) => Promise<R>, success?: string | ((res: R, vars: V) => string)) {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation({
    mutationFn,
    onSuccess: (res, vars) => {
      if (success) toast(typeof success === 'string' ? success : success(res, vars));
      return queryClient.invalidateQueries({ queryKey: ['parent'] });
    },
    onError: (e) => toast(errorMessage(e), 'bad'),
  });
}

/** Rands typed into a text box, or null if it isn't a usable amount. */
export function parseAmount(text: string): number | null {
  const value = Number(text.trim().replace(',', '.'));
  return text.trim() === '' || !Number.isFinite(value) ? null : value;
}

/** Child picker. With `allLabel`, an empty choice means every child. */
export function ChildSelect({
  id,
  learners,
  value,
  onChange,
  allLabel,
}: {
  id?: string;
  learners: LearnerView[];
  value: number | undefined;
  onChange: (id: number | undefined) => void;
  allLabel?: string;
}) {
  return (
    <div className="w-44">
      <Select id={id} value={value ?? ''} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : undefined)}>
        {allLabel && <option value="">{allLabel}</option>}
        {learners.map((l) => (
          <option key={l.id} value={l.id}>
            {l.firstName}
          </option>
        ))}
      </Select>
    </div>
  );
}

/** Weekday and date for a tuckshop day, e.g. "Wednesday 28 Oct". */
export function dayAndDate(iso: string): string {
  return new Date(`${iso}T00:00:00`).toLocaleDateString('en-ZA', { weekday: 'long', day: 'numeric', month: 'short' });
}
