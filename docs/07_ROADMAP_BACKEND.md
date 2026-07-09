# 07_ROADMAP_BACKEND.md - Roadmap de Implementação do Backend

Este documento é o roadmap ordenado, sprint a sprint, de implementação real do backend Java/Spring Boot do **IronTrack**, referenciando exatamente as entidades, DTOs, endpoints e regras de negócio já fixados em `02_SCHEMA_SQLITE.md` a `06_LOGICA_DE_PROGRESSAO.md`. É consumido por uma IA de codificação para gerar o código Java/Spring Boot propriamente dito — por isso cada tarefa abaixo aponta para o contrato exato que satisfaz (nome de classe sugerido, endpoint, seção da regra de negócio), sem deixar "detalhes a definir na hora de codificar".

O roadmap do **frontend** é objeto de `08_ROADMAP_FRONTEND.md` — não deste documento.

---

## A) Ordem de Execução e Dependências

A ordem de implementação segue estritamente a ordem de sprints definida em `00_PRD_IRONTRACK.md` §4 (Sprint 0 → 1 → 2 → 3 → 4 → 5). Essa ordem não é apenas uma convenção de planejamento — é uma **dependência técnica real**, no seguinte encadeamento:

* **Sprint 2 (Gestão de Ciclos de Treino) não pode começar antes de Sprint 1 (Autenticação)** porque todo endpoint documentado a partir da Seção 3 de `03_CONTRATOS_API.md` exige o header `Authorization: Bearer <JWT_ACCESS_TOKEN>` (`03_CONTRATOS_API.md` §1.3) — sem `AuthController`/`JwtTokenProvider` funcionando, nenhum outro controller pode ser exercitado de ponta a ponta, nem mesmo em testes de integração.
* **Sprint 3 (Registro de Sessões) depende de Sprint 2** porque `POST /sessions/start` (`03_CONTRATOS_API.md` §5.1) lê `training_day_exercises` — o template de exercícios por dia — que só existe depois que a composição de dias/exercícios (`03_CONTRATOS_API.md` §3.8-§3.15) estiver implementada. Não há como iniciar uma sessão de treino sem um template para copiar.
* **Sprint 4 (Métricas, Progressão e Alertas) depende de Sprint 3** porque todo o motor formalizado em `06_LOGICA_DE_PROGRESSAO.md` (Seções C a E) lê `exercise_sets` e `session_exercises` históricos — não há dado de execução real para calcular sugestão de carga, estagnação ou PR antes que sessões possam ser registradas e finalizadas.
* **Sprint 5 (Suporte Offline e Notificações) é a única camada parcialmente paralelizável com Sprint 4** — a tarefa de reforço de idempotência (Seção C.5 abaixo) audita endpoints de escrita já construídos na Sprint 3 e não depende de métricas prontas; já a tarefa de notificações push depende apenas de Sprint 1 (usuário autenticado) estar pronta. Ainda assim, por simplicidade de planejamento e por já estar alinhado ao roadmap ágil do produto, este documento mantém Sprint 5 como último bloco sequencial.

---

## B) Estrutura de Pacotes de Referência

A árvore de pacotes é a já fixada em `01_ARQUITETURA_E_PADROES.md` §2.1 — reproduzida aqui apenas como referência rápida, não uma nova definição:

```text
com.irontrack.api/
│
├── config/              # Configurações do framework (Security, SQLite, Swagger/OpenAPI)
├── controllers/         # Controladores REST (HTTP Entrypoints)
├── services/            # Camada de lógica de negócios (Serviços e Regras)
│   └── impl/            # Implementações concretas de serviços (se necessário)
├── repositories/        # Interfaces do Spring Data JPA (Acesso ao Banco SQLite)
├── entities/            # Entidades do modelo relacional JPA (Tabelas do Banco)
├── dto/                 # Data Transfer Objects (Comunicação de Entrada/Saída)
│   ├── request/         # DTOs de entrada (Payloads de requisições)
│   └── response/        # DTOs de saída (Payloads de respostas)
├── exceptions/          # Exceções customizadas e Global Exception Handler
├── security/            # Filtros JWT, Provedores de Autenticação e Configurações de Criptografia
└── utils/               # Utilitários, validadores gerais e funções auxiliares
```

A tabela abaixo mapeia, por sprint, quais classes concretas nascem em cada pacote — nomes sugeridos, para que a IA de codificação não precise inventá-los:

| Sprint | `entities/` | `dto/request/` + `dto/response/` | `repositories/` | `services/` | `controllers/` |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | `User`, `RefreshToken` | `RegisterRequest`, `LoginRequest`, `RefreshRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `UpdateProfileRequest`, `ChangePasswordRequest`, `DeleteAccountRequest`, `UserResponse`, `AuthTokensResponse`, `DeletionScheduledResponse` | `UserRepository`, `RefreshTokenRepository` | `AuthService`, `UserService`, `EmailService`, `JwtTokenProvider`, `AccountDeletionSchedulerService` | `AuthController`, `UserController` |
| 2 | `TrainingCycle`, `TrainingDay`, `TrainingDayExercise`, `ExerciseLibrary` | `CreateCycleRequest`, `UpdateCycleRequest`, `CreateDayRequest`, `UpdateDayRequest`, `ReorderDaysRequest`, `CreateDayExerciseRequest`, `UpdateDayExerciseRequest`, `ReorderDayExercisesRequest`, `CreateExerciseRequest`, `UpdateExerciseRequest`, `CycleResponse`, `TrainingDayResponse`, `TrainingDayExerciseResponse`, `ExerciseResponse` | `TrainingCycleRepository`, `TrainingDayRepository`, `TrainingDayExerciseRepository`, `ExerciseLibraryRepository` | `CyclesService`, `ExercisesService` | `CyclesController`, `ExercisesController` |
| 3 | `TrainingSession`, `SessionExercise`, `ExerciseSet`, `ExerciseSetTechnique` | `StartSessionRequest`, `RegisterSetRequest`, `UpdateSetRequest`, `SessionResponse`, `SessionExerciseResponse`, `ExerciseSetResponse`, `SessionSummaryResponse` | `TrainingSessionRepository`, `SessionExerciseRepository`, `ExerciseSetRepository` | `SessionsService` | `SessionsController` |
| 4 | `StagnationAlert` | `SuggestionResponse`, `PrResponse`, `OverviewResponse`, `FrequencyResponse`, `MuscleGroupVolumeResponse`, `AlertResponse`, `SnoozeAlertRequest` | `StagnationAlertRepository` | `ProgressiveOverloadService`, `MetricsService`, `AlertsService` | `MetricsController`, `AlertsController` |
| 4 (utilitário) | — | — | — | `SetCountabilityRules` (utilitário puro, sem repositório) | — |
| 5 | `PushSubscription` | `PushSubscriptionRequest` | `PushSubscriptionRepository` | `PushService`, `ReminderSchedulerService` | `PushController` |

---

## C) Roadmap Sprint a Sprint

### C.0. Sprint 0 — Fundação Técnica

1. **Estrutura inicial do projeto Spring Boot** seguindo `01_ARQUITETURA_E_PADROES.md` §2.1 (pacotes `config/`, `controllers/`, `services/`, `repositories/`, `entities/`, `dto/`, `exceptions/`, `security/`, `utils/`).
2. **Configuração da conexão SQLite** via Spring Data JPA — `application.properties` lendo `DATABASE_PATH` de variável de ambiente (`05_DEVOPS_E_SEGURANCA.md` §D) e ativação de `PRAGMA foreign_keys = ON` por conexão (`02_SCHEMA_SQLITE.md` §2).
3. **Migrations** executando o DDL completo de `02_SCHEMA_SQLITE.md` — incluindo os patches deste documento (Seção 1: `refresh_tokens`, colunas de verificação/reset em `users`, `stagnation_alerts`, `push_subscriptions`, colunas de snapshot em `session_exercises`). Ferramenta obrigatória: **Flyway** (`01_ARQUITETURA_E_PADROES.md` §2.4), um arquivo de migration por tabela/alteração, na ordem em que as tabelas são referenciadas por FK.
4. **Seed de dados de desenvolvimento**: os exercícios de musculação reais do MVP (`02_SCHEMA_SQLITE.md` §3.2).
5. **`@RestControllerAdvice` global de exceções** (`01_ARQUITETURA_E_PADROES.md` §4.1), implementando o payload de erro padronizado exato de `03_CONTRATOS_API.md` §1.4, incluindo o mapeamento completo de status HTTP da tabela em `01_ARQUITETURA_E_PADROES.md` §4.1 (`400`, `401`, `403`, `404`, `422`, `500`).
6. **Esqueleto de segurança** — filtro JWT e configuração base do Spring Security (sem endpoints de negócio ainda), incluindo os cabeçalhos HTTP de segurança obrigatórios (`05_DEVOPS_E_SEGURANCA.md` §E.1: `X-Content-Type-Options`, `Strict-Transport-Security`, `X-Frame-Options`, `Content-Security-Policy`).
7. **Configuração do Swagger/OpenAPI** (`springdoc-openapi`, pacote `config/` conforme árvore da Seção B) — geração automática da documentação interativa a partir das anotações dos controllers, satisfazendo o gate "OpenAPI/Swagger atualizado" da Definition of Done (`00_PRD_IRONTRACK.md` §4.8) em toda tarefa futura, sem precisar de manutenção manual de um arquivo `openapi.yaml`.

### C.1. Sprint 1 — Autenticação e Perfil

1. **Entidades `User` e `RefreshToken`** mapeando exatamente as tabelas `users` e `refresh_tokens` (`02_SCHEMA_SQLITE.md` §2), incluindo as colunas de verificação de e-mail e reset de senha.
2. **DTOs de request/response** para cada endpoint da Seção 2 de `03_CONTRATOS_API.md`: `RegisterRequest`/`UserResponse` (§2.1), `LoginRequest`/`AuthTokensResponse` (§2.2), `RefreshRequest` (§2.4), `verify-email` sem body de request (§2.6), `ForgotPasswordRequest`/`ResetPasswordRequest` (§2.7), `UpdateProfileRequest` (§2.8), `ChangePasswordRequest` (§2.9).
3. **`AuthController`/`AuthService`** implementando: `register` (§2.1 — **atualizado por `13_ADR_LOG.md` ADR-018**: auto-verifica `email_verified_at = now()`, não dispara mais `EmailService` para verificação); `login` (§2.2 — **checagem de `email_verified_at` desativada por ADR-018**, bloqueio de 5 falhas via Caffeine — `05_DEVOPS_E_SEGURANCA.md` §E.2, e emissão de `RefreshToken` continuam ativos); `refresh` (§2.4, com rotação obrigatória de token); `logout` (§2.5); `verifyEmail` (§2.6, dormente — mantido implementado, sem fluxo ativo que gere token para ele); `forgotPassword`/`resetPassword` (§2.7, com revogação de todos os `refresh_tokens` do usuário no reset — continua ativo, `INVALID_OR_EXPIRED_TOKEN` ainda é alcançável por este fluxo).
4. **`UserController`/`UserService`** implementando: `me` (§2.3); `updateProfile` (§2.8, com reverificação de e-mail quando `email` muda); `changePassword` (§2.9, com revogação de tokens); `requestDeletion` (§2.10, `DELETE /users/me`) e `cancelDeletion` (§2.11) conforme `11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md` §B.
5. **`AccountDeletionSchedulerService`** — job `@Scheduled` diário que varre `users` com `deletion_requested_at` mais antigo que 30 dias e executa a exclusão física (cascata via FKs já definidas em `02_SCHEMA_SQLITE.md`), conforme `11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md` §B.4.
6. **`JwtTokenProvider`** gerando tokens com `JWT_ACCESS_TOKEN_EXPIRATION_MS` (15 minutos) e `JWT_REFRESH_TOKEN_EXPIRATION_MS` (7 dias), conforme `00_PRD_IRONTRACK.md` §4.3 e variáveis de `05_DEVOPS_E_SEGURANCA.md` §D.
7. **`EmailService`** (interface + implementação SMTP, variáveis `SMTP_HOST`/`SMTP_PORT`/`SMTP_USER`/`SMTP_PASSWORD`/`MAIL_FROM_ADDRESS` de `05_DEVOPS_E_SEGURANCA.md` §D) com templates de e-mail de verificação de cadastro e de redefinição de senha.

### C.2. Sprint 2 — Gestão de Ciclos de Treino

1. **Entidades `TrainingCycle`, `TrainingDay`, `TrainingDayExercise`, `ExerciseLibrary`** mapeando as tabelas correspondentes de `02_SCHEMA_SQLITE.md` §2, incluindo `archived_at` em `TrainingCycle` e `load_increment_kg` em `ExerciseLibrary`.
2. **DTOs** para cada endpoint das Seções 3 e 4 de `03_CONTRATOS_API.md`: criação/edição/arquivamento/ativação de ciclos (§3.1, §3.5-§3.7), CRUD e reordenação de dias (§3.8-§3.11), CRUD e reordenação de exercícios do template do dia (§3.12-§3.15), CRUD de exercícios customizados (§4.2-§4.4).
3. **`CyclesController`/`CyclesService`** implementando todos os endpoints das Seções 3.1-3.15, com atenção especial à **transação atômica** de `PATCH /cycles/{id}/activate` (`03_CONTRATOS_API.md` §3.7): desativar o ciclo antigo e ativar o novo em uma única transação, respeitando o índice único parcial `idx_cycles_user_single_active` (`02_SCHEMA_SQLITE.md`) sem jamais deixar o usuário com dois ciclos ativos simultaneamente. Também implementar a regra de `422` em `DELETE /cycles/{cycleId}/days/{dayId}` (§3.10) quando o dia já possui sessões executadas (`ON DELETE RESTRICT` de `training_sessions.training_day_id`). Em `GET /cycles/active` (§3.2), calcular `nextSuggestedTrainingDayId` (não persistido — busca a sessão `COMPLETED` mais recente do ciclo e sugere o próximo `training_day` por `orderIndex`, voltando ao primeiro após o último; sem sessão `COMPLETED` ainda, sugere `orderIndex = 1`).
4. **`ExercisesController`/`ExercisesService`** implementando `GET`/`POST /exercises` (§4.1-§4.2) e `PATCH`/`DELETE /exercises/{exerciseId}` (§4.3-§4.4), com a checagem de posse (`isCustom = true` + `user_id` do autenticado, `403 Forbidden` caso contrário) e o `422` de remoção de exercício já utilizado em sessão (`ON DELETE RESTRICT` de `session_exercises.exercise_id`).

### C.3. Sprint 3 — Registro de Sessões

1. **Entidades `TrainingSession`, `SessionExercise` (com os campos de snapshot `training_day_exercise_id`/`target_sets`/`target_reps_min`/`target_reps_max`, `02_SCHEMA_SQLITE.md` / `06_LOGICA_DE_PROGRESSAO.md` §1), `ExerciseSet`, `ExerciseSetTechnique`**.
2. **DTOs** para todos os endpoints da Seção 5 de `03_CONTRATOS_API.md`: `StartSessionRequest`/`SessionResponse` com `exercises[]`/`lastPerformance` (§5.1), `RegisterSetRequest`/`ExerciseSetResponse` com `clientGeneratedId`/`techniques` (§5.2), `UpdateSetRequest` (§5.3), `SessionSummaryResponse` (§5.5), listagem paginada (§5.6-§5.7).
3. **`SessionsController`/`SessionsService`**, com atenção especial a:
   * **(a) `start`** — copiar o template de `training_day_exercises` para `session_exercises` **persistindo o snapshot** de meta (`03_CONTRATOS_API.md` §5.1, `06_LOGICA_DE_PROGRESSAO.md` §1), buscar `lastPerformance` via `idx_sessions_user_day_start`, e retornar `422` se já existir sessão `IN_PROGRESS` aberta.
   * **(b) `sets`** — idempotência via `clientGeneratedId` (`03_CONTRATOS_API.md` §5.2): um reenvio com o mesmo `clientGeneratedId` deve retornar o registro já existente (mesmo status `201`), nunca duplicar nem falhar com erro genérico — a constraint `UNIQUE(client_generated_id)` (`02_SCHEMA_SQLITE.md`) é o mecanismo de detecção.
   * **(c) `finish`** — calcular `totalVolume` (soma de `weight × reps` de todas as séries da sessão) e `durationMinutes` (`03_CONTRATOS_API.md` §5.5) e, na sequência dentro da mesma operação, **disparar a reavaliação de estagnação** (`03_CONTRATOS_API.md` §7.1, via `ProgressiveOverloadService.detectStagnation`) para cada exercício daquela sessão.
   * **(d) `PATCH`/`DELETE` de séries** — sem renumeração automática de `set_number` (`03_CONTRATOS_API.md` §5.3-§5.4): a remoção deixa "buracos" na sequência deliberadamente.

### C.4. Sprint 4 — Métricas e Sobrecarga Progressiva

1. **`ProgressiveOverloadService`** implementando **literalmente** o algoritmo de `06_LOGICA_DE_PROGRESSAO.md` §C — incluindo os passos 3a/3b (corte de séries-bônus e definição de `pesoReferencia`) e os 4 CASOs (sem histórico, histórico insuficiente, teto atingido, teto não atingido) — e `detectStagnation` (`06_LOGICA_DE_PROGRESSAO.md` §D). A consulta de "sessão anterior de referência" reutiliza o índice `idx_sessions_user_day_start` (`02_SCHEMA_SQLITE.md`), já existente — nenhum índice novo é necessário.
2. **`SetCountabilityRules`** como utilitário puro testável (`06_LOGICA_DE_PROGRESSAO.md` §B/§F) — ex. `isCountable(techniques: Set<Technique>): boolean` — sem I/O, sem dependência de repositório.
3. **`MetricsController`/`MetricsService`** para todos os endpoints da Seção 6 de `03_CONTRATOS_API.md`: `history` (§6.1, inalterado), `suggestion` (§6.2, delega a `ProgressiveOverloadService.calculateSuggestion`), `pr` (§6.3), `overview` (§6.4), `frequency` (§6.5), `muscle-groups` (§6.6) — os últimos quatro usando exclusivamente séries contáveis, conforme `SetCountabilityRules`.
4. **Entidade `StagnationAlert`** e **`AlertsController`/`AlertsService`** para `GET /alerts` e `POST /alerts/{alertId}/snooze` (`03_CONTRATOS_API.md` §7.1-§7.2), incluindo a lógica de deduplicação (não criar novo alerta se já existir um em aberto para o mesmo par `exercise_id`/`training_day_id`) e de resolução automática (marcar `resolved_at` quando `detectStagnation` passar a retornar `false` para o par).
5. **Exigência de teste explícita:** cobertura de teste unitário de `SetCountabilityRules` e `ProgressiveOverloadService` deve usar **literalmente** os 4 cenários numéricos de `06_LOGICA_DE_PROGRESSAO.md` §H (H.1-H.4) como casos de teste — os mesmos números de entrada/saída documentados ali, não valores "inspirados" neles.

### C.5. Sprint 5 — Suporte Offline (Backend) e Notificações

1. **(a) Auditoria de idempotência** em todos os endpoints de escrita da sessão ativa (`POST .../sets`, `PATCH .../sets/{setId}`, `PATCH /sessions/{id}/finish`) — reforço de cobertura de teste sobre o que já foi construído na Sprint 3, especificamente para cenários de reenvio pós-reconexão (fila de sincronização do frontend, `04_FRONTEND_UI_COMPONENTES.md` §E.2). Não é código novo de produção — é cobertura de teste adicional garantindo que reenvios com o mesmo `clientGeneratedId` continuem idempotentes sob concorrência realista.
2. **(b) Entidade `PushSubscription`** e **`PushController`/`PushService`** implementando `POST`/`DELETE /users/me/push-subscription` (`03_CONTRATOS_API.md` §7.3-§7.4), com `PushSubscriptionRequest` refletindo `expoPushToken` (Expo Push Service — um único token por dispositivo, sem chaves de assinatura) em vez do formato Web Push original.
3. **(c) `ReminderSchedulerService`** com um job `@Scheduled` (o único ponto do backend que usa agendamento temporal em vez de reação a evento) rodando a cada minuto: compara `reminder_days`/`reminder_time` de cada `push_subscriptions` habilitada com o dia/horário atual, e pula o envio se o usuário já tiver uma sessão `COMPLETED` no dia corrente para o `training_day_id` esperado. O envio é uma chamada HTTP ao Expo Push API (`https://exp.host/--/api/v2/push/send`) usando o `expo_push_token` como destinatário — não uma biblioteca de Web Push assinada com VAPID.

---

## D) Definition of Done (Referência)

Nenhuma tarefa das Seções C.0-C.5 acima é considerada concluída sem satisfazer integralmente a Definition of Done já fixada em `00_PRD_IRONTRACK.md` §4.8 (padrões de código, cobertura de teste unitário mínima de 80%, testes de integração em novos endpoints, code review aprovado, merge sem conflitos com deploy em staging, OpenAPI/Swagger atualizado) — este documento não repete aquela lista, apenas a referencia como gate obrigatório de cada tarefa. Ênfase específica: a cobertura de teste unitário `>= 80%` é particularmente crítica (e particularmente barata de atingir) para as classes de regra de negócio pura sem I/O — `SetCountabilityRules`, `ProgressiveOverloadService` e o cálculo de `totalVolume`/`durationMinutes` em `SessionsService` — que devem ser tratadas como prioridade de teste desde o primeiro commit de cada uma, não como pendência para o fim da sprint.

---

## E) Fora de Escopo deste Roadmap

Para não gerar confusão em revisões futuras, os itens abaixo são explicitamente **fora do escopo** deste roadmap de backend:

* **Upload de foto de perfil** (`03_CONTRATOS_API.md` §2.8) — depende de uma decisão de infraestrutura de object storage ainda não tomada. Nenhum campo `photoUrl` deve ser adicionado a nenhum DTO nas sprints acima.
* **Exportação de histórico em PDF/CSV** — backlog pós-MVP.
* **Templates de ciclos prontos** (biblioteca de programas pré-configurados) — backlog pós-MVP.
* **Comparativo entre ciclos** (dashboard comparando dois ou mais ciclos de treino lado a lado) — backlog pós-MVP.
* **Integração com wearables** (smartwatches, monitores de frequência cardíaca) — backlog pós-MVP.
* **Modo compartilhado** (múltiplos usuários acompanhando o mesmo treino, ex: personal trainer e aluno) — backlog pós-MVP.
* **O roadmap do frontend** — é o objeto de `08_ROADMAP_FRONTEND.md`, o próximo documento da série, não deste.

Todos os itens de backlog pós-MVP acima estão registrados conforme o planejamento de sprints e o roadmap de `00_PRD_IRONTRACK.md` fornecidos pelo usuário, e não devem ser implementados nem parcialmente esboçados como parte das Sprints 0-5 deste documento.
