# 15_DESIGN_SYSTEM_UI_UX.md - Sistema de Design e Planejamento de UI

Este documento é a fonte única de verdade para **identidade visual, tokens de design e posicionamento de tela** do **IronTrack**. Complementa `04_FRONTEND_UI_COMPONENTES.md` (que define navegação, árvore de componentes e arquitetura de estado) sem duplicá-lo: `04` responde "o que existe e como se comunica com a API"; este documento responde "como cada coisa deve parecer, se mover e onde deve ficar na tela". Toda tela/componente descrito em `04` tem uma especificação visual correspondente aqui (Seção I).

Decisão registrada em `13_ADR_LOG.md`, ADR-019.

---

## A) Princípios de Design

1. **Agressivo, porém minimalista.** Fundo quase preto, poucas cores, tipografia de alto contraste e um único acento cromático usado com intenção — nunca decoração gratuita. "Agressivo" vem da tipografia (Oswald condensada/maiúscula) e do neon vermelho, não de excesso de elementos.
2. **Monocromático + um único acento (vermelho neon).** Toda a UI vive em tons de preto/cinza/branco-quebrado. O vermelho neon é a única cor de destaque do app inteiro — nunca introduza uma segunda cor de acento (azul, verde, amarelo) sem uma nova decisão Tipo B. Hierarquia entre ações é feita por **intensidade e movimento do próprio vermelho**, não por matizes diferentes (Seção B.3).
3. **Ícone antes de texto.** Toda ação recorrente deve ser reconhecível pelo ícone sozinho. Texto acompanha o ícone apenas quando a ação é ambígua, destrutiva ou a primeira vez que aparece no fluxo (Seção D).
4. **Feedback imediato e inequívoco, sempre.** Nenhuma ação do usuário (tocar em salvar, confirmar série, excluir algo) pode deixar a tela como estava antes sem alguma resposta visual/tátil dentro de 150ms (Seção F) — a mesma exigência que já existe para `SetRow` em `04` §C.3, generalizada para o app inteiro.
5. **Movimento com propósito, nunca decorativo.** Toda transição de tela, entrada de componente ou pulso de destaque existe para comunicar uma mudança de estado real (Seção G). Nunca animar por animar.
6. **Posicionamento previsível.** O usuário nunca precisa "procurar" o botão principal de uma tela ou temer tocar perto de uma ação destrutiva por acidente (Seção I e `04` §C.2).

---

## B) Paleta de Cores

### B.1. Tokens Base (neutros)

| Token | Valor | Uso |
| :--- | :--- | :--- |
| `color.bg` | `#0A0A0C` | Fundo base de toda tela. |
| `color.bg.elevated` | `#131316` | Superfícies logo acima do fundo (tab bar, header, sheets). |
| `color.surface` | `#1A1A1E` | Cards, inputs, itens de lista. |
| `color.surface.alt` | `#202024` | Estado pressionado/selecionado de uma superfície; skeleton de loading. |
| `color.border` | `#2A2A2F` | Borda padrão (hairline) entre elementos. |
| `color.border.strong` | `#38383E` | Divisores com mais ênfase, borda de card em foco neutro. |
| `color.text.primary` | `#F2F2F0` | Texto principal (branco-quebrado, nunca `#FFFFFF` puro — reduz o aspecto "genérico"). |
| `color.text.secondary` | `#9C9CA3` | Labels, subtítulos, texto de apoio. |
| `color.text.muted` | `#6B6B72` | Placeholders, timestamps, texto terciário. |
| `color.text.disabled` | `#46464C` | Texto/ícone de elemento desabilitado. |

### B.2. Tokens de Acento (vermelho neon — único)

| Token | Valor | Uso |
| :--- | :--- | :--- |
| `color.accent` | `#FF1440` | Cor base do único acento do app. |
| `color.accent.glow.soft` | `rgba(255, 20, 64, 0.35)` | Glow de ações primárias/contidas (Seção B.3). |
| `color.accent.glow.strong` | `rgba(255, 42, 77, 0.55)` | Glow de ações destrutivas/críticas e do pulso de PR. |
| `color.accent.dim` | `rgba(255, 20, 64, 0.16)` | Fundo tintado sutil (estado pressionado de botão outline, badge "Ativo"). |

Não existe `color.success` verde nem `color.warning` amarelo neste sistema — ver Seção B.3 para como o app comunica sucesso/erro/celebração usando apenas neutros + o próprio vermelho.

### B.3. Regra de Hierarquia por Intensidade (decisão fechada)

Como só existe um acento, ações **primárias** e **destrutivas** se diferenciam por **intensidade de glow + comportamento**, nunca por matiz:

| Papel | Cor de borda/texto | Glow | Motion | Exemplo |
| :--- | :--- | :--- | :--- | :--- |
| **Primária/contida** | `color.accent` | `glow.soft`, estático | Nenhum pulso; só resposta a toque (Seção G.2) | Botão "Entrar", "Salvar treino", tab ativa |
| **Destrutiva/crítica** | `color.accent` (mesmo tom) | `glow.strong`, pode pulsar 1x ao aparecer | Ícone de alerta obrigatório + confirmação (Seção D, Seção I) | "Excluir conta", `FinishWorkoutDialog`, estado `CONFLICT` de `SetRow` |
| **Celebração/PR** | `color.accent` | `glow.strong`, pulso único de 600ms | Pulso + leve *scale* (`04` §C.3), não repete | Série que supera o histórico |

Nenhuma dessas três reintroduz uma cor nova — a diferença está inteiramente em intensidade de glow e em movimento (Seção G), exatamente a decisão validada com o usuário para este documento.

### B.4. Feedback Semântico Sem Verde/Amarelo

* **Sucesso (não-destrutivo):** ícone `check-circle` em `color.text.primary` (branco-quebrado) + toast neutro (Seção F.1) — sucesso não usa o acento vermelho, para não competir com a semântica de alerta/energia do vermelho.
* **Erro/validação:** `color.accent` na variante crítica (borda + ícone `alert-circle`), conforme B.3.
* **Informação/estado neutro (ex: "Modo Offline Ativo"):** `color.text.secondary` sobre `color.bg.elevated`, sem acento.

---

## C) Tipografia

**Fonte de display:** [Oswald](https://fonts.google.com/specimen/Oswald) (condensada, geométrica, alto contraste de peso — reforça o tom "agressivo"). **Fonte de corpo:** [Inter](https://fonts.google.com/specimen/Inter) (alta legibilidade em telas pequenas, ampla cobertura de pesos). Carregadas via `@expo-google-fonts/oswald` e `@expo-google-fonts/inter` (Seção J).

### C.1. Escala Tipográfica

| Token | Fonte/Peso | Tamanho/Altura de linha | Tracking | Uso |
| :--- | :--- | :--- | :--- | :--- |
| `type.display.xl` | Oswald 700 | 32/38 | +1.5px, MAIÚSCULAS | Logo/wordmark, tela de splash, título de `LoginScreen`/`RegisterScreen`. |
| `type.display.lg` | Oswald 700 | 24/30 | +1.0px, MAIÚSCULAS | Título de tela (`Header`, Seção I). |
| `type.display.md` | Oswald 500 | 18/24 | +0.5px, MAIÚSCULAS | Título de seção/card dentro de uma tela (ex: cabeçalho de `ExerciseCard`). |
| `type.body.lg` | Inter 400 | 16/22 | 0 | Texto principal, mensagens de tela cheia. |
| `type.body.md` | Inter 400 | 14/20 | 0 | Texto padrão de UI, labels de input, itens de lista. |
| `type.body.sm` | Inter 500 | 12/16 | 0 | Captions, timestamps, texto auxiliar, badges. |
| `type.numeric` | Inter 500, `font-variant-numeric: tabular-nums` | 20-28 (contextual) | 0 | Valores de carga/reps/RPE — tamanho maior em `ActiveWorkoutScreen` (tela mais lida à distância/sob fadiga). |

### C.2. Regras de Uso
* Oswald **nunca** é usada para parágrafos ou texto de formulário — só títulos e o wordmark. Misturar as duas fontes no mesmo bloco de texto corrido é proibido.
* MAIÚSCULAS são reservadas à Oswald (`type.display.*`); texto em Inter nunca é forçado para maiúsculas (prejudica legibilidade e foge do padrão que gerou a decisão).
* Todo número que representa uma métrica do treino (peso, reps, volume, contagem de séries) usa `type.numeric`, nunca `type.body.*` — dá consistência visual a todos os valores que o usuário mais observa durante o treino.

---

## D) Iconografia

**Biblioteca:** `@expo/vector-icons`, conjunto **MaterialCommunityIcons** como padrão exclusivo (já empacotado pelo Expo — sem custo de dependência nova real, apenas confirmação de uso, Seção J). Não misturar conjuntos de ícones diferentes (Ionicons, FontAwesome, etc.) na mesma tela.

| Token | Tamanho | Uso |
| :--- | :--- | :--- |
| `icon.inline` | 16px | Dentro de texto/caption, badges. |
| `icon.default` | 20px | Prefixo de input, itens de lista, corpo de UI geral. |
| `icon.action` | 24px | Botões de ação, tab bar, ícones de header. |
| `icon.feature` | 32px | Estados vazios, destaque de card, splash. |

### D.1. Regras
* Ação **frequente e não-ambígua** (ex: "Iniciar Treino", item de tab bar, incrementar peso): **ícone sozinho**, com `accessibilityLabel` obrigatório (leitor de tela sempre recebe o texto, mesmo quando a tela não mostra nenhum).
* Ação **destrutiva ou irreversível** (excluir conta, remover exercício, finalizar treino): **ícone + texto sempre visíveis**, nunca só ícone — reduz toque acidental e reforça a gravidade (consistente com `04` §C.2).
* Ação de **primeira aparição em um fluxo novo** (ex: primeiro CTA de uma tela que o usuário nunca viu): ícone + texto na primeira versão; se o mesmo padrão se repetir em outra tela já familiar ao usuário, pode reduzir para ícone sozinho.
* Nunca mais de **um ícone de ação no canto superior direito** de qualquer header (Seção I) — evita a "poluição de ícones perdidos" que motivou este documento.

---

## E) Espaçamento, Grid e Touch Targets

* **Unidade base:** 4px. Escala de espaçamento: `space.xs=4`, `space.sm=8`, `space.md=16`, `space.lg=24`, `space.xl=32`, `space.xxl=48`.
* **Margem lateral padrão de tela:** `space.md` (16px) em todas as telas, exceto `ActiveWorkoutScreen` que usa `space.sm` (8px) para maximizar densidade útil de séries visíveis sem exigir margem generosa.
* **Raio de borda:** `radius.sm=4px` (inputs, chips), `radius.md=10px` (cards), `radius.lg=16px` (modais/sheets), `radius.full` (badges, avatar).
* **Touch targets:** reforça, não substitui, `04_FRONTEND_UI_COMPONENTES.md` §C.2 — mínimo 44x44px geral, 56x56px para ações frequentes/críticas, espaçamento mínimo de 8px entre alvos adjacentes.

---

## F) Feedback de Ações

### F.1. Toast/Snackbar (componente próprio, sem biblioteca de terceiros)

Ancorado na base da tela, acima da tab bar (quando presente) ou acima da margem inferior segura (`SafeAreaView`). Fila de no máximo 1 visível por vez (o próximo espera o atual sair). Variantes:

| Variante | Borda superior | Ícone | Duração | Dispensável por swipe |
| :--- | :--- | :--- | :--- | :--- |
| Sucesso | `color.text.primary` (fina, 2px) | `check-circle` | 3s | Sim |
| Erro | `color.accent` (crítico) | `alert-circle` | 5s (ou até toque) | Sim |
| Informação | `color.border.strong` | contextual (`wifi-off`, `sync`) | 4s | Sim |

Toast é para feedback **transiente** de uma ação pontual (ex: "Treino sincronizado ✓"). Estado **persistente** (ex: "Modo Offline Ativo" enquanto a fila não esvazia, `04` §E.2) continua sendo um **banner fixo**, não um toast — os dois padrões coexistem com papéis diferentes, não são intercambiáveis.

### F.2. Estados de Carregamento
* **Nível de tela** (listas, dashboard, métricas): *skeleton* — blocos com `color.surface.alt` pulsando suavemente (opacidade 0.6↔1.0, 900ms, loop), no formato aproximado do conteúdo real. Nunca um spinner central de tela cheia, exceto no *splash* de abertura do app.
* **Nível de botão** (submissão de formulário, ex: "Entrar"): o próprio botão substitui o texto por um spinner pequeno (`color.accent`) e fica desabilitado — nunca bloqueia a tela inteira por uma ação local.

### F.3. Validação Inline
* Campo inválido: borda `color.accent` (crítico) + ícone `alert-circle` (16px) + `type.body.sm` abaixo do campo + *shake* horizontal único (Seção G.3) na tentativa de submissão.
* Nunca depender só de cor para indicar erro (acessibilidade a daltonismo) — o ícone e o texto são obrigatórios, a cor é reforço.

### F.4. Haptics (`expo-haptics`, nova dependência — Seção J)

| Evento | Chamada |
| :--- | :--- |
| Toque em ação primária/confirmação de série | `Haptics.impactAsync(ImpactFeedbackStyle.Light)` |
| Sucesso de operação crítica (login, salvar, sincronizar) | `Haptics.notificationAsync(NotificationFeedbackType.Success)` |
| Erro de validação/falha de operação | `Haptics.notificationAsync(NotificationFeedbackType.Error)` |
| Superação de carga/PR | `Haptics.notificationAsync(NotificationFeedbackType.Success)` (mesmo evento de sucesso — PR é um sucesso especial, não uma categoria tátil própria) |
| Abertura de diálogo destrutivo (`FinishWorkoutDialog`, excluir conta) | `Haptics.impactAsync(ImpactFeedbackStyle.Medium)` |

Respeita silenciosamente a ausência de suporte a haptics do dispositivo (a própria API do Expo já lida com isso sem necessidade de tratamento extra).

---

## G) Motion e Transições (`react-native-reanimated`)

### G.1. Tokens de Tempo e Curva
* `motion.fast` = 120ms — resposta a toque (scale de botão).
* `motion.base` = 220ms — entrada/saída de toast, crossfade de tab.
* `motion.slow` = 380ms — transição de tela (push/pop de stack), pulso de PR.
* Curva padrão de entrada: `Easing.out(Easing.cubic)`. Curva padrão de saída: `Easing.in(Easing.cubic)`.
* Todas as animações checam `AccessibilityInfo.isReduceMotionEnabled()` antes de disparar (já um requisito de `04` §C.3, generalizado aqui para todo o app) — com "reduzir movimento" ativado, toda transição vira um corte direto (fade rápido de 80ms no máximo), sem scale/slide/pulso.

### G.2. Padrões por Tipo de Transição
* **Push/pop de stack** (ex: entrar em `CycleFormScreen`, `SessionHistoryScreen`): slide horizontal padrão do React Navigation (nativo), `motion.slow`.
* **Tela modal/full-screen** (`ActiveWorkoutScreen` sobre o `AppStack`): slide-from-bottom + fade, `motion.slow` — comunica "isto é um contexto de foco único", reforçando a regra de `04` §A ("sem tab bar visível").
* **Troca de aba** (bottom tabs): crossfade simples, `motion.base` — nunca slide (slide entre abas do mesmo nível visual é ambíguo sobre direção/hierarquia).
* **Toque em botão:** *scale* 1.0 → 0.97 → 1.0, `motion.fast`.
* **Pulso de PR/celebração:** glow (`box-shadow`/`shadow*` animado) de `glow.soft` → `glow.strong` → `glow.soft`, um único ciclo, `motion.slow` (600ms totais, conforme já especificado em `04` §C.3).
* **Shake de erro:** translação horizontal ±4px, 3 ciclos, ~300ms total.

---

## H) Componentes-Base (retrofit de `components/common/`)

Especificação visual dos componentes já existentes em `frontend/components/common/` (Sprint 0) — este documento **redefine a aparência**, não a API/props já implementada, exceto onde indicado.

### H.1. `Button`
* **Variante `primary`:** contorno 1.5px `color.accent`, fundo transparente sobre `color.bg`/`color.surface`, texto `color.accent` em `type.body.md` maiúsculo com tracking leve, `radius.sm`, glow `glow.soft` constante. Toque: fundo tinge para `color.accent.dim` + scale (G.2).
* **Variante `secondary`/`ghost`:** contorno `color.border.strong`, texto `color.text.primary`, sem glow. Para ações não-primárias (ex: "Esqueceu a senha?").
* **Variante `destructive`:** contorno `color.accent` com `glow.strong`, ícone + texto sempre visíveis (Seção D.1), nunca desencadeia a ação sem uma confirmação (modal/dialog).
* **Variante `icon` (botão só-ícone):** área mínima 44x44 (56x56 se frequente), fundo `color.surface` circular ou `radius.sm`, ícone `color.text.secondary` (neutro) ou `color.accent` quando representa a ação primária da tela (ex: FAB-like "Iniciar Treino" no Dashboard, Seção I).
* **Estado `disabled`:** opacidade 40%, sem glow, sem resposta a toque.

### H.2. `Input`
* Estilo "underline" (borda inferior única, sem caixa fechada nos outros três lados) — `color.border` em repouso, `color.accent` com `glow.soft` em foco, `color.accent` (crítico) em erro.
* Ícone prefixo opcional (`icon.default`, 20px), `color.text.muted` em repouso, `color.accent` em foco.
* Label flutuante acima do campo (`type.body.sm`, `color.text.secondary`), nunca placeholder-only (placeholder sozinho força o usuário a lembrar o que já digitou — mesmo princípio de clareza dos demais componentes).

### H.3. `Card`
* Fundo `color.surface`, borda 1px `color.border`, `radius.md`. Nenhuma sombra (`shadow*`/`elevation`) — hairline de borda é o único recurso de separação visual, consistente com a estética "linhas" pedida.

### H.4. `Modal`
* Fundo `color.bg.elevated`, `radius.lg`.
* **Confirmação simples** (informativo, sem risco): fade + scale central, `motion.base`.
* **Confirmação destrutiva/de sessão** (`FinishWorkoutDialog`, excluir conta): sheet deslizando de baixo, `motion.slow`, botão destrutivo (H.1) sempre com o texto explícito da consequência (ex: "Excluir conta permanentemente"), nunca "Confirmar" genérico.

### H.5. Novos Componentes Base (nascem com este documento)

* **`components/common/Toast.tsx`** — implementa Seção F.1. Sem biblioteca de terceiros (mesmo princípio de baixa dependência já aplicado a Zustand/Redis neste projeto, `AGENTS.md` §2).
* **`components/layout/TabBar.tsx`** — bottom tab bar customizada (ainda não implementada; nasce quando `AppStack` virar `BottomTabNavigator` real na Sprint 2). Fundo `color.bg.elevated`, hairline superior `color.border`. 5 ícones (`icon.action`, 24px): `home-variant` (Dashboard), `calendar-sync` (Ciclos), `dumbbell` (Exercícios), `chart-line` (Métricas), `account-circle` (Perfil). Aba ativa: ícone `color.accent` + indicador (traço fino, sem glow) abaixo; abas inativas: ícone `color.text.secondary`. Nenhuma aba mostra label de texto exceto a ativa (`type.body.sm`, abaixo do ícone) — reduz poluição textual mantendo orientação mínima.
* **`components/layout/Header.tsx`** — cabeçalho padrão das telas do `AppStack` (exceto `DashboardScreen`, que não tem seta de voltar por ser a raiz). Ícone `chevron-left` (24px) à esquerda só quando há navegação de volta, título `type.display.lg` centralizado ou alinhado à esquerda, no máximo um ícone de ação à direita (Seção D.1).

---

## I) Planejamento de Telas (posicionamento tela a tela)

Esta seção percorre cada tela de `04_FRONTEND_UI_COMPONENTES.md` §A e fixa **onde** cada elemento fica, para eliminar decisões de posicionamento improvisadas em qualquer sprint futura.

| Tela | Header | Corpo | Ação primária | Ações secundárias | Ícones-chave |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`LoginScreen`** | Wordmark centralizado (`type.display.xl`, Seção K), sem seta de voltar | Inputs de e-mail/senha empilhados, centralizados verticalmente | Botão `primary` "Entrar", largura total, logo abaixo dos inputs | "Esqueceu a senha?" (`ghost`, texto pequeno, abaixo do botão primário); "Cancelar exclusão da conta" inline (só quando `ACCOUNT_PENDING_DELETION`, `04` §A) | `mail`, `lock` (prefixo dos inputs) |
| **`RegisterScreen`** | Wordmark menor (`type.display.lg`) + seta de voltar para `LoginScreen` | Inputs nome/e-mail/senha/confirmação empilhados | Botão `primary` "Criar conta", largura total, fim do formulário | Link "Já tem conta? Entrar" abaixo do botão | `account`, `mail`, `lock` |
| **`VerifyEmailScreen`** (dormente) | Header padrão sem ação | Estado único: ícone `icon.feature` + mensagem de status (sucesso/erro do token) | Botão `primary` "Ir para o login" | — | `email-check`/`alert-circle` conforme resultado |
| **`ForgotPasswordScreen`** | Header padrão, seta de voltar | Input de e-mail único, texto explicativo `type.body.md` acima | Botão `primary` "Enviar link" | Link "Voltar ao login" | `mail` |
| **`ResetPasswordScreen`** | Header padrão, sem seta de voltar (chegada via deep link) | Inputs de nova senha + confirmação | Botão `primary` "Redefinir senha" | — | `lock-reset` |
| **`DashboardScreen`** (home, sem seta de voltar) | Saudação (`type.display.md`, "Olá, {name}") + ícone de perfil no canto superior direito (único ícone de ação do header, Seção D.1) | Card do ciclo ativo/dia sugerido (topo), `OverviewCards` (Sprint 4), banner de alerta de estagnação (quando houver, logo abaixo do card de ciclo) | Botão `icon` grande de destaque "Iniciar Treino" (variante primária, ícone `play` + label, único elemento com glow constante na tela), fixo na região inferior central, acima da tab bar | Botão "Histórico" (`ghost`, ícone `history`) próximo ao card de ciclo | `play` (CTA), `history`, `account-circle` (header) |
| **`CyclesListScreen`** | Título "Ciclos" (`type.display.lg`) + ícone de ação "novo ciclo" (`plus`, canto superior direito) | Lista de cards de ciclo, badge "Arquivado" (Seção H — badge neutro) nos arquivados | Ícone `plus` do header (não há botão flutuante solto na tela — a ação "criar" mora no header, nunca um FAB avulso) | Ação de ativar/arquivar por item de lista (ícone inline por item) | `plus`, `archive`, `check-circle` (ativar) |
| **`CycleFormScreen`** | Título contextual ("Novo ciclo"/nome do ciclo), seta de voltar | Formulário em passos (nested stack, `04` §C.2), um `DayCard` por dia | Botão `primary` "Salvar"/"Avançar" fixo na base do passo atual | Botão "Adicionar dia"/"Adicionar exercício" (`ghost` + ícone `plus`) dentro do corpo, nunca flutuante | `plus`, `drag` (reordenação), `trash` (remoção, sempre com confirmação) |
| **`ActiveWorkoutScreen`** (full-screen, sem tab bar) | `WorkoutHeader`: nome do dia + cronômetro (`type.numeric`) à esquerda, botão destrutivo "Finalizar" (H.1) à direita | Lista de `ExerciseCard`, cada um com suas `SetRow` | Botão de confirmar série (`onConfirm`) — ícone `check`, 56x56 (Seção E), à direita de cada `SetRowForm` | `RestTimerModal` sobreposto (não bloqueia navegação de fundo); `SuggestionBadge` inline no topo de cada `ExerciseCard` | `check` (confirmar série), `timer` (descanso), `alert` (estado `CONFLICT`) |
| **`SessionHistoryScreen`** | Título "Histórico", seta de voltar para `DashboardScreen` | Lista de sessões (data, dia, duração, volume) | — (tela de consulta, sem ação primária) | Filtro por dia de treino (ícone `filter`, header) | `filter`, `chevron-right` (abrir detalhe) |
| **`ExercisesLibraryScreen`** | Título "Exercícios" + ícone "novo exercício" (`plus`, header) | Lista filtrável por grupo muscular (chips/badges no topo do corpo) | Ícone `plus` do header (mesma regra anti-FAB de `CyclesListScreen`) | Edição/remoção só em exercícios `isCustom` (ícones inline no item) | `plus`, `pencil`, `trash` |
| **`MetricsScreen`** | Título "Métricas" | Gráficos empilhados (Victory Native): histórico de carga, PR, frequência, grupos musculares — cada um em um `Card` (H.3) | — (tela de consulta) | Seletor de exercício (para o gráfico de histórico) no topo do corpo | `chart-line`, `trophy` (PR) |
| **`ProfileScreen`** | Título "Perfil" | Formulário de nome/e-mail, seção de troca de senha, seção de preferências de push, tudo em `Card`s separados | Botão `primary` "Salvar alterações" ao fim de cada seção editável | Botão `destructive` "Excluir conta" isolado ao final da tela, com espaçamento extra (`space.xl`) acima para nunca ficar adjacente a um botão frequente (`04` §C.2) | `account`, `lock`, `bell`, `trash` (excluir, sempre com texto) |

### I.1. Regras Gerais de Posicionamento (aplicam-se a todas as telas acima)
* **Nunca um botão flutuante solto (FAB) sem contexto de header/rodapé** — toda ação "criar" mora no header (ícone `plus` no canto superior direito) ou em um botão de largura total no rodapé do formulário. Exceção deliberada e única: o botão "Iniciar Treino" do `DashboardScreen`, que é grande e central por ser *a* ação mais importante do app — mesmo assim, ele fica ancorado (não flutua livremente sobre o conteúdo ao rolar a tela).
* **Ação destrutiva nunca fica adjacente a uma ação frequente** sem espaçamento/barreira visual clara (`04` §C.2, reforçado na Seção H.1 acima).
* **Voltar é sempre canto superior esquerdo; a única ação de destaque do header é sempre canto superior direito.** Nunca inverter, nunca duplicar.
* **Ação primária de qualquer formulário fica ancorada à base do formulário/tela**, nunca no topo — o usuário lê/preenche de cima para baixo e confirma no fim, sem precisar rolar de volta para cima.

---

## J) Novas Dependências

| Pacote | Motivo | Instalação |
| :--- | :--- | :--- |
| `@expo-google-fonts/oswald` | Fonte de display (Seção C). | `npx expo install @expo-google-fonts/oswald expo-font` |
| `@expo-google-fonts/inter` | Fonte de corpo (Seção C). | `npx expo install @expo-google-fonts/inter` |
| `expo-haptics` | Feedback tátil (Seção F.4). | `npx expo install expo-haptics` |
| `@expo/vector-icons` | Iconografia (Seção D) — já embutido no template padrão do Expo; comando abaixo só garante presença explícita no `package.json`. | `npx expo install @expo/vector-icons` |

Nenhuma biblioteca de gráficos, animação, gerenciamento de estado ou UI-kit adicional é introduzida — `react-native-reanimated` (motion, já presente), `NativeWind` (estilização, já presente) e Victory Native (gráficos, já decidido em `04` §A) continuam sendo as únicas dependências de UI do projeto além das listadas acima.

---

## K) Identidade Visual

* **Wordmark (uso principal, cobre o MVP/demo acadêmica):** "IRONTRACK" em Oswald 700, maiúsculo, `type.display.xl`, `color.text.primary`, com um traço fino horizontal (`color.accent`, 2-3px, ~30% da largura do texto) centralizado logo abaixo — o mesmo padrão já validado na prévia visual apresentada ao usuário nesta sessão. Usado no splash screen e nos headers de `LoginScreen`/`RegisterScreen`.
* **Ícone do app (`app.json`/EAS, ativo estático — não é hot-reload de código):** recomenda-se um monograma simples — a letra "I" em Oswald 700 sobre `color.bg`, com um traço/aresta em `color.accent`. Construir em uma ferramenta vetorial (Figma, gratuito) usando os tokens exatos desta seção, **não** um gerador de imagem por IA — ícone de app exige controle vetorial preciso e *safe areas* específicas por plataforma (máscara circular iOS, camadas adaptativas do Android) que ferramentas generativas não produzem de forma confiável. Um monograma tipográfico simples é suficiente para o escopo de demonstração/acadêmico deste projeto; um glifo mais elaborado (ex: silhueta de anilha/peso) fica como polimento opcional futuro, não bloqueante.

---

## L) Fora de Escopo deste Documento

* Definição de uma segunda cor de acento, tema claro, ou temas customizáveis pelo usuário — o app é dark-only, mono-acento, por decisão deliberada (Seção B).
* Ilustrações customizadas/mascote — o app se apoia em tipografia + ícones + o próprio neon como identidade, não em arte customizada (`04`/`08` não alocam tempo de sprint para isso).
* Qualquer biblioteca de UI-kit pronta (React Native Paper, NativeBase, Tamagui, etc.) — os componentes-base (Seção H) continuam sendo construídos manualmente sobre NativeWind, mesmo espírito de baixa dependência já aplicado ao restante do projeto.
