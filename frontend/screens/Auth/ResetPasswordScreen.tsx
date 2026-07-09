import { zodResolver } from '@hookform/resolvers/zod';
import * as Haptics from 'expo-haptics';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ScrollView, Text, View } from 'react-native';

import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Header } from '../../components/layout/Header';
import { Screen } from '../../components/layout/Screen';
import { useToast } from '../../hooks/useToast';
import { authService } from '../../services/authService';
import type { RootStackScreenProps } from '../../navigation/types';
import { resetPasswordSchema, type ResetPasswordFormValues } from './ResetPasswordScreen.schema';

/**
 * Acessada via deep link (`irontrack://reset-password/{token}`), registrada
 * no navigator raiz para permanecer alcançável mesmo com o usuário já
 * autenticado (04_FRONTEND_UI_COMPONENTES.md §A.1).
 */
export function ResetPasswordScreen({ route, navigation }: RootStackScreenProps<'ResetPassword'>) {
  const { token } = route.params;
  const { showToast } = useToast();
  const [success, setSuccess] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  async function onSubmit(values: ResetPasswordFormValues) {
    try {
      await authService.resetPassword({ token, newPassword: values.newPassword });
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      setSuccess(true);
    } catch (error) {
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      showToast(
        'error',
        error instanceof Error ? error.message : 'Não foi possível redefinir a senha.',
      );
    }
  }

  if (success) {
    return (
      <Screen>
        <Header title="Nova senha" />
        <View className="flex-1 items-center justify-center gap-4 px-md">
          <Text className="font-oswald-bold text-display-lg uppercase text-primary">
            Senha redefinida
          </Text>
          <Text className="text-center text-body-lg text-secondary">
            Sua senha foi alterada com sucesso. Todas as sessões anteriores foram encerradas por
            segurança.
          </Text>
          <Button label="Ir para o login" onPress={() => navigation.navigate('AuthStack')} />
        </View>
      </Screen>
    );
  }

  return (
    <Screen>
      <Header title="Nova senha" />
      <ScrollView contentContainerClassName="flex-grow justify-center px-md py-lg">
        <View className="gap-4">
          <Controller
            control={control}
            name="newPassword"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Nova senha"
                icon="lock-reset"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                errorMessage={errors.newPassword?.message}
                secureTextEntry
                accessibilityLabel="Nova senha"
              />
            )}
          />

          <Controller
            control={control}
            name="confirmPassword"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Confirmar nova senha"
                icon="lock-reset"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                errorMessage={errors.confirmPassword?.message}
                secureTextEntry
                accessibilityLabel="Confirmar nova senha"
              />
            )}
          />

          <Button
            label="Redefinir senha"
            onPress={handleSubmit(onSubmit)}
            disabled={isSubmitting}
          />
        </View>
      </ScrollView>
    </Screen>
  );
}
