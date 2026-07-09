import type {
  ChangePasswordRequest,
  DeleteAccountRequest,
  DeleteAccountResponse,
  UpdateMeRequest,
  UserResponse,
} from '../types/api.types';
import { apiClient } from './apiClient';

/** Endpoints de perfil do usuário autenticado (03_CONTRATOS_API.md §2.3/§2.8-§2.10). */
export const usersService = {
  getMe(): Promise<UserResponse> {
    return apiClient.get<UserResponse>('/users/me').then((response) => response.data);
  },

  updateMe(payload: UpdateMeRequest): Promise<UserResponse> {
    return apiClient.patch<UserResponse>('/users/me', payload).then((response) => response.data);
  },

  changePassword(payload: ChangePasswordRequest): Promise<void> {
    return apiClient.post('/users/me/change-password', payload).then(() => undefined);
  },

  deleteAccount(payload: DeleteAccountRequest): Promise<DeleteAccountResponse> {
    return apiClient
      .delete<DeleteAccountResponse>('/users/me', { data: payload })
      .then((response) => response.data);
  },
};
