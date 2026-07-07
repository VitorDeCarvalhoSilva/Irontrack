# AGENT_LOG.md — Log Cumulativo de Atividades de Agentes (IronTrack)

> Formato e regras completas em [`AGENTS.md`](AGENTS.md) §10. Resumo: este
> log é **append-only** (só adiciona, nunca remove/reescreve linhas
> antigas), uma linha por tarefa/sessão de trabalho, sempre no final da
> tabela. **Leia este arquivo por completo antes de iniciar qualquer
> tarefa no projeto.**

As linhas abaixo, até a marca `-- início do log ao vivo --`, são um
**backfill retroativo** reconstruído a partir do histórico da sessão de
planejamento que originou os documentos `docs/00`-`08` e este próprio
arquivo — registrado de uma vez, de forma resumida, para que o log não
comece "em branco" apesar de todo o trabalho já realizado.

| Data/Hora | Área | Tipo | Resumo | Arquivos Afetados |
| :--- | :--- | :--- | :--- | :--- |
| 2026-07-07 09:00 | Docs | Criação | Handover recebido: `00`-`03` (PRD, Arquitetura, Schema, Contratos API) já existiam de rodada anterior, aprovados. | `00`, `01`, `02`, `03` |
| 2026-07-07 09:30 | Frontend | Criação | `04_FRONTEND_UI_COMPONENTES.md` gerado (rotas, árvore de componentes, UX mobile-first, arquitetura offline-first). | `04` |
| 2026-07-07 10:00 | Frontend | Correção | Corrigidos: numeração de seções (A.x/B.x quebrada), tipos TS não definidos (`WorkoutSet`/`LoadSuggestion`/`RegisterSetPayload`), status `CONFLICT` ausente da union, escopo ambíguo de `/cycles/new`. | `04` |
| 2026-07-07 10:30 | DevOps | Criação | `05_DEVOPS_E_SEGURANCA.md` gerado (Docker, CI/CD, segredos, hardening de segurança). | `05` |
| 2026-07-07 10:45 | DevOps | Correção | Corrigido bug de permissão no Dockerfile do backend (`USER` antes de `COPY`) e ambiguidade no script de backup do SQLite. | `05` |
| 2026-07-07 11:15 | Multi | Correção | Reconciliação cruzada a partir de um planejamento Jira detalhado fornecido pelo usuário: template de dia (`training_day_exercises`), técnicas N:N, `clientGeneratedId`, `refresh_tokens` sem Redis, `load_increment_kg` por exercício, padronização `/api/v1`, fix `{exerciseId}`→`{sessionExerciseId}`, sugestão de carga escopada por sessão, CRUD completo de ciclos/dias/exercícios. | `00`, `02`, `03`, `04` |
| 2026-07-07 12:00 | Backend | Criação | `06_LOGICA_DE_PROGRESSAO.md` gerado (dupla progressão, classificação de séries contáveis, detecção de estagnação, definição de PR) + patch de snapshot imutável em `session_exercises`. | `02`, `03`, `06` |
| 2026-07-07 12:30 | Backend | Correção | Fechados 2 gaps do algoritmo de sugestão (peso heterogêneo entre séries contáveis; séries bônus além de `targetSets`) — CASO 3/CASO 4 redefinidos como mutuamente exclusivos e exaustivos; novo exemplo numérico H.4. | `06` |
| 2026-07-07 13:00 | Backend | Criação | `07_ROADMAP_BACKEND.md` gerado (roadmap sprint a sprint) + patch formalizando ciclo de vida completo de auth, perfil, métricas de dashboard, alertas de estagnação e notificações push. | `02`, `03`, `05`, `07` |
| 2026-07-07 13:30 | Frontend | Criação | `08_ROADMAP_FRONTEND.md` gerado (versão PWA) + patch em `04` (rotas de auth/perfil, dashboard e métricas expandidas, decisão de Recharts, resolução `idb` vs. Dexie.js). | `04`, `08` |
| 2026-07-07 14:00 | Multi | Migração | Migração de stack frontend de PWA (React/Vite) para app nativo (**React Native via Expo**) — reescrita de `01` §3, `04` e `08`; patch em `00` (stack), `02`/`03` (`push_subscriptions` via Expo Push Token em vez de Web Push/VAPID), `05` (remoção de VAPID, deploy Docker/Nginx substituído por EAS Build/Submit/Update), `07` (ajuste pontual da tarefa de push). | `00`, `01`, `02`, `03`, `04`, `05`, `07`, `08` |
| 2026-07-07 14:20 | Docs | Criação | `AGENTS.md` criado — constituição de comportamento para agentes de IA (leitura obrigatória, regra Tipo A/Tipo B, pipeline de execução, permissões/restrições, padrões de código, anti-padrões, checklist final). | `AGENTS.md` |
| 2026-07-07 14:25 | Docs | Auditoria | Inspetor de Projeto disparado em paralelo — auditoria completa de `00`-`08` em busca de resíduos da migração PWA→nativo, referências cruzadas quebradas, inconsistências de nomenclatura, e lacunas de documentação (LGPD, estratégia de testes, glossário, ADRs). Relatório final em `docs/09_RELATORIO_DE_INSPECAO_INICIAL.md` quando concluído. | `00`-`08` (múltiplos), `docs/09_RELATORIO_DE_INSPECAO_INICIAL.md` |

`-- início do log ao vivo --`

| Data/Hora | Área | Tipo | Resumo | Arquivos Afetados |
| :--- | :--- | :--- | :--- | :--- |
| 2026-07-07 14:30 | Docs | Criação | `AGENT_LOG.md` criado — log cumulativo de atividades, com backfill retroativo desta sessão registrado acima da marca de início do log ao vivo. | `AGENT_LOG.md` |
| 2026-07-07 14:50 | Docs | Auditoria | Inspeção geral de `00`-`08` concluída: 14 correções Tipo A aplicadas (destaque: ~27 referências cruzadas fantasmas a `02_SCHEMA_SQLITE.md §3.X`/`§1.X` e `07_ROADMAP_BACKEND.md §1.X` — numeração de prompts de origem já apagados que nunca correspondeu a headings reais; resíduos de terminologia PWA remanescentes em `00`/`01` fora do escopo da rodada de migração; 2 notas de "fora de escopo"/"futuro endpoint" em `06` desatualizadas após alertas de estagnação e PR terem sido implementados em `07`/`03`). 5 achados Tipo B reportados (LGPD/retenção de dados, estratégia de testes, glossário de domínio, ADR log, outras lacunas) com 4 documentos sugeridos (não criados). `prompt.md` desta tarefa arquivado em `docs/_prompts_historico/09_prompt_inspecao.md` (não apagado, por ser tarefa de auditoria — regra `AGENTS.md` §8). | `00`, `01`, `02`, `03`, `04`, `06`, `07`, `docs/09_RELATORIO_DE_INSPECAO_INICIAL.md`, `docs/_prompts_historico/09_prompt_inspecao.md` |
| 2026-07-07 15:15 | Docs | Criação | Revisado o relatório do Inspetor e criados os 5 documentos sugeridos (mais 1 bônus): `10_ESTRATEGIA_DE_TESTES.md` (pirâmide, TDD, ferramentas por camada, Maestro para E2E), `11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md` (LGPD, exclusão em 30 dias) com patch de `deletion_requested_at` em `02` e endpoints `DELETE /users/me`/`POST /auth/cancel-deletion` + `AccountDeletionSchedulerService` em `03`/`07`, `12_GLOSSARIO_DE_DOMINIO.md`, `13_ADR_LOG.md` (11 ADRs retroativos), `14_CATALOGO_DE_ERROS_DE_NEGOCIO.md` (13 códigos de erro consolidados). Adicionado mandato de TDD obrigatório (Seção 6.1) ao `AGENTS.md`. `README.md` reescrito (estava com encoding corrompido e desatualizado citando "React" puro) com pitch do produto, stack, tabela de todos os docs `00`-`14`, seção de governança de agentes, e placeholder de setup local. | `02`, `03`, `07`, `10`, `11`, `12`, `13`, `14`, `AGENTS.md`, `README.md` |
