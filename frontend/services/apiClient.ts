import axios from 'axios';
import type { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

import type { ApiErrorPayload, ErrorCode, TokenPairResponse } from '../types/api.types';
import { clearRefreshToken, getRefreshToken, setRefreshToken } from './tokenStorage';

/** Erro normalizado de qualquer resposta 4xx/5xx (03_CONTRATOS_API.md §1.4). */
export class ApiError extends Error {
  errorCode?: ErrorCode;
  status?: number;

  constructor(message: string, errorCode?: ErrorCode, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.errorCode = errorCode;
    this.status = status;
  }
}

/**
 * Instância central do Axios (01_ARQUITETURA_E_PADROES.md §3.1/§4.2).
 * Sprint 1 adiciona: o header de autenticação (accessToken em memória) e o
 * fluxo de refresh automático de token (401 -> POST /auth/refresh, uma
 * única vez por requisição, 08_ROADMAP_FRONTEND.md §C.1).
 */
// eslint-disable-next-line import/no-named-as-default-member -- falso positivo conhecido do plugin com o dual export do axios
export const apiClient = axios.create({
  baseURL: process.env.EXPO_PUBLIC_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

/** accessToken vive apenas em memória — nunca em AsyncStorage/SecureStore. */
let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

/**
 * Registrado pelo AuthContext: chamado quando o refresh falha
 * definitivamente, para limpar a sessão e forçar a navegação de volta para
 * AuthStack (04_FRONTEND_UI_COMPONENTES.md §A.1).
 */
let unauthorizedHandler: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

const AUTH_ENDPOINTS_WITHOUT_RETRY = ['/auth/login', '/auth/register', '/auth/refresh'];

function isAuthEndpoint(url: string): boolean {
  return AUTH_ENDPOINTS_WITHOUT_RETRY.some((path) => url.includes(path));
}

/** Garante uma única chamada de refresh em voo, mesmo com 401s concorrentes. */
let refreshPromise: Promise<string | null> | null = null;

async function performRefresh(): Promise<string | null> {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  try {
    const response = await apiClient.post<TokenPairResponse>('/auth/refresh', { refreshToken });
    setAccessToken(response.data.accessToken);
    await setRefreshToken(response.data.refreshToken);
    return response.data.accessToken;
  } catch {
    return null;
  }
}

type RetriableRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean };

/**
 * Exportado separadamente do `.use()` para ser testável sem depender de
 * internals do Axios (services/apiClient.test.ts mocka `tokenStorage` e
 * chama esta função diretamente).
 */
export async function handleResponseError(
  error: AxiosError<ApiErrorPayload>,
): Promise<AxiosResponse> {
  const originalRequest = error.config as RetriableRequestConfig | undefined;
  const status = error.response?.status;
  const url = originalRequest?.url ?? '';

  if (status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint(url)) {
    originalRequest._retry = true;
    refreshPromise ??= performRefresh();
    const newAccessToken = await refreshPromise;
    refreshPromise = null;

    if (newAccessToken) {
      originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
      return apiClient.request(originalRequest);
    }

    await clearRefreshToken();
    setAccessToken(null);
    unauthorizedHandler?.();
  }

  const payload = error.response?.data;
  if (payload && typeof payload.message === 'string') {
    throw new ApiError(payload.message, payload.errorCode, status);
  }

  throw error;
}

apiClient.interceptors.response.use((response) => response, handleResponseError);
