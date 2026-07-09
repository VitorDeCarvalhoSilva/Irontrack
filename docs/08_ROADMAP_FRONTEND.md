# 08_ROADMAP_FRONTEND.md - Roadmap de Implementação do Frontend

Este documento é o roadmap ordenado, sprint a sprint, de implementação real do frontend **React Native (Expo)** do **IronTrack**, no mesmo nível de precisão de `07_ROADMAP_BACKEND.md`: cada tarefa aponta para o componente/hook/tela/endpoint exato que implementa, sem "detalhes a definir na hora de codificar". É consumido por uma IA de codificação para gerar o código React Native/TypeScript propriamente dito.

O roadmap de backend já existe (`07_ROADMAP_BACKEND.md`) — não é objeto deste documento. Upload de foto de perfil permanece fora de escopo (mesma decisão de `07_ROADMAP_BACKEND.md` §E — sem infraestrutura de object storage definida).

---

## A) Ordem de Execução e Dependências

O frontend pode começar em **paralelo ao backend** assim que os contratos estiverem congelados — e já estão, em `02_SCHEMA_SQLITE.md` a `07_ROADMAP_BACKEND.md`. Enquanto os endpoints reais não estiverem disponíveis para integração, o desenvolvimento roda sobre uma camada de mock de API: **`msw`** (Mock Service Worker) continua válido em ambiente React Native/Jest para interceptar chamadas do Axios em testes; para o app rodando em simulador/dispositivo durante o desenvolvimento manual, onde `msw` pode ter fricção de configuração em ambiente RN, a alternativa é mockar diretamente a camada `services/` via Jest mocks ou uma flag de ambiente que aponta `apiClient` para um servidor de mock local (`json-server` ou equivalente) — em ambos os casos, servindo respostas conforme os exemplos JSON **exatos** já documentados em `03_CONTRATOS_API.md`, nunca payloads inventados ad-hoc.

Mesmo com essa possibilidade de paralelismo em relação ao backend, a ordem de **sprints internas do frontend** segue a mesma sequência de `00_PRD_IRONTRACK.md` §4 (Sprint 0 → 1 → 2 → 3 → 4 → 5), porque há dependências técnicas reais entre as sprints do próprio frontend:

* **Sprint 2 (Gestão de Ciclos) depende da Sprint 1 (Autenticação)** porque todo o `AppStack` só é alcançável a partir de uma sessão válida em `AuthContext` (`04_FRONTEND_UI_COMPONENTES.md` §A.1) — sem login funcionando, nenhuma tela privada pode ser navegada nem testada de ponta a ponta.
* **Sprint 3 (Registro de Sessões, `ActiveWorkoutScreen`) depende da Sprint 2** porque a tela crítica de treino consome ciclos com template de exercícios já navegáveis — sem `CycleFormScreen` (Sprint 2) produzindo ciclos com `training_day_exercises` compostos, não há dado real para popular a tela de diário de bordo.
* **Sprint 4 (Métricas e Sobrecarga Progressiva) depende da Sprint 3** porque os gráficos e cards de PR/sugestão de carga só têm dado para exibir depois que sessões podem ser registradas e finalizadas.
* **Sprint 5 (Offline-First Nativo e Notificações) é a sprint final**, pois a fila de sincronização offline (`04_FRONTEND_UI_COMPONENTES.md` §E.2) audita e reforça o fluxo de escrita da sessão ativa já construído na Sprint 3 — não faz sentido implementar resiliência offline para uma tela que ainda não existe.
* **Dependência adicional específica de mobile:** builds de teste instaláveis (`eas build --profile preview`) só ficam disponíveis depois que a Sprint 0 configurar o `eas.json` (`05_DEVOPS_E_SEGURANCA.md` §A.2) — até lá, o desenvolvimento e a validação manual acontecem via Expo Go ou simulador/emulador local, sem gerar um binário instalável para revisores externos.

---

## B) Estrutura de Pastas e Mapeamento por Sprint

A árvore de pastas é a já fixada em `01_ARQUITETURA_E_PADROES.md` §3.1 — reproduzida aqui apenas como referência rápida, não uma nova definição:

```text
app/                       # Ou src/, conforme convenção do Expo Router ou navegação manual
│
├── assets/                # Ícones, fontes, imagens, splash screen
├── components/
│   ├── common/            # Componentes atômicos (Button, Input, Card, Modal) — NativeWind
│   └── layout/            # Componentes de estrutura (Header, TabBar customizado)
├── screens/                # Telas (equivalente a "pages" no mundo web)
│   ├── Auth/               # LoginScreen, RegisterScreen, VerifyEmailScreen, etc.
│   ├── Dashboard/
│   ├── WorkoutCycle/
│   └── ActiveWorkout/      # Diário de bordo em tempo real
├── navigation/             # Configuração do React Navigation (stacks, tabs, guards)
├── hooks/                  # Custom hooks globais (useAuth, useNetworkStatus, useInterval)
├── contexts/               # Provedores de estado global (AuthContext, WorkoutSessionContext)
├── services/               # Integração com API e infraestrutura
│   ├── apiClient.ts         # Instância central do Axios com interceptores
│   ├── authService.ts
│   └── storage/             # Integração com AsyncStorage para persistência offline
├── utils/                   # Funções de utilidade geral
├── theme/                   # Configuração do NativeWind/tokens de design
└── types/                   # Tipagem estrita do TypeScript
```

A tabela abaixo mapeia, por sprint, quais artefatos concretos nascem em cada pasta — nomes definidos, sem espaço para invenção na hora de codificar:

| Sprint | `screens/` | `components/` | `hooks/` | `contexts/` | `services/` | `types/` |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 0 | — | `common/Button`, `common/Input`, `common/Card`, `common/Modal`, `layout/Header`, `layout/TabBar` | — | — | `apiClient.ts` (base, sem refresh) | — |
| 1 | `Auth/LoginScreen`, `Auth/RegisterScreen`, `Auth/VerifyEmailScreen`, `Auth/ForgotPasswordScreen`, `Auth/ResetPasswordScreen`, `Profile/ProfileScreen` | `navigation/RootNavigator`, `navigation/AuthStack`, `navigation/AppStack` | `useAuth` | `AuthContext` | `authService.ts`, `userService.ts` (interceptor de refresh em `apiClient.ts`) | `auth.types.ts` (`User`, `AuthTokens`) |
| 2 | `Dashboard/DashboardScreen` (versão inicial: ciclo ativo + dia sugerido), `WorkoutCycle/CyclesListScreen`, `WorkoutCycle/CycleFormScreen`, `Exercises/ExercisesLibraryScreen` | `WorkoutCycle/DayCard`, `WorkoutCycle/DayExerciseRow` | — | — | `cyclesService.ts`, `exercisesService.ts` | `cycle.types.ts`, `exercise.types.ts` |
| 3 | `ActiveWorkout/ActiveWorkoutScreen`, `SessionHistory/SessionHistoryScreen` | `ActiveWorkout/WorkoutHeader`, `ActiveWorkout/ExerciseCard`, `ActiveWorkout/SetRow`, `ActiveWorkout/SetRowForm`, `ActiveWorkout/RestTimerModal`, `ActiveWorkout/SuggestionBadge`, `ActiveWorkout/FinishWorkoutDialog` | `ActiveWorkout/useActiveWorkout`, `ActiveWorkout/useRestTimer` | `WorkoutSessionContext` | `workoutService.ts`, `sessionsService.ts` | `workout.types.ts` (`WorkoutSet`, `RegisterSetPayload`, `SetTechnique`, `LoadSuggestion`) |
| 4 | `Dashboard/DashboardScreen` (adiciona métricas/alertas), `Metrics/MetricsScreen` | `Dashboard/StagnationAlertBanner`, `Dashboard/OverviewCards`, `Metrics/HistoryChart`, `Metrics/FrequencyChart`, `Metrics/MuscleGroupChart`, `Metrics/PrCard` (todos via Victory Native) | — | — | `metricsService.ts`, `alertsService.ts` | `metrics.types.ts`, `alert.types.ts` |
| 5 | — | `layout/OfflineBanner`, `Profile/PushPreferencesForm` | `useNetworkStatus` | `NetworkStatusContext` | `storage/workoutStore.ts` (via AsyncStorage), `pushService.ts` | `sync.types.ts` (`SyncQueueItem`) |

---

## C) Roadmap Sprint a Sprint

### C.0. Sprint 0 — Fundação Técnica

1. Setup do projeto via `npx create-expo-app` (template TypeScript), com o **Expo SDK travado na versão 54** (`01_ARQUITETURA_E_PADROES.md` §3.5, `13_ADR_LOG.md` ADR-017) — nunca escalonar via `@latest` sem fixar a versão em seguida (`npx expo install expo@54.0.0 && npx expo install --fix`).
2. Configuração do `eas.json` (perfis `development`/`preview`/`production`, `05_DEVOPS_E_SEGURANCA.md` §A.2) e `app.json`/`app.config.ts` (nome do app, ícone, splash screen, `bundleIdentifier` iOS, `package` Android).
3. ESLint/Prettier (`01_ARQUITETURA_E_PADROES.md` §5).
4. Estrutura de pastas completa da Seção B acima.
5. `services/apiClient.ts` — instância central do Axios com interceptores **base** (tratamento de erro padronizado conforme `03_CONTRATOS_API.md` §1.4), ainda **sem** lógica de refresh automático (isso é Sprint 1).
6. Componentes visuais atômicos de `components/common/`: `Button`, `Input`, `Card`, `Modal`, estilizados com **NativeWind** — já respeitando Touch Targets (`04_FRONTEND_UI_COMPONENTES.md` §C.2) e `keyboardType` correto para campos numéricos (§C.1).
7. Configuração do NativeWind (`tailwind.config.js`, babel plugin `nativewind`).
8. Configuração do **React Navigation**: `RootNavigator` esqueleto (alternância `AuthStack`/`AppStack` observando um `AuthContext` ainda vazio), sem telas de negócio implementadas ainda.
9. Instalação de `react-hook-form` + `zod` + `@hookform/resolvers/zod` (`01_ARQUITETURA_E_PADROES.md` §3.4, `13_ADR_LOG.md` ADR-015) — decisão fechada de validação de formulário, usada por toda tela com formulário a partir da Sprint 1 (Seção C.1 abaixo).

### C.1. Sprint 1 — Autenticação e Perfil

1. `AuthContext` + hook `useAuth` — estado de sessão (usuário autenticado, tokens).
2. Telas: `LoginScreen` (`POST /auth/login`, `03_CONTRATOS_API.md` §2.2), `RegisterScreen` (`POST /auth/register`, §2.1), `VerifyEmailScreen` (`GET /auth/verify-email/{token}`, §2.6, acessada via deep link), `ForgotPasswordScreen` (`POST /auth/forgot-password`, §2.7), `ResetPasswordScreen` (`POST /auth/reset-password`, §2.7, via deep link). Todo formulário desta lista (`LoginScreen`, `RegisterScreen`, `ForgotPasswordScreen`, `ResetPasswordScreen`) usa **`react-hook-form` + `zod`** (`01_ARQUITETURA_E_PADROES.md` §3.4, instalado na Sprint 0) — nunca `useState` manual para controle/validação de campo.
3. `ProfileScreen` (também via `react-hook-form`/`zod`, `01` §3.4) com edição de perfil (`PATCH /users/me`, §2.8), troca de senha (`POST /users/me/change-password`, §2.9) e a seção de preferências de push (`04_FRONTEND_UI_COMPONENTES.md` §A) — a **UI** de preferências nasce aqui como formulário estático; a **ativação real** via `expo-notifications` (`Notifications.requestPermissionsAsync()`/`getExpoPushTokenAsync()`) e as chamadas a `POST`/`DELETE /users/me/push-subscription` (§7.3-§7.4) ficam para a Sprint 5. Incluir também o botão "Excluir conta" (`DELETE /users/me`, §2.10, com diálogo de confirmação de senha). O cancelamento (`POST /auth/cancel-deletion`, §2.11, agora reverificação direta de `email`/`password`, sem token) não tem tela própria: em `LoginScreen`, ao receber o erro `ACCOUNT_PENDING_DELETION` (403), exiba inline um botão "Cancelar exclusão da conta" que reenvia o `email`/`password` já digitados no formulário para esse endpoint.
4. Interceptor de refresh automático em `apiClient.ts` (`01_ARQUITETURA_E_PADROES.md` §4.2): intercepta `401`, chama `POST /auth/refresh` (§2.4) uma única vez, e refaz a chamada original com o novo `accessToken`; falha do refresh limpa a sessão e navega para `AuthStack`.
5. `navigation/RootNavigator`, `AuthStack`, `AppStack` — guarda de navegação consumindo `AuthContext` (`04_FRONTEND_UI_COMPONENTES.md` §A.1).
6. **Armazenamento de tokens:** o `refreshToken` é persistido via **`expo-secure-store`** (`SecureStore`) — não `AsyncStorage` puro. `SecureStore` usa Keychain (iOS)/Keystore (Android), apropriado para dados sensíveis, ao contrário do `AsyncStorage` (key-value simples sem criptografia, adequado para o snapshot de treino da Sprint 3, mas não para tokens de autenticação). O `accessToken` vive apenas em memória (estado do `AuthContext`), reidratado via *silent refresh* no mount do app a partir do `refreshToken` em `SecureStore`.

### C.2. Sprint 2 — Gestão de Ciclos de Treino

1. `CyclesListScreen` — lista de ciclos com ações de ativar (`PATCH /cycles/{cycleId}/activate`, `03_CONTRATOS_API.md` §3.7) e arquivar (`DELETE /cycles/{cycleId}`, §3.6), com badge visual "Arquivado" para ciclos com `archivedAt` não-nulo.
2. `CycleFormScreen` (formulários via `react-hook-form`/`zod`, `01_ARQUITETURA_E_PADROES.md` §3.4) — árvore completa já especificada em `04_FRONTEND_UI_COMPONENTES.md` §A: criação do ciclo (§3.1), CRUD e reordenação de dias (§3.8-§3.11), CRUD e reordenação de exercícios do template do dia (§3.12-§3.15), via os componentes de apoio `WorkoutCycle/DayCard` e `WorkoutCycle/DayExerciseRow`. Adaptado à navegação nativa: pode usar um fluxo de múltiplos passos com nested stack do React Navigation em vez de uma tela longa com scroll.
3. `ExercisesLibraryScreen` — biblioteca de exercícios (`GET /exercises`, §4.1), criação (`POST /exercises`, §4.2, formulário via `react-hook-form`/`zod`) e edição/remoção (`PATCH`/`DELETE /exercises/{exerciseId}`, §4.3-§4.4) visíveis apenas em exercícios `isCustom = true` do próprio usuário.
4. `DashboardScreen` (versão inicial) — exibe o ciclo ativo e o `nextSuggestedTrainingDayId` retornados por `GET /cycles/active` (`03_CONTRATOS_API.md` §3.2). O botão **"Iniciar Treino"** já aparece na UI, mas sua integração real com `POST /sessions/start` só é ligada na Sprint 3 (task 5 de `C.3` abaixo) — nesta sprint ele pode ficar desabilitado ou navegar para um placeholder, já que `ActiveWorkoutScreen` ainda não existe.
5. `services/cyclesService.ts`, `services/exercisesService.ts`.

### C.3. Sprint 3 — Registro de Sessões (Tela Crítica)

Esta sprint **não introduz nenhuma decisão nova** além do que `04_FRONTEND_UI_COMPONENTES.md` já define com precisão — a tarefa aqui é implementar, na ordem exata abaixo, o que já está especificado:

1. Árvore de componentes completa da Seção B de `04`: `ActiveWorkoutScreen` (B.1) → `WorkoutHeader` (B.2) → `ExerciseCard` (B.3, incluindo os tipos `WorkoutSet`/`RegisterSetPayload`/`LoadSuggestion`/`SetTechnique`) → `SetRow` (B.4, incluindo os 5 estados de `SetSyncStatus`, com tratamento visual explícito do estado `CONFLICT`) → `RestTimerModal` (B.5).
2. Regras de UX da Seção C de `04`: inputs numéricos com `keyboardType` nativo (C.1), Touch Targets mínimos (C.2), feedback visual imediato de superação de carga/PR via `react-native-reanimated` (C.3).
3. Gerenciamento de estado da Seção D de `04`: `WorkoutSessionContext` via `useReducer` (D.1) e a estratégia de Auto-save assíncrona-local-antes-da-rede via `AsyncStorage` (D.2), com os hooks `useActiveWorkout` e `useRestTimer`.
4. `services/workoutService.ts` — integração com `POST /sessions/start` (§5.1), `POST .../sets` (§5.2, com `clientGeneratedId` obrigatório para idempotência), `PATCH`/`DELETE .../sets/{setId}` (§5.3-§5.4), `PATCH .../finish` (§5.5).
5. Ligar o botão **"Iniciar Treino"** da `DashboardScreen` (criada na Sprint 2, task 4) a `services/workoutService.ts`: ao tocar, chama `POST /sessions/start` com o `trainingDayId` sugerido (ou outro escolhido pelo usuário), e navega para `ActiveWorkoutScreen` com o `sessionId` retornado.
6. `SessionHistoryScreen` (`04_FRONTEND_UI_COMPONENTES.md` §A) — lista via `GET /sessions` (§5.6, filtro simples por `trainingDayId`) com data/dia/duração/volume por item; detalhe somente-leitura via `GET /sessions/{sessionId}` (§5.7). Acessível por um botão "Histórico" na `DashboardScreen`. `services/sessionsService.ts`.

### C.4. Sprint 4 — Métricas e Sobrecarga Progressiva

1. `MetricsScreen` (`04_FRONTEND_UI_COMPONENTES.md` §A): `Metrics/HistoryChart` (`GET .../history`, §6.1), `Metrics/PrCard` (`GET .../pr`, §6.3), `Metrics/FrequencyChart` (`GET /metrics/frequency`, §6.5), `Metrics/MuscleGroupChart` (`GET /metrics/muscle-groups`, §6.6) — todos os gráficos renderizados com **Victory Native** (decisão registrada em `04_FRONTEND_UI_COMPONENTES.md` §A).
2. `ActiveWorkout/SuggestionBadge` (já existente na árvore de `04` §B.3) — ligado a `GET /sessions/{sessionId}/exercises/{sessionExerciseId}/suggestion` (§6.2).
3. `Dashboard/StagnationAlertBanner` na `DashboardScreen` (`04_FRONTEND_UI_COMPONENTES.md` §A) — consome `GET /alerts` (§7.1) e `POST /alerts/{alertId}/snooze` (§7.2) inline.
4. `Dashboard/OverviewCards` na `DashboardScreen` (`04_FRONTEND_UI_COMPONENTES.md` §A) — bloco de métricas gerais consumindo `GET /metrics/overview` (§6.4: sessões da semana/mês, volume da semana/mês, streak de dias).
5. `services/metricsService.ts`, `services/alertsService.ts`.

### C.5. Sprint 5 — Offline-First Nativo e Notificações

Implementa **exatamente** a Seção E de `04_FRONTEND_UI_COMPONENTES.md` (AsyncStorage + NetInfo, sem Service Worker):

1. Cache de leitura (§E.1): wrapper em `services/apiClient.ts` que serve a última resposta salva em `AsyncStorage` para `GET /exercises`/`GET /cycles/active` em caso de falha de rede.
2. Fila de sincronização (§E.2): `services/storage/workoutStore.ts` usando `AsyncStorage`, `NetworkStatusContext` + hook `useNetworkStatus` para o banner global "Modo Offline Ativo", detecção de reconexão via **`@react-native-community/netinfo`** (`NetInfo.addEventListener`) — sem necessidade de fallback de listener `online`/Service Worker, esse problema inteiro não existe fora do navegador.
3. Integração completa de **`expo-notifications`** (§E.3): fluxo de permissão (`Notifications.requestPermissionsAsync()`), obtenção do Expo Push Token (`Notifications.getExpoPushTokenAsync()`), registro via `POST /users/me/push-subscription` (§7.3) e desativação via `DELETE .../push-subscription` (§7.4) — ativando de fato a UI estática de preferências já construída em `ProfileScreen` na Sprint 1.
4. `Profile/PushPreferencesForm` (componente) e `services/pushService.ts`.
5. Configuração de **`eas update`** (`05_DEVOPS_E_SEGURANCA.md` §A.2/§C.1) para deploy OTA de mudanças JS-only sem passar por revisão de loja.

---

## D) Definition of Done (Referência)

Mesma referência de `07_ROADMAP_BACKEND.md` §D — `00_PRD_IRONTRACK.md` §4.8 — não repetida aqui, apenas apontada como gate obrigatório de cada tarefa das Seções C.0-C.5. Ênfase específica do frontend: testes de componente (Jest + React Native Testing Library) para `SetRow`/`ExerciseCard` (lógica de exibição de PR e dos 5 estados de `status`, incluindo `CONFLICT`) e para o reducer de `WorkoutSessionContext` (transições de estado determinísticas — `ADD_SET`, `UPDATE_SET`, `MARK_SET_SYNCED`, `FINISH_SESSION` — testáveis isoladamente, sem necessidade de renderizar a árvore de componentes). Builds de `eas build --profile preview` (`05_DEVOPS_E_SEGURANCA.md` §B) substituem o critério antigo de "funciona no navegador" como gate de validação manual do PO em cada sprint — cada sprint só é considerada demonstrável quando existe um build instalável correspondente.

---

## E) Fora de Escopo deste Roadmap

Mesma lista de `07_ROADMAP_BACKEND.md` §E — upload de foto de perfil, exportação de histórico em PDF/CSV, templates de ciclos prontos, comparativo entre ciclos, integração com wearables, modo compartilhado — não repetida aqui; `07_ROADMAP_BACKEND.md` §E é a fonte de justificativa para cada item. Nenhuma tela para essas funcionalidades deve ser esboçada, nem mesmo como placeholder, em nenhuma das Sprints 0-5 acima.

Adicionalmente: **publicação nas lojas de aplicativo (App Store/Play Store) usando credenciais reais de produção** é tratada como uma atividade de DevOps pontual, fora do escopo deste roadmap de features — ela pertence à seção "Build e Distribuição Mobile (EAS)" de `05_DEVOPS_E_SEGURANCA.md` §A.2/§C.2, não a uma tarefa de sprint de produto.
