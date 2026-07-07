# 00_PRD_IRONTRACK.md - Product Requirements Document (PRD)

## 1. Visão Geral do Produto

O **Irontrack** é uma aplicação voltada para praticantes de musculação clássica e hipertrofia que desejam acompanhar seu desempenho real e planejar sua evolução de forma estratégica. Em vez de apenas "dar um check" nos exercícios do dia, o usuário utilizará o software como um diário de bordo inteligente.

A aplicação permitirá o cadastro de diferentes ciclos de treino (como divisões ABC, push/pull/legs) e o registro detalhado de cada sessão. O grande diferencial do sistema é o foco em métricas: o usuário poderá registrar a carga utilizada, o número de repetições alcançadas e as técnicas de intensificação aplicadas em cada série (falha, drop-set, rest-pause, pausa, negativa forçada). Com isso, o software constrói um histórico organizado que ajuda o praticante a aplicar o princípio da sobrecarga progressiva, seja no aumento contínuo de peso em levantamentos tradicionais, seja no avanço de repetições dentro da faixa-alvo definida para cada exercício.

## 2. Stack Tecnológico e Infraestrutura

O aplicativo será desenvolvido com as seguintes tecnologias principais:

- **Backend:** Spring Boot (Java) para construção de uma API RESTful robusta.
- **Frontend:** React Native (via Expo) para um aplicativo móvel nativo real (iOS e Android), com suporte offline via armazenamento local e sincronização em segundo plano, distribuído pela Play Store e App Store.
- **Banco de Dados:** SQLite para armazenamento leve, relacional e de fácil configuração inicial.
- **DevOps & Deploy:** Pipelines de CI/CD estruturadas (podendo utilizar GitLab, Jenkins ou Harness) com escaneamento de segurança automatizado via GitGuardian antes das entregas. O deploy final poderá ser provisionado em instâncias na nuvem, como a Oracle Cloud.

## 3. Arquitetura Documental do Projeto

Para que a IA de desenvolvimento atue sem erros, o escopo do projeto está dividido em 8 arquivos estritos. Este documento (00) governa os demais:

1.  **`00_PRD_IRONTRACK.md`**: Este documento (Visão geral e roadmap).
2.  **`01_ARQUITETURA_E_PADROES.md`**: Regras de código, SOLID e estrutura de pastas.
3.  **`02_SCHEMA_SQLITE.md`**: Modelagem de dados, diagramas ER e relacionamentos.
4.  **`03_CONTRATOS_API.md`**: Definição exata de endpoints, requests e responses do Spring Boot.
5.  **`04_FRONTEND_UI_COMPONENTES.md`**: Árvore de componentes React, estado e roteamento.
6.  **`05_DEVOPS_E_SEGURANCA.md`**: Regras de pipeline, Docker, segurança de credenciais.
7.  **`06_LOGICA_DE_PROGRESSAO.md`**: Motor de recomendação de sobrecarga progressiva.
8.  **`07_ROADMAP_BACKEND.md` & `08_ROADMAP_FRONTEND.md`**: Tarefas de codificação passo a passo.

---

## 4. Planejamento de Produto (Roadmap Ágil)

O desenvolvimento será guiado pelo seguinte planejamento estruturado em épicos e sprints[cite: 1].

### 4.1. ÉPICOS

- **EP-01 (Autenticação e Perfil do Usuário):** Cadastro, login, perfil e preferências do usuário[cite: 1].
- **EP-02 (Gestão de Ciclos de Treino):** Criação e gerenciamento de divisões e programas de treino[cite: 1].
- **EP-03 (Registro de Sessões de Treino):** Diário de bordo de cada sessão realizada[cite: 1].
- **EP-04 (Métricas e Histórico):** Visualização de evolução, gráficos e comparativos[cite: 1].
- **EP-05 (Sobrecarga Progressiva):** Sugestões e alertas baseados no histórico para superar marcas[cite: 1].
- **EP-06 (Infraestrutura e Fundação Técnica):** Setup inicial, CI/CD, banco de dados, autenticação técnica[cite: 1].

### 4.2. SPRINT 0 — Fundação Técnica (2 semanas)

**Objetivo:** Preparar toda a infraestrutura técnica, padrões de projeto e ambiente de desenvolvimento antes do desenvolvimento de features[cite: 1].

- **Configuração do ambiente de desenvolvimento (EP-06):** Criar repositório Git (main, develop, feature/\*)[cite: 1], documentar README e `.env.example`[cite: 1], configurar Docker Compose (API + Banco)[cite: 1]* e estabelecer linters/formatters (ESLint/Prettier) com pre-commit hooks[cite: 1].
- **Configuração de CI/CD (EP-06):** Criar pipeline de CI executando testes e lint a cada push[cite: 1], configurar pipeline de CD para staging automático após merge em `develop`[cite: 1] e definir aprovação manual para produção[cite: 1].

  \* Não há serviço de cache de infraestrutura (Redis ou equivalente) nesta arquitetura. Qualquer cache é *client-side* — o wrapper customizado de cache local no frontend (`01_ARQUITETURA_E_PADROES.md` §3.3) — nunca um serviço adicional no `docker-compose.yml`.
- **Modelagem e criação do banco de dados (EP-06):** Criar diagrama ER cobrindo entidades como `users`, `training_cycles`, `training_days`, `exercises`, `exercise_library`, `training_sessions`, `session_exercises` e `exercise_sets`[cite: 1]. Gerar migrations e seeds de dados[cite: 1].

### 4.3. SPRINT 1 — Autenticação e Perfil (2 semanas)

**Objetivo:** Usuário consegue se cadastrar, fazer login seguro e configurar seu perfil básico[cite: 1].

- **Cadastro de novo usuário (EP-01):** Endpoints e UI para nome, e-mail único e senha forte, com fluxo de verificação por e-mail[cite: 1].
- **Login e logout (EP-01):** Implementação baseada em JWT (access e refresh token)[cite: 1], bloqueio temporário de 15 minutos após 5 falhas consecutivas[cite: 1] e armazenamento seguro no frontend[cite: 1].
- **Recuperação de senha (EP-01):** Envio de link de redefinição com expiração de 1 hora via e-mail[cite: 1].
- **Perfil do usuário (EP-01):** Edição de nome e e-mail (troca de e-mail exige nova verificação) e troca de senha, exigindo confirmações para alterações sensíveis[cite: 1]. Upload de foto de perfil está fora de escopo do MVP — depende de decisão de infraestrutura de object storage ainda não tomada (`07_ROADMAP_BACKEND.md` §E).

### 4.4. SPRINT 2 — Gestão de Ciclos de Treino (2 semanas)

**Objetivo:** Usuário consegue criar e gerenciar seus programas/divisões de treino[cite: 1].

- **Criar ciclo de treino (EP-02):** Criação de ciclos nomeados (ex: ABC)[cite: 1], com dias específicos (ex: Treino A) e possibilidade de reordenação[cite: 1].
- **Biblioteca de exercícios (EP-02):** Biblioteca padrão com +50 exercícios base classificados por grupo muscular e tipo[cite: 1], permitindo que o usuário cadastre movimentos personalizados[cite: 1].
- **Compor dia de treino (EP-02):** Selecionar exercícios para o dia[cite: 1], definindo séries, repetições alvo, observações e ordem de execução[cite: 1].

### 4.5. SPRINT 3 — Registro de Sessões (2 semanas)

**Objetivo:** Usuário consegue iniciar, registrar e finalizar uma sessão de treino completa[cite: 1].

- **Iniciar sessão (EP-03):** Usuário seleciona o dia a treinar[cite: 1] e o sistema carrega o diário exibindo as metas e o último treino realizado como referência[cite: 1].
- **Registrar séries (EP-03):** Input de peso, repetições realizadas e técnicas aplicadas (ex: falha, rest-pause)[cite: 1]. O sistema deve salvar os dados automaticamente (auto-save) e fornecer um timer de descanso configurável[cite: 1].
- **Finalizar sessão e Histórico (EP-03):** Geração de resumo (duração e volume total movimentado)[cite: 1], atualizando o status da sessão[cite: 1]. Acesso a listagem com filtros para consultar todas as sessões anteriores[cite: 1].

### 4.6. SPRINT 4 — Métricas e Sobrecarga Progressiva (2 semanas)

**Objetivo:** Usuário visualiza sua evolução e recebe insights para aplicar sobrecarga progressiva[cite: 1].

- **Dashboard de evolução (EP-04):** Gráficos de carga máxima e volume ao longo do tempo por exercício[cite: 1], recorde pessoal (PR) de carga e de volume por exercício, além de métricas gerais (frequência, volume semanal/mensal e grupos musculares)[cite: 1].
- **Sugestão de carga (EP-05):** Motor que analisa o treino anterior e sugere aumento de carga (se o teto de repetições foi atingido) ou a manutenção com meta de mais repetições[cite: 1].
- **Alertas de estagnação (EP-05):** Detecção de exercícios sem progressão por mais de 3 semanas para alertar o usuário[cite: 1].

### 4.7. SPRINT 5 — Polimento, Notificações e Offline-First Nativo (2 semanas)

**Objetivo:** Melhorar a experiência do usuário com notificações, modo offline e refinamentos de UX[cite: 1].

- **Modo offline (EP-03):** Armazenamento local (AsyncStorage) e detecção de reconexão (NetInfo) para permitir registros sem internet[cite: 1], sincronizando os dados (com idempotência) ao reconectar[cite: 1].
- **Notificações (EP-01):** Configuração de notificações push nativas (Expo Push Service / `expo-notifications`) para lembretes de treino nos dias e horários selecionados pelo usuário[cite: 1].

### 4.8. Definition of Done (DoD)

Para que qualquer tarefa ou sprint seja considerado finalizado, as seguintes condições devem ser atendidas[cite: 1]:

- Código segue padrões definidos[cite: 1].
- Testes unitários com cobertura mínima de 80%[cite: 1].
- Testes de integração em novos endpoints[cite: 1].
- Code review aprovado[cite: 1].
- PR mergeado em `develop` sem conflitos[cite: 1] e deploy em staging[cite: 1].
- Critérios validados, sem erros críticos, com OpenAPI/Swagger atualizados[cite: 1].
