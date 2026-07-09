import { fireEvent, render, waitFor } from '@testing-library/react-native';

import { ToastProvider } from '../../contexts/ToastContext';
import { useAuth } from '../../hooks/useAuth';
import type { AuthStackScreenProps } from '../../navigation/types';
import { LoginScreen } from './LoginScreen';

jest.mock('../../hooks/useAuth');

const mockedUseAuth = jest.mocked(useAuth);

function mockAuthValue(login: jest.Mock) {
  mockedUseAuth.mockReturnValue({
    status: 'unauthenticated',
    user: null,
    isAuthenticated: false,
    login,
    logout: jest.fn(),
    updateUser: jest.fn(),
  });
}

async function renderLoginScreen(
  login: jest.Mock,
  params?: AuthStackScreenProps<'Login'>['route']['params'],
) {
  mockAuthValue(login);

  const navigate = jest.fn();
  const navigation = { navigate } as unknown as AuthStackScreenProps<'Login'>['navigation'];
  const route = { params } as AuthStackScreenProps<'Login'>['route'];

  const view = await render(
    <ToastProvider>
      <LoginScreen navigation={navigation} route={route} />
    </ToastProvider>,
  );

  return { navigate, ...view };
}

describe('LoginScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('exibe a mensagem de confirmação recebida via parâmetro de rota (ex: pós-cadastro)', async () => {
    const { getByText } = await renderLoginScreen(jest.fn(), {
      confirmationMessage: 'Conta criada! Verifique seu e-mail e faça login.',
    });

    expect(getByText('Conta criada! Verifique seu e-mail e faça login.')).toBeTruthy();
  });

  it('em login bem-sucedido, aciona login(email, password) — RootNavigator troca de stack sozinho', async () => {
    const login = jest.fn().mockResolvedValue(undefined);
    const { getByLabelText, getByRole, queryByText } = await renderLoginScreen(login);

    await fireEvent.changeText(getByLabelText('E-mail'), 'gabriel.silva@email.com');
    await fireEvent.changeText(getByLabelText('Senha'), 'SenhaSegura123');
    await fireEvent.press(getByRole('button', { name: 'Entrar' }));

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith('gabriel.silva@email.com', 'SenhaSegura123');
    });

    // LoginScreen nunca navega manualmente em caso de sucesso — a troca para
    // AppStack é 100% consequência de AuthContext.isAuthenticated mudar,
    // observada pelo RootNavigator (04_FRONTEND_UI_COMPONENTES.md §A.1).
    expect(queryByText(/não foi possível/i)).toBeNull();
  });
});
