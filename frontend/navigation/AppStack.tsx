import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { colors } from '../theme/colors';
import { DashboardScreen } from '../screens/Dashboard/DashboardScreen';
import { ProfileScreen } from '../screens/Auth/ProfileScreen';
import type { AppStackParamList } from './types';

const Stack = createNativeStackNavigator<AppStackParamList>();

/**
 * Telas de negócio com sessão autenticada (04_FRONTEND_UI_COMPONENTES.md §A).
 * Header nativo desligado — cada tela monta components/layout/Header.tsx
 * (15_DESIGN_SYSTEM_UI_UX.md §H.5/§I). `DashboardScreen` é a tela inicial
 * (placeholder pré-Sprint 2); `ProfileScreen` é alcançada a partir dela. As
 * demais abas (`CyclesListScreen`, etc.) chegam nas Sprints 2-4; o
 * `BottomTabNavigator` completo só faz sentido a partir de então.
 */
export function AppStack() {
  return (
    <Stack.Navigator
      screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg } }}
    >
      <Stack.Screen name="Dashboard" component={DashboardScreen} />
      <Stack.Screen name="Profile" component={ProfileScreen} />
    </Stack.Navigator>
  );
}
