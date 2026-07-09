import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useEffect, useRef } from 'react';
import { AccessibilityInfo, PanResponder, Pressable, Text } from 'react-native';
import Animated, {
  Easing,
  runOnJS,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '../../theme/colors';
import { motion } from '../../theme/motion';
import { spacing } from '../../theme/spacing';
import type { ToastItem } from '../../contexts/ToastContext';

const VARIANT_CONFIG: Record<
  ToastItem['variant'],
  { borderColor: string; icon: keyof typeof MaterialCommunityIcons.glyphMap; durationMs: number }
> = {
  success: { borderColor: colors.textPrimary, icon: 'check-circle', durationMs: 3000 },
  error: { borderColor: colors.accent, icon: 'alert-circle', durationMs: 5000 },
  info: { borderColor: colors.borderStrong, icon: 'information', durationMs: 4000 },
};

const SWIPE_DISMISS_THRESHOLD = 80;

export interface ToastProps {
  item: ToastItem;
  onDismiss: () => void;
}

/**
 * Toast/Snackbar (15_DESIGN_SYSTEM_UI_UX.md §F.1) — fila de 1 item
 * gerenciada por ToastContext; este componente só renderiza o item atual,
 * dispensável por swipe horizontal, sem biblioteca de terceiros.
 */
export function Toast({ item, onDismiss }: ToastProps) {
  const insets = useSafeAreaInsets();
  const config = VARIANT_CONFIG[item.variant];

  const translateY = useSharedValue(24);
  const translateX = useSharedValue(0);
  const opacity = useSharedValue(0);
  const reduceMotionRef = useRef(false);

  useEffect(() => {
    let cancelled = false;

    AccessibilityInfo.isReduceMotionEnabled().then((enabled) => {
      if (cancelled) return;
      reduceMotionRef.current = enabled;
      const duration = enabled ? motion.reducedMotion : motion.base;
      const easing = Easing.out(Easing.cubic);
      translateY.value = withTiming(0, { duration, easing });
      opacity.value = withTiming(1, { duration, easing });
    });

    const timer = setTimeout(() => {
      dismiss();
    }, config.durationMs);

    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- roda uma vez por item (key={item.id} força remount)
  }, []);

  function dismiss() {
    const duration = reduceMotionRef.current ? motion.reducedMotion : motion.base;
    const easing = Easing.in(Easing.cubic);
    opacity.value = withTiming(0, { duration, easing });
    translateY.value = withTiming(24, { duration, easing }, (finished) => {
      if (finished) runOnJS(onDismiss)();
    });
  }

  const panResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: (_, gesture) => Math.abs(gesture.dx) > 4,
      onPanResponderMove: (_, gesture) => {
        translateX.value = gesture.dx;
      },
      onPanResponderRelease: (_, gesture) => {
        if (Math.abs(gesture.dx) > SWIPE_DISMISS_THRESHOLD) {
          const duration = reduceMotionRef.current ? motion.reducedMotion : motion.base;
          translateX.value = withTiming(
            gesture.dx > 0 ? 400 : -400,
            { duration, easing: Easing.out(Easing.cubic) },
            (finished) => {
              if (finished) runOnJS(onDismiss)();
            },
          );
        } else {
          translateX.value = withTiming(0, { duration: motion.fast });
        }
      },
    }),
  ).current;

  const animatedStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
    transform: [{ translateY: translateY.value }, { translateX: translateX.value }],
  }));

  return (
    <Animated.View
      {...panResponder.panHandlers}
      style={[
        {
          position: 'absolute',
          left: spacing.md,
          right: spacing.md,
          bottom: insets.bottom + spacing.md,
          borderTopWidth: 2,
          borderTopColor: config.borderColor,
          backgroundColor: colors.surface,
          borderRadius: 10,
          paddingVertical: spacing.sm,
          paddingHorizontal: spacing.md,
          flexDirection: 'row',
          alignItems: 'center',
          gap: spacing.sm,
        },
        animatedStyle,
      ]}
      accessibilityRole="alert"
      accessibilityLiveRegion="polite"
    >
      <MaterialCommunityIcons name={config.icon} size={20} color={config.borderColor} />
      <Text className="flex-1 font-inter-regular text-sm" style={{ color: colors.textPrimary }}>
        {item.message}
      </Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Dispensar"
        onPress={dismiss}
        className="min-h-[44px] min-w-[44px] items-center justify-center"
      >
        <MaterialCommunityIcons name="close" size={16} color={colors.textMuted} />
      </Pressable>
    </Animated.View>
  );
}
