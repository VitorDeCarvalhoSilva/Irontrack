import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ToastProvider } from '../../contexts/ToastContext';
import { authService } from '../../services/authService';
import type { AuthStackScreenProps } from '../../navigation/types';
import { RegisterScreen } from './RegisterScreen';

jest.mock('../../services/authService');

const mockedRegister = jest.mocked(authService.register);

const TEST_SAFE_AREA_METRICS = {
  frame: { x: 0, y: 0, width: 390, height: 844 },
  insets: { top: 47, left: 0, right: 0, bottom: 34 },
};

async function renderRegisterScreen() {
  const navigate = jest.fn();
  const navigation = { navigate } as unknown as AuthStackScreenProps<'Register'>['navigation'];
  const route = {} as AuthStackScreenProps<'Register'>['route'];

  const view = await render(
    <SafeAreaProvider initialMetrics={TEST_SAFE_AREA_METRICS}>
      <ToastProvider>
        <RegisterScreen navigation={navigation} route={route} />
      </ToastProvider>
    </SafeAreaProvider>,
  );

  return { navigate, ...view };
}

async function fillForm(
  view: Awaited<ReturnType<typeof renderRegisterScreen>>,
  overrides: { password?: string; confirmPassword?: string } = {},
) {
  await fireEvent.changeText(view.getByLabelText('Nome'), 'Gabriel Silva');
  await fireEvent.changeText(view.getByLabelText('E-mail'), 'gabriel.silva@email.com');
  await fireEvent.changeText(view.getByLabelText('Senha'), overrides.password ?? 'SenhaSegura123');
  await fireEvent.changeText(
    view.getByLabelText('Confirmar senha'),
    overrides.confirmPassword ?? overrides.password ?? 'SenhaSegura123',
  );
}

describe('RegisterScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('após cadastro com sucesso, navega para Login com mensagem de confirmação e nunca autentica', async () => {
    mockedRegister.mockResolvedValue({
      id: 'usr-1',
      name: 'Gabriel Silva',
      email: 'gabriel.silva@email.com',
      emailVerifiedAt: null,
      createdAt: '2026-07-01T10:00:00.000Z',
    });

    const view = await renderRegisterScreen();
    const { navigate, getByRole } = view;

    await fillForm(view);
    await fireEvent.press(getByRole('button', { name: 'Cadastrar' }));

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith('Login', {
        confirmationMessage: expect.stringContaining('Faça login'),
      });
    });

    expect(mockedRegister).toHaveBeenCalledWith({
      name: 'Gabriel Silva',
      email: 'gabriel.silva@email.com',
      password: 'SenhaSegura123',
    });
    expect(navigate).not.toHaveBeenCalledWith('AppStack');
  });

  it('acusa senhas diferentes sem chamar a API', async () => {
    const view = await renderRegisterScreen();
    const { getByRole, findByText } = view;

    await fillForm(view, { password: 'SenhaSegura123', confirmPassword: 'Outra12345' });
    await fireEvent.press(getByRole('button', { name: 'Cadastrar' }));

    expect(await findByText('As senhas não coincidem.')).toBeTruthy();
    expect(mockedRegister).not.toHaveBeenCalled();
  });

  it('em caso de erro da API, exibe um toast e não navega', async () => {
    mockedRegister.mockRejectedValue(new Error('E-mail já cadastrado.'));

    const view = await renderRegisterScreen();
    const { navigate, getByRole, findByText } = view;

    await fillForm(view);
    await fireEvent.press(getByRole('button', { name: 'Cadastrar' }));

    expect(await findByText('E-mail já cadastrado.')).toBeTruthy();
    expect(navigate).not.toHaveBeenCalled();
  });
});
