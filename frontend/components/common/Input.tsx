import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useState } from 'react';
import { Text, TextInput, type TextInputProps, View } from 'react-native';

import { colors } from '../../theme/colors';

export interface InputProps extends TextInputProps {
  label?: string;
  errorMessage?: string;
  /** Ícone de prefixo (15_DESIGN_SYSTEM_UI_UX.md §H.2), ex: "email", "lock". */
  icon?: keyof typeof MaterialCommunityIcons.glyphMap;
}

/**
 * Estilo "underline" (15_DESIGN_SYSTEM_UI_UX.md §H.2): borda inferior única,
 * cor de repouso/foco/erro por token. Label acima do campo (nunca
 * placeholder-only).
 */
export function Input({
  label,
  errorMessage,
  icon,
  accessibilityLabel,
  onFocus,
  onBlur,
  ...textInputProps
}: InputProps) {
  const [focused, setFocused] = useState(false);
  const hasError = !!errorMessage;

  const borderColor = hasError ? colors.accent : focused ? colors.accent : colors.border;
  const iconColor = hasError || focused ? colors.accent : colors.textMuted;

  return (
    <View className="w-full">
      {label ? <Text className="mb-1 text-sm text-secondary">{label}</Text> : null}
      <View
        className="min-h-[44px] flex-row items-center gap-2 border-b px-1"
        style={{ borderColor }}
      >
        {icon ? <MaterialCommunityIcons name={icon} size={20} color={iconColor} /> : null}
        <TextInput
          accessibilityLabel={accessibilityLabel ?? label}
          placeholderTextColor={colors.textMuted}
          className="flex-1 py-2 font-inter-regular text-base"
          style={{ color: colors.textPrimary }}
          onFocus={(event) => {
            setFocused(true);
            onFocus?.(event);
          }}
          onBlur={(event) => {
            setFocused(false);
            onBlur?.(event);
          }}
          {...textInputProps}
        />
      </View>
      {errorMessage ? (
        <View className="mt-1 flex-row items-center gap-1">
          <MaterialCommunityIcons name="alert-circle" size={16} color={colors.accent} />
          <Text className="text-sm" style={{ color: colors.accent }}>
            {errorMessage}
          </Text>
        </View>
      ) : null}
    </View>
  );
}
