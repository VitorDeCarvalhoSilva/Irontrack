import { z } from 'zod';

import { passwordRule } from './passwordRule';

export const profileSchema = z.object({
  name: z.string().min(1, 'Informe seu nome.'),
  email: z.string().min(1, 'Informe seu e-mail.').email('Informe um e-mail válido.'),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Informe sua senha atual.'),
    newPassword: passwordRule,
    confirmNewPassword: z.string().min(1, 'Confirme a nova senha.'),
  })
  .refine((values) => values.newPassword === values.confirmNewPassword, {
    message: 'As senhas não coincidem.',
    path: ['confirmNewPassword'],
  });

export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;

export const deleteAccountSchema = z.object({
  password: z.string().min(1, 'Informe sua senha atual.'),
});

export type DeleteAccountFormValues = z.infer<typeof deleteAccountSchema>;
