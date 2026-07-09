import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Pressable, Text, View } from 'react-native';

import { colors } from '../../theme/colors';

export interface HeaderAction {
  icon: keyof typeof MaterialCommunityIcons.glyphMap;
  onPress: () => void;
  accessibilityLabel: string;
}

export interface HeaderProps {
  title: string;
  /** `md` é usado só pela saudação de DashboardScreen (15_DESIGN_SYSTEM_UI_UX.md §I). */
  titleSize?: 'lg' | 'md';
  onBackPress?: () => void;
  /** No máximo uma ação à direita (15_DESIGN_SYSTEM_UI_UX.md §D.1). */
  rightAction?: HeaderAction;
}

/**
 * Cabeçalho reutilizável (15_DESIGN_SYSTEM_UI_UX.md §H.5): seta de voltar
 * condicional à esquerda, título centralizado/alinhado, no máximo um ícone
 * de ação à direita. Usado tanto em telas do AppStack (Profile, Dashboard)
 * quanto em telas de AuthStack que precisam de "Header padrão" (§I:
 * ForgotPassword, ResetPassword, VerifyEmail).
 */
export function Header({ title, titleSize = 'lg', onBackPress, rightAction }: HeaderProps) {
  return (
    <View className="min-h-[56px] flex-row items-center justify-between border-b border-border bg-bg px-md py-sm">
      <View className="min-h-[44px] min-w-[44px] items-center justify-center">
        {onBackPress ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Voltar"
            onPress={onBackPress}
            className="min-h-[44px] min-w-[44px] items-center justify-center"
          >
            <MaterialCommunityIcons name="chevron-left" size={24} color={colors.textPrimary} />
          </Pressable>
        ) : null}
      </View>

      <Text
        className={`flex-1 text-center font-oswald-bold uppercase text-primary ${
          titleSize === 'lg' ? 'text-display-lg' : 'text-display-md'
        }`}
        numberOfLines={1}
      >
        {title}
      </Text>

      <View className="min-h-[44px] min-w-[44px] items-center justify-center">
        {rightAction ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={rightAction.accessibilityLabel}
            onPress={rightAction.onPress}
            className="min-h-[44px] min-w-[44px] items-center justify-center"
          >
            <MaterialCommunityIcons name={rightAction.icon} size={24} color={colors.textPrimary} />
          </Pressable>
        ) : null}
      </View>
    </View>
  );
}
