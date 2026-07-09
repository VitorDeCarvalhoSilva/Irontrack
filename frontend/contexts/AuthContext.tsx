import { createContext, useCallback, useEffect, useMemo, useReducer } from 'react';
import type { ReactNode } from 'react';

import { authService } from '../services/authService';
import { setAccessToken, setUnauthorizedHandler } from '../services/apiClient';
import { clearRefreshToken, getRefreshToken, setRefreshToken } from '../services/tokenStorage';
import { usersService } from '../services/usersService';
import type { UserResponse } from '../types/api.types';
import { authReducer, initialAuthState } from './authReducer';
import type { AuthState } from './authReducer';

export interface AuthContextValue extends AuthState {
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: UserResponse) => void;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialAuthState);

  // Registra o handler chamado pelo apiClient quando o refresh falha
  // definitivamente (04_FRONTEND_UI_COMPONENTES.md §A.1 — sessão inválida
  // sempre força AuthStack, nunca deixa a UI em um estado autenticado stale).
  useEffect(() => {
    setUnauthorizedHandler(() => {
      void clearRefreshToken();
      dispatch({ type: 'AUTH_FAILURE' });
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  // Silent refresh no mount: reidrata a sessão a partir do refreshToken em
  // SecureStore, sem exigir novo login a cada abertura do app.
  useEffect(() => {
    let cancelled = false;

    async function restoreSession() {
      dispatch({ type: 'RESTORE_START' });
      const refreshToken = await getRefreshToken();

      if (!refreshToken) {
        if (!cancelled) dispatch({ type: 'AUTH_FAILURE' });
        return;
      }

      try {
        const tokens = await authService.refresh({ refreshToken });
        setAccessToken(tokens.accessToken);
        await setRefreshToken(tokens.refreshToken);
        const user = await usersService.getMe();
        if (!cancelled) dispatch({ type: 'AUTH_SUCCESS', user });
      } catch {
        await clearRefreshToken();
        setAccessToken(null);
        if (!cancelled) dispatch({ type: 'AUTH_FAILURE' });
      }
    }

    void restoreSession();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authService.login({ email, password });
    setAccessToken(tokens.accessToken);
    await setRefreshToken(tokens.refreshToken);
    const user = await usersService.getMe();
    dispatch({ type: 'AUTH_SUCCESS', user });
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = await getRefreshToken();
    if (refreshToken) {
      try {
        await authService.logout({ refreshToken });
      } catch {
        // Best-effort — revogação já é idempotente no backend (03_CONTRATOS_API.md §2.5);
        // uma falha de rede aqui nunca deve impedir o logout local.
      }
    }
    await clearRefreshToken();
    setAccessToken(null);
    dispatch({ type: 'LOGOUT' });
  }, []);

  const updateUser = useCallback((user: UserResponse) => {
    dispatch({ type: 'USER_UPDATED', user });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      isAuthenticated: state.status === 'authenticated',
      login,
      logout,
      updateUser,
    }),
    [state, login, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
