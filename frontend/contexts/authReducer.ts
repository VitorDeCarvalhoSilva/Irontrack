import type { UserResponse } from '../types/api.types';

export interface AuthState {
  status: 'idle' | 'loading' | 'authenticated' | 'unauthenticated';
  user: UserResponse | null;
}

export type AuthAction =
  | { type: 'RESTORE_START' }
  | { type: 'AUTH_SUCCESS'; user: UserResponse }
  | { type: 'AUTH_FAILURE' }
  | { type: 'LOGOUT' }
  | { type: 'USER_UPDATED'; user: UserResponse };

export const initialAuthState: AuthState = { status: 'idle', user: null };

export function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'RESTORE_START':
      return { ...state, status: 'loading' };
    case 'AUTH_SUCCESS':
      return { status: 'authenticated', user: action.user };
    case 'AUTH_FAILURE':
      return { status: 'unauthenticated', user: null };
    case 'LOGOUT':
      return { status: 'unauthenticated', user: null };
    case 'USER_UPDATED':
      return state.status === 'authenticated' ? { ...state, user: action.user } : state;
    default:
      return state;
  }
}
