import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { CompositeScreenProps } from '@react-navigation/native';

/** 04_FRONTEND_UI_COMPONENTES.md §A — telas exibidas sem sessão válida. */
export type AuthStackParamList = {
  /**
   * `confirmationMessage` é usado por RegisterScreen (e, no futuro,
   * ResetPasswordScreen) para exibir um banner de sucesso ao redirecionar
   * de volta para o login (04_FRONTEND_UI_COMPONENTES.md §A).
   */
  Login: { confirmationMessage?: string } | undefined;
  Register: undefined;
  ForgotPassword: undefined;
};

/** 04_FRONTEND_UI_COMPONENTES.md §A — telas exibidas com sessão autenticada. */
export type AppStackParamList = {
  Dashboard: undefined;
  Profile: undefined;
};

/**
 * VerifyEmail/ResetPassword vivem no navigator raiz, fora de AuthStack/AppStack
 * — precisam ser alcançáveis via deep link independentemente do estado de
 * autenticação (04_FRONTEND_UI_COMPONENTES.md §A.1).
 */
export type RootStackParamList = {
  AuthStack: undefined;
  AppStack: undefined;
  VerifyEmail: { token: string };
  ResetPassword: { token: string };
};

export type AuthStackScreenProps<T extends keyof AuthStackParamList> = CompositeScreenProps<
  NativeStackScreenProps<AuthStackParamList, T>,
  NativeStackScreenProps<RootStackParamList>
>;

export type AppStackScreenProps<T extends keyof AppStackParamList> = CompositeScreenProps<
  NativeStackScreenProps<AppStackParamList, T>,
  NativeStackScreenProps<RootStackParamList>
>;

export type RootStackScreenProps<T extends keyof RootStackParamList> = NativeStackScreenProps<
  RootStackParamList,
  T
>;
