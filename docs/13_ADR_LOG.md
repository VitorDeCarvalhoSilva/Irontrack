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
