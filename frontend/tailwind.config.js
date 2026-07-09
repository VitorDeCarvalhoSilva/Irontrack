/**
 * Extensões do tema NativeWind espelhando exatamente os tokens de
 * theme/colors.ts, theme/typography.ts e theme/spacing.ts
 * (15_DESIGN_SYSTEM_UI_UX.md §B/§C/§E). Duplicado aqui (não importado
 * diretamente de theme/*.ts) porque este arquivo roda em Node puro/CJS,
 * fora do pipeline de transpilação do Metro — mantenha os dois em sincronia
 * manualmente ao alterar um valor de token.
 */

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './App.tsx',
    './components/**/*.{js,jsx,ts,tsx}',
    './screens/**/*.{js,jsx,ts,tsx}',
    './navigation/**/*.{js,jsx,ts,tsx}',
  ],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        bg: '#0A0A0C',
        elevated: '#131316',
        surface: '#1A1A1E',
        'surface-alt': '#202024',
        border: '#2A2A2F',
        'border-strong': '#38383E',
        primary: '#F2F2F0',
        secondary: '#9C9CA3',
        muted: '#6B6B72',
        disabled: '#46464C',
        accent: '#FF1440',
        'accent-glow-soft': 'rgba(255, 20, 64, 0.35)',
        'accent-glow-strong': 'rgba(255, 42, 77, 0.55)',
        'accent-dim': 'rgba(255, 20, 64, 0.16)',
      },
      fontFamily: {
        'oswald-medium': ['Oswald_500Medium'],
        'oswald-bold': ['Oswald_700Bold'],
        'inter-regular': ['Inter_400Regular'],
        'inter-medium': ['Inter_500Medium'],
      },
      borderRadius: {
        sm: '4px',
        md: '10px',
        lg: '16px',
      },
      fontSize: {
        'display-xl': ['32px', { lineHeight: '38px', letterSpacing: '1.5px' }],
        'display-lg': ['24px', { lineHeight: '30px', letterSpacing: '1px' }],
        'display-md': ['18px', { lineHeight: '24px', letterSpacing: '0.5px' }],
        'body-lg': ['16px', { lineHeight: '22px' }],
        'body-md': ['14px', { lineHeight: '20px' }],
        'body-sm': ['12px', { lineHeight: '16px' }],
        numeric: ['24px', { lineHeight: '28px' }],
      },
      spacing: {
        xs: '4px',
        sm: '8px',
        md: '16px',
        lg: '24px',
        xl: '32px',
        xxl: '48px',
      },
    },
  },
  plugins: [],
};
