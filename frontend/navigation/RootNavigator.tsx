import type { LinkingOptions, Theme } from '@react-navigation/native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import * as Linking from 'expo-linking';
import { ActivityIndicator } from 'react-native';

import { Screen } from '../components/layout/Screen';
import { useAuth } from '../hooks/useAuth';
import { ResetPasswordScreen } from '../screens/Auth/ResetPasswordScreen';
import { VerifyEmailScreen } from '../screens/Auth/VerifyEmailScreen';
import { colors } from '../theme/colors';
import { AppStack } from './AppStack';
import { AuthStack } from './AuthStack';
import type { RootStackParamList } from './types';

const RootStack = createNativeStackNavigator<RootStackParamList>();

/** Tema dark-only do app (15_DESIGN_SYSTEM_UI_UX.md §A/§B) aplicado ao chrome do React Navigation. */
const navigationTheme: Theme = {
  dark: true,
  colors: {
    primary: colors.accent,
    background: colors.bg,
    card: colors.bgElevated,
    text: colors.textPrimary,
    border: colors.border,
    notification: colors.accent,
  },
  fonts: {
    regular: { fontFamily: 'Inter_400Regular', fontWeight: '400' },
    medium: { fontFamily: 'Inter_500Medium', fontWeight: '500' },
    bold: { fontFamily: 'Oswald_700Bold', fontWeight: '700' },
    heavy: { fontFamily: 'Oswald_700Bold', fontWeight: '700' },
  },
};

/**
 * Esquema `irontrack://` (app.json §`scheme`) — combinado com o backend via
 * AGENT_LOG.md (08_ROADMAP_FRONTEND.md §C.1, tarefa 2): os e-mails de
 * verificação/reset devem apontar para
 * `irontrack://verify-email/{token}` e `irontrack://reset-password/{token}`.
 */
const linking: LinkingOptions<RootStackParamList> = {
  prefixes: [Linking.createURL('/'), 'irontrack://'],
  config: {
    screens: {
      VerifyEmail: 'verify-email/:token',
      ResetPassword: 'reset-password/:token',
      AuthStack: 'auth',
      AppStack: 'app',
    },
  },
};

/**
 * Alterna entre AuthStack/AppStack observando AuthContext
 * (04_FRONTEND_UI_COMPONENTES.md §A.1). VerifyEmail/ResetPassword ficam
 * fora dessa alternância — permanecem alcançáveis via deep link
 * independentemente do status de autenticação.
 */
export function RootNavigator() {
  const { status, isAuthenticated } = useAuth();

  if (status === 'idle' || status === 'loading') {
    return (
      <Screen className="flex-1 items-center justify-center bg-bg">
        <ActivityIndicator size="large" color={colors.accent} />
      </Screen>
    );
  }

  return (
    <NavigationContainer linking={linking} theme={navigationTheme}>
      <RootStack.Navigator
        screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg } }}
      >
        {isAuthenticated ? (
          <RootStack.Screen name="AppStack" component={AppStack} />
        ) : (
          <RootStack.Screen name="AuthStack" component={AuthStack} />
        )}
        <RootStack.Screen name="VerifyEmail" component={VerifyEmailScreen} />
        <RootStack.Screen name="ResetPassword" component={ResetPasswordScreen} />
      </RootStack.Navigator>
    </NavigationContainer>
  );
}
