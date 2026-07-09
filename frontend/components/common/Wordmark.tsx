import { Text, View } from 'react-native';

export interface WordmarkProps {
  /** `xl` (splash/LoginScreen) ou `lg` (RegisterScreen) — 15_DESIGN_SYSTEM_UI_UX.md §K. */
  size?: 'xl' | 'lg';
}

/**
 * Wordmark "IRONTRACK" (15_DESIGN_SYSTEM_UI_UX.md §K): Oswald 700 maiúsculo
 * com um traço fino de acento centralizado abaixo. Compartilhado entre
 * LoginScreen (`xl`) e RegisterScreen (`lg`, §I).
 */
export function Wordmark({ size = 'xl' }: WordmarkProps) {
  const textClass = size === 'xl' ? 'text-display-xl' : 'text-display-lg';

  return (
    <View className="items-center">
      <Text className={`font-oswald-bold uppercase text-primary ${textClass}`}>IronTrack</Text>
      <View className="mt-1 h-[3px] w-1/3 rounded-full bg-accent" />
    </View>
  );
}
