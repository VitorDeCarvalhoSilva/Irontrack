import { MaterialCommunityIcons } from '@expo/vector-icons';
import type { ReactNode } from 'react';
import {
  Modal as RNModal,
  type ModalProps as RNModalProps,
  Pressable,
  Text,
  View,
} from 'react-native';

import { colors } from '../../theme/colors';

export interface ModalProps extends Pick<RNModalProps, 'visible' | 'animationType'> {
  onClose: () => void;
  title?: string;
  children?: ReactNode;
}

/**
 * 15_DESIGN_SYSTEM_UI_UX.md §H.4 — `animationType="fade"` (padrão) é a
 * confirmação simples, centralizada; `animationType="slide"` é a
 * confirmação destrutiva/de sessão, como sheet ancorado à base da tela.
 */
export function Modal({ visible, animationType = 'fade', onClose, title, children }: ModalProps) {
  const isSheet = animationType === 'slide';

  return (
    <RNModal visible={visible} animationType={animationType} transparent onRequestClose={onClose}>
      <View
        className={`flex-1 bg-black/60 px-4 ${isSheet ? 'justify-end pb-6' : 'items-center justify-center'}`}
      >
        <View className={`w-full bg-elevated p-4 ${isSheet ? 'rounded-t-lg' : 'rounded-lg'}`}>
          <View className="mb-2 flex-row items-center justify-between">
            {title ? (
              <Text className="font-oswald-bold text-display-md uppercase text-primary">
                {title}
              </Text>
            ) : (
              <View />
            )}
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Fechar"
              onPress={onClose}
              className="min-h-[44px] min-w-[44px] items-center justify-center"
            >
              <MaterialCommunityIcons name="close" size={20} color={colors.textSecondary} />
            </Pressable>
          </View>
          {children}
        </View>
      </View>
    </RNModal>
  );
}
