# 04_FRONTEND_UI_COMPONENTES.md - Arquitetura de UI, Componentes e App Nativo

Este documento é o guia arquitetural estrito do frontend do **IronTrack** (React Native + TypeScript, via Expo). Complementa [`01_ARQUITETURA_E_PADROES.md`](./01_ARQUITETURA_E_PADROES.md) §3 (árvore de pastas Expo) detalhando navegação, árvore de componentes, regras de UX/acessibilidade mobile nativa, gerenciamento de estado da sessão ativa e a arquitetura offline-first do app. Os contratos de payload referenciados seguem estritamente [`03_CONTRATOS_API.md`](./03_CONTRATOS_API.md) (`weight`, `reps`, `rpe`). Para a especificação visual completa (paleta, tipografia, ícones, motion) e o posicionamento tela a tela de cada rota abaixo, ver [`15_DESIGN_SYSTEM_UI_UX.md`](./15_DESIGN_SYSTEM_UI_UX.md) — este documento define arquitetura/navegação, aquele define aparência/posicionamento.

> **Nota de migração:** este documento descreve a versão **nativa** (React Native/Expo) do frontend, substituindo integralmente a versão anterior baseada em PWA web (React + Vite + Service Worker). A decisão e sua justificativa estão registradas em `00_PRD_IRONTRACK.md` §2 e `05_DEVOPS_E_SEGURANCA.md` (Seção A.2 — build e distribuição via EAS). Nenhuma linha de código da versão PWA chegou a ser escrita — esta é uma reescrita de planejamento, não um retrabalho de implementação.

---

## A) Navegação (React Navigation)

A navegação usa **React Navigation** (native stack + bottom tabs), estruturada como um *navigator híbrido* controlado por um `RootNavigator` que observa `AuthContext`:

* **`AuthStack`** (native stack) — exibido quando não há sessão válida em `AuthContext`: `LoginScreen`, `RegisterScreen`, `VerifyEmailScreen`, `ForgotPasswordScreen`, `ResetPasswordScreen`.
* **`AppStack`** (`BottomTabNavigator`) — exibido quando autenticado: `DashboardScreen`, `CyclesListScreen`, `ExercisesLibraryScreen`, `MetricsScreen`, `ProfileScreen` como abas; `CycleFormScreen` é empilhada por cima da aba `CyclesListScreen` (não é uma aba própria).
* **`ActiveWorkoutScreen`** é empilhada **por cima** de todo o `BottomTabNavigator` (não é uma aba) — é uma tela modal/full-screen de foco único, sem tab bar visível, para não distrair o usuário durante o treino.

| Tela | Stack | Finalidade |
| :--- | :--- | :--- |
| `LoginScreen` | `AuthStack` | Autenticação via `POST /auth/login` (`03_CONTRATOS_API.md` §2.2). Navega para `AppStack` em caso de sucesso. |
| `RegisterScreen` | `AuthStack` | Cadastro via `POST /auth/register` (§2.1). Após sucesso (`201`), navega para `LoginScreen` com mensagem de confirmação simples (ex: "Conta criada! Faça login.") — a conta já nasce verificada (`13_ADR_LOG.md` ADR-018), sem menção a verificar e-mail. Continua **nunca autenticando automaticamente** (login é uma ação explícita do usuário). |
| `VerifyEmailScreen` | `AuthStack` | **Dormente** (ADR-018): nenhum fluxo ativo hoje produz um deep link válido para esta tela (registro não gera mais token de verificação). Continua implementada/registrada para reativação futura sem retrabalho, mas não é alcançada na prática. |
| `ForgotPasswordScreen` | `AuthStack` | Formulário de e-mail, dispara `POST /auth/forgot-password` (§2.7). Sempre exibe a mesma mensagem de sucesso genérica retornada pela API. |
| `ResetPasswordScreen` | `AuthStack` | Formulário de nova senha + confirmação, dispara `POST /auth/reset-password` (§2.7) com o `token` recebido via deep link. Após sucesso, navega para `LoginScreen` com mensagem de confirmação. |
| `DashboardScreen` | `AppStack` (aba) | Home. Exibe o ciclo ativo (`GET /cycles/active`, incluindo `nextSuggestedTrainingDayId` — o próximo dia por `orderIndex` após o último treinado, calculado pelo backend), botão **"Iniciar Treino"** (`POST /sessions/start`, navega para `ActiveWorkoutScreen`), bloco de métricas gerais (`GET /metrics/overview`, §6.4) e banner de alertas de estagnação (`GET /alerts`, §7.1) com ação de adiar inline (`POST /alerts/{alertId}/snooze`, §7.2). |
| `CyclesListScreen` | `AppStack` (aba) | Lista de ciclos, com ações de ativar (`PATCH /cycles/{cycleId}/activate`, §3.7) e arquivar (`DELETE /cycles/{cycleId}`, §3.6). Ciclo arquivado exibido com badge "Arquivado", sem reativação direta na listagem. |
| `CycleFormScreen` | `AppStack` (empilhada sobre `CyclesListScreen`) | Composição completa de um ciclo: criação (`POST /cycles`), CRUD/reordenação de dias (§3.8-§3.11), composição de exercícios por dia (§3.12-§3.15). |
| `ActiveWorkoutScreen` | Empilhada sobre `AppStack` inteiro | **Tela crítica.** Diário de bordo em tempo real da sessão em andamento (`sessionId` mantido em `WorkoutSessionContext`, não em parâmetro de rota, para sobreviver a reinícios do app). |
| `SessionHistoryScreen` | Empilhada sobre `DashboardScreen` (acessada por um botão "Histórico" na `DashboardScreen`) | Lista as sessões já realizadas (`GET /sessions`, §5.6, com filtro simples por `trainingDayId`), cada item exibindo data, dia treinado, duração e volume total. Toque em um item abre o detalhe somente-leitura (`GET /sessions/{sessionId}`, §5.7): exercícios e séries completas daquela sessão. |
| `ExercisesLibraryScreen` | `AppStack` (aba) | Biblioteca de exercícios (`GET /exercises`), filtro por grupo muscular, criação e edição/remoção de customizados (`PATCH`/`DELETE /exercises/{exerciseId}`, §4.3-§4.4) restritas a `isCustom = true` do próprio usuário. |
| `MetricsScreen` | `AppStack` (aba) | Gráfico de evolução por exercício (§6.1), card de PR — carga e volume (§6.3), gráfico de frequência (§6.5) e gráfico de distribuição por grupo muscular (§6.6). Gráficos renderizados com **Victory Native** (Recharts é baseado em DOM/SVG web e não roda em React Native; Victory Native é construído sobre `react-native-svg`, com tipagem TypeScript nativa — decisão registrada aqui e usada como referência em todo `08_ROADMAP_FRONTEND.md`). |
| `ProfileScreen` | `AppStack` (aba) | Visualização/edição de `name`/`email` (`PATCH /users/me`, §2.8 — trocar e-mail exige nova verificação), troca de senha (`POST /users/me/change-password`, §2.9), preferências de notificação push (seletor de dias, horário, toggle — `POST`/`DELETE /users/me/push-subscription`, §7.3-§7.4, via `expo-notifications`), e um botão "Excluir conta" (`DELETE /users/me`, §2.10, com diálogo de confirmação de senha). Cancelamento dentro do período de carência (`POST /auth/cancel-deletion`, §2.11) não tem tela própria: quando `LoginScreen` recebe o erro `ACCOUNT_PENDING_DELETION` (403) ao tentar logar, exibe inline um botão "Cancelar exclusão da conta" que reenvia o mesmo `email`/`password` já digitados para `POST /auth/cancel-deletion` — sem token, sem link por e-mail, sem tela nova (`03_CONTRATOS_API.md` §2.11 simplificado para reverificação direta de senha). |

### A.1. Regras de Guarda de Navegação
* `RootNavigator` alterna entre `AuthStack` e `AppStack` observando `AuthContext` — ausência de sessão válida força `AuthStack`, sem rota alguma do `AppStack` acessível por deep link direto.
* Se `WorkoutSessionContext` detectar uma sessão `IN_PROGRESS` persistida (`AsyncStorage`) ao montar o `AppStack`, a aplicação exibe um banner **"Treino em andamento"** com atalho para retomar `ActiveWorkoutScreen` — o usuário nunca deve perder o contexto de um treino não finalizado, mesmo após o app ser fechado pelo sistema operacional e reaberto.
* Tentar navegar para `ActiveWorkoutScreen` sem sessão ativa em contexto redireciona para `DashboardScreen` (não há treino "ativo" sem `sessionId`).
* `VerifyEmailScreen` e `ResetPasswordScreen` são acessíveis via **deep link** (Expo Linking) mesmo com `AuthContext` já autenticado presente — o usuário pode precisar verificar/trocar e-mail ou resetar senha de outra conta sem deslogar da sessão atual; essas telas não dependem do token de sessão ativo.

---

## B) Árvore de Componentes (Design Pattern)

Estrutura de pastas alinhada à árvore definida em `01_ARQUITETURA_E_PADROES.md` §3.1 (`screens/`, `components/`):

```text
screens/ActiveWorkout/
│
├── ActiveWorkoutScreen.tsx      # Smart Component (Container / Tela)
├── components/
│   ├── WorkoutHeader.tsx        # Dumb Component
│   ├── ExerciseCard.tsx         # Smart Component (sub-container por exercício)
│   ├── SetRow.tsx               # Dumb Component
│   ├── SetRowForm.tsx           # Dumb Component (inputs de edição de uma série)
│   ├── RestTimerModal.tsx       # Smart Component (cronômetro com efeitos colaterais)
│   ├── SuggestionBadge.tsx      # Dumb Component (exibe sugestão de sobrecarga)
│   └── FinishWorkoutDialog.tsx  # Dumb Component (confirmação de finalização)
└── hooks/
    ├── useActiveWorkout.ts      # Orquestra WorkoutSessionContext + persistência local
    └── useRestTimer.ts          # Lógica isolada do cronômetro de descanso
```

Os componentes abaixo são **independentes de plataforma** — a mesma árvore, responsabilidades e interfaces TypeScript já especificadas continuam válidas em React Native; apenas comentários que mencionavam elementos HTML/CSS foram ajustados.

### B.1. `ActiveWorkoutScreen` (Smart / Container)
* **Responsabilidade:** Ponto de entrada da tela de treino ativo. Consome `WorkoutSessionContext` via `useActiveWorkout()`, obtém a lista de exercícios da sessão corrente e orquestra o ciclo de vida da tela (montagem, auto-save, finalização).
* **Não possui estilo relevante próprio** — delega toda renderização visual aos filhos.
* **Efeitos colaterais:** inicia o cronômetro total da sessão ao montar; assina o `WorkoutSessionContext` para reidratar estado em caso de o app ser encerrado/reaberto pelo sistema operacional.

### B.2. `WorkoutHeader` (Dumb)
Exibe o cronômetro total da sessão e a ação de finalizar treino.

```ts
interface WorkoutHeaderProps {
  trainingDayName: string;
  elapsedSeconds: number;          // formatado internamente como HH:MM:SS
  onFinishWorkout: () => void;     // dispara FinishWorkoutDialog no container
  isFinishing: boolean;            // desabilita botão durante PATCH /sessions/{id}/finish
}
```
* Puramente visual: recebe o tempo já calculado (ticking é responsabilidade do hook `useActiveWorkout`, não do componente).
* Botão "Finalizar" é um Touch Target de destaque (ver Seção C) — ação crítica e irreversível, portanto exige confirmação via `FinishWorkoutDialog`.

### B.3. `ExerciseCard` (Smart — Container de um exercício da sessão)
Container de um exercício específico dentro da sessão ativa (`sessionExerciseId`).

```ts
interface ExerciseCardProps {
  sessionExerciseId: string;
  exerciseName: string;
  primaryMuscle: string;
  sets: WorkoutSet[];               // estado local (fonte: WorkoutSessionContext)
  suggestion?: LoadSuggestion;      // GET /sessions/{sessionId}/exercises/{sessionExerciseId}/suggestion (cache)
  onAddSet: (sessionExerciseId: string) => void;
  onRegisterSet: (setPayload: RegisterSetPayload) => Promise<void>;
}
```

Tipos de suporte referenciados acima, alinhados estritamente aos contratos de `03_CONTRATOS_API.md` (§5.2 e §6.2) e ao schema de `exercise_sets` em `02_SCHEMA_SQLITE.md` (§2):

```ts
// Espelha o enum exercise_set_techniques.technique (02_SCHEMA_SQLITE.md)
type SetTechnique = 'FALHA' | 'DROP_SET' | 'REST_PAUSE' | 'PAUSA' | 'NEGATIVA_FORCADA';

// Estado local de uma série já registrada na sessão ativa (WorkoutSessionContext)
interface WorkoutSet {
  id: string | null;               // null até a reconciliação com o id retornado pelo backend
  clientGeneratedId: string;       // UUID gerado no client — usado para idempotência (Seção E.2)
  setNumber: number;
  weight: number | null;
  reps: number | null;
  rpe: number | null;
  techniques: SetTechnique[];      // pode ser vazio
  notes: string | null;
  status: SetSyncStatus;           // ver união de tipos em SetRowProps (Seção B.4)
}

// Espelha o request body de POST /sessions/{sessionId}/exercises/{sessionExerciseId}/sets (03_CONTRATOS_API.md §5.2)
interface RegisterSetPayload {
  clientGeneratedId: string;
  setNumber: number;
  weight: number | null;
  reps: number | null;
  rpe: number | null;
  techniques: SetTechnique[];      // pode ser vazio
  notes: string | null;
}

// Espelha a response de GET /sessions/{sessionId}/exercises/{sessionExerciseId}/suggestion (03_CONTRATOS_API.md §6.2)
interface LoadSuggestion {
  exerciseId: string;
  targetWeight: number;
  targetReps: number;
  basis: string;
}
```

* **Responsabilidade:** mantém a lista de `SetRow` do exercício, calcula localmente se uma série superou o histórico (para acionar feedback visual — ver Seção C), e delega a persistência de cada série ao hook `useActiveWorkout` (que por sua vez aciona o Auto-save e a fila de sincronização offline).
* É "smart" porque decide **quando** chamar a API (via callback do container pai), mas não sabe **como** a chamada é feita — isso é responsabilidade da camada `services/`.

### B.4. `SetRow` (Dumb — puro)
Componente visual com os inputs de uma série específica. É o componente mais tocado durante o treino — deve ser leve e sem lógica de rede.

```ts
type SetSyncStatus = 'PENDING' | 'SAVED' | 'SYNCING' | 'OFFLINE_QUEUED' | 'CONFLICT';

interface SetRowProps {
  setNumber: number;
  weight: number | null;
  reps: number | null;
  rpe: number | null;
  isPersonalRecord: boolean;        // acionado pelo ExerciseCard para animação (Seção C)
  status: SetSyncStatus;
  onChange: (field: 'weight' | 'reps' | 'rpe', value: number | null) => void;
  onConfirm: () => void;            // dispara onRegisterSet no ExerciseCard (debounced/blur)
}
```
* Não conhece `sessionId`, `exerciseId` nem endpoints — apenas emite mudanças de valor e o evento de confirmação.
* O indicador `status` renderiza um ícone sutil (relógio para `SYNCING`, nuvem cortada para `OFFLINE_QUEUED`) sem bloquear a interação do usuário com a próxima série.
* **Estado `CONFLICT`:** ocorre quando a fila de sincronização (Seção E.2, passo 6) recebe um erro 4xx de negócio ao reenviar a série (ex: `422` por tentativa de gravação em uma sessão já finalizada). O `SetRow` renderiza um ícone de alerta (⚠) em destaque de cor de erro, torna os inputs somente-leitura e exibe uma ação explícita ("Revisar") que abre um diálogo com a mensagem de erro retornada pelo backend — esse estado nunca é resolvido automaticamente por retentativa, pois representa uma falha de regra de negócio, não uma falha transiente de rede.

### B.5. `RestTimerModal` (Smart — cronômetro com efeito colateral)
Cronômetro regressivo de descanso entre séries, disparado automaticamente após `onConfirm` de um `SetRow`.

```ts
interface RestTimerModalProps {
  isOpen: boolean;
  durationSeconds: number;          // padrão configurável por exercício/usuário
  onSkip: () => void;
  onComplete: () => void;           // fecha o modal automaticamente ao zerar
  onExtend: (extraSeconds: number) => void; // botão "+15s"
}
```
* Usa `useRestTimer` internamente (baseado em `setInterval` + `Date` de referência absoluta, não contagem por tick, para resistir a *throttling* do timer quando o app vai para segundo plano no SO).
* Modal não-bloqueante para a UI de fundo: o usuário pode fechar/pular e continuar navegando na `ActiveWorkoutScreen` sem perder o cronômetro (ele continua em segundo plano, refletido no `WorkoutHeader` ou em um mini-indicador flutuante).

---

## C) UX e Acessibilidade (Mobile Nativo)

O uso durante o treino, muitas vezes com as mãos suadas, trêmulas ou com atenção dividida, é a restrição de design dominante. Toda decisão de UI em `ActiveWorkoutScreen` prioriza **velocidade de input** e **tolerância a erro de toque** sobre densidade de informação.

### C.1. Inputs Numéricos
* **Obrigatório:** todo input de `weight`, `reps` e `rpe` usa o componente `TextInput` do React Native com `keyboardType="decimal-pad"` (para `weight`, que aceita casas decimais como `80.5`) ou `keyboardType="number-pad"` (para `reps` e `rpe`, sempre inteiros) — o teclado numérico nativo do SO é obtido diretamente pela prop `keyboardType`, sem workarounds.
  ```tsx
  <TextInput
    keyboardType="decimal-pad"
    value={weight?.toString() ?? ''}
    onChangeText={handleWeightChange}
    accessibilityLabel="Carga em quilogramas"
  />
  ```
  > **Nota técnica:** ao contrário da versão web (que exigia `type="text"` combinado com `inputMode` como workaround, pois `type="number"` HTML introduz spinners indesejados e `inputMode` tem suporte inconsistente em iOS Safari), React Native não tem esse problema — `keyboardType` é uma prop nativa do `TextInput`, sem ressalvas de compatibilidade entre iOS/Android. A máscara/validação decimal continua tratada em `utils/numberMask.ts`.
* Todo campo numérico deve possuir `accessibilityLabel` explícito (equivalente nativo ao `aria-label` web), já que o usuário pode estar navegando por leitor de tela (VoiceOver/TalkBack) intermediado por controle assistivo com mãos ocupadas.

### C.2. Touch Targets
* Toda área interativa (botões de incremento/decremento de peso, botão de confirmar série, itens de lista tocáveis) deve respeitar o mínimo de **44x44px** (diretriz WCAG 2.5.5 / Apple HIG), mesmo quando o ícone visual for menor — a área de toque é expandida via padding (`StyleSheet`/classes NativeWind), não via redimensionamento do ícone.
* Botões de ação crítica e frequente (`onConfirm` do `SetRow`, `+15s` do `RestTimerModal`) devem ter no mínimo **56x56px** (`min-w-[56px] min-h-[56px]` em NativeWind) e espaçamento mínimo de **8px** entre alvos adjacentes, para reduzir toques acidentais em séries erradas.
* O botão "Finalizar Treino" no `WorkoutHeader`, por ser destrutivo/irreversível, nunca fica adjacente a um botão de ação frequente sem uma barreira de confirmação (`FinishWorkoutDialog`).

### C.3. Feedback Visual Imediato
* **Superação de carga (Sobrecarga Progressiva):** ao confirmar uma série (`onConfirm`), o `ExerciseCard` compara `weight`/`reps` contra o último registro histórico equivalente (via `suggestion.basis` ou cache local de `GET /metrics/exercises/{id}/history`). Se superado, o `SetRow` correspondente dispara uma animação curta via **`react-native-reanimated`** (`withSequence`/`withTiming`: pulso de borda em cor de destaque + leve *scale* de 1.0 → 1.04 → 1.0, ~400ms), respeitando a preferência de movimento reduzido do sistema (`AccessibilityInfo.isReduceMotionEnabled()`), e marca `isPersonalRecord=true`.
* **Confirmação de persistência:** toda mudança de `status` em `SetRow` (`PENDING` → `SAVED`) deve refletir em no máximo 150ms após o `onConfirm`, mesmo que a chamada de rede real ainda esteja em andamento — o Auto-save local (Seção D) garante que o dado já está seguro antes da resposta da API chegar, e a UI reflete isso imediatamente para não gerar ansiedade de "perdi o dado?" no usuário.
* Estados de erro de validação (ex: campo vazio ao tentar confirmar) usam contorno vermelho + `accessibilityState={{ invalid: true }}`, nunca apenas cor (para acessibilidade a daltonismo).

---

## D) Gerenciamento de Estado Global e Sessão Ativa

### D.1. Ferramenta
* **Estado global leve** (sessão de autenticação, preferências visuais, banner de modo offline): `React Context API`, conforme já definido em `01_ARQUITETURA_E_PADROES.md` §3.3 — continua válido integralmente em React Native, não é uma API exclusiva de navegador/DOM.
* **Estado do treino ativo:** `WorkoutSessionContext` dedicado, também via Context API + `useReducer` (não Zustand) — a escolha por Context+Reducer aqui é deliberada: o estado da sessão ativa é hierárquico e transacional (sessão → exercícios → séries), o que se modela melhor com um reducer de ações explícitas (`ADD_SET`, `UPDATE_SET`, `MARK_SET_SYNCED`, `FINISH_SESSION`) do que com um store flat. Zustand permanece reservado a estados verdadeiramente globais e simples (ex: tema, flags de feature).

### D.2. Estratégia de Auto-save
A cada série registrada, o estado deve ser persistido de forma **síncrona e local antes** de qualquer tentativa de rede — a chamada à API é tratada como *best-effort* em paralelo, nunca como pré-condição para não perder o dado.

Fluxo de `onConfirm` em `SetRow`:

```text
1. Usuário confirma a série (weight, reps, rpe preenchidos)
2. dispatch({ type: 'ADD_SET', payload }) → reducer atualiza WorkoutSessionContext em memória
3. Middleware do reducer persiste o snapshot completo da sessão em AsyncStorage
   (chave: `workout-session:{sessionId}`) — operação ASSÍNCRONA (await antes do
   próximo dispatch, mas não bloqueia a thread JS, ao contrário do localStorage
   web original que motivou a rejeição de engines síncronas)
4. UI já reflete status='SAVED' (dado seguro localmente)
5. Em paralelo (não bloqueante): services/workoutService.registerSet() tenta
   POST /sessions/{sessionId}/exercises/{sessionExerciseId}/sets
   → sucesso: status permanece 'SAVED', reconcilia id retornado pelo backend
   → falha de rede: enfileira em AsyncStorage (fila de sincronização, Seção E) e
     marca status='OFFLINE_QUEUED'
```

* **Por que AsyncStorage:** `AsyncStorage.setItem`/`getItem` (`@react-native-async-storage/async-storage`) são operações assíncronas nativas do React Native, a mesma abstração já usada no padrão de acesso definido para o snapshot de sessão (nunca precisou de consultas relacionais complexas — um snapshot único por sessão + uma fila de itens simples). Usa-se uma camada de abstração (`services/storage/workoutStore.ts`) que expõe uma API simples (`saveSnapshot`, `loadSnapshot`, `clearSnapshot`), preservando a mesma interface e a mesma justificativa de auto-save síncrono-local-antes-da-rede da versão anterior — apenas a engine de persistência mudou.
* **Reidratação:** ao montar `ActiveWorkoutScreen`, `useActiveWorkout()` primeiro tenta carregar o snapshot do `AsyncStorage` local antes de qualquer chamada de rede — se existir uma sessão `IN_PROGRESS` local com `sessionId` compatível, a UI é restaurada instantaneamente, e a sincronização com o backend ocorre em segundo plano (reconciliação, não bloqueio). Como o app roda como processo nativo (não uma aba de navegador facilmente perdida), essa reidratação cobre tanto o caso de o app ser minimizado/restaurado pelo SO quanto o caso de ser encerrado completamente e reaberto.
* **Limpeza:** o snapshot só é removido do `AsyncStorage` após `PATCH /sessions/{id}/finish` retornar `200 OK` **e** a fila de sincronização estar vazia para aquela sessão — garantindo que nenhuma série fique órfã.

---

## E) Arquitetura Offline-First Nativa

Esta seção é **mais simples** que a versão PWA original, porque React Native não depende de Service Worker, Cache API ou Background Sync API — nenhuma dessas existe fora do navegador, e o app nativo não precisa de nenhum mecanismo de fallback para compensar suporte inconsistente entre browsers (o problema de "Background Sync não existe no Safari", por exemplo, deixa de existir por completo).

### E.1. Cache de Leitura
* Implementado diretamente na camada `services/apiClient.ts`: um wrapper simples que tenta a rede primeiro e, em caso de falha, serve o último resultado salvo em `AsyncStorage` para aquele endpoint — mesmo espírito de um `StaleWhileRevalidate`, sem precisar de uma API de Service Worker para isso.
* Aplicado a `GET /exercises` e `GET /cycles/active` — biblioteca de exercícios e ciclo ativo não mudam durante o treino, então servir a última cópia local em caso de falha de rede é seguro.
* Escritas (`POST`/`PATCH`/`DELETE`) **nunca** passam por este cache de leitura — são tratadas exclusivamente pela fila de sincronização (Seção E.2).

### E.2. Fila de Sincronização (Reconexão via NetInfo)
Cenário-âncora: usuário confirma uma série no fundo da academia sem sinal de internet.

```text
┌─────────────────────────────────────────────────────────────────────┐
│ 1. SetRow.onConfirm() → dispatch ADD_SET (estado local + AsyncStorage)│
│    → status imediato: 'SAVED' (dado já está seguro localmente)      │
└───────────────────────────────┬───────────────────────────────────-┘
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 2. workoutService.registerSet() chama apiClient.post(...)            │
│    Interceptor do Axios (services/apiClient.ts) captura falha de     │
│    rede física (offline / timeout / DNS) — distinta de erro HTTP 4xx │
└───────────────────────────────┬────────────────────────────────────┘
                                 ▼
┌────────────────────────────────────────────────────────────────────┐
│ 3. Interceptor grava a operação pendente em                          │
│    AsyncStorage → chave 'syncQueue':                                 │
│    { id, method: 'POST', url, payload, sessionId, createdAt,         │
│      retryCount: 0 }                                                 │
│    SetRow correspondente atualiza status → 'OFFLINE_QUEUED'          │
│    Banner global "Modo Offline Ativo" é exibido (via                 │
│    NetworkStatusContext, ouvindo NetInfo.addEventListener)           │
└───────────────────────────────┬────────────────────────────────────┘
                                 ▼
┌────────────────────────────────────────────────────────────────────┐
│ 4. NetInfo.addEventListener (@react-native-community/netinfo)        │
│    monitora o estado da conexão continuamente — não é necessário     │
│    registrar nenhuma "tag" de sincronização como na Background Sync  │
│    API web; o listener já dispara diretamente em JS quando a conexão │
│    muda, sem depender de um Service Worker em segundo plano          │
└───────────────────────────────┬────────────────────────────────────┘
                                 ▼
┌────────────────────────────────────────────────────────────────────┐
│ 5. Conexão restabelecida → listener transiciona para                 │
│    `isConnected: true` → percorre 'syncQueue' em AsyncStorage em     │
│    ordem de 'createdAt' (FIFO, preserva ordem das séries) →          │
│    reenvia cada requisição via apiClient                             │
└───────────────────────────────┬────────────────────────────────────┘
                                 ▼
┌────────────────────────────────────────────────────────────────────┐
│ 6. Por item da fila:                                                 │
│    → 2xx: remove da 'syncQueue', atualiza SetRow → status 'SAVED',   │
│      reconcilia id retornado pelo backend no snapshot da sessão      │
│    → 4xx (erro de negócio, ex: 422 sessão já finalizada): remove da  │
│      fila, marca status 'CONFLICT' e notifica o usuário para revisão │
│      manual (não retenta indefinidamente um erro não-transiente)     │
│    → falha de rede novamente: mantém na fila, incrementa retryCount, │
│      aguarda o próximo evento de mudança de conectividade (backoff   │
│      exponencial limitado, máx. 5 tentativas automáticas antes de    │
│      exigir ação manual do usuário)                                  │
└────────────────────────────────────────────────────────────────────┘
```

* **Ordem e idempotência:** cada item da fila carrega um `clientGeneratedId` (UUID gerado no `SetRow` no momento do `dispatch`), enviado no payload como correlação — permite que o backend trate reenvios de forma idempotente caso uma resposta 2xx tenha sido perdida antes de a fila ser limpa (timeout de rede após o servidor já ter persistido).
* **Escopo:** a fila de sincronização cobre exclusivamente operações de escrita críticas da sessão ativa (`POST .../sets`, `PATCH .../finish`, `POST /sessions/start`). Leituras (`GET /exercises`, `GET /metrics/...`) não entram na fila — dependem do cache de leitura da Seção E.1.
* **Visibilidade para o usuário:** o banner de "Modo Offline Ativo" permanece visível enquanto `syncQueue` não estiver vazia, com contador de itens pendentes, e transiciona para um toast de confirmação ("Treino sincronizado ✓") quando a fila zera — feedback explícito para que o usuário nunca fique em dúvida se o treino foi salvo no servidor.

### E.3. Notificações de Lembrete (`expo-notifications`)
* Fluxo de ativação (iniciado em `ProfileScreen`, Seção A): `Notifications.requestPermissionsAsync()` solicita permissão do sistema operacional; em caso de concessão, `Notifications.getExpoPushTokenAsync()` obtém o Expo Push Token do dispositivo, enviado ao backend via `POST /users/me/push-subscription` (`03_CONTRATOS_API.md` §7.3) junto com `reminderDays`/`reminderTime`.
* O envio efetivo dos lembretes é responsabilidade exclusiva do backend (`ReminderSchedulerService`, `07_ROADMAP_BACKEND.md` §C.5) — o app apenas registra/desregistra o token e as preferências; não há lógica de agendamento local de notificação no cliente.
* Desativar as notificações (toggle em `ProfileScreen`) dispara `DELETE /users/me/push-subscription` (§7.4) com o `expoPushToken` do dispositivo atual.
