import type {
  CancelDeletionRequest,
  ForgotPasswordRequest,
  LoginRequest,
  LogoutRequest,
  RegisterRequest,
  RefreshRequest,
  ResetPasswordRequest,
  TokenPairResponse,
  UserResponse,
  VerifyEmailResponse,
} from '../types/api.types';
import { apiClient } from './apiClient';

/** Endpoints de autenticação (03_CONTRATOS_API.md §2) — todos públicos. */
export const authService = {
  register(payload: RegisterRequest): Promise<UserResponse> {
    return apiClient
      .post<UserResponse>('/auth/register', payload)
      .then((response) => response.data);
  },

  login(payload: LoginRequest): Promise<TokenPairResponse> {
    return apiClient
      .post<TokenPairResponse>('/auth/login', payload)
      .then((response) => response.data);
  },

  refresh(payload: RefreshRequest): Promise<TokenPairResponse> {
    return apiClient
      .post<TokenPairResponse>('/auth/refresh', payload)
      .then((response) => response.data);
  },

  logout(payload: LogoutRequest): Promise<void> {
    return apiClient.post('/auth/logout', payload).then(() => undefined);
  },

  verifyEmail(token: string): Promise<VerifyEmailResponse> {
    return apiClient
      .get<VerifyEmailResponse>(`/auth/verify-email/${token}`)
      .then((response) => response.data);
  },

  forgotPassword(payload: ForgotPasswordRequest): Promise<void> {
    return apiClient.post('/auth/forgot-password', payload).then(() => undefined);
  },

  resetPassword(payload: ResetPasswordRequest): Promise<void> {
    return apiClient.post('/auth/reset-password', payload).then(() => undefined);
  },

  cancelDeletion(payload: CancelDeletionRequest): Promise<void> {
    return apiClient.post('/auth/cancel-deletion', payload).then(() => undefined);
  },
};
