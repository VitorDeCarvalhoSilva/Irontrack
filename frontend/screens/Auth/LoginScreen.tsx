import { zodResolver } from '@hookform/resolvers/zod';
import * as Haptics from 'expo-haptics';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ScrollView, Text, View } from 'react-native';

import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Wordmark } from '../../components/common/Wordmark';
import { Screen } from '../../components/layout/Screen';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { ApiError } from '../../services/apiClient';
import { authService } from '../../services/authService';
import type { AuthStackScreenProps } from '../../navigation/types';
import { loginSchema, type LoginFormValues } from './LoginScreen.schema';

/** 14_CATALOGO_DE_ERROS_DE_NEGOCIO.md — mensagens específicas por errorCode de POST /auth/login. */
function mapLoginErrorMessage(error: ApiError): string {
  switch (error.errorCode) {
    case 'EMAIL_NOT_VERIFIED':
      return 'Você precisa verificar seu e-mail antes de entrar. Confira sua caixa de entrada.';
    case 'ACCOUNT_PENDING_DELETION':
      return 'Sua conta tem uma exclusão agendada. Você pode cancelar abaixo.';
    case 'TOO_MANY_LOGIN_ATTEMPTS':
      return 'Muitas tentativas de login. Aguarde alguns minutos antes de tentar novamente.';
    default:
      return error.message;
  }
}

export function LoginScreen({ navigation, route }: AuthStackScreenProps<'Login'>) {
  const { login } = useAuth();
  const { showToast } = useToast();
  const confirmationMessage = route.params?.confirmationMessage;
  const [pendingDeletionMessage, setPendingDeletionMessage] = useState<string | null>(null);
  const [pendingDeletionCredentials, setPendingDeletionCredentials] =
    useState<LoginFormValues | null>(null);
  const [cancelingDeletion, setCancelingDeletion] = useState(false);
  const [cancelDeletionSuccess, setCancelDeletionSuccess] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  async function onSubmit(values: LoginFormValues) {
    setPendingDeletionCredentials(null);
    setPendingDeletionMessage(null);
    setCancelDeletionSuccess(false);

    try {
      await login(values.email, values.password);
    } catch (error) {
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);

      if (error instanceof ApiError && error.errorCode === 'ACCOUNT_PENDING_DELETION') {
        setPendingDeletionCredentials(values);
        setPendingDeletionMessage(mapLoginErrorMessage(error));
        return;
      }

      const message =
        error instanceof ApiError
          ? mapLoginErrorMessage(error)
          : 'Não foi possível conectar. Tente novamente.';
      showToast('error', message);
    }
  }

  async function handleCancelDeletion() {
    if (!pendingDeletionCredentials) return;

    setCancelingDeletion(true);
    try {
      await authService.cancelDeletion(pendingDeletionCredentials);
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      setCancelDeletionSuccess(true);
      setPendingDeletionCredentials(null);
      setPendingDeletionMessage(null);
    } catch (error) {
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      showToast(
        'error',
        error instanceof Error ? error.message : 'Não foi possível cancelar a exclusão.',
      );
    } finally {
      setCancelingDeletion(false);
    }
  }

  return (
    <Screen>
      <ScrollView
        contentContainerClassName="flex-grow justify-center px-md py-xl"
        className="flex-1"
      >
        <View className="mb-xl items-center">
          <Wordmark />
        </View>

        <View className="gap-4">
          {confirmationMessage ? (
            <Text className="text-body-md text-primary">{confirmationMessage}</Text>
          ) : null}

          <Controller
            control={control}
            name="email"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="E-mail"
                icon="email"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                errorMessage={errors.email?.message}
                autoCapitalize="none"
                keyboardType="email-address"
                accessibilityLabel="E-mail"
              />
            )}
          />

          <Controller
            control={control}
            name="password"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Senha"
                icon="lock"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                errorMessage={errors.password?.message}
                secureTextEntry
                accessibilityLabel="Senha"
              />
            )}
          />

          {cancelDeletionSuccess ? (
            <Text className="text-body-md text-primary">
              Exclusão cancelada com sucesso. Você já pode entrar normalmente.
            </Text>
          ) : null}

          {pendingDeletionMessage ? (
            <View className="gap-2">
              <Text className="text-body-md text-accent">{pendingDeletionMessage}</Text>
              <Button
                label="Cancelar exclusão da conta"
                icon="account-cancel"
                variant="secondary"
                onPress={handleCancelDeletion}
                disabled={cancelingDeletion}
              />
            </View>
          ) : null}

          <Button label="Entrar" onPress={handleSubmit(onSubmit)} disabled={isSubmitting} />

          <Button
            label="Esqueci minha senha"
            variant="secondary"
            onPress={() => navigation.navigate('ForgotPassword')}
          />

          <Button
            label="Criar conta"
            variant="secondary"
            onPress={() => navigation.navigate('Register')}
          />
        </View>
      </ScrollView>
    </Screen>
  );
}
