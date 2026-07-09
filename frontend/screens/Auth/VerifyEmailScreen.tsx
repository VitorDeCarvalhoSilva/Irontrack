import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Text, View } from 'react-native';

import { Button } from '../../components/common/Button';
import { Header } from '../../components/layout/Header';
import { Screen } from '../../components/layout/Screen';
import { authService } from '../../services/authService';
import { colors } from '../../theme/colors';
import type { RootStackScreenProps } from '../../navigation/types';

type VerificationStatus = 'verifying' | 'success' | 'error';

/**
 * Dormente (13_ADR_LOG.md ADR-018) — nenhum fluxo ativo hoje produz um deep
 * link válido para esta tela (registro não gera mais token de verificação).
 * Continua implementada/registrada no navigator raiz para reativação
 * futura, mas não é alcançada na prática (04_FRONTEND_UI_COMPONENTES.md §A).
 */
export function VerifyEmailScreen({ route, navigation }: RootStackScreenProps<'VerifyEmail'>) {
  const { token } = route.params;
  const [status, setStatus] = useState<VerificationStatus>('verifying');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    authService
      .verifyEmail(token)
      .then(() => {
        if (!cancelled) setStatus('success');
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        setErrorMessage(error instanceof Error ? error.message : 'Token inválido ou expirado.');
        setStatus('error');
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  function handleContinue() {
    if (navigation.canGoBack()) {
      navigation.goBack();
    } else {
      navigation.navigate('AuthStack');
    }
  }

  return (
    <Screen>
      <Header title="Verificar e-mail" />
      <View className="flex-1 items-center justify-center gap-4 px-md">
        {status === 'verifying' ? (
          <>
            <ActivityIndicator size="large" color={colors.accent} />
            <Text className="text-center text-body-lg text-secondary">
              Verificando seu e-mail...
            </Text>
          </>
        ) : status === 'success' ? (
          <>
            <MaterialCommunityIcons name="email-check" size={32} color={colors.textPrimary} />
            <Text className="font-oswald-bold text-display-lg uppercase text-primary">
              E-mail verificado
            </Text>
            <Text className="text-center text-body-lg text-secondary">
              Seu e-mail foi confirmado com sucesso.
            </Text>
            <Button label="Continuar" onPress={handleContinue} />
          </>
        ) : (
          <>
            <MaterialCommunityIcons name="alert-circle" size={32} color={colors.accent} />
            <Text className="font-oswald-bold text-display-lg uppercase text-primary">
              Não foi possível verificar
            </Text>
            <Text className="text-center text-body-lg text-accent">{errorMessage}</Text>
            <Button label="Voltar" onPress={handleContinue} />
          </>
        )}
      </View>
    </Screen>
  );
}
