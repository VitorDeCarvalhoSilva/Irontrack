# 03_CONTRATOS_API.md - Contratos de API RESTful

Este documento descreve detalhadamente a especificação e os contratos de payload (JSON DTOs) dos endpoints RESTful da API do **IronTrack** (V1).

---

## 1. Diretrizes de Escopo e Padrões Globais

### 1.1. Escopo Restrito (Musculação e Hipertrofia)
De acordo com o direcionamento do projeto, esta API foca **exclusivamente na prática de musculação, força e hipertrofia em ambiente de academia**. Os contratos foram desenhados para capturar métricas de carga (*weight*), repetições (*reps*), séries (*sets*), percepção de esforço (*RPE*) e técnicas de intensificação (falha, drop-set, rest-pause, pausa, negativa forçada). Parâmetros e tabelas relacionados a holds isométricos ou estágios de habilidades calistênicas estão desabilitados ou omitidos desta camada de transporte para simplificar a integração com a UI.

### 1.2. Padrões de Comunicação
* **Protocolo:** HTTP/1.1 sobre SSL/TLS (HTTPS).
* **Base URL:** `/api/v1`
* **Content-Type:** `application/json` (para requisições e respostas).
* **Padrão de Propriedades:** `camelCase` para JSON; parâmetros de rota e query em `camelCase` ou `kebab-case` onde explicitado.
* **Formatos de Data/Hora:** Strings ISO8601 UTC (`YYYY-MM-DDThh:mm:ss.sssZ`).
* **Prefixo de versão obrigatório:** todos os endpoints documentados abaixo já exibem o path completo, incluindo o prefixo `/api/v1` — esse prefixo é **sempre** parte do path real, nunca omitido. Qualquer referência que mostre rotas sem o prefixo de versão (inclusive em outros documentos deste projeto ou em planejamentos externos, ex: ferramentas de gestão de sprint) deve ser lida como abreviação informal, não como contrato real.

### 1.3. Segurança e Autenticação
* Endpoints sob o path `/api/v1/auth/*` são abertos ao público.
* Todos os demais endpoints exigem obrigatoriamente a presença do header:
  `Authorization: Bearer <JWT_ACCESS_TOKEN>`

### 1.4. Formato de Erro Padronizado (Global Exception Payload)
Qualquer requisição que resulte em status HTTP >= 400 retornará o JSON estruturado abaixo:

```json
{
  "timestamp": "2026-07-01T15:30:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "A senha informada deve conter no mínimo 8 caracteres contendo letras e números.",
  "path": "/api/v1/auth/register"
}
```

---

## 2. Autenticação e Usuários (Auth & Users)

### 2.1. `POST /api/v1/auth/register`
Efetua o cadastro de um novo praticante de musculação.

* **Request Body:**
```json
{
  "name": "Gabriel Silva",
  "email": "gabriel.calistenia@email.com",
  "password": "SenhaSegura123!"
}
```

* **Response (201 Created):**
```json
{
  "id": "usr-9a2f-4881",
  "name": "Gabriel Silva",
  "email": "gabriel.calistenia@email.com",
  "emailVerifiedAt": null,
  "createdAt": "2026-07-01T10:00:00.000Z"
}
```
* **Regra de negócio:** o registro gera `email_verification_token_hash`/`email_verification_expires_at` (`02_SCHEMA_SQLITE.md`, tabela `users`) e dispara o e-mail de verificação via `EmailService`. `emailVerifiedAt` começa `null` e só é preenchido por `GET /auth/verify-email/{token}` (§2.4).

---

### 2.2. `POST /api/v1/auth/login`
Autentica o usuário e devolve a dupla de tokens de acesso JWT.

* **Request Body:**
```json
{
  "email": "gabriel.calistenia@email.com",
  "password": "SenhaSegura123!"
}
```

* **Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c3ItOWEyZi00ODgxIiwiaWF0IjoxNzg1Njk2MDAwfQ...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c3ItOWEyZi00ODgxIiwidHlwZSI6InJlZnJlc2gifQ..."
}
```
* **Regras de negócio:**
  1. Retorna `403 Forbidden` se `users.email_verified_at IS NULL` — o usuário não pode logar sem verificar o e-mail primeiro (`00_PRD_IRONTRACK.md` §4.3).
  2. Um login bem-sucedido cria uma nova linha em `refresh_tokens` (`02_SCHEMA_SQLITE.md`) correspondente ao `refreshToken` emitido.
  3. O bloqueio temporário de 15 minutos após 5 falhas consecutivas já está especificado em `05_DEVOPS_E_SEGURANCA.md` §E.2 — não reescrito aqui, apenas referenciado.

---

### 2.3. `GET /api/v1/users/me`
Recupera os detalhes de perfil do usuário logado baseado no token de autenticação enviado.

* **Response (200 OK):**
```json
{
  "id": "usr-9a2f-4881",
  "name": "Gabriel Silva",
  "email": "gabriel.calistenia@email.com",
  "emailVerifiedAt": "2026-07-01T10:05:00.000Z",
  "createdAt": "2026-07-01T10:00:00.000Z"
}
```

---

### 2.4. `POST /api/v1/auth/refresh`
Renova o par de tokens a partir de um `refreshToken` válido.

* **Request Body:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiIs..." }
```
* **Regra de negócio — rotação obrigatória:** valida o token contra `refresh_tokens` (por `token_hash`, não-revogado, não-expirado). Revoga (`revoked_at`) o refresh token usado e emite um novo par de tokens (nova linha em `refresh_tokens`) — **nunca reemite o mesmo refresh token**. Isso limita a janela de uso de um refresh token vazado a uma única renovação.
* **Response (200 OK):** mesmo formato de `POST /auth/login` (§2.2).
* **Erros:** `401 Unauthorized` se o token for inválido, expirado ou já revogado.

---

### 2.5. `POST /api/v1/auth/logout`
Revoga um refresh token, encerrando a sessão correspondente no dispositivo.

* **Request Body:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiIs..." }
```
* **Regra de negócio:** marca `revoked_at` na linha correspondente de `refresh_tokens`. Idempotente — revogar um token já revogado não é um erro.
* **Response (204 No Content)**.

---

### 2.6. `GET /api/v1/auth/verify-email/{token}`
Confirma o e-mail do usuário a partir do link enviado no cadastro (§2.1) ou após troca de e-mail (§2.8).

* **Regra de negócio:** `{token}` é o valor bruto enviado por e-mail; o backend calcula o hash e compara com `users.email_verification_token_hash`, validando `email_verification_expires_at`. Em caso de sucesso, seta `email_verified_at = now()` e limpa os campos de token (`email_verification_token_hash`, `email_verification_expires_at`).
* **Response (200 OK):**
```json
{ "email": "gabriel.calistenia@email.com", "verifiedAt": "2026-07-01T10:05:00.000Z" }
```
* **Erros:** `400 Bad Request` se o token for inválido ou já expirado.

---

### 2.7. `POST /api/v1/auth/forgot-password` e `POST /api/v1/auth/reset-password`
Fluxo de recuperação de senha em duas etapas.

* **`POST /auth/forgot-password`** — Request:
  ```json
  { "email": "gabriel.calistenia@email.com" }
  ```
  **Sempre** responde `202 Accepted` com uma mensagem genérica, independentemente de o e-mail existir na base ou não (evita enumeração de usuários). Se o e-mail existir, gera `password_reset_token_hash` + `password_reset_expires_at` (validade de **1 hora**, `00_PRD_IRONTRACK.md` §4.3) e dispara o e-mail com o link de redefinição.

* **`POST /auth/reset-password`** — Request:
  ```json
  { "token": "...", "newPassword": "NovaSenhaSegura456!" }
  ```
  Valida o token (hash + `password_reset_expires_at`) e a força da nova senha (mínimo 8 caracteres, contendo letras e números — `00_PRD_IRONTRACK.md` §4.3). Atualiza `password_hash`, limpa os campos de reset (`password_reset_token_hash`, `password_reset_expires_at`) e **revoga todos os `refresh_tokens` ativos do usuário** — força logout de todas as sessões/dispositivos como medida de segurança padrão pós-reset. Response `200 OK`.

---

### 2.8. `PATCH /api/v1/users/me`
Edita o perfil do usuário autenticado.

* **Request Body** (todos os campos opcionais):
```json
{
  "name": "Gabriel Silva Santos",
  "email": "gabriel.novo@email.com"
}
```
* **Regra de negócio:** se `email` for enviado e for diferente do valor atual, o backend seta `email_verified_at = NULL` e regenera `email_verification_token_hash`/`email_verification_expires_at`, reexigindo a verificação do novo e-mail pelo mesmo mecanismo de `GET /auth/verify-email/{token}` (§2.6) usado no cadastro.
* **Response (200 OK):** mesmo formato de `GET /users/me` (§2.3).
* **Nota de escopo:** upload de foto de perfil **não** faz parte deste contrato — depende de uma decisão de infraestrutura de object storage ainda não tomada. Nenhum campo `photoUrl` existe em nenhum DTO desta API nesta versão.

---

### 2.9. `POST /api/v1/users/me/change-password`
Troca a senha do usuário autenticado, já logado (fluxo distinto do reset via e-mail em §2.7).

* **Request Body:**
```json
{ "currentPassword": "SenhaAtual123!", "newPassword": "NovaSenhaSegura456!" }
```
* **Regra de negócio:** valida `currentPassword` contra `password_hash`; valida a força de `newPassword` (mesma regra de §2.7); atualiza `password_hash`; **revoga todos os `refresh_tokens` ativos do usuário** (mesma lógica de segurança do reset de senha).
* **Response (204 No Content)**.
* **Erros:** `401 Unauthorized` se `currentPassword` não conferir.

---

### 2.10. `DELETE /api/v1/users/me`
Solicita a exclusão da conta do usuário autenticado (`11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md` §B/§D).

* **Request Body:**
```json
{ "password": "SenhaAtual123!" }
```
* **Regra de negócio:** reconfirma a senha atual (proteção contra sequestro de sessão). Seta `users.deletion_requested_at = now()`, revoga todos os `refresh_tokens` do usuário e remove suas `push_subscriptions`. A conta entra em período de carência de 30 dias antes da exclusão física (job agendado, não um endpoint).
* **Response (202 Accepted):**
```json
{ "deletionScheduledFor": "2026-08-06T00:00:00.000Z" }
```
* **Erros:** `401 Unauthorized` se `password` não conferir.

---

### 2.11. `POST /api/v1/auth/cancel-deletion`
Cancela uma exclusão de conta solicitada, dentro do período de carência.

* **Regra de negócio:** mesmo mecanismo de prova de posse de `POST /auth/forgot-password` (§2.7) — envia link por e-mail; ao ser confirmado, seta `users.deletion_requested_at = NULL`.
* **Response (200 OK)**.
* **Erros:** `400 Bad Request` se não houver exclusão pendente para o e-mail informado, ou se o período de carência já tiver expirado.

---

## 3. Gestão de Treino (Ciclos e Dias)

### 3.1. `POST /api/v1/cycles`
Cria um novo ciclo de planejamento de treinos com foco em hipertrofia ou força.

* **Request Body:**
```json
{
  "name": "Foco Hipertrofia Pernas e Ombros (ABC)",
  "startDate": "2026-07-01T00:00:00.000Z"
}
```

* **Response (201 Created):**
```json
{
  "id": "cyc-4e8c-8f43",
  "name": "Foco Hipertrofia Pernas e Ombros (ABC)",
  "isActive": true,
  "startDate": "2026-07-01T00:00:00.000Z",
  "endDate": null,
  "archivedAt": null,
  "createdAt": "2026-07-01T10:05:00.000Z"
}
```

---

### 3.2. `GET /api/v1/cycles/active`
Obtém os dados completos do ciclo que está ativo no momento para o usuário autenticado, listando de forma aninhada todos os dias de treino configurados para este ciclo.

* **Response (200 OK):**
```json
{
  "id": "cyc-4e8c-8f43",
  "name": "Ciclo Foco Força & Hipertrofia",
  "isActive": true,
  "startDate": "2026-07-01T00:00:00.000Z",
  "endDate": null,
  "archivedAt": null,
  "createdAt": "2026-07-01T10:05:00.000Z",
  "trainingDays": [
    {
      "id": "day-3a21-9d8a",
      "name": "Dia A - Supino & Ombros (Push)",
      "colorCode": "#E74C3C",
      "orderIndex": 1
    },
    {
      "id": "day-4a52-1f3c",
      "name": "Dia B - Costas & Bíceps (Pull)",
      "colorCode": "#2ECC71",
      "orderIndex": 2
    },
    {
      "id": "day-5b12-9c8e",
      "name": "Dia C - Pernas Completo (Legs)",
      "colorCode": "#3498DB",
      "orderIndex": 3
    }
  ]
}
```

---

### 3.3. `GET /api/v1/cycles`
Lista todos os ciclos (ativos e arquivados) do usuário autenticado, ordenados por `createdAt DESC`.

* **Response (200 OK):**
```json
[
  {
    "id": "cyc-4e8c-8f43",
    "name": "Ciclo Foco Força & Hipertrofia",
    "isActive": true,
    "startDate": "2026-07-01T00:00:00.000Z",
    "endDate": null,
    "archivedAt": null,
    "createdAt": "2026-07-01T10:05:00.000Z"
  },
  {
    "id": "cyc-1a2b-3c4d",
    "name": "Ciclo Volume Inicial",
    "isActive": false,
    "startDate": "2026-04-01T00:00:00.000Z",
    "endDate": "2026-06-30T00:00:00.000Z",
    "archivedAt": "2026-06-30T09:00:00.000Z",
    "createdAt": "2026-04-01T09:00:00.000Z"
  }
]
```

---

### 3.4. `GET /api/v1/cycles/{cycleId}`
Obtém o detalhe de um ciclo específico (ativo ou não), com os dias de treino aninhados — mesma forma de response de `GET /cycles/active` (§3.2), porém por `cycleId` explícito.

---

### 3.5. `PATCH /api/v1/cycles/{cycleId}`
Edita `name`, `startDate` e/ou `endDate` de um ciclo existente. Todos os campos do request body são opcionais (atualização parcial).

* **Request Body:**
```json
{
  "name": "Ciclo Foco Força & Hipertrofia (Revisado)",
  "endDate": "2026-09-30T00:00:00.000Z"
}
```

* **Response (200 OK):** objeto do ciclo atualizado, mesma forma de §3.1.

---

### 3.6. `DELETE /api/v1/cycles/{cycleId}`
Arquiva o ciclo (seta `archivedAt`, `02_SCHEMA_SQLITE.md`, tabela `training_cycles`) — **não** é uma exclusão física de dados. Um ciclo arquivado é automaticamente desativado (`isActive` passa a `false`) caso estivesse ativo.

* **Response (200 OK):**
```json
{
  "id": "cyc-1a2b-3c4d",
  "name": "Ciclo Volume Inicial",
  "isActive": false,
  "startDate": "2026-04-01T00:00:00.000Z",
  "endDate": "2026-06-30T00:00:00.000Z",
  "archivedAt": "2026-06-30T09:00:00.000Z",
  "createdAt": "2026-04-01T09:00:00.000Z"
}
```

---

### 3.7. `PATCH /api/v1/cycles/{cycleId}/activate`
Define o ciclo informado como o ciclo ativo do usuário.

* **Regra de negócio:** como `training_cycles` possui um índice único parcial garantindo **um único ciclo ativo por usuário** (`idx_cycles_user_single_active`, `02_SCHEMA_SQLITE.md`), este endpoint executa a troca em uma **única transação atômica**: desativa o ciclo atualmente ativo (se houver) e ativa o ciclo alvo, sem jamais deixar o usuário com dois ciclos ativos simultaneamente nem sem nenhum ciclo ativo entre os dois passos. Se a transação falhar por qualquer motivo (ex: violação do índice único por condição de corrida), a API retorna `422 Unprocessable Entity` e nenhuma alteração é persistida.

* **Response (200 OK):** objeto do ciclo recém-ativado, mesma forma de §3.1, com `isActive: true`.

---

### 3.8. `POST /api/v1/cycles/{cycleId}/days`
Cria um novo dia de treino dentro do ciclo (ex: "Dia A - Push").

* **Request Body:**
```json
{
  "name": "Dia A - Supino & Ombros (Push)",
  "colorCode": "#E74C3C",
  "orderIndex": 1
}
```

* **Response (201 Created):**
```json
{
  "id": "day-3a21-9d8a",
  "cycleId": "cyc-4e8c-8f43",
  "name": "Dia A - Supino & Ombros (Push)",
  "colorCode": "#E74C3C",
  "orderIndex": 1,
  "createdAt": "2026-07-01T10:10:00.000Z"
}
```

---

### 3.9. `PATCH /api/v1/cycles/{cycleId}/days/{dayId}`
Edita `name` e/ou `colorCode` de um dia de treino existente (campos opcionais, atualização parcial). Reordenação de dias não é feita por este endpoint — ver §3.11.

* **Response (200 OK):** objeto do dia atualizado, mesma forma de §3.8.

---

### 3.10. `DELETE /api/v1/cycles/{cycleId}/days/{dayId}`
Remove um dia de treino do ciclo. A remoção também apaga (via `ON DELETE CASCADE`) o template de exercícios associado (`training_day_exercises`, §3.12).

* **Regra de negócio:** `training_sessions.training_day_id` referencia `training_days` com `ON DELETE RESTRICT` (`02_SCHEMA_SQLITE.md` §2) — um dia que já possui pelo menos uma sessão de treino executada (mesmo `CANCELLED`) **não pode ser removido**, e a API retorna `422 Unprocessable Entity` nesse caso, para nunca perder o histórico de treinos já realizados.

* **Response (204 No Content)** em caso de sucesso.

---

### 3.11. `PATCH /api/v1/cycles/{cycleId}/days/reorder`
Reordena os dias de treino de um ciclo.

* **Request Body:**
```json
{
  "dayIds": ["day-4a52-1f3c", "day-3a21-9d8a", "day-5b12-9c8e"]
}
```
O `orderIndex` de cada dia é recalculado com base na posição do respectivo `id` no array (índice 0 → `orderIndex = 1`, e assim sucessivamente).

* **Response (200 OK):** array dos dias do ciclo já reordenados, mesma forma de §3.8.

---

### 3.12. `POST /api/v1/cycles/{cycleId}/days/{dayId}/exercises`
Adiciona um exercício ao **template** do dia de treino (tabela `training_day_exercises`, `02_SCHEMA_SQLITE.md`) — a composição planejada que será copiada para `session_exercises` toda vez que uma sessão for iniciada a partir deste dia (`POST /sessions/start`, §5.1).

* **Request Body:**
```json
{
  "exerciseId": "exe-0001-bench",
  "targetSets": 3,
  "targetRepsMin": 8,
  "targetRepsMax": 12,
  "notes": "Foco em controle excêntrico"
}
```

* **Response (201 Created):**
```json
{
  "id": "tde-7f21-88ab",
  "trainingDayId": "day-3a21-9d8a",
  "exerciseId": "exe-0001-bench",
  "exerciseName": "Supino Reto com Barra",
  "orderIndex": 1,
  "targetSets": 3,
  "targetRepsMin": 8,
  "targetRepsMax": 12,
  "notes": "Foco em controle excêntrico",
  "createdAt": "2026-07-01T10:12:00.000Z"
}
```

---

### 3.13. `PATCH /api/v1/cycles/{cycleId}/days/{dayId}/exercises/{dayExerciseId}`
Edita a meta planejada (`targetSets`, `targetRepsMin`, `targetRepsMax`) e/ou `notes` de um exercício já presente no template do dia (campos opcionais, atualização parcial). Não altera `exerciseId` — para trocar o exercício, remova (§3.14) e adicione novamente (§3.12).

* **Response (200 OK):** objeto atualizado, mesma forma de §3.12.

---

### 3.14. `DELETE /api/v1/cycles/{cycleId}/days/{dayId}/exercises/{dayExerciseId}`
Remove um exercício do template do dia de treino. Não afeta sessões já executadas (`session_exercises` já copiados permanecem intactos — a relação é apenas de origem no momento da cópia, não uma referência viva).

* **Response (204 No Content)**.

---

### 3.15. `PATCH /api/v1/cycles/{cycleId}/days/{dayId}/exercises/reorder`
Reordena os exercícios do template do dia.

* **Request Body:**
```json
{
  "dayExerciseIds": ["tde-9b31-11cd", "tde-7f21-88ab"]
}
```

* **Response (200 OK):** array dos exercícios do template já reordenados, mesma forma de §3.12.

---

## 4. Biblioteca de Exercícios (Focus: Gym / Strength)

### 4.1. `GET /api/v1/exercises`
Retorna a lista de exercícios disponíveis para compor os treinos. Pode ser filtrada por grupo muscular e distinção entre padrão ou customizados criados pelo próprio usuário.

* **Query Parameters:**
  * `muscleGroup` (String, opcional): Grupo muscular primário (ex: "Peito", "Costas", "Pernas", "Ombros").
  * `isCustom` (Boolean, opcional): `true` para retornar apenas os personalizados criados pelo usuário, `false` para exercícios padrão.

* **Request Example:**
  `GET /api/v1/exercises?muscleGroup=Peito&isCustom=false`

* **Response (200 OK):**
```json
[
  {
    "id": "exe-0001-bench",
    "name": "Supino Reto com Barra",
    "primaryMuscle": "Peito",
    "type": "STRENGTH",
    "loadIncrementKg": 2.5
  },
  {
    "id": "exe-0005-incline-db",
    "name": "Supino Inclinado com Halteres",
    "primaryMuscle": "Peito",
    "type": "STRENGTH",
    "loadIncrementKg": 2.5
  },
  {
    "id": "exe-0009-pec-deck",
    "name": "Crucifixo Máquina (Pec Deck)",
    "primaryMuscle": "Peito",
    "type": "STRENGTH",
    "loadIncrementKg": 2.5
  }
]
```

---

### 4.2. `POST /api/v1/exercises`
Permite ao usuário cadastrar um exercício de musculação personalizado na biblioteca privada.

* **Request Body:**
```json
{
  "name": "Supino Inclinado Articulado Convergente",
  "primaryMuscle": "Peito",
  "loadIncrementKg": 2.5
}
```
`loadIncrementKg` é opcional — se omitido, assume o padrão `2.5` (`02_SCHEMA_SQLITE.md`, coluna `exercise_library.load_increment_kg`).

* **Response (201 Created):**
```json
{
  "id": "exe-9f8e-4a3b",
  "name": "Supino Inclinado Articulado Convergente",
  "primaryMuscle": "Peito",
  "type": "STRENGTH",
  "isCustom": true,
  "loadIncrementKg": 2.5,
  "createdAt": "2026-07-01T15:10:00.000Z"
}
```

---

### 4.3. `PATCH /api/v1/exercises/{exerciseId}`
Edita um exercício customizado do próprio usuário (`name`, `primaryMuscle`, `loadIncrementKg` — campos opcionais).

* **Regra de negócio:** só é permitido quando `isCustom = true` **e** o exercício pertence ao usuário autenticado (`exercise_library.user_id`). Caso contrário, retorna `403 Forbidden` — inclusive para exercícios padrão da biblioteca (`isCustom = false`), que nunca são editáveis via API.

* **Response (200 OK):** objeto atualizado, mesma forma de §4.2.

---

### 4.4. `DELETE /api/v1/exercises/{exerciseId}`
Remove um exercício customizado do próprio usuário.

* **Regra de negócio:** mesma restrição de `isCustom = true` + posse do §4.3 (`403 Forbidden` caso contrário). Como `session_exercises.exercise_id` referencia `exercise_library` com `ON DELETE RESTRICT` (`02_SCHEMA_SQLITE.md` §2), a remoção falha com `422 Unprocessable Entity` se o exercício já foi utilizado em alguma sessão registrada.

* **Response (204 No Content)** em caso de sucesso.

---

## 5. Diário de Bordo (Registro de Sessões - Core)

Esta seção orquestra o início, execução e finalização das sessões de treino, capturando séries individuais de musculação.

### 5.1. `POST /api/v1/sessions/start`
Inicia um treino ativo com base em um dia de treino do ciclo. O backend copia o template de exercícios do dia (`training_day_exercises`, ordenado por `orderIndex`, `02_SCHEMA_SQLITE.md`) para novas linhas em `session_exercises`, e busca a sessão `COMPLETED` mais recente para o mesmo `trainingDayId` (usando o índice `idx_sessions_user_day_start`, `02_SCHEMA_SQLITE.md`) para popular a referência do último desempenho de cada exercício.

* **Persistência do snapshot de meta:** ao copiar o template, o backend grava `targetSets`/`targetRepsMin`/`targetRepsMax` (junto com `trainingDayExerciseId`, para rastreabilidade de origem) diretamente em cada linha de `session_exercises` criada — não apenas os retorna na response abaixo. Esses três campos são um **snapshot congelado** no instante da cópia (colunas de `session_exercises` descritas em `02_SCHEMA_SQLITE.md` e `06_LOGICA_DE_PROGRESSAO.md` §1): se o template do dia for editado depois, sessões já iniciadas/concluídas mantêm a meta original com que foram executadas.

* **Request Body:**
```json
{
  "trainingDayId": "day-3a21-9d8a"
}
```

* **Response (201 Created):**
```json
{
  "id": "ses-2b81-9f93",
  "userId": "usr-9a2f-4881",
  "trainingDayId": "day-3a21-9d8a",
  "startTime": "2026-07-01T15:00:00.000Z",
  "status": "IN_PROGRESS",
  "createdAt": "2026-07-01T15:00:00.000Z",
  "exercises": [
    {
      "sessionExerciseId": "sexe-e101",
      "exerciseId": "exe-0001-bench",
      "exerciseName": "Supino Reto com Barra",
      "orderIndex": 1,
      "targetSets": 3,
      "targetRepsMin": 8,
      "targetRepsMax": 12,
      "notes": null,
      "lastPerformance": {
        "date": "2026-06-24",
        "sets": [
          { "setNumber": 1, "weight": 80.0, "reps": 10, "rpe": 8 },
          { "setNumber": 2, "weight": 80.0, "reps": 10, "rpe": 9 },
          { "setNumber": 3, "weight": 80.0, "reps": 8, "rpe": 10 }
        ]
      }
    }
  ]
}
```
Se não houver sessão `COMPLETED` anterior para o mesmo `trainingDayId`, `lastPerformance` é `null` para aquele exercício.

* **Regra de negócio:** retorna `422 Unprocessable Entity` se o usuário já possuir uma sessão `IN_PROGRESS` em aberto — evita duas sessões simultâneas e garante que "último treino" (§ acima) seja sempre determinístico.

---

### 5.2. `POST /api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets`
Adiciona ou atualiza uma série executada no exercício dentro da sessão atual. Este é o endpoint mais crucial da aplicação de treino.

> **Nota sobre o path param:** `{sessionExerciseId}` (não `{exerciseId}`) — referencia a instância específica de `session_exercises` da sessão em andamento, não o catálogo `exercise_library`. Como `session_exercises` não garante exercício único por sessão (o mesmo exercício pode ser reexecutado deliberadamente, ex: bi-set com séries intercaladas), o path precisa apontar para a instância exata, nunca para o exercício genérico.

* **Request Body:**
```json
{
  "clientGeneratedId": "c7e1b2a0-3f5d-4a1e-9c8b-1a2b3c4d5e6f",
  "setNumber": 1,
  "weight": 80.5,
  "reps": 10,
  "rpe": 8,
  "techniques": ["DROP_SET"],
  "notes": "Pausa técnica de 2 seg"
}
```
* `clientGeneratedId` (String, **obrigatório**): UUID gerado no cliente (mapeia para `exercise_sets.client_generated_id`, `02_SCHEMA_SQLITE.md`). Reenvios com o mesmo valor são **idempotentes** — o serviço retorna o registro já existente (mesmo status HTTP `201 Created`) em vez de duplicar ou falhar, viabilizando o reenvio automático da fila de sincronização offline (`04_FRONTEND_UI_COMPONENTES.md` §E.2) sem risco de série duplicada.
* `techniques` (Array de String, opcional, padrão `[]`): valores do enum `exercise_set_techniques.technique` (`02_SCHEMA_SQLITE.md`): `FALHA`, `DROP_SET`, `REST_PAUSE`, `PAUSA`, `NEGATIVA_FORCADA`.

* **Response (201 Created):**
```json
{
  "id": "set-e1s1",
  "sessionExerciseId": "sexe-e101",
  "clientGeneratedId": "c7e1b2a0-3f5d-4a1e-9c8b-1a2b3c4d5e6f",
  "setNumber": 1,
  "weight": 80.5,
  "reps": 10,
  "repsTarget": 10,
  "holdTimeSeconds": null,
  "holdTimeTarget": null,
  "progressionStepId": null,
  "rpe": 8,
  "techniques": ["DROP_SET"],
  "notes": "Pausa técnica de 2 seg",
  "createdAt": "2026-07-01T15:05:00.000Z"
}
```

---

### 5.3. `PATCH /api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{setId}`
Edita uma série já registrada na sessão em andamento (`weight`, `reps`, `rpe`, `techniques`, `notes` — todos opcionais, atualização parcial). `setNumber` e `clientGeneratedId` são imutáveis após a criação.

* **Response (200 OK):** objeto da série atualizada, mesma forma de §5.2.

---

### 5.4. `DELETE /api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/sets/{setId}`
Remove uma série já registrada.

* **Regra de negócio:** a remoção **não** renumera automaticamente o `setNumber` das séries restantes — evita reescrever histórico silenciosamente. A UI deve exibir os números de série como estão, com possíveis "buracos" na sequência após uma remoção (ex: séries 1 e 3 permanecem após remover a 2).

* **Response (204 No Content)**.

---

### 5.5. `PATCH /api/v1/sessions/{sessionId}/finish`
Finaliza o treino ativo e calcula automaticamente as estatísticas de volume total levantado e tempo total sob esforço.

* **Response (200 OK):**
```json
{
  "id": "ses-2b81-9f93",
  "status": "COMPLETED",
  "startTime": "2026-07-01T15:00:00.000Z",
  "endTime": "2026-07-01T16:15:00.000Z",
  "durationMinutes": 75,
  "totalVolume": 2415.0
}
```
> **Nota de Cálculo (Backend):** O `totalVolume` corresponde à soma de todos os `weight` x `reps` das séries registradas nesta sessão específica de treino. A `durationMinutes` é a diferença absoluta de tempo entre o `startTime` e o `endTime` convertido para minutos inteiros.

---

### 5.6. `GET /api/v1/sessions`
Lista as sessões de treino do usuário autenticado, com filtros e paginação.

* **Query Parameters:**
  * `cycleId` (String, opcional): filtra sessões cujo `trainingDayId` pertença a este ciclo.
  * `trainingDayId` (String, opcional): filtra por dia de treino específico.
  * `from` / `to` (String ISO8601, opcionais): intervalo de `startTime`.
  * `page` (Integer, opcional, padrão `0`): página solicitada (0-indexed).
  * `pageSize` (Integer, opcional, padrão `20`, máximo `100`).

* **Response (200 OK):**
```json
{
  "content": [
    {
      "id": "ses-2b81-9f93",
      "trainingDayId": "day-3a21-9d8a",
      "trainingDayName": "Dia A - Supino & Ombros (Push)",
      "startTime": "2026-07-01T15:00:00.000Z",
      "endTime": "2026-07-01T16:15:00.000Z",
      "status": "COMPLETED",
      "durationMinutes": 75,
      "totalVolume": 2415.0
    }
  ],
  "page": 0,
  "pageSize": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 5.7. `GET /api/v1/sessions/{sessionId}`
Obtém o detalhe completo de uma sessão específica, incluindo exercícios e séries registradas.

* **Response (200 OK):** mesma forma de resposta de `POST /sessions/start` (§5.1), com a diferença de que cada item de `exercises[]` inclui também a lista `sets[]` já registrada naquela sessão (mesma forma de item de `POST .../sets`, §5.2), além dos campos `status`, `endTime`, `durationMinutes` e `totalVolume` quando a sessão já estiver `COMPLETED` (mesma forma de `PATCH .../finish`, §5.5).

---

## 6. Métricas e Progressão de Carga (Insights)

### 6.1. `GET /api/v1/metrics/exercises/{exerciseId}/history`
Obtém a lista histórica das execuções de um determinado exercício nas sessões passadas do usuário para plotagem de gráficos na UI do frontend.

* **Response (200 OK):**
```json
[
  {
    "date": "2026-06-15",
    "maxWeight": 74.0,
    "totalVolume": 2220.0
  },
  {
    "date": "2026-06-22",
    "maxWeight": 78.0,
    "totalVolume": 2340.0
  },
  {
    "date": "2026-07-01",
    "maxWeight": 80.5,
    "totalVolume": 2415.0
  }
]
```

---

### 6.2. `GET /api/v1/sessions/{sessionId}/exercises/{sessionExerciseId}/suggestion`
Consulta o motor de progressão de carga para sugerir as metas operacionais do exercício **dentro da sessão ativa** — contextual ao treino de hoje, não um endpoint de catálogo desacoplado de sessão (substitui o antigo `GET /metrics/exercises/{exerciseId}/suggestion`, removido nesta revisão).

* **Response (200 OK):**
```json
{
  "sessionExerciseId": "sexe-e101",
  "exerciseId": "exe-0001-bench",
  "targetWeight": 82.0,
  "targetReps": 10,
  "basis": "No seu treino de 2026-07-01, você atingiu a meta de 3 séries com 10 repetições usando 80.5kg (RPE 8), no teto da faixa alvo de 8-12 repetições do template. Para manter a sobrecarga progressiva no Supino Reto, o sistema sugere um incremento de 2.0kg na carga (load_increment_kg = 2.5, arredondado à menor fração de anilha disponível) mantendo o alvo de 10 repetições."
}
```
* **Base do cálculo** (contrato de transporte apenas — a lógica de negócio completa é formalizada em `06_LOGICA_DE_PROGRESSAO.md`):
  1. a meta do template do dia (`targetRepsMin`/`targetRepsMax`) — lida do **snapshot congelado** já persistido em `session_exercises` (`02_SCHEMA_SQLITE.md`, colunas de snapshot descritas em `06_LOGICA_DE_PROGRESSAO.md` §1) no momento em que a sessão começou (§5.1), **nunca** de uma nova consulta a `training_day_exercises`, que pode já ter sido editado desde então;
  2. o último desempenho registrado para este `sessionExercise` (campo `lastPerformance` retornado por `POST /sessions/start`, §5.1);
  3. `exercise_library.loadIncrementKg` (§4.1) para calcular o incremento sugerido quando o teto da faixa de repetições foi atingido.

`GET /metrics/exercises/{exerciseId}/history` (§6.1) permanece inalterado e desacoplado de sessão — é um endpoint de catálogo/histórico para gráficos, não o de sugestão ao vivo.

---

### 6.3. `GET /api/v1/metrics/exercises/{exerciseId}/pr`
Retorna os recordes pessoais do usuário para um exercício, conforme as duas definições formalizadas em `06_LOGICA_DE_PROGRESSAO.md` §E.

* **Response (200 OK):**
```json
{
  "exerciseId": "exe-0001-bench",
  "loadPr": { "weight": 82.5, "reps": 8, "date": "2026-07-01" },
  "volumePr": { "totalVolume": 2415.0, "date": "2026-07-01" }
}
```
`loadPr` e/ou `volumePr` são `null` se o usuário nunca executou o exercício. Calculado exclusivamente sobre séries **contáveis** (`06_LOGICA_DE_PROGRESSAO.md` §B) — séries com técnica não-contável (`DROP_SET`, `REST_PAUSE`, `NEGATIVA_FORCADA`) nunca contam para nenhum dos dois PRs.

---

### 6.4. `GET /api/v1/metrics/overview`
Resumo geral de atividade do usuário autenticado, para o dashboard.

* **Response (200 OK):**
```json
{
  "sessionsThisWeek": 3,
  "sessionsThisMonth": 11,
  "totalVolumeThisWeek": 7200.0,
  "totalVolumeThisMonth": 28400.0,
  "currentStreakDays": 5
}
```
Todos os campos são calculados exclusivamente sobre sessões `COMPLETED` do usuário autenticado.

---

### 6.5. `GET /api/v1/metrics/frequency`
Série temporal de frequência de treino, para gráficos de assiduidade.

* **Query Parameters:** `period` (String, obrigatório): `week` ou `month`.
* **Response (200 OK):**
```json
[
  { "date": "2026-06-30", "sessionCount": 1 },
  { "date": "2026-07-01", "sessionCount": 0 }
]
```
Uma entrada por dia dentro do período solicitado — dias sem treino aparecem explicitamente com `sessionCount: 0` (nunca omitidos), para que o frontend possa plotar um gráfico contínuo sem preencher lacunas manualmente.

---

### 6.6. `GET /api/v1/metrics/muscle-groups`
Distribuição de volume total por grupo muscular, para identificar desequilíbrios de treino.

* **Query Parameters:** `period` (String, obrigatório): `week` ou `month`.
* **Response (200 OK):**
```json
[
  { "muscleGroup": "Peito", "totalVolume": 3200.0 },
  { "muscleGroup": "Costas", "totalVolume": 2800.0 }
]
```
Agrega `exercise_library.primary_muscle` através de `session_exercises`/`exercise_sets`, considerando apenas séries **contáveis** (`06_LOGICA_DE_PROGRESSAO.md` §B) e apenas sessões `COMPLETED` dentro do período solicitado.

---

## 7. Alertas e Notificações

### 7.1. `GET /api/v1/alerts`
Lista os alertas de estagnação (`06_LOGICA_DE_PROGRESSAO.md` §D, materializados em `stagnation_alerts`, `02_SCHEMA_SQLITE.md`) ativos para o usuário autenticado.

* **Regra de negócio:** retorna alertas com `resolvedAt IS NULL` **e** (`snoozedUntil IS NULL` OU `snoozedUntil` já no passado). Um alerta é criado quando `ProgressiveOverloadService.detectStagnation` (`06_LOGICA_DE_PROGRESSAO.md` §F) retorna `true` para um par `exercise_id`/`training_day_id` que ainda não tenha alerta em aberto para o mesmo par — evita duplicatas a cada sessão enquanto a estagnação persistir. O gatilho de detecção é **síncrono**, disparado ao final de `PATCH /sessions/{id}/finish` (§5.5): após marcar a sessão como `COMPLETED`, o backend reavalia estagnação para cada exercício daquela sessão — não há job agendado (`cron`) separado para isso. Um alerta é automaticamente considerado resolvido (`resolvedAt` setado) na próxima vez que `detectStagnation` for reavaliado para o mesmo par e retornar `false` (o usuário voltou a progredir).

* **Response (200 OK):**
```json
[
  {
    "id": "alt-7c21-99ab",
    "exerciseId": "exe-0001-bench",
    "exerciseName": "Supino Reto com Barra",
    "trainingDayId": "day-3a21-9d8a",
    "detectedAt": "2026-07-01T16:15:00.000Z",
    "snoozedUntil": null
  }
]
```

---

### 7.2. `POST /api/v1/alerts/{alertId}/snooze`
Adia a exibição de um alerta de estagnação sem marcá-lo como resolvido.

* **Request Body:**
```json
{ "days": 7 }
```
* **Response (200 OK):** objeto do alerta atualizado (mesma forma de §7.1), com `snoozedUntil = now() + days`.

---

### 7.3. `POST /api/v1/users/me/push-subscription`
Cria ou atualiza a inscrição de notificações push nativas (Expo Push Service) do dispositivo atual do usuário.

* **Request Body:**
```json
{
  "expoPushToken": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
  "reminderDays": ["MON", "WED", "FRI"],
  "reminderTime": "18:30"
}
```
* **Regra de negócio:** cria ou atualiza (`UNIQUE(user_id, expo_push_token)`, `02_SCHEMA_SQLITE.md`) a inscrição correspondente.
* **Response (201 Created):**
```json
{
  "id": "psub-4f11-88cd",
  "expoPushToken": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
  "reminderDays": ["MON", "WED", "FRI"],
  "reminderTime": "18:30",
  "enabled": true,
  "createdAt": "2026-07-01T18:00:00.000Z"
}
```

---

### 7.4. `DELETE /api/v1/users/me/push-subscription`
Remove (ou desativa) a inscrição de notificações push de um dispositivo.

* **Request Body:**
```json
{ "expoPushToken": "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]" }
```
* **Response (204 No Content)**.

> **Nota de execução:** o envio efetivo dos lembretes é feito por um job agendado (`@Scheduled`, `07_ROADMAP_BACKEND.md` §C.5) que roda a cada minuto, compara `reminderDays`/`reminderTime` com o dia/horário atual, e **pula o envio** se o usuário já tiver uma sessão `COMPLETED` no dia corrente para o `trainingDayId` esperado — evita notificar quem já treinou. O envio é uma chamada HTTP ao Expo Push API (`https://exp.host/--/api/v2/push/send`) usando o `expoPushToken` como destinatário — não exige chaves de assinatura próprias (VAPID) para o caso de uso padrão; ver `05_DEVOPS_E_SEGURANCA.md` §D.
