# 14_CATALOGO_DE_ERROS_DE_NEGOCIO.md - Catálogo de Erros de Negócio

Índice único de todas as condições de erro de negócio (`401`/`403`/`404`/`422`)
já especificadas, espalhadas por `03_CONTRATOS_API.md` e `11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md`.
O formato de payload de erro é o já definido em `03_CONTRATOS_API.md` §1.4 —
este catálogo apenas consolida as condições e sugere um `errorCode` estável
(campo adicional recomendado no payload de erro, para o frontend distinguir
erros programaticamente sem fazer parsing de `message`).

| `errorCode` sugerido | Status HTTP | Endpoint | Condição |
| :--- | :--- | :--- | :--- |
| `EMAIL_NOT_VERIFIED` | 403 | `POST /auth/login` (§2.2) | `users.email_verified_at IS NULL`. |
| `ACCOUNT_PENDING_DELETION` | 403 | `POST /auth/login` (§2.2) | `users.deletion_requested_at IS NOT NULL` (`11` §D). |
| `INVALID_REFRESH_TOKEN` | 401 | `POST /auth/refresh` (§2.4) | Token inválido, expirado ou já revogado. |
| `INVALID_CURRENT_PASSWORD` | 401 | `POST /users/me/change-password` (§2.9) | `currentPassword` não confere com `password_hash`. |
| `INVALID_OR_EXPIRED_TOKEN` | 400 | `GET /auth/verify-email/{token}` (§2.6) | Token de verificação inválido ou expirado. |
| `INVALID_OR_EXPIRED_RESET_TOKEN` | 400 | `POST /auth/reset-password` (§2.7) | Token de reset inválido ou expirado. |
| `INVALID_PASSWORD` | 401 | `DELETE /users/me` (§2.10) | `password` não confere, ao solicitar exclusão de conta. |
| `NO_PENDING_DELETION` | 400 | `POST /auth/cancel-deletion` (§2.11) | Não há exclusão pendente para o e-mail, ou o período de carência já expirou. |
| `CYCLE_ACTIVATION_CONFLICT` | 422 | `PATCH /cycles/{cycleId}/activate` (§3.7) | Falha na transação atômica de troca de ciclo ativo (ex: condição de corrida). |
| `DAY_HAS_EXECUTED_SESSIONS` | 422 | `DELETE /cycles/{cycleId}/days/{dayId}` (§3.10) | O dia já possui pelo menos uma sessão executada (`ON DELETE RESTRICT`). |
| `EXERCISE_NOT_OWNED` | 403 | `PATCH`/`DELETE /exercises/{exerciseId}` (§4.3-§4.4) | Exercício não é customizado (`isCustom = false`) ou não pertence ao usuário autenticado. |
| `EXERCISE_IN_USE` | 422 | `DELETE /exercises/{exerciseId}` (§4.4) | Exercício já foi utilizado em alguma sessão registrada (`ON DELETE RESTRICT`). |
| `SESSION_ALREADY_IN_PROGRESS` | 422 | `POST /sessions/start` (§5.1) | Usuário já possui uma sessão `IN_PROGRESS` em aberto. |

**Convenção para novos erros:** ao adicionar um endpoint com uma nova
condição de erro de negócio, adicione a linha correspondente aqui **e**
inclua o `errorCode` no exemplo de payload de erro na seção relevante de
`03_CONTRATOS_API.md` — este catálogo deve permanecer um espelho fiel, nunca
uma fonte paralela que pode divergir do contrato real.
