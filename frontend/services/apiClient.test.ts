import type { AxiosError, InternalAxiosRequestConfig } from 'axios';

import type { ApiErrorPayload } from '../types/api.types';

import { clearRefreshToken, getRefreshToken, setRefreshToken } from './tokenStorage';
import {
  apiClient,
  ApiError,
  handleResponseError,
  setAccessToken,
  setUnauthorizedHandler,
} from './apiClient';

jest.mock('./tokenStorage');

const mockedGetRefreshToken = jest.mocked(getRefreshToken);
const mockedSetRefreshToken = jest.mocked(setRefreshToken);
const mockedClearRefreshToken = jest.mocked(clearRefreshToken);

function buildError(
  status: number,
  payload: Partial<ApiErrorPayload>,
  config: Partial<InternalAxiosRequestConfig>,
): AxiosError<ApiErrorPayload> {
  return {
    isAxiosError: true,
    name: 'AxiosError',
    message: 'Request failed',
    toJSON: () => ({}),
    config: config as InternalAxiosRequestConfig,
    response: {
      status,
      data: payload as ApiErrorPayload,
      statusText: '',
      headers: {},
      config: config as InternalAxiosRequestConfig,
    },
  } as AxiosError<ApiErrorPayload>;
}

describe('apiClient — handleResponseError', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    setAccessToken(null);
    setUnauthorizedHandler(null);
  });

  it('mapeia um erro 4xx comum para ApiError preservando message e errorCode', async () => {
    const error = buildError(
      403,
      { message: 'E-mail não verificado.', errorCode: 'EMAIL_NOT_VERIFIED' },
      { url: '/auth/login', headers: {} as never },
    );

    await expect(handleResponseError(error)).rejects.toMatchObject({
      message: 'E-mail não verificado.',
      errorCode: 'EMAIL_NOT_VERIFIED',
    });
    await expect(handleResponseError(error)).rejects.toBeInstanceOf(ApiError);
  });

  it('nunca tenta refresh para uma falha do próprio /auth/login', async () => {
    mockedGetRefreshToken.mockResolvedValue('refresh-abc');
    const error = buildError(
      401,
      { message: 'Credenciais inválidas.' },
      { url: '/auth/login', headers: {} as never },
    );

    await expect(handleResponseError(error)).rejects.toMatchObject({
      message: 'Credenciais inválidas.',
    });
    expect(mockedGetRefreshToken).not.toHaveBeenCalled();
  });

  it('em um 401 de outro endpoint, renova o token e refaz a chamada original', async () => {
    mockedGetRefreshToken.mockResolvedValue('refresh-abc');
    const requestSpy = jest.spyOn(apiClient, 'request').mockResolvedValue({ data: 'ok' });
    const postSpy = jest.spyOn(apiClient, 'post').mockResolvedValue({
      data: { accessToken: 'new-access', refreshToken: 'new-refresh' },
    });

    const originalRequest: Partial<InternalAxiosRequestConfig> = {
      url: '/users/me',
      headers: {} as never,
    };
    const error = buildError(401, { message: 'Unauthorized' }, originalRequest);

    await handleResponseError(error);

    expect(postSpy).toHaveBeenCalledWith('/auth/refresh', { refreshToken: 'refresh-abc' });
    expect(mockedSetRefreshToken).toHaveBeenCalledWith('new-refresh');
    expect(requestSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/users/me',
        headers: expect.objectContaining({ Authorization: 'Bearer new-access' }),
      }),
    );

    requestSpy.mockRestore();
    postSpy.mockRestore();
  });

  it('sem refreshToken local, dispara o handler de sessão expirada sem chamar a API', async () => {
    mockedGetRefreshToken.mockResolvedValue(null);
    const postSpy = jest.spyOn(apiClient, 'post');
    const unauthorizedHandler = jest.fn();
    setUnauthorizedHandler(unauthorizedHandler);

    const error = buildError(
      401,
      { message: 'Unauthorized' },
      { url: '/users/me', headers: {} as never },
    );

    await expect(handleResponseError(error)).rejects.toBeInstanceOf(ApiError);
    expect(postSpy).not.toHaveBeenCalled();
    expect(unauthorizedHandler).toHaveBeenCalledTimes(1);
    expect(mockedClearRefreshToken).toHaveBeenCalledTimes(1);

    postSpy.mockRestore();
  });

  it('se o próprio refresh falhar, limpa a sessão e nunca tenta de novo (sem loop)', async () => {
    mockedGetRefreshToken.mockResolvedValue('refresh-abc');
    const postSpy = jest.spyOn(apiClient, 'post').mockRejectedValue(new Error('network down'));
    const unauthorizedHandler = jest.fn();
    setUnauthorizedHandler(unauthorizedHandler);

    const error = buildError(
      401,
      { message: 'Unauthorized' },
      { url: '/users/me', headers: {} as never },
    );

    await expect(handleResponseError(error)).rejects.toBeInstanceOf(ApiError);
    expect(postSpy).toHaveBeenCalledTimes(1);
    expect(unauthorizedHandler).toHaveBeenCalledTimes(1);

    postSpy.mockRestore();
  });

  it('nunca tenta refresh duas vezes na mesma requisição (_retry já marcado)', async () => {
    mockedGetRefreshToken.mockResolvedValue('refresh-abc');
    const postSpy = jest.spyOn(apiClient, 'post');

    const error = buildError(401, { message: 'Unauthorized' }, {
      url: '/users/me',
      headers: {} as never,
      _retry: true,
    } as never);

    await expect(handleResponseError(error)).rejects.toBeInstanceOf(ApiError);
    expect(postSpy).not.toHaveBeenCalled();

    postSpy.mockRestore();
  });
});
