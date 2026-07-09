import { Header } from '../../components/layout/Header';
import { Screen } from '../../components/layout/Screen';
import { useAuth } from '../../hooks/useAuth';
import type { AppStackScreenProps } from '../../navigation/types';

/**
 * Placeholder **temporário** pré-Sprint 2 (prompt-frontend-simplify-and-home.md)
 * — só para o AppStack ter uma tela inicial sensata em vez de cair direto em
 * ProfileScreen. A DashboardScreen real (ciclo ativo, nextSuggestedTrainingDayId
 * via GET /cycles/active, botão "Iniciar Treino") é a tarefa 4 de
 * `08_ROADMAP_FRONTEND.md` §C.2 — quando a Sprint 2 for implementada,
 * **expanda este mesmo arquivo**, não crie um novo.
 */
export function DashboardScreen({ navigation }: AppStackScreenProps<'Dashboard'>) {
  const { user } = useAuth();

  return (
    <Screen>
      <Header
        title={`Olá, ${user?.name ?? ''}`}
        titleSize="md"
        rightAction={{
          icon: 'account-circle',
          accessibilityLabel: 'Ver perfil',
          onPress: () => navigation.navigate('Profile'),
        }}
      />
    </Screen>
  );
}
