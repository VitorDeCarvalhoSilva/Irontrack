import { authReducer, initialAuthState } from './authReducer';
import type { UserResponse } from '../types/api.types';

const user: UserResponse = {
  id: 'usr-9a2f-4881',
  name: 'Gabriel Silva',
  email: 'gabriel.silva@email.com',
  emailVerifiedAt: '2026-07-01T10:05:00.000Z',
  createdAt: '2026-07-01T10:00:00.000Z',
};

describe('authReducer', () => {
  it('começa no estado idle, sem usuário', () => {
    expect(initialAuthState).toEqual({ status: 'idle', user: null });
  });

  it('RESTORE_START move para loading preservando o restante do estado', () => {
    const state = authReducer(initialAuthState, { type: 'RESTORE_START' });

    expect(state).toEqual({ status: 'loading', user: null });
  });

  it('AUTH_SUCCESS autentica com o usuário retornado', () => {
    const state = authReducer({ status: 'loading', user: null }, { type: 'AUTH_SUCCESS', user });

    expect(state).toEqual({ status: 'authenticated', user });
  });

  it('AUTH_FAILURE move para unauthenticated e limpa o usuário', () => {
    const state = authReducer({ status: 'loading', user: null }, { type: 'AUTH_FAILURE' });

    expect(state).toEqual({ status: 'unauthenticated', user: null });
  });

  it('LOGOUT limpa a sessão mesmo partindo de authenticated', () => {
    const state = authReducer({ status: 'authenticated', user }, { type: 'LOGOUT' });

    expect(state).toEqual({ status: 'unauthenticated', user: null });
  });

  it('USER_UPDATED atualiza os dados do usuário quando autenticado', () => {
    const updatedUser: UserResponse = { ...user, name: 'Gabriel Silva Santos' };

    const state = authReducer(
      { status: 'authenticated', user },
      { type: 'USER_UPDATED', user: updatedUser },
    );

    expect(state).toEqual({ status: 'authenticated', user: updatedUser });
  });

  it('USER_UPDATED é ignorado quando não há sessão autenticada', () => {
    const state = authReducer(
      { status: 'unauthenticated', user: null },
      { type: 'USER_UPDATED', user },
    );

    expect(state).toEqual({ status: 'unauthenticated', user: null });
  });
});
