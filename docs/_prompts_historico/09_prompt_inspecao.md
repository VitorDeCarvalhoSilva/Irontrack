# PROMPT — Inspetor Geral de Projeto (IronTrack)

> Você está atuando como **Inspetor de Projeto** — um papel diferente dos
> prompts anteriores. Sua função não é implementar uma funcionalidade nova,
> é **auditar tudo que existe** em `docs/` (frutos de 8 rodadas anteriores
> de planejamento) em busca de falhas, pontas soltas, inconsistências
> residuais, e lacunas de documentação — e então **corrigir o que for
> mecânico e inequívoco**, e **reportar (sem decidir sozinho) o que exigir
> julgamento de produto/arquitetura**.

---

## 0. Contexto obrigatório

Leia **integralmente**, nesta ordem, todos os documentos existentes:
1. `docs/00_PRD_IRONTRACK.md`
2. `docs/01_ARQUITETURA_E_PADROES.md`
3. `docs/02_SCHEMA_SQLITE.md`
4. `docs/03_CONTRATOS_API.md`
5. `docs/04_FRONTEND_UI_COMPONENTES.md`
6. `docs/05_DEVOPS_E_SEGURANCA.md`
7. `docs/06_LOGICA_DE_PROGRESSAO.md`
8. `docs/07_ROADMAP_BACKEND.md`
9. `docs/08_ROADMAP_FRONTEND.md`

**Produto:** IronTrack — app nativo (React Native/Expo) para musculação
clássica e hipertrofia, com backend Java/Spring Boot + SQLite. O frontend
migrou recentemente de PWA para app nativo — **preste atenção redobrada a
resíduos da arquitetura antiga** que possam ter sobrevivido à migração.

## 1. Regra de conduta (leia com atenção — isto governa todo o resto)

Você vai encontrar dois tipos de problema. Trate-os de forma **diferente**:

* **Tipo A — Mecânico e inequívoco** (corrija diretamente, sem perguntar):
  referência cruzada quebrada (`§X.Y` que não existe mais ou mudou de
  número), nome de campo/endpoint/classe inconsistente entre documentos
  (ex: um documento diz `sessionExerciseId`, outro ainda diz `exerciseId`
  no mesmo contexto), terminologia residual da arquitetura antiga (PWA/web)
  que sobrou após a migração para nativo, erro de digitação, contradição
  factual clara entre dois documentos sobre a mesma regra já decidida.
* **Tipo B — Decisão de produto/arquitetura em aberto** (NÃO decida
  sozinho, apenas relate com uma recomendação): qualquer lacuna que exija
  escolher entre alternativas de negócio, introduzir uma nova dependência
  de infraestrutura, ou mudar o escopo do produto. Se você não tem certeza
  se algo é Tipo A ou Tipo B, trate como Tipo B.

Essa distinção é a mesma usada em todas as rodadas anteriores deste
projeto — correções mecânicas são baratas e não exigem aprovação; decisões
de produto exigem o usuário humano no loop.

---

## 2. Checklist de Inspeção (percorra cada item, em cada documento relevante)

### 2.1. Resíduos da migração PWA → React Native
Procure especificamente por: `react-router-dom`, `Service Worker`,
`vite-plugin-pwa`, `Workbox`, `IndexedDB`, `idb`, `Dexie`, `Recharts`,
`manifest.json`, `localStorage`, `Nginx` (no contexto de frontend),
`VAPID`, sufixo `Page` em nomes de componente (deveria ser `Screen`),
`inputMode`/`<input>` HTML. Qualquer ocorrência remanescente em `01`, `04`,
`05`, `08` (ou referenciada por outro documento) é **Tipo A** — corrija para
o equivalente já decidido na migração (React Navigation, AsyncStorage,
NetInfo, Victory Native, NativeWind, EAS, `Screen`, `TextInput`).

### 2.2. Integridade de referências cruzadas
Para cada menção a `§X.Y` ou a um nome de seção em outro documento,
confirme que a seção referenciada existe e tem o conteúdo esperado. Preste
atenção especial a documentos que foram alvo de múltiplos patches ao longo
do projeto (`02`, `03`, `05` receberam adições em pelo menos 3 rodadas
diferentes) — é onde mais provavelmente sobrou uma numeração defasada.

### 2.3. Consistência de nomenclatura entre documentos
Cruze nomes de: tabelas/colunas (`02`) ↔ campos de DTO em `camelCase` (`03`)
↔ tipos TypeScript (`04`) ↔ nomes de classe sugeridos (`07`/`08`). Qualquer
divergência de nome para o mesmo conceito é **Tipo A**.

### 2.4. Coerência do roadmap de sprints (`00` vs. `07`/`08`)
`00_PRD_IRONTRACK.md` §4 descreve sprints com escopo que foi **expandido**
em rodadas posteriores (ciclo de vida completo de auth, perfil, métricas de
dashboard, alertas de estagnação, notificações push — todos adicionados
durante a preparação de `07`/`08`, não estavam no `00` original). Verifique
se os critérios de aceite de `00` §4.3-§4.7 ainda refletem com precisão o
que `07`/`08` de fato planejam entregar em cada sprint. Divergências de
**escopo já decidido** (algo que `07`/`08` implementam mas `00` não lista)
são **Tipo A** — atualize `00` para refletir a realidade já decidida.
Divergências que revelem uma **lacuna de produto real** (algo que nenhum
documento cobre) são **Tipo B**.

### 2.5. Declarações de "Fora de Escopo" — ainda válidas e não-contraditórias?
`06`, `07` e `08` têm seções explícitas de itens fora de escopo (upload de
foto de perfil, exportação PDF/CSV, templates de ciclo, wearables, modo
compartilhado, etc.). Confirme que nenhum outro documento implementa ou
pressupõe silenciosamente algo dessa lista. Se encontrar uma dessas listas
desatualizada (um item que na verdade já foi implementado em outra rodada e
deveria ter saído da lista), corrija — **Tipo A**.

### 2.6. Segurança e conformidade
* Verifique aderência ao "Zero Hardcoded Secrets" (`01` §6.1) em todos os
  exemplos de código de `05`/`07` — nenhum valor de segredo real, apenas
  placeholders.
* **Dado pessoal e LGPD:** o app coleta nome, e-mail e um histórico
  detalhado de desempenho físico do usuário. Nenhum documento atual
  descreve uma política de retenção/exclusão de dados (ex: o que acontece
  com os dados de um usuário que pede exclusão de conta — direito já
  previsto na LGPD brasileira). Isso é **Tipo B** — relate como lacuna,
  não invente a política sozinho.

### 2.7. Cobertura de teste — estratégia consolidada existe?
A cobertura mínima de 80% aparece repetida em `00`, `05`, `06`, `07`, `08`,
mas nenhum documento único descreve a **estratégia** de teste (pirâmide de
testes, ferramentas por camada, convenção de nomenclatura de arquivos de
teste, o que é unitário vs. integração vs. E2E). Avalie se isso merece um
documento dedicado (ver Seção 3 abaixo) — **Tipo B**, apenas relate.

### 2.8. Glossário de domínio
`06_LOGICA_DE_PROGRESSAO.md` introduz termos técnicos específicos (dupla
progressão, série contável, `pesoReferencia`, PR de carga vs. PR de volume,
estagnação) que são centrais ao produto mas só existem espalhados dentro
daquele documento. Avalie se um glossário centralizado ajudaria
onboarding de novos desenvolvedores/agentes de IA — **Tipo B**, apenas relate.

### 2.9. Registro de decisões arquiteturais (ADRs)
Ao longo das rodadas deste projeto, decisões consequentes foram tomadas
(SQLite em vez de Postgres, sem Redis, método de dupla progressão, Expo em
vez de React Native bare, AsyncStorage em vez de SQLite local, etc.) — os
documentos `01`-`08` registram **o quê** foi decidido, mas nem sempre **por
quê** (as alternativas consideradas e o motivo da rejeição, quando existiu
uma escolha real). Avalie se um log leve de ADRs (Architecture Decision
Records) seria valioso para manutenção futura — **Tipo B**, apenas relate.

### 2.10. Outras lacunas de documentação
Avalie livremente (além dos itens 2.6-2.9 já sugeridos) se falta algum
documento que ajudaria uma IA de codificação futura a trabalhar com menos
ambiguidade — por exemplo: um guia de onboarding/setup de ambiente de
desenvolvimento local (`README.md` do repositório está genérico demais?),
um catálogo consolidado de todos os erros de negócio (`422`) espalhados
pelas Seções de `03`, ou uma política de versionamento/depreciação da API
(`/api/v1` → `/api/v2` no futuro). Relate como **Tipo B** cada sugestão,
com uma frase justificando o valor prático.

---

## 3. Entregável

Ao final da inspeção:

1. **Aplique diretamente** todas as correções Tipo A encontradas, nos
   próprios documentos onde o problema está (edições cirúrgicas, não
   reescritas).
2. **Crie** um novo documento `docs/09_RELATORIO_DE_INSPECAO_INICIAL.md`
   com três seções:
   * **A) Correções Aplicadas** — lista objetiva de cada correção Tipo A
     feita, com o arquivo e uma frase descrevendo o que foi corrigido.
   * **B) Pontos em Aberto (Decisões Necessárias)** — lista de cada achado
     Tipo B, com: descrição do problema/lacuna, por que importa, e uma
     recomendação clara (não uma decisão tomada) para o usuário avaliar.
   * **C) Documentos Sugeridos para a Pasta `docs/`** — lista de novos
     documentos candidatos (ex: `09_ESTRATEGIA_DE_TESTES.md`,
     `10_GLOSSARIO_DE_DOMINIO.md`, `11_ADR_LOG.md`, etc. — numere a partir
     de onde a série realmente parar, sem colidir com este relatório), cada
     um com: nome sugerido, uma frase de propósito, e prioridade
     (Alta/Média/Baixa) na sua avaliação.
3. Não implemente nenhum documento novo da lista C — apenas sugira. Cabe ao
   usuário decidir quais valem a pena.
4. Ao final, **não apague este `prompt.md`** — diferente das rodadas
   anteriores, mantenha-o no repositório como registro do prompt que gerou
   a inspeção (mova-o para `docs/_prompts_historico/09_prompt_inspecao.md`
   se preferir manter a raiz do projeto limpa, criando essa pasta se não
   existir).
