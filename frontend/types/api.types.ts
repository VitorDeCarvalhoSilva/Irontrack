/**
 * Códigos de erro de negócio estáveis (14_CATALOGO_DE_ERROS_DE_NEGOCIO.md) —
 * campo opcional no payload de erro, usado pela UI para reagir de forma
 * diferente de acordo com a condição, sem fazer parsing de `message`.
 */
export type ErrorCode =
  | 'TOO_MANY_LOGIN_ATTEMPTS'
  | 'EMAIL_NOT_VERIFIED'
  | 'ACCOUNT_PENDING_DELETION'
  | 'INVALID_REFRESH_TOKEN'
  | 'INVALID_CURRENT_PASSWORD'
  | 'INVALID_OR_EXPIRED_TOKEN'
  | 'INVALID_OR_EXPIRED_RESET_TOKEN'
  | 'INVALID_PASSWORD'
  | 'NO_PENDING_DELETION'
  | 'CYCLE_ACTIVATION_CONFLICT'
  | 'DAY_HAS_EXECUTED_SESSIONS'
  | 'EXERCISE_NOT_OWNED'
  | 'EXERCISE_IN_USE'
  | 'SESSION_ALREADY_IN_PROGRESS';

/** Espelha o payload de erro padronizado do backend (03_CONTRATOS_API.md §1.4). */
export interface ApiErrorPayload {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  errorCode?: ErrorCode;
}

/** 03_CONTRATOS_API.md §2.1/§2.3/§2.8 — mesma forma de resposta nos 3 endpoints. */
export interface UserResponse {
  id: string;
  name: string;
  email: string;
  emailVerifiedAt: string | null;
  createdAt: string;
}

/** 03_CONTRATOS_API.md §2.1 */
export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

/** 03_CONTRATOS_API.md §2.2 */
export interface LoginRequest {
  email: string;
  password: string;
}

/** 03_CONTRATOS_API.md §2.2/§2.4 — mesma forma de resposta em login e refresh. */
export interface TokenPairResponse {
  accessToken: string;
  refreshToken: string;
}

/** 03_CONTRATOS_API.md §2.4 */
export interface RefreshRequest {
  refreshToken: string;
}

/** 03_CONTRATOS_API.md §2.5 */
export interface LogoutRequest {
  refreshToken: string;
}

/** 03_CONTRATOS_API.md §2.6 */
export interface VerifyEmailResponse {
  email: string;
  verifiedAt: string;
}

/** 03_CONTRATOS_API.md §2.7 */
export interface ForgotPasswordRequest {
  email: string;
}

/** 03_CONTRATOS_API.md §2.7 */
export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

/** 03_CONTRATOS_API.md §2.8 — todos os campos opcionais (atualização parcial). */
export interface UpdateMeRequest {
  name?: string;
  email?: string;
}

/** 03_CONTRATOS_API.md §2.9 */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** 03_CONTRATOS_API.md §2.10 */
export interface DeleteAccountRequest {
  password: string;
}

/** 03_CONTRATOS_API.md §2.10 */
export interface DeleteAccountResponse {
  deletionScheduledFor: string;
}

/** 03_CONTRATOS_API.md §2.11 */
export interface CancelDeletionRequest {
  email: string;
  password: string;
}
