/**
 * Tokens de motion (15_DESIGN_SYSTEM_UI_UX.md §G.1) — react-native-reanimated.
 * Toda animação deve checar AccessibilityInfo.isReduceMotionEnabled() antes
 * de disparar (04_FRONTEND_UI_COMPONENTES.md §C.3, generalizado em 15 §G.1).
 */
export const motion = {
  fast: 120,
  base: 220,
  slow: 380,
  /** Corte direto quando "reduzir movimento" está ativo (15 §G.1). */
  reducedMotion: 80,
} as const;

export type MotionToken = keyof typeof motion;
