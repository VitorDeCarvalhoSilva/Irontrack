import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { colors } from '../theme/colors';
import { ForgotPasswordScreen } from '../screens/Auth/ForgotPasswordScreen';
import { LoginScreen } from '../screens/Auth/LoginScreen';
import { RegisterScreen } from '../screens/Auth/RegisterScreen';
import type { AuthStackParamList } from './types';

const Stack = createNativeStackNavigator<AuthStackParamList>();

/**
 * Telas de autenticação (04_FRONTEND_UI_COMPONENTES.md §A). Header nativo
 * sempre desligado — cada tela monta seu próprio topo (wordmark em
 * Login/Register, Header padrão em ForgotPassword — 15_DESIGN_SYSTEM_UI_UX.md
 * §I). VerifyEmail e ResetPassword não vivem aqui — ver navigation/RootNavigator.tsx.
 */
export function AuthStack() {
  return (
    <Stack.Navigator
      screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg } }}
    >
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
      <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
    </Stack.Navigator>
  );
}
