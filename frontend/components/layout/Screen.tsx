import type { ReactNode } from 'react';
import type { Edge } from 'react-native-safe-area-context';
import { SafeAreaView } from 'react-native-safe-area-context';

export interface ScreenProps {
  children: ReactNode;
  edges?: Edge[];
  className?: string;
}

/**
 * Wrapper raiz de toda tela. Usa o `SafeAreaView` de
 * `react-native-safe-area-context` (não o componente legado de
 * `react-native`, que é no-op no Android) para garantir que nenhum conteúdo
 * fique sob a status bar/notch/câmera (topo) nem sob a barra de
 * gestos/home indicator do SO (base) — todo `headerShown` do React
 * Navigation está desligado (navigation/*Stack.tsx), então nenhuma tela
 * recebe inset automaticamente sem este wrapper.
 */
export function Screen({
  children,
  edges = ['top', 'bottom', 'left', 'right'],
  className,
}: ScreenProps) {
  return (
    <SafeAreaView edges={edges} className={className ?? 'flex-1 bg-bg'}>
      {children}
    </SafeAreaView>
  );
}
