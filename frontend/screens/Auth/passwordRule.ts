import { z } from 'zod';

/**
 * Espelha a regra de força de senha do backend (00_PRD_IRONTRACK.md §4.3,
 * 03_CONTRATOS_API.md §2.7): mínimo 8 caracteres, contendo letras e
 * números. Validação client-side é só uma otimização de UX — o backend
 * sempre revalida (01_ARQUITETURA_E_PADROES.md §3.4).
 */
export const passwordRule = z
  .string()
  .min(8, 'A senha deve ter no mínimo 8 caracteres.')
  .regex(/[A-Za-z]/, 'A senha deve conter ao menos uma letra.')
  .regex(/[0-9]/, 'A senha deve conter ao menos um número.');
