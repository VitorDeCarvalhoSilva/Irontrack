# 13_ADR_LOG.md - Log de Decisões Arquiteturais (ADRs)

Registro leve de Architecture Decision Records — uma entrada por decisão
consequente tomada ao longo do planejamento do **IronTrack**. Cada entrada
segue o formato: Contexto, Decisão, Alternativas Consideradas, Consequências,
Status. Novas decisões futuras devem ser **adicionadas ao final**, nunca
reescrever uma entrada existente (se uma decisão for revertida, registre
uma nova ADR referenciando a antiga como superada).

---

## ADR-001 — SQLite em vez de PostgreSQL
* **Contexto:** o projeto precisa de um banco relacional simples, com baixo custo operacional, para uma instância única de aplicação.
* **Decisão:** SQLite como único banco de dados, backend e local (histórico — descartado no cliente, ver ADR-005).
* **Alternativas consideradas:** PostgreSQL (mais robusto para concorrência, mas exige um serviço de infraestrutura adicional, contrário à filosofia de simplicidade operacional do projeto).
* **Consequências:** sem suporte nativo a múltiplas instâncias de escrita concorrente — arquitetura de deploy limitada a uma única instância (`05_DEVOPS_E_SEGURANCA.md`). Backup precisa de estratégia própria (`.backup` do SQLite), não replicação nativa.
* **Status:** Ativo.

## ADR-002 — Sem Redis / Cache Exclusivamente Client-Side
* **Contexto:** rate-limiting de login e cache de leitura foram cogitados com Redis.
* **Decisão:** rate-limiting via Caffeine (cache em memória do próprio processo backend); cache de leitura é responsabilidade exclusiva do cliente (frontend).
* **Alternativas consideradas:** Redis (padrão de mercado para rate-limit distribuído, mas desnecessário para uma instância única e adiciona um serviço de infraestrutura).
* **Consequências:** contadores de rate-limit são perdidos a cada reinício do backend (aceito conscientemente, `05_DEVOPS_E_SEGURANCA.md` §E.2).
* **Status:** Ativo.

## ADR-003 — Método de Dupla Progressão para o Motor de Sobrecarga
* **Contexto:** o motor de sugestão de carga precisa de uma regra determinística para decidir quando aumentar peso vs. repetições.
* **Decisão:** método de dupla progressão (faixa de reps-alvo por exercício; sobe carga só ao esgotar a faixa em todas as séries planejadas).
* **Alternativas consideradas:** progressão linear simples (sempre subir carga a cada sessão — insustentável a médio prazo); RPE-based autoregulation (mais sofisticado, mas não-determinístico e mais difícil de testar; registrado como possível refinamento futuro, `06_LOGICA_DE_PROGRESSAO.md` §G).
* **Consequências:** motor 100% determinístico e testável sem I/O; RPE vira dado apenas informativo nesta versão.
* **Status:** Ativo.

## ADR-004 — Expo (Managed Workflow) em vez de React Native Bare
* **Contexto:** migração de PWA para app nativo, decidida antes de qualquer código de frontend existir.
* **Decisão:** React Native via Expo, managed workflow, com EAS Build/Submit/Update.
* **Alternativas considerada:** React Native bare (mais controle sobre módulos nativos, mas exige hardware Mac local para build iOS e pipeline de CI mais complexo).
* **Consequências:** builds iOS na nuvem (EAS Build) sem precisar de Mac; deploy OTA de mudanças JS via EAS Update sem repassar pela revisão de loja a cada fix.
* **Status:** Ativo.

## ADR-005 — AsyncStorage em vez de SQLite/IndexedDB no Cliente
* **Contexto:** persistência local do snapshot de sessão ativa e fila de sincronização offline.
* **Decisão:** AsyncStorage (key-value simples).
* **Alternativas consideradas:** SQLite local via `expo-sqlite` (relacional, mas desnecessário — o padrão de acesso é sempre "snapshot único por sessão" e "fila FIFO", nunca uma consulta relacional complexa); `idb`/IndexedDB (era a escolha da era PWA, descartada junto com a migração para nativo, ADR-004).
* **Consequências:** abstração `saveSnapshot`/`loadSnapshot`/`clearSnapshot` permanece idêntica através da migração — só o motor por trás trocou.
* **Status:** Ativo.

## ADR-006 — Victory Native em vez de Recharts
* **Contexto:** biblioteca de gráficos para `MetricsScreen` (histórico de carga, frequência, grupos musculares).
* **Decisão:** Victory Native (baseado em `react-native-svg`).
* **Alternativas consideradas:** Recharts (era a escolha da era PWA — baseado em DOM/SVG web, não roda em React Native, descartado junto com a migração, ADR-004).
* **Consequências:** nenhuma, é uma troca direta de biblioteca sem impacto de contrato de API.
* **Status:** Ativo.

## ADR-007 — NativeWind para Estilização
* **Contexto:** necessidade de um design system consistente e rápido de manter no app nativo.
* **Decisão:** NativeWind (sintaxe Tailwind CSS sobre React Native).
* **Alternativas consideradas:** `StyleSheet.create` puro (zero-dependência, mas mais verboso e sem um sistema de design tokens embutido).
* **Consequências:** dependência adicional aceita em troca de velocidade de desenvolvimento consistente.
* **Status:** Ativo.

## ADR-008 — Snapshot Imutável de Meta em `session_exercises`
* **Contexto:** ao formalizar o motor de progressão (`06`), identificou-se que `session_exercises` não persistia a meta copiada do template no início da sessão.
* **Decisão:** adicionar `training_day_exercise_id`/`target_sets`/`target_reps_min`/`target_reps_max` como colunas de `session_exercises`, congeladas no momento da cópia.
* **Alternativas consideradas:** reconsultar `training_day_exercises` a cada chamada de sugestão (rejeitada — quebraria se o template fosse editado no meio de um ciclo, alterando retroativamente a meta de sessões já iniciadas).
* **Consequências:** garante que editar um template não afeta sessões já em andamento ou já concluídas.
* **Status:** Ativo.

## ADR-009 — Idempotência via `clientGeneratedId`, não Apenas Retry Simples
* **Contexto:** a fila de sincronização offline pode reenviar uma operação já processada pelo servidor (timeout de rede após sucesso).
* **Decisão:** todo registro de série carrega um `clientGeneratedId` (UUID) gerado no cliente, com constraint `UNIQUE` no backend — reenvios retornam o registro existente em vez de duplicar.
* **Alternativas consideradas:** confiar apenas em retentativas com backoff sem chave de idempotência (rejeitada — risco real de duplicar séries em reconexões instáveis, comum em ambiente de academia).
* **Consequências:** toda a cadeia (frontend, contrato, schema) precisa propagar o campo consistentemente — já implementado em `02`/`03`/`04`.
* **Status:** Ativo.

## ADR-010 — EAS Build/Submit/Update em vez de Infraestrutura Própria de Build Mobile
* **Contexto:** parte da migração para React Native (ADR-004) — como compilar e distribuir os builds iOS/Android.
* **Decisão:** EAS (Expo Application Services) para build, submissão às lojas e atualização OTA.
* **Alternativas consideradas:** pipeline próprio com Fastlane + runners macOS auto-hospedados (mais controle, mas custo de manutenção e hardware significativamente maior para o estágio atual do projeto).
* **Consequências:** dependência do serviço Expo/EAS (incluindo custo de plano pago acima de um determinado volume de builds); documentado em `05_DEVOPS_E_SEGURANCA.md`.
* **Status:** Ativo.

## ADR-011 — Exclusão de Conta: Hard Delete Após Carência, Sem Anonimização
* **Contexto:** formalização da política de privacidade/LGPD (`11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md`).
* **Decisão:** exclusão física completa (cascata via FKs) após 30 dias de carência; sem retenção de dados anonimizados para fins estatísticos.
* **Alternativas consideradas:** anonimizar e reter dados agregados (rejeitada — adiciona complexidade de conformidade, sem benefício de produto suficiente no estágio atual).
* **Consequências:** nenhuma métrica agregada histórica sobrevive à exclusão de um usuário; backups retêm uma cópia por até ~14 dias adicionais (divulgado ao usuário).
* **Status:** Ativo.

## ADR-012 — Reversão do Requisito de Motor Polimórfico (Calistenia/Isometria)
* **Contexto:** auditoria de documentação de 2026-07-08 identificou que `01_ARQUITETURA_E_PADROES.md` §7 (antiga "Flexibilidade de Domínio — Requisito Obrigatório") exigia um motor de progressão polimórfico (Strategy/Factory) cobrindo calistenia isométrica e progressões de habilidade (SKILL) — em contradição direta com `00_PRD_IRONTRACK.md`, `AGENTS.md` §2, `03_CONTRATOS_API.md` §1.1 e o próprio `06_LOGICA_DE_PROGRESSAO.md` (a especificação final do motor, que só cobre `STRENGTH`), todos os quais já travavam o escopo ativo em musculação clássica/hipertrofia.
* **Decisão:** `01` §7 reescrita para deixar claro que apenas a **tolerância de schema** (colunas anuláveis, `exercise_library.type` com 3 valores) é mantida como extensibilidade barata; nenhuma lógica de aplicação polimórfica deve ser implementada. Calistenia/isometria permanecem fora do escopo ativo do produto.
* **Alternativas consideradas:** manter o requisito e expandir `06`/`07`/`08` para cobrir os 3 tipos de treino (rejeitada explicitamente pelo usuário — aumentaria a complexidade do MVP sem necessidade de produto validada).
* **Consequências:** nenhum código de `ISOMETRIC`/`SKILL` deve ser escrito nas Sprints 0-5 de `07`/`08`. Os exemplos de seed de calistenia em `02_SCHEMA_SQLITE.md` §3.2 continuam existindo apenas como demonstração de extensibilidade, nunca como dado real do MVP (já era este o caso antes desta ADR).
* **Status:** Ativo.

## ADR-013 — Versões e Bibliotecas Core do Backend (Java 21, Spring Boot 3.3.x, jjwt, Flyway)
* **Contexto:** nenhum documento fixava a versão exata do Java/Spring Boot nem as bibliotecas concretas de JWT e migration — um agente de codificação autônomo precisaria decidir isso sozinho a cada sprint, risco de inconsistência entre sprints/agentes diferentes.
* **Decisão:** Java 21 (LTS, já implícito no `Dockerfile`); Spring Boot 3.3.x; `io.jsonwebtoken:jjwt` 0.12.x para geração/validação de JWT (assinatura HMAC simétrica); Flyway (somente `flyway-core` — ver correção abaixo) como ferramenta de migration obrigatória, não mais "recomendada". Detalhes em `01_ARQUITETURA_E_PADROES.md` §2.4.
* **Correção pós-implementação (Sprint 0 do backend, 2026-07-08):** esta ADR originalmente citava `org.flywaydb:flyway-database-sqlite` como módulo adicional necessário — esse artefato **não existe** no Maven Central. O suporte a SQLite já vem embutido em `flyway-core`. Apenas a coordenada de artefato estava errada; a decisão de usar Flyway como ferramenta obrigatória permanece inalterada.
* **Alternativas consideradas:** Liquibase (mais verboso em XML/YAML para o caso de uso simples deste projeto); `auth0/java-jwt` (API equivalente a `jjwt`, escolha arbitrária entre duas opções maduras — `jjwt` escolhida por ser mais comumente usada em tutoriais/documentação Spring Security, reduzindo fricção de qualquer agente de codificação).
* **Consequências:** `pom.xml` do Sprint 0 já nasce com essas dependências fixas, sem decisão pendente na primeira tarefa de código do projeto.
* **Status:** Ativo.

## ADR-014 — Sem Lombok: DTOs como `record`, Entidades JPA com Getters/Setters Explícitos
* **Contexto:** `01_ARQUITETURA_E_PADROES.md` §2.2 já recomendava `record` para DTOs, mas não havia decisão explícita sobre Lombok para entidades JPA (que não podem ser `record` por exigirem construtor vazio e proxies de lazy-loading).
* **Decisão:** nenhum uso de Lombok neste projeto. DTOs usam `record` nativo do Java; entidades JPA usam getters/setters explícitos.
* **Alternativas consideradas:** Lombok em entidades apenas (rejeitada — mistura de convenção, parte do código com anotações geradoras de boilerplate e parte sem, para uma economia de linhas que não justifica a dependência adicional e a curva de familiaridade de anotações `@Data`/`@Builder` para quem lê o código gerado).
* **Consequências:** entidades JPA um pouco mais verbosas, em troca de zero dependência de annotation processing adicional e código 100% explícito/navegável.
* **Status:** Ativo.

## ADR-015 — `react-hook-form` + `zod` para Validação de Formulário no Frontend
* **Contexto:** múltiplas telas (`LoginScreen`, `RegisterScreen`, `ProfileScreen`, `CycleFormScreen`, etc.) têm formulários com regras de validação já formalizadas em `03_CONTRATOS_API.md` (ex: senha mínima 8 caracteres com letras e números), mas nenhum documento decidia a biblioteca de validação client-side a usar.
* **Decisão:** `react-hook-form` (controle de formulário/performance de re-render) + `zod` (schema de validação) + `@hookform/resolvers/zod` (integração entre os dois). Detalhes em `01_ARQUITETURA_E_PADROES.md` §3.4.
* **Alternativas consideradas:** Formik + Yup (combinação equivalente e igualmente popular, mas `react-hook-form` tem melhor performance em React Native por minimizar re-renders — relevante em `ActiveWorkoutScreen`, tela mais sensível a performance do app, mesmo que ela em si não seja um formulário tradicional); validação manual com `useState` (rejeitada — duplicaria lógica de validação de forma inconsistente entre telas, o oposto do objetivo de eliminar ambiguidade).
* **Consequências:** duas dependências novas (`react-hook-form`, `zod`) adicionadas ao `package.json` do Sprint 0/1 — ambas leves e amplamente adotadas no ecossistema React Native/Expo.
* **Status:** Ativo.

## ADR-016 — Remoção Total da Tolerância de Schema para Calistenia/Isometria (Supersede ADR-012)
* **Contexto:** o usuário, ao orquestrar um agente separado, notou o app "com foco em musculação e calistenia" ao ler `01`/`02` e ficou confuso sobre o escopo real — mesmo depois de `ADR-012` já ter removido o requisito de *implementação* polimórfica, a tolerância de schema (colunas `hold_time_seconds`/`hold_time_target`/`progression_step_id`, tabela `progression_steps`, `exercise_library.type IN ('STRENGTH','ISOMETRIC','SKILL')`, seed de exemplo com Handstand/Planche/Muscle-Up) continuava presente e gerando confusão, mesmo sem nenhum código ativo a usá-la. Contexto adicional: este é um projeto acadêmico, com preferência explícita por simplicidade sobre generalização especulativa.
* **Decisão:** remover **fisicamente** de `02_SCHEMA_SQLITE.md` a tabela `progression_steps`, as colunas `hold_time_seconds`/`hold_time_target`/`progression_step_id` de `exercise_sets`, e a coluna `exercise_library.type` inteira (não apenas os valores `ISOMETRIC`/`SKILL` — com um único valor possível, a coluna não carregaria informação). Seed simplificado para conter apenas exercícios de musculação. `01` §7 reescrita novamente para refletir que não há mais nenhuma tolerância, apenas a orientação de que uma expansão futura de escopo (para qualquer modalidade, não só calistenia) exigiria uma decisão Tipo B e uma migração feita no momento em que isso for decidido.
* **Alternativas consideradas:** generalizar o modelo para registrar "qualquer tipo de treino" (métricas configuráveis por exercício) — rejeitada explicitamente pelo usuário nesta rodada por aumentar a complexidade do motor de progressão (`06`) e da UI sem necessidade validada para o escopo acadêmico do projeto; manter a tolerância de schema como estava (mantendo `ADR-012`) — rejeitada por ainda gerar confusão de escopo ao ser lida por agentes/pessoas novas no projeto.
* **Consequências:** `02_SCHEMA_SQLITE.md`, `03_CONTRATOS_API.md` (campos `type`/`holdTimeSeconds`/`holdTimeTarget`/`progressionStepId` removidos dos exemplos JSON) e `01_ARQUITETURA_E_PADROES.md` §7 atualizados nesta mesma sessão. Nenhuma migração de banco real é afetada, pois nenhum código foi escrito ainda. `ADR-012` permanece no log como registro histórico da decisão intermediária, mas está superada por esta.
* **Status:** Ativo (supersede ADR-012).

## ADR-017 — Expo SDK Travado na Versão 54 (Falha em Produção do Prompt de Sprint 0)
* **Contexto:** o prompt de kickoff da Sprint 0 do frontend instruía `npx create-expo-app@latest`, sem fixar a versão do SDK. Ao ser executado, isso resolveu para o SDK mais recente disponível no momento — que não rodava no ambiente do usuário (incompatibilidade de versão do Expo Go/ferramentas locais). Esta é uma falha real de execução, não uma preferência arquitetural — deveria ter sido travada desde `01_ARQUITETURA_E_PADROES.md` §2.4 (o equivalente já existe para o backend: Java 21, Spring Boot 3.3.x), mas o `08_ROADMAP_FRONTEND.md`/prompt de Sprint 0 não replicou esse mesmo rigor para o Expo SDK.
* **Decisão:** travar **Expo SDK 54** como versão obrigatória do projeto. Todas as dependências gerenciadas pela Expo (`react-native`, `react`, `expo-*`) devem ser resolvidas através de `npx expo install --fix` visando `expo@54.x` — nunca via `npm install`/`create-expo-app@latest` sem especificar a versão, e nunca com números de versão de `react-native`/`react` escolhidos manualmente (a matriz de compatibilidade é mantida pela própria Expo; fixar esses números à mão é o tipo de erro que causou esta ADR).
* **Alternativas consideradas:** deixar a versão do SDK em aberto/"sempre a mais recente" (rejeitada — foi exatamente o que causou a falha que gerou esta ADR); fixar manualmente as versões de `react-native`/`expo`/`react` uma a uma no `package.json` (rejeitada — a Expo já resolve isso de forma mais confiável via `expo install --fix`, fixar à mão é reintroduzir o mesmo tipo de risco).
* **Consequências:** `01_ARQUITETURA_E_PADROES.md` ganhou uma nova §3.5 formalizando isso; qualquer scaffold futuro do projeto (recriação do zero, `create-expo-app`, ou upgrade de SDK) deve alvejar explicitamente a versão 54, nunca `@latest`. Upgrade de SDK no futuro é uma decisão Tipo B nova, não algo a fazer via `@latest` casualmente.
* **Status:** Ativo.

## ADR-018 — Verificação de E-mail Desativada no Fluxo Ativo (Decisão Tipo B do Usuário)
* **Contexto:** durante teste manual de integração, o usuário identificou que o fluxo de verificação de e-mail (`03_CONTRATOS_API.md` §2.1/§2.2/§2.6) exige uma etapa manual incompatível com uma demonstração/apresentação do app — o link de confirmação (`irontrack://verify-email/{token}`) é um deep link de esquema customizado, que **não é aberto pelo Expo Go** (só funcionaria em um build standalone/`eas build`), e não há SMTP real configurado (`SMTP_HOST` vazio em dev, e-mail só é logado). Isso obrigava copiar o token do log do backend manualmente para continuar testando — inviável para apresentar o app funcionando.
* **Decisão:** desativar a exigência de verificação de e-mail no fluxo ativo. `POST /auth/register` passa a marcar `email_verified_at = now()` automaticamente no cadastro, sem gerar token nem disparar `EmailService`. `POST /auth/login` não verifica mais `email_verified_at` (a condição nunca mais será verdadeira de qualquer forma). O endpoint `GET /auth/verify-email/{token}` e as colunas `email_verification_token_hash`/`email_verification_expires_at` em `users` **permanecem no schema/contrato, dormentes** — não foram fisicamente removidos, para permitir reativação futura sem nova migração, caso o produto volte a exigir verificação real (ex: antes de um lançamento público de verdade, fora do contexto de apresentação/demo acadêmica).
* **Alternativas consideradas:** manter a verificação e resolver o deep link com um build standalone (`eas build --profile development`) antes da apresentação — rejeitada por adicionar tempo/complexidade de build nativo a uma decisão que só precisa valer para demonstração; remover fisicamente as colunas/endpoint (mais "limpo", seguindo o mesmo espírito do `ADR-016`) — rejeitada aqui porque, ao contrário da calistenia (que nunca fez parte do escopo de produto), verificação de e-mail é um requisito real de `00_PRD_IRONTRACK.md` que só está sendo suspenso temporariamente, não descartado — manter o schema dormente é mais barato que remover e recriar depois.
* **Consequências:** `00_PRD_IRONTRACK.md` §4.3, `03_CONTRATOS_API.md` §2.1/§2.2/§2.6, `07_ROADMAP_BACKEND.md` §C.1 e `08_ROADMAP_FRONTEND.md` §C.1 atualizados para refletir o estado "dormente". `14_CATALOGO_DE_ERROS_DE_NEGOCIO.md`: `EMAIL_NOT_VERIFIED`/`INVALID_OR_EXPIRED_TOKEN` (este último também usado por reset de senha, que continua ativo) marcados como não-alcançáveis pelo fluxo de registro atual. Reativar exige apenas reverter a lógica de `AuthService.register()`/`login()` — nenhuma migração de banco necessária.
* **Status:** Ativo.

## ADR-019 — Sistema de Design: Monocromático + Acento Único Vermelho Neon, Diferenciado por Intensidade

* **Contexto:** o app não tinha nenhum documento de UI/UX formal — a interface implementada nas Sprints 0/1 usa componentes genéricos sem identidade visual, o que o usuário identificou como um problema explícito ("interface muito feia, sem seguir bons padrões de UX e totalmente genérica"). Uma prévia visual foi construída e validada interativamente com o usuário nesta sessão (widget de mockup, não um artefato do repositório) antes de formalizar a decisão: a primeira versão testava azul + vermelho como dois acentos, rejeitada explicitamente pelo usuário ("azul e vermelho juntos fica feio"); a segunda versão, monocromática com um único acento vermelho neon e uso de linhas/glow, foi aprovada.
* **Decisão:** paleta dark-only, monocromática (preto/cinza/branco-quebrado) com **um único acento cromático** (vermelho neon, `#FF1440`). Como não há uma segunda cor para diferenciar ações primáras de destrutivas, a diferenciação é feita por **intensidade de glow + comportamento de movimento** do mesmo tom (contida/estática para ações primárias; saturada/com pulso para ações destrutivas e celebração de PR) — opção escolhida explicitamente pelo usuário entre três alternativas apresentadas (as outras eram "vermelho só em destrutivo, primário em contorno neutro" e "diferenciar por preenchimento vs. contorno do botão"). Tipografia: Oswald (display) + Inter (corpo). Iconografia: `@expo/vector-icons` (MaterialCommunityIcons), com a regra "ícone antes de texto, exceto em ações destrutivas". Motion: `react-native-reanimated` (já presente na stack) com tokens de duração/curva formalizados. Especificação completa, incluindo planejamento tela a tela, em `15_DESIGN_SYSTEM_UI_UX.md`.
* **Alternativas consideradas:** paleta com dois acentos (azul + vermelho) — rejeitada pelo usuário na prévia visual; diferenciar ação primária/destrutiva por matiz diferente em vez de intensidade — rejeitada para não reintroduzir uma segunda cor de acento, contrariando o pedido explícito de "quero apenas vermelho"; adotar um UI-kit pronto (React Native Paper/NativeBase/Tamagui) para acelerar a estilização — rejeitada por manter o mesmo espírito de baixa dependência já aplicado ao restante do projeto (Context API em vez de Zustand, Caffeine em vez de Redis) e por UI-kits prontos tenderem a produzir a aparência "genérica" que motivou este documento.
* **Consequências:** três novas dependências (`@expo-google-fonts/oswald`, `@expo-google-fonts/inter`, `expo-haptics`) e confirmação explícita de `@expo/vector-icons` no `package.json` (`15_DESIGN_SYSTEM_UI_UX.md` §J). Componentes já implementados na Sprint 0 (`Button`, `Input`, `Card`, `Modal`) precisam de retrofit visual (sem mudança de API/props). Nenhuma tela nova é criada por esta decisão — ela redefine a aparência de telas já existentes e fixa o padrão para as telas ainda não implementadas (Sprints 2-5, `08_ROADMAP_FRONTEND.md`).
* **Status:** Ativo.
