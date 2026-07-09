import { View, type ViewProps } from 'react-native';

export interface CardProps extends ViewProps {
  className?: string;
}

/** 15_DESIGN_SYSTEM_UI_UX.md §H.3 — hairline de borda é o único recurso de separação visual, sem sombra/elevation. */
export function Card({ className, children, ...viewProps }: CardProps) {
  return (
    <View
      className={`rounded-md border border-border bg-surface p-4 ${className ?? ''}`}
      {...viewProps}
    >
      {children}
    </View>
  );
}
