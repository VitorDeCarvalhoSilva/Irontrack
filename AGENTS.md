# AGENTS.md — Constituição de Comportamento para Agentes de IA (IronTrack)

> Este documento é de **leitura obrigatória, na íntegra, antes de qualquer
> tarefa** — seja um agente Claude, Gemini, Cursor, Aider, Windsurf ou
> qualquer outra ferramenta de codificação assistida por IA. Ele não
> substitui os documentos de `docs/`; ele governa **como** você deve se
> comportar ao trabalhar com eles. Se uma instrução do usuário em uma
> conversa específica conflitar com este documento, a instrução explícita
> do usuário prevalece — mas sinalize o conflito antes de agir.

---

## 1. Leitura Obrigatória Antes de Iniciar Qualquer Tarefa

**Passo 0, antes de tudo:** leia `AGENT_LOG.md` (raiz do projeto) por
completo — é o log cumulativo de tudo que qualquer agente já fez neste
projeto (Seção 10 deste documento explica o formato e a obrigação de
atualizá-lo). Ele existe justamente para você não retrabalhar, não reverter
acidentalmente uma correção já aplicada, e saber o estado real do projeto
antes de confiar cegamente no conteúdo de `docs/`.

Depois do log, nenhuma tarefa de código ou documentação começa sem antes
ler, **nesta ordem**, os documentos relevantes de `docs/`. A ordem importa:
cada documento assume as decisões dos anteriores como já resolvidas — ler
fora de ordem produz suposições erradas.

1. `docs/00_PRD_IRONTRACK.md` — visão de produto, stack, roadmap de sprints. **Sempre leia, para qualquer tarefa.**
2. `docs/01_ARQUITETURA_E_PADROES.md` — regras de arquitetura, SOLID, nomenclatura, estrutura de pastas. **Sempre leia.**
3. `docs/02_SCHEMA_SQLITE.md` — schema relacional completo.
4. `docs/03_CONTRATOS_API.md` — todos os endpoints, request/response DTOs.
5. `docs/04_FRONTEND_UI_COMPONENTES.md` — arquitetura de UI, navegação, componentes, offline-first. Para qualquer tarefa que toque UI/estilo/telas, leia também `docs/15_DESIGN_SYSTEM_UI_UX.md` (paleta, tipografia, ícones, motion, posicionamento tela a tela) logo em seguida.
6. `docs/05_DEVOPS_E_SEGURANCA.md` — CI/CD, segredos, segurança operacional.
7. `docs/06_LOGICA_DE_PROGRESSAO.md` — motor de sobrecarga progressiva (regra de negócio mais crítica do produto).
8. `docs/07_ROADMAP_BACKEND.md` — roadmap de implementação backend, sprint a sprint.
9. `docs/08_ROADMAP_FRONTEND.md` — roadmap de implementação frontend, sprint a sprint.
10. Quaisquer documentos numerados posteriormente em `docs/` (relatórios de inspeção, glossário, ADRs, estratégia de testes) — trate como parte igualmente obrigatória da leitura assim que existirem.

Se a tarefa for restrita a uma camada específica (ex: só backend), ainda
assim leia **00 e 01 por completo**, e pelo menos a seção relevante de cada
um dos demais — nunca pule 00/01.

---

## 2. Resumo Rápido (não substitui a leitura completa)

* **Produto:** diário de bordo inteligente para **musculação clássica e
  hipertrofia** (carga, reps, séries, RPE, técnicas de intensificação).
  O domínio é **exclusivamente musculação** — nenhuma outra modalidade de
  treino existe em nenhuma camada, nem mesmo como tolerância de schema
  (histórico completo em `13_ADR_LOG.md`, ADR-016). Não reintroduza isso
  sem uma decisão Tipo B explícita.
* **Stack travada** (não introduza alternativas sem aprovação humana explícita — ver Seção 5):
  * Backend: Java/Spring Boot, **SQLite** (sem Postgres), JWT, Caffeine para rate-limit em memória. **Sem Redis, sem Kubernetes.**
  * Frontend: **React Native via Expo** (EAS Build/Submit/Update), React Navigation, `expo-linking` (deep links de verificação de e-mail/reset de senha, `04` §A.1), AsyncStorage + `expo-secure-store`, NetInfo, Victory Native, NativeWind, `react-native-reanimated`, `expo-notifications`, `react-hook-form` + `zod` (validação de formulário, `01` §3.4), `@expo/vector-icons` (MaterialCommunityIcons), `@expo-google-fonts/oswald` + `@expo-google-fonts/inter`, `expo-haptics` (sistema de design, `01` §3.6, `15_DESIGN_SYSTEM_UI_UX.md`, ADR-019). **Sem react-router-dom, sem Service Worker/PWA, sem IndexedDB, sem Recharts, sem Redux, sem Zustand, sem UI-kit pronto (React Native Paper/NativeBase/Tamagui)** — todo estado global é Context API puro (`01` §3.3), componentes-base são construídos manualmente (`15` §H).
  * DevOps: GitHub Actions, Docker (só backend), GitGuardian, EAS (frontend). Deploy em instância única de nuvem.
* **Motor de negócio central:** o algoritmo de dupla progressão em `06` — nunca reimplemente essa lógica de forma diferente em outro lugar; sempre referencie `06`.

---

## 3. Regra de Conduta: Tipo A vs. Tipo B

Toda mudança que você considerar fazer se encaixa em um destes dois tipos.
**Classifique antes de agir:**

* **Tipo A — Mecânico e inequívoco.** Corrija diretamente, sem perguntar:
  referência cruzada quebrada, nome inconsistente entre documentos/camadas,
  erro de digitação, resíduo de uma decisão já superada (ex: terminologia de
  uma stack antiga sobrevivendo depois de uma migração), formatação,
  aplicar um padrão já decidido a um lugar onde ele ainda não foi aplicado.
* **Tipo B — Decisão de produto/arquitetura em aberto.** Nunca decida
  sozinho. Isso inclui: introduzir qualquer nova dependência/biblioteca não
  listada na Seção 2 acima, mudar o escopo do produto, alterar uma regra de
  negócio já formalizada (ex: qualquer fórmula de `06`), qualquer operação
  destrutiva ou difícil de reverter (migrations que removem/renomeiam
  colunas já em uso, `git push --force`, `git reset --hard`, deletar
  branches, pular hooks/testes). Para Tipo B: **pare, explique o que
  encontrou, proponha 1-2 alternativas com recomendação, e espere resposta.**

Se estiver em dúvida entre A e B, trate como B.

---

## 4. Pipeline de Execução Obrigatório

Toda tarefa não-trivial (gerar/alterar um documento, implementar uma
funcionalidade, corrigir um bug) segue este pipeline, na ordem — não pule
etapas mesmo sob pressão de tempo:

1. **Ler o contexto** (Seção 1 e 2 deste documento, mais qualquer arquivo
   diretamente tocado pela tarefa).
2. **Identificar pré-requisitos.** Antes de implementar a tarefa pedida,
   verifique se ela depende de algo que ainda não existe ou está incompleto
   nos documentos/código atuais (esse projeto tem histórico de tarefas que
   revelaram lacunas de schema/contrato só ao serem especificadas em
   detalhe — ex: `06` exigiu um patch em `02`/`03` que não tinha sido
   antecipado antes de formalizar o algoritmo). Se encontrar um
   pré-requisito faltante que seja Tipo A, resolva-o primeiro,
   explicitamente, antes da tarefa principal. Se for Tipo B, pare e reporte.
3. **Implementar a tarefa principal** apontando sempre para o contrato/regra
   exata já existente (nome de tabela, endpoint, classe, seção de
   documento) — nunca inventar um nome novo para um conceito que já tem
   nome definido em outro lugar.
4. **Verificar consistência** — releia o que foi produzido e confirme: (a)
   nenhuma referência cruzada quebrada; (b) nomenclatura idêntica em todas
   as camadas tocadas (schema ↔ DTO ↔ tipo ↔ nome de classe); (c) nada da
   lista de "Fora de Escopo" do documento relevante foi implementado.
5. **Reportar em resumo curto** o que foi feito, o que ficou de fora
   (referenciando por que), e qualquer achado Tipo B não resolvido.
6. **Registrar no `AGENT_LOG.md`** (Seção 10) uma linha resumindo a tarefa
   — este passo não é opcional, mesmo para tarefas pequenas.

Isso vale tanto para gerar documentação em `docs/` quanto para escrever
código real depois que a fase de implementação começar.

---

## 5. Permissões e Restrições

### Pode fazer sem pedir aprovação:
* Correções Tipo A (Seção 3).
* Criar arquivos dentro do escopo já pedido pelo usuário na tarefa atual.
* Rodar testes, linters, formatters.
* Adicionar comentários/notas explicativas em código ou documentação
  quando a ausência deles gerar ambiguidade real (não comentários
  redundantes com o próprio código).

### Precisa de aprovação humana explícita antes de fazer:
* Qualquer item da lista "Fora de Escopo" de `06`/`07`/`08` ou de qualquer
  relatório de inspeção.
* Introduzir uma biblioteca/dependência/serviço de infraestrutura não
  listado na Seção 2 (ex: qualquer cache de servidor, qualquer banco além
  de SQLite, qualquer biblioteca de UI/gráficos/animação diferente da já
  decidida).
* Qualquer operação destrutiva ou de difícil reversão em git (force-push,
  reset --hard, deletar branch, amend de commit já publicado) ou em banco
  de dados (migration que remove dados/colunas em uso).
* Pular hooks de commit, desabilitar testes, ignorar falhas de CI para
  "destravar" um merge.
* Mudar uma regra de negócio já formalizada em `06` (o algoritmo de
  progressão é a especificação final, não um rascunho).
* Publicar/submeter builds para App Store/Play Store, ou fazer deploy em
  produção.

---

## 6. Padrões de Código (referência rápida — fonte de verdade é `01` §5)

| Escopo | Padrão |
| :--- | :--- |
| Java: classes/interfaces/records/enums | `PascalCase` |
| Java: métodos/variáveis/parâmetros | `camelCase` |
| Java: constantes | `UPPER_SNAKE_CASE` |
| React Native: componentes/telas/contexts | `PascalCase`, sufixo `Screen` para telas |
| React Native: hooks | `camelCase`, prefixo `use` |
| React Native: variáveis/funções/props | `camelCase` |
| SQLite: tabelas/colunas | `snake_case` (tabelas no plural) |
| JSON (API) | `camelCase` |
| Commits | Conventional Commits (`feat`, `fix`, `docs`, `refactor`, `test`, `chore` — `01` §5.2) |

Princípios não-negociáveis (`01` §2.2-2.3): DTO nunca é a Entity exposta
diretamente; injeção de dependência via construtor, nunca `@Autowired` em
campo; controllers nunca contêm lógica de negócio; SRP e OCP para motores
de cálculo (o motor de progressão em `06` é o exemplo canônico de OCP —
extensível por estratégia, não por `if/else` em cadeia).

### 6.1. TDD Obrigatório para Código de Regra de Negócio

**Toda função/método que implemente uma regra de negócio — qualquer classe
em `services/` no backend, qualquer hook/reducer/utilitário puro no
frontend que decida comportamento — deve ser desenvolvida em TDD
(Test-Driven Development):** escreva o teste que expressa o comportamento
esperado **antes** de qualquer linha de implementação (Red), implemente o
mínimo necessário para o teste passar (Green), depois refatore mantendo os
testes verdes (Refactor). Nunca implemente a lógica primeiro e escreva o
teste depois "para garantir cobertura" — isso não é TDD e não cumpre esta
regra, mesmo que o número de cobertura final seja idêntico.

Isso é **inegociável** para: `ProgressiveOverloadService`,
`SetCountabilityRules` (`06_LOGICA_DE_PROGRESSAO.md`), e qualquer serviço de
cálculo/decisão equivalente que vier a existir (motores de estagnação, de
PR, de agregação de métricas). Controllers finos que só delegam para
services, DTOs/entities sem lógica, e componentes puramente visuais
(`Dumb Components`) são a exceção documentada — para esses, teste após a
implementação é aceitável, mas ainda obrigatório antes do PR ser aberto.

A pirâmide de testes, as ferramentas por camada, a convenção de
nomenclatura de arquivos de teste, e os casos de teste literais obrigatórios
(os 4 cenários numéricos de `06` §H) estão consolidados em
[`docs/10_ESTRATEGIA_DE_TESTES.md`](docs/10_ESTRATEGIA_DE_TESTES.md) — leitura
obrigatória antes de escrever o primeiro teste deste projeto.

Nenhum PR que introduza uma classe de regra de negócio nova deve ser aberto
sem os testes correspondentes já existentes no mesmo PR — "adiciono o teste
depois" não é uma opção válida para este tipo de código.

---

## 7. Anti-Padrões Proibidos (lições já aprendidas neste projeto)

Estes já aconteceram ou quase aconteceram durante o planejamento deste
projeto — trate como erros conhecidos, não hipotéticos:

* **Inventar um contrato de API sem checar `03` primeiro.** Se o endpoint
  que você precisa não existe, isso é um pré-requisito (Seção 4, passo 2),
  não uma licença para improvisar um formato diferente.
* **Deixar "a definir"/"TBD"/uma alternativa em aberto** em qualquer
  especificação nova. Toda regra de negócio documentada neste projeto até
  agora foi fechada com um valor concreto, mesmo quando a escolha era
  arbitrária (ex: arredondamento de carga para múltiplos de 1,25kg,
  bloqueio de 15 minutos após 5 falhas) — a ausência de ambiguidade é um
  requisito de qualidade deste projeto, não um nice-to-have.
* **Reintroduzir tecnologia fora da stack travada** (Seção 2) só porque é
  familiar ou "mais simples" — cada exclusão (Redis, Postgres, Kubernetes,
  react-router-dom, Service Worker) foi uma decisão deliberada e justificada
  em algum documento; não a reverta silenciosamente.
* **Deixar nomes inconsistentes entre camadas** — um campo que se chama
  `target_reps_max` no schema, `targetRepsMax` no JSON, e algo diferente em
  um tipo TypeScript é um bug de documentação/código, não um detalhe menor.
* **Reescrever um documento inteiro quando só uma correção cirúrgica é
  necessária.** Preserve o que já está correto; edite apenas o que precisa
  mudar.
* **Silenciar uma migração/decisão de arquitetura anterior** (ex: sobrar
  menção a PWA depois de uma migração para nativo) — sempre grep/procure
  ativamente por resíduos da abordagem anterior ao concluir uma mudança de
  direção.
* **Escrever a implementação antes do teste** em código de regra de negócio
  (Seção 6.1) — mesmo que a cobertura final seja 100%, isso não é TDD e viola
  o mandato deste documento.

---

## 8. Gestão de Prompts de Trabalho Descartáveis

Este projeto usa o padrão de escrever um `prompt.md` na raiz do repositório
como instrução de trabalho de uma tarefa específica (não um documento
arquitetural permanente). Se você seguir esse padrão:
* Sempre inclua a Seção 1 deste documento (leitura obrigatória) no início
  do `prompt.md`, mesmo que resumida.
* Aplique patches cirúrgicos, nunca reescritas desnecessárias.
* Ao final, apague o `prompt.md` — a menos que a própria tarefa seja, ela
  mesma, produzir um relatório/registro (ex: uma auditoria de projeto),
  caso em que o prompt pode ser arquivado deliberadamente em vez de
  apagado, a critério de quem o escreveu.

---

## 9. Checklist Final Antes de Considerar Qualquer Tarefa Concluída

- [ ] Toda referência cruzada nova/alterada aponta para uma seção que
      realmente existe, com o conteúdo esperado.
- [ ] Nenhum nome de campo/endpoint/classe/tabela diverge entre os
      documentos ou camadas tocados pela tarefa.
- [ ] Nada da(s) lista(s) de "Fora de Escopo" relevante(s) foi implementado.
- [ ] Nenhuma tecnologia fora da Seção 2 (Stack Travada) foi introduzida
      sem aprovação humana explícita.
- [ ] Se a tarefa introduziu código de regra de negócio, os testes foram
      escritos **antes** da implementação (Seção 6.1/TDD), não depois.
- [ ] Nenhum achado Tipo B foi resolvido unilateralmente — todos foram
      reportados ao usuário.
- [ ] O resumo final da tarefa é curto, direto, e menciona explicitamente
      qualquer coisa deixada de fora e por quê.
- [ ] A linha correspondente foi adicionada ao `AGENT_LOG.md` (Seção 10).

---

## 10. Log Cumulativo de Atividades (`AGENT_LOG.md`)

Todo agente que executa qualquer tarefa neste projeto — documentação,
código, correção, migração — **deve registrar uma linha** em
`AGENT_LOG.md` (raiz do projeto) ao final da tarefa. É o histórico
compartilhado entre agentes de ferramentas/sessões diferentes — pense nele
como "o git dos agentes": não substitui o controle de versão real, mas
registra em linguagem natural o que foi feito, por quê, e o estado
resultante, para que o próximo agente (ou o mesmo agente em uma sessão
futura) não precise reconstruir esse contexto do zero nem repita trabalho.

### 10.1. Regras do log
* **Somente adiciona, nunca remove ou reescreve linhas antigas.** É um log
  append-only — se uma entrada antiga se revelar errada, adicione uma nova
  linha corrigindo/atualizando, não edite a linha original.
* **Uma linha por tarefa/sessão de trabalho**, não uma linha por edição
  pequena dentro da mesma tarefa — mantenha o log enxuto.
* **Leia o log inteiro antes de começar qualquer tarefa** (Seção 1, Passo 0)
  — ele pode revelar que algo que você acha que precisa fazer já foi feito,
  ou que uma decisão que você ia tomar já foi tomada (e por quê).
* Novas linhas sempre vão **no final da tabela** (ordem cronológica
  crescente).

### 10.2. Formato (tabela Markdown)

| Data/Hora | Área | Tipo | Resumo | Arquivos Afetados |
| :--- | :--- | :--- | :--- | :--- |
| `AAAA-MM-DD HH:MM` (hora aproximada se não souber a exata) | `Backend` / `Frontend` / `Docs` / `DevOps` / `Schema` / `Multi` | `Criação` / `Correção` / `Migração` / `Decisão` / `Auditoria` | Uma frase objetiva descrevendo o que mudou e por quê (não repita o óbvio do nome do arquivo) | Lista curta dos arquivos tocados |

Use `Multi` na coluna Área quando a tarefa tocou mais de uma camada (ex:
uma migração que mexeu em schema, contrato e frontend ao mesmo tempo) em
vez de criar várias linhas para a mesma tarefa.
