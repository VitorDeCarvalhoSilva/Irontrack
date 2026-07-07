# 09_RELATORIO_DE_INSPECAO_INICIAL.md - Relatório de Inspeção Geral do Projeto

Este documento reporta os resultados da primeira auditoria completa da pasta `docs/` do **IronTrack** (documentos `00` a `08`, produto de 8 rodadas anteriores de planejamento). Segue a distinção de conduta já estabelecida no projeto: **Tipo A** (mecânico, inequívoco, corrigido diretamente nesta rodada) e **Tipo B** (decisão de produto/arquitetura em aberto, apenas relatado).

---

## A) Correções Aplicadas (Tipo A)

### `00_PRD_IRONTRACK.md`
1. §4.7 (Sprint 5): título e critérios de aceite ainda diziam "PWA", "Service Workers", "IndexedDB" e "Web Push API" — resíduos da arquitetura descartada na migração para nativo. Corrigido para "Offline-First Nativo", `AsyncStorage`/`NetInfo` e Expo Push Service (`expo-notifications`).
2. §4.3 (Sprint 1): critério de aceite ainda listava "edição de foto de perfil", mas upload de foto é explicitamente fora de escopo desde `07_ROADMAP_BACKEND.md`/`08_ROADMAP_FRONTEND.md` §E (decisão de rodada posterior nunca refletida de volta em `00`). Corrigido para remover a menção e listar edição de e-mail/senha, que de fato estão no escopo.
3. §4.6 (Sprint 4): critério de aceite do dashboard não mencionava recorde pessoal (PR), embora o endpoint `GET /metrics/exercises/{exerciseId}/pr` já exista desde a preparação de `07`. Adicionado.

### `01_ARQUITETURA_E_PADROES.md`
4. §1 (Visão Arquitetural de Alto Nível): descrição do frontend ainda dizia "PWA (Progressive Web App)" — não foi tocada na rodada de migração porque aquela rodada só reescreveu §3. Corrigida para descrever o app nativo React Native/Expo.
5. §4.2 (Interceptors): fluxo de falha de refresh ainda mencionava redirecionar para a rota web `/login`; fluxo de fallback offline ainda mencionava "fila de sincronização do IndexedDB". Corrigidos para `LoginScreen`/`AsyncStorage`.
6. §6.1 (Segurança): exemplo de variável de ambiente do frontend ainda dizia "variáveis de ambiente gerenciadas no PWA". Corrigido para a convenção real (`EXPO_PUBLIC_*`).

### `02_SCHEMA_SQLITE.md`
7. Removidas **10 referências cruzadas fantasmas** em comentários do DDL (`-- ... (Seção 3.1)`, `(Seção 3.2)`, `(Seção 3.3)`, `(Seção 3.4)`, `(Seção 3.5)` ×2, `(Seção 3.6)` ×2, `(Seção 3.7)`) que apontavam para subseções que **nunca existiram** dentro do próprio `02_SCHEMA_SQLITE.md` — eram, na verdade, a numeração interna dos `prompt.md` de origem (já apagados) que introduziram cada tabela/coluna. O `### 3.X` real do documento é sobre exemplos de seed, um assunto completamente diferente. As anotações foram simplificadas para descrições autocontidas, sem apontar para um número inexistente.
8. Corrigidas 2 referências a `07_ROADMAP_BACKEND.md §1.4`/`§1.5` (também inexistentes — mesma causa raiz) para os endereços reais `§C.4`/`§C.5`.

### `03_CONTRATOS_API.md`
9. Removidas **13 referências cruzadas fantasmas** a `02_SCHEMA_SQLITE.md §3.X`/`§1.X`, mesma causa raiz do item 7 — substituídas por menção direta ao nome da tabela/coluna real (já suficiente para localizar o conteúdo, e mais confiável que um número de seção inexistente).

### `04_FRONTEND_UI_COMPONENTES.md`
10. Removida 1 referência cruzada fantasma a `02_SCHEMA_SQLITE.md §3.2` em um comentário de tipo TypeScript.

### `06_LOGICA_DE_PROGRESSAO.md`
11. Removidas 3 referências cruzadas fantasmas a `02_SCHEMA_SQLITE.md §3.X`.
12. Seção D (Detecção de Estagnação): o texto ainda afirmava, duas vezes, que a materialização de alertas persistentes estava "explicitamente fora do escopo deste documento" — mas essa materialização (tabela `stagnation_alerts`, `GET /alerts`, `POST /alerts/{alertId}/snooze`) **já foi implementada** na preparação de `07_ROADMAP_BACKEND.md`. Texto corrigido para apontar para a implementação real em vez de negar sua existência.
13. Seção E (Definição de PR): o texto ainda dizia que um "futuro endpoint `GET .../pr`" exporia os PRs — esse endpoint já existe (`03_CONTRATOS_API.md` §6.3). Corrigido para referenciá-lo diretamente.

### `07_ROADMAP_BACKEND.md`
14. Removidas 4 referências cruzadas fantasmas a `02_SCHEMA_SQLITE.md §X.Y`.

**Total: 14 correções Tipo A aplicadas, tocando 7 dos 9 documentos existentes** (`05_DEVOPS_E_SEGURANCA.md` e `08_ROADMAP_FRONTEND.md` foram auditados e não apresentaram achados Tipo A — já haviam sido objeto de correções cirúrgicas equivalentes nas rodadas anteriores mais recentes).

---

## B) Pontos em Aberto (Decisões Necessárias) — Tipo B

### B.1. Política de Retenção e Exclusão de Dados (LGPD)
**Problema:** o app coleta nome, e-mail e um histórico detalhado de desempenho físico (cargas, repetições, RPE, técnicas aplicadas por sessão). Nenhum documento atual descreve o que acontece quando um usuário pede exclusão de conta — direito de apagamento previsto na LGPD (Lei 13.709/2018, art. 18, VI). Também não há definição de: prazo de retenção de dados após inatividade, se há exclusão física (`DELETE`) ou anonimização (`UPDATE` removendo PII mantendo dados agregados/estatísticos), nem se backups do SQLite (`05_DEVOPS_E_SEGURANCA.md` §E.4) precisam de um processo de expurgo correspondente quando um usuário é excluído.
**Por que importa:** é uma obrigação legal, não uma preferência de produto — lançar o app sem essa política definida é um risco de conformidade real, não apenas uma lacuna de documentação.
**Recomendação:** decisão de produto (não técnica) sobre a política em si, seguida de um patch em `03_CONTRATOS_API.md` (endpoint `DELETE /users/me` ou equivalente) e `02_SCHEMA_SQLITE.md` (estratégia de exclusão em cascata vs. anonimização) e `05_DEVOPS_E_SEGURANCA.md` (expurgo de backups).

### B.2. Estratégia de Teste Consolidada
**Problema:** a cobertura mínima de 80% é repetida em `00`, `05`, `06`, `07` e `08`, mas nenhum documento único descreve a **estratégia**: pirâmide de testes (proporção unitário/integração/E2E), ferramentas por camada (já mencionadas de forma espalhada: JUnit implícito no backend, JaCoCo, Jest + React Native Testing Library no frontend), convenção de nomenclatura de arquivos de teste, ou o que exatamente separa um teste "unitário" de um "de integração" neste projeto.
**Por que importa:** sem uma referência única, cada sprint corre o risco de reinterpretar "teste" de forma diferente, e a exigência de "usar os 4 cenários de `06` §H como casos de teste literais" (`07` §C.4) fica sem um padrão consistente de onde/como esses testes devem viver no código.
**Recomendação:** ver Seção C.

### B.3. Glossário de Domínio
**Problema:** `06_LOGICA_DE_PROGRESSAO.md` introduz termos técnicos centrais ao produto — dupla progressão, série contável/não-contável, `pesoReferencia`, PR de Carga vs. PR de Volume, estagnação — mas eles só existem definidos dentro daquele documento. Um agente de IA ou desenvolvedor trabalhando em `07`/`08` precisa já ter lido `06` inteiro para entender referências como "série contável" que aparecem soltas em outros documentos.
**Por que importa:** onboarding mais lento, risco de um agente de codificação futuro reimplementar a lógica com um entendimento sutilmente diferente do termo.
**Recomendação:** ver Seção C.

### B.4. Log de Decisões Arquiteturais (ADRs)
**Problema:** ao longo das 8+ rodadas deste projeto, decisões consequentes foram tomadas (SQLite em vez de PostgreSQL, sem Redis, método de dupla progressão em vez de alternativas, Expo managed workflow em vez de React Native bare, AsyncStorage em vez de SQLite local no cliente, Victory Native em vez de Recharts/outras libs de gráfico, NativeWind, `idb` cogitado e depois todo o IndexedDB descartado na migração). Os documentos `01`-`08` registram **o quê** foi decidido e frequentemente o porquê imediato, mas não há um log centralizado das alternativas consideradas e rejeitadas.
**Por que importa:** sem esse histórico, uma revisão futura pode reabrir uma decisão já tomada (ex: "por que não usamos Postgres?") sem contexto de que a alternativa já foi avaliada e descartada por um motivo específico.
**Recomendação:** ver Seção C.

### B.5. Outras Lacunas de Documentação Observadas
* **`README.md` do repositório é um placeholder genérico** (3 linhas, e ainda cita "Frontend: React" — desatualizado em relação à migração para React Native/Expo já registrada em `00`/`01`/`04`/`08`). Não é parte do escopo desta auditoria de `docs/`, mas vale nota: um `README.md` básico com setup local (clonar, `docker-compose up`, `npx expo start`) reduziria fricção de onboarding.
* **Catálogo consolidado de erros de negócio (`422`)** — `03_CONTRATOS_API.md` define dezenas de condições de `422 Unprocessable Entity` espalhadas por seção (sessão já em progresso, ciclo já ativo, dia com sessões executadas, exercício em uso, etc.), sem um índice único. Útil para o backend implementar um catálogo de `ErrorCode` consistente e para QA ter uma checklist de casos de erro a testar.
* **Política de versionamento/depreciação da API** (`/api/v1` → `/api/v2` no futuro) — não existe hoje, e não é urgente pré-lançamento, mas vale decidir a convenção (ex: quanto tempo uma versão antiga fica ativa) antes da primeira mudança que quebre compatibilidade.

---

## C) Documentos Sugeridos para `docs/`

Numeração continuando a partir de onde a série `00`-`09` realmente para (`09` já é este relatório):

| # | Nome Sugerido | Propósito | Prioridade |
| :--- | :--- | :--- | :--- |
| 10 | `10_ESTRATEGIA_DE_TESTES.md` | Consolidar a pirâmide de testes, ferramentas por camada (JUnit/JaCoCo no backend, Jest/RNTL no frontend), convenção de nomenclatura e a distinção unitário/integração/E2E — referenciado por `00`/`05`/`06`/`07`/`08` mas nunca centralizado. | **Alta** — a exigência de "usar os cenários de `06` §H como testes literais" já pressupõe uma convenção que ainda não existe por escrito. |
| 11 | `11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md` | Formalizar a política de exclusão/retenção de dados do usuário (LGPD), incluindo o que acontece com histórico de treino e backups ao excluir uma conta. | **Alta** — obrigação legal, não apenas conveniência de documentação (ver B.1). |
| 12 | `12_GLOSSARIO_DE_DOMINIO.md` | Centralizar os termos de negócio introduzidos em `06` (dupla progressão, série contável, PR de Carga/Volume, estagnação, `pesoReferencia`) para consulta rápida sem precisar reler o documento inteiro. | **Média** — valor de onboarding, não bloqueia nenhuma implementação. |
| 13 | `13_ADR_LOG.md` | Registro leve de Architecture Decision Records — uma entrada por decisão consequente (contexto, alternativas consideradas, decisão, consequências), começando pelas já tomadas nas rodadas 1-9. | **Média** — mais valioso à medida que o projeto envelhece e a equipe cresce; custo baixo de manter se iniciado agora. |
| 14 | `14_CATALOGO_DE_ERROS_DE_NEGOCIO.md` | Índice único de todas as condições de erro `422`/`403`/`404` de negócio já espalhadas por `03_CONTRATOS_API.md`, com código de erro sugerido por condição. | **Baixa** — conveniência de implementação/QA, não bloqueia nenhuma sprint listada em `07`/`08`. |

Nenhum destes documentos foi criado nesta rodada — cabe ao usuário decidir quais valem a pena, conforme a instrução deste relatório.
