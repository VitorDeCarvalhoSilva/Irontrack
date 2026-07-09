/**
 * Paleta de cores (15_DESIGN_SYSTEM_UI_UX.md §B, ADR-019) — dark-only,
 * monocromática, um único acento (vermelho neon). Espelha exatamente os
 * valores hex/rgba do documento; não invente um valor diferente daqui.
 */
export const colors = {
  bg: '#0A0A0C',
  bgElevated: '#131316',
  surface: '#1A1A1E',
  surfaceAlt: '#202024',
  border: '#2A2A2F',
  borderStrong: '#38383E',
  textPrimary: '#F2F2F0',
  textSecondary: '#9C9CA3',
  textMuted: '#6B6B72',
  textDisabled: '#46464C',

  accent: '#FF1440',
  accentGlowSoft: 'rgba(255, 20, 64, 0.35)',
  accentGlowStrong: 'rgba(255, 42, 77, 0.55)',
  accentDim: 'rgba(255, 20, 64, 0.16)',
} as const;

export type ColorToken = keyof typeof colors;
