import * as SecureStore from 'expo-secure-store';

/**
 * O refreshToken é o único dado de sessão persistido em disco, via
 * expo-secure-store (Keychain/Keystore) — nunca AsyncStorage
 * (01_ARQUITETURA_E_PADROES.md §3.1, 08_ROADMAP_FRONTEND.md §C.1, item 7).
 * O accessToken vive só em memória (services/apiClient.ts).
 */
const REFRESH_TOKEN_KEY = 'ironTrack.refreshToken';

export function getRefreshToken(): Promise<string | null> {
  return SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
}

export function setRefreshToken(token: string): Promise<void> {
  return SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token);
}

export function clearRefreshToken(): Promise<void> {
  return SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
}
