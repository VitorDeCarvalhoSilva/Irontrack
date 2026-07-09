/**
 * Escala tipográfica (15_DESIGN_SYSTEM_UI_UX.md §C.1) — Oswald (display) +
 * Inter (corpo), carregadas via @expo-google-fonts/* (Seção J). Os nomes de
 * fontFamily abaixo são exatamente os exportados por useFonts em App.tsx.
 */
export const fontFamily = {
  oswaldMedium: 'Oswald_500Medium',
  oswaldBold: 'Oswald_700Bold',
  interRegular: 'Inter_400Regular',
  interMedium: 'Inter_500Medium',
} as const;

export interface TypeToken {
  fontFamily: string;
  fontSize: number;
  lineHeight: number;
  letterSpacing: number;
  textTransform?: 'uppercase';
}

export const typography = {
  displayXl: {
    fontFamily: fontFamily.oswaldBold,
    fontSize: 32,
    lineHeight: 38,
    letterSpacing: 1.5,
    textTransform: 'uppercase',
  },
  displayLg: {
    fontFamily: fontFamily.oswaldBold,
    fontSize: 24,
    lineHeight: 30,
    letterSpacing: 1.0,
    textTransform: 'uppercase',
  },
  displayMd: {
    fontFamily: fontFamily.oswaldMedium,
    fontSize: 18,
    lineHeight: 24,
    letterSpacing: 0.5,
    textTransform: 'uppercase',
  },
  bodyLg: {
    fontFamily: fontFamily.interRegular,
    fontSize: 16,
    lineHeight: 22,
    letterSpacing: 0,
  },
  bodyMd: {
    fontFamily: fontFamily.interRegular,
    fontSize: 14,
    lineHeight: 20,
    letterSpacing: 0,
  },
  bodySm: {
    fontFamily: fontFamily.interMedium,
    fontSize: 12,
    lineHeight: 16,
    letterSpacing: 0,
  },
  numeric: {
    fontFamily: fontFamily.interMedium,
    fontSize: 24,
    lineHeight: 28,
    letterSpacing: 0,
  },
} as const satisfies Record<string, TypeToken>;

export type TypographyToken = keyof typeof typography;
