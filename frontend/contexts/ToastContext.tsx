import { createContext, useCallback, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

import { Toast } from '../components/common/Toast';

export type ToastVariant = 'success' | 'error' | 'info';

export interface ToastItem {
  id: string;
  variant: ToastVariant;
  message: string;
}

export interface ToastContextValue {
  showToast: (variant: ToastVariant, message: string) => void;
}

export const ToastContext = createContext<ToastContextValue | undefined>(undefined);

/**
 * Fila de no máximo 1 toast visível por vez (15_DESIGN_SYSTEM_UI_UX.md §F.1)
 * — o próximo espera o atual sair (por timeout ou swipe).
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [queue, setQueue] = useState<ToastItem[]>([]);
  const current = queue[0];

  const showToast = useCallback((variant: ToastVariant, message: string) => {
    setQueue((previous) => [
      ...previous,
      { id: `${Date.now()}-${Math.random()}`, variant, message },
    ]);
  }, []);

  const dismissCurrent = useCallback(() => {
    setQueue((previous) => previous.slice(1));
  }, []);

  const value = useMemo<ToastContextValue>(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      {current ? <Toast key={current.id} item={current} onDismiss={dismissCurrent} /> : null}
    </ToastContext.Provider>
  );
}
