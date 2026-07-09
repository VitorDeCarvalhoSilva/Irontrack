import { zodResolver } from '@hookform/resolvers/zod';
import * as Haptics from 'expo-haptics';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ScrollView, Text, View } from 'react-native';

import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Header } from '../../components/layout/Header';
import { Screen } from '../../components/layout/Screen';
import { authService } from '../../services/authService';
import type { AuthStackScreenProps } from '../../navigation/types';
import { forgotPasswordSchema, type ForgotPasswordFormValues } from './ForgotPasswordScreen.schema';

export function ForgotPasswordScreen({ navigation }: AuthStackScreenProps<'ForgotPassword'>) {
  const [submitted, setSubmitted] = useState(false);

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  async function onSubmit(values: ForgotPasswordFormValues) {
    // 03_CONTRATOS_API.md §2.7 — sempre 202 com mensagem genérica,
    // independente de o e-mail existir na base (evita enumeração de usuários).
    await authService.forgotPassword(values);
    void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    setSubmitted(true);
  }

  return (
    <Screen>
      <Header title="Recuperar senha" onBackPress={() => navigation.navigate('Login')} />
      <ScrollView contentContainerClassName="flex-grow justify-center px-md py-lg">
        <View className="gap-4">
          {submitted ? (
            <Text className="text-body-lg text-primary">
              Se o e-mail informado existir em nossa base, enviaremos um link de redefinição de
              senha, válido por 1 hora.
            </Text>
          ) : (
            <>
              <Text className="text-body-md text-secondary">
                Informe seu e-mail cadastrado para receber o link de redefinição de senha.
              </Text>

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

              <Button
                label="Enviar link"
                onPress={handleSubmit(onSubmit)}
                disabled={isSubmitting}
              />
            </>
          )}

          <Button
            label="Voltar para o login"
            variant="secondary"
            onPress={() => navigation.navigate('Login')}
          />
        </View>
      </ScrollView>
    </Screen>
  );
}
