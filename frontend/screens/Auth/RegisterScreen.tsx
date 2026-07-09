import { zodResolver } from '@hookform/resolvers/zod';
import * as Haptics from 'expo-haptics';
import { Controller, useForm } from 'react-hook-form';
import { ScrollView, View } from 'react-native';

import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Wordmark } from '../../components/common/Wordmark';
import { Screen } from '../../components/layout/Screen';
import { useToast } from '../../hooks/useToast';
import { authService } from '../../services/authService';
import type { AuthStackScreenProps } from '../../navigation/types';
import { registerSchema, type RegisterFormValues } from './RegisterScreen.schema';

/**
 * 04_FRONTEND_UI_COMPONENTES.md §A — após POST /auth/register (201), navega
 * para LoginScreen com mensagem de confirmação. A conta já nasce verificada
 * (13_ADR_LOG.md ADR-018 — verificação de e-mail desativada no fluxo ativo),
 * mas continua **nunca autenticando automaticamente**: login é sempre uma
 * ação explícita do usuário.
 */
const REGISTER_CONFIRMATION_MESSAGE = 'Conta criada! Faça login.';

export function RegisterScreen({ navigation }: AuthStackScreenProps<'Register'>) {
  const { showToast } = useToast();

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: '', email: '', password: '', confirmPassword: '' },
  });

  async function onSubmit(values: RegisterFormValues) {
    try {
      await authService.register({
        name: values.name,
        email: values.email,
        password: values.password,
      });
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      navigation.navigate('Login', { confirmationMessage: REGISTER_CONFIRMATION_MESSAGE });
    } catch (error) {
      void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      showToast(
        'error',
        error instanceof Error ? error.message : 'Não foi possível concluir o cadastro.',
      );
    }
  }

  return (
    <Screen>
      <ScrollView contentContainerClassName="flex-grow px-md py-lg" className="flex-1">
        <View className="mb-lg flex-row items-center">
          <Button
            label="Voltar"
            variant="icon"
            icon="chevron-left"
            onPress={() => navigation.navigate('Login')}
          />
          <View className="flex-1 items-center pr-[44px]">
            <Wordmark size="lg" />
          </View>
        </View>

        <View className="gap-4">
          <Controller
            control={control}
            name="name"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Nome"
                icon="account"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                errorMessage={errors.name?.message}
                accessibilityLabel="Nome"
              />
            )}
          />

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

          <Controller
            control={control}
            name="confirmPassword"
            render={({ field: { onChange, onBlur, value } }) => (
              <Input
                label="Confirmar senha"
                icon="lock"
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                errorMessage={errors.confirmPassword?.message}
                secureTextEntry
                accessibilityLabel="Confirmar senha"
              />
            )}
          />

          <Button label="Cadastrar" onPress={handleSubmit(onSubmit)} disabled={isSubmitting} />
          <Button
            label="Já tenho conta"
            variant="secondary"
            onPress={() => navigation.navigate('Login')}
          />
        </View>
      </ScrollView>
    </Screen>
  );
}
