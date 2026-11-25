import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { cx } from './ui';

type Tone = 'ok' | 'bad';
interface ToastItem {
  id: number;
  tone: Tone;
  text: string;
}

const ToastContext = createContext<(text: string, tone?: Tone) => void>(() => {});

/** Short confirmations after an action ("R100.00 added to the wallet."). Disappear after 4s. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);

  const show = useCallback((text: string, tone: Tone = 'ok') => {
    const id = Date.now() + Math.random();
    setItems((all) => [...all, { id, tone, text }]);
    setTimeout(() => setItems((all) => all.filter((t) => t.id !== id)), 4000);
  }, []);

  return (
    <ToastContext.Provider value={show}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-[calc(5rem+env(safe-area-inset-bottom))] z-50 flex md:bottom-4 flex-col items-center gap-2 px-4" aria-live="polite">
        {items.map((t) => (
          <div
            key={t.id}
            className={cx('pointer-events-auto max-w-md rounded-xl px-4 py-3 text-sm font-semibold shadow-lg', t.tone === 'ok' ? 'bg-ink text-paper' : 'bg-bad text-white')}
          >
            {t.text}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

/** const toast = useToast(); toast('Saved.'); toast(errorMessage(e), 'bad'); */
export function useToast() {
  return useContext(ToastContext);
}
