import { z } from 'zod';

import { passwordRule } from './passwordRule';

export const registerSchema = z
  .object({
    name: z.string().min(1, 'Informe seu nome.'),
    email: z.string().min(1, 'Informe seu e-mail.').email('Informe um e-mail válido.'),
    password: passwordRule,
    confirmPassword: z.string().min(1, 'Confirme a senha.'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'As senhas não coincidem.',
    path: ['confirmPassword'],
  });

export type RegisterFormValues = z.infer<typeof registerSchema>;
