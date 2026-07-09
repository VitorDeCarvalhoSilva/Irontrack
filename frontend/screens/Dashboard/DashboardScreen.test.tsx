import { fireEvent, render } from '@testing-library/react-native';

import { useAuth } from '../../hooks/useAuth';
import type { AppStackScreenProps } from '../../navigation/types';
import { DashboardScreen } from './DashboardScreen';

jest.mock('../../hooks/useAuth');

const mockedUseAuth = jest.mocked(useAuth);

describe('DashboardScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('exibe a saudação com o nome do usuário autenticado', async () => {
    mockedUseAuth.mockReturnValue({
      status: 'authenticated',
      user: {
        id: 'usr-1',
        name: 'Gabriel Silva',
        email: 'gabriel.silva@email.com',
        emailVerifiedAt: '2026-07-01T10:00:00.000Z',
        createdAt: '2026-07-01T10:00:00.000Z',
      },
      isAuthenticated: true,
      login: jest.fn(),
      logout: jest.fn(),
      updateUser: jest.fn(),
    });

    const navigation = {
      navigate: jest.fn(),
    } as unknown as AppStackScreenProps<'Dashboard'>['navigation'];
    const route = {} as AppStackScreenProps<'Dashboard'>['route'];

    const { getByText, getByRole } = await render(
      <DashboardScreen navigation={navigation} route={route} />,
    );

    expect(getByText('Olá, Gabriel Silva')).toBeTruthy();

    await fireEvent.press(getByRole('button', { name: 'Ver perfil' }));
    expect(navigation.navigate).toHaveBeenCalledWith('Profile');
  });
});
