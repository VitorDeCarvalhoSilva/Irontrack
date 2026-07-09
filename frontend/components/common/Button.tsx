import { MaterialCommunityIcons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useRef } from 'react';
import {
  Pressable,
  type GestureResponderEvent,
  type PressableProps,
  Text,
  View,
} from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';

import { colors } from '../../theme/colors';
import { motion } from '../../theme/motion';

/**
 * 15_DESIGN_SYSTEM_UI_UX.md §H.1 — variantes diferenciadas por intensidade
 * de glow/movimento (§B.3), nunca por matiz (único acento: vermelho neon).
 */
export type ButtonVariant = 'primary' | 'secondary' | 'destructive' | 'icon';

export interface ButtonProps extends Omit<PressableProps, 'children'> {
  label: string;
  variant?: ButtonVariant;
  /** Ícone antes do texto (15 §D) — obrigatório na variante `icon`. */
  icon?: keyof typeof MaterialCommunityIcons.glyphMap;
  /** Ações críticas/frequentes exigem Touch Target de 56x56px (04_FRONTEND_UI_COMPONENTES.md §C.2). */
  large?: boolean;
}

const ICON_COLOR: Record<ButtonVariant, string> = {
  primary: colors.accent,
  secondary: colors.textPrimary,
  destructive: colors.accent,
  icon: colors.textSecondary,
};

export function Button({
  label,
  variant = 'primary',
  icon,
  large = false,
  disabled,
  onPress,
  ...pressableProps
}: ButtonProps) {
  const scale = useSharedValue(1);
  const containerRef = useRef<View>(null);

  const touchTargetClasses = large ? 'min-h-[56px] min-w-[56px]' : 'min-h-[44px] min-w-[44px]';

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  // Glow constante das variantes primary/destructive (15 §H.1/§B.3) — shadow*
  // no iOS, elevation tintada no Android (aproximação, sem lib nativa extra).
  const glowStyle =
    !disabled && (variant === 'primary' || variant === 'destructive')
      ? {
          shadowColor: colors.accent,
          shadowOffset: { width: 0, height: 0 },
          shadowOpacity: variant === 'destructive' ? 0.55 : 0.35,
          shadowRadius: variant === 'destructive' ? 10 : 6,
          elevation: variant === 'destructive' ? 6 : 3,
        }
      : undefined;

  function handlePressIn() {
    scale.value = withTiming(0.97, { duration: motion.fast });
  }

  function handlePressOut() {
    scale.value = withTiming(1, { duration: motion.fast });
  }

  function handlePress(event: GestureResponderEvent) {
    if (variant === 'primary') {
      void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    }
    onPress?.(event);
  }

  const containerClasses = [
    'flex-row items-center justify-center gap-2 px-4',
    touchTargetClasses,
    variant === 'icon' ? 'rounded-sm bg-surface' : 'rounded-sm',
    variant === 'primary' ? 'border-[1.5px] border-accent bg-transparent active:bg-accent-dim' : '',
    variant === 'secondary' ? 'border border-border-strong bg-transparent active:bg-surface' : '',
    variant === 'destructive'
      ? 'border-[1.5px] border-accent bg-transparent active:bg-accent-dim'
      : '',
    disabled ? 'opacity-40' : '',
  ].join(' ');

  const textClasses = [
    'font-inter-medium text-base tracking-wide',
    variant === 'primary' || variant === 'destructive' ? 'text-accent' : '',
    variant === 'secondary' ? 'text-primary' : '',
  ].join(' ');

  return (
    <Animated.View style={[glowStyle, animatedStyle]}>
      <Pressable
        ref={containerRef}
        accessibilityRole="button"
        accessibilityLabel={label}
        accessibilityState={{ disabled: !!disabled }}
        disabled={disabled}
        onPress={handlePress}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        className={containerClasses}
        {...pressableProps}
      >
        {icon ? (
          <MaterialCommunityIcons
            name={icon}
            size={20}
            color={disabled ? colors.textDisabled : ICON_COLOR[variant]}
          />
        ) : null}
        {variant === 'icon' ? null : <Text className={textClasses}>{label}</Text>}
      </Pressable>
    </Animated.View>
  );
}
