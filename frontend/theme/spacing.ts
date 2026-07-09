/**
 * Espaçamento, grid e raio de borda (15_DESIGN_SYSTEM_UI_UX.md §E) —
 * unidade base 4px.
 */
export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
} as const;

export const radius = {
  sm: 4,
  md: 10,
  lg: 16,
  full: 9999,
} as const;

/** Margem lateral padrão de tela (15 §E) — exceção: ActiveWorkoutScreen usa spacing.sm (Sprint 3). */
export const screenMarginHorizontal = spacing.md;
