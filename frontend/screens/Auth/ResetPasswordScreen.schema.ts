import { z } from 'zod';

import { passwordRule } from './passwordRule';

export const resetPasswordSchema = z
  .object({
    newPassword: passwordRule,
    confirmPassword: z.string().min(1, 'Confirme a nova senha.'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: 'As senhas não coincidem.',
    path: ['confirmPassword'],
  });

export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
