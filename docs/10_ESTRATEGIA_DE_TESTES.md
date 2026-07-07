# 10_ESTRATEGIA_DE_TESTES.md - Estratégia de Testes e TDD

Este documento consolida, em um único lugar, a estratégia de testes do **IronTrack** — hoje espalhada em menções repetidas de "cobertura mínima de 80%" em `00`, `05`, `06`, `07` e `08` sem uma referência única de **como** chegar lá. Ele é a fonte de verdade para: a pirâmide de testes, as ferramentas por camada, a convenção de nomenclatura, a distinção unitário/integração/E2E, e a obrigatoriedade de **TDD (Test-Driven Development)** para código de regra de negócio, conforme mandato de [`AGENTS.md`](../AGENTS.md).

---

## A) Filosofia: TDD Obrigatório para Regra de Negócio

Para **toda** função/método que implemente uma regra de negócio (qualquer classe em `services/` no backend; qualquer hook/reducer/utilitário puro no frontend que decida um comportamento, não apenas renderize), o ciclo de desenvolvimento é:

1. **Red** — escreva o teste que expressa o comportamento esperado, antes de qualquer linha de implementação. O teste deve falhar (a função ainda não existe ou não faz o que o teste espera).
2. **Green** — escreva o mínimo de código necessário para o teste passar. Nada além do necessário.
3. **Refactor** — limpe a implementação (nomes, duplicação, clareza) mantendo os testes verdes.

Código de regra de negócio **não deve ser mesclado sem os testes correspondentes já existentes** — não é uma etapa de "depois eu escrevo o teste". Isso vale com força total para `ProgressiveOverloadService` e `SetCountabilityRules` (`06_LOGICA_DE_PROGRESSAO.md`), por serem a lógica mais crítica e mais barata de testar (sem I/O) do produto.

**Exceções deliberadas ao TDD estrito** (onde escrever o teste primeiro tem pouco valor prático): controllers finos que só delegam para services (cobertos por teste de integração, não TDD unitário linha a linha); DTOs/entities sem lógica; componentes puramente visuais (`Dumb Components`) sem branching de comportamento — para esses, teste após a implementação é aceitável, mas ainda obrigatório antes do PR.

---

## B) Pirâmide de Testes

```text
        /\
       /  \        E2E (poucos, fluxos críticos)
      /----\
     /      \      Integração (moderado)
    /--------\
   /          \    Unitário (a maioria)
  /------------\
```

* **Unitário (maioria dos testes):** funções/classes isoladas, sem banco de dados, sem rede, sem simulador. Dependências externas mockadas.
* **Integração (quantidade moderada):** exercita a integração real entre camadas — controller + service + repositório contra um banco SQLite real (arquivo temporário/em memória), ou uma tela React Native completa com `services/` mockado via Jest.
* **E2E (poucos, só os fluxos mais críticos):** o aplicativo completo (build de teste) executando um fluxo do usuário de ponta a ponta contra um backend real de teste. Reservado para os fluxos que, se quebrados, inviabilizam o uso do app: registro → login → criar ciclo → iniciar sessão → registrar série → finalizar sessão.

---

## C) Ferramentas por Camada

| Camada | Unitário | Integração | E2E |
| :--- | :--- | :--- | :--- |
| Backend (Spring Boot) | JUnit 5 + Mockito | `@SpringBootTest`/`@DataJpaTest` com SQLite em arquivo temporário (não use H2 — o dialeto SQLite tem particularidades, ex: `PRAGMA foreign_keys`, que H2 não replica fielmente) + MockMvc/`@AutoConfigureMockMvc` para os controllers | — (E2E cobre o app inteiro, não o backend isolado) |
| Frontend (React Native/Expo) | Jest + React Native Testing Library | Jest + RNTL com `services/` mockado (`msw` ou mocks manuais de `apiClient`) | **Maestro** (fluxos YAML simples, roda contra builds `eas build --profile preview`) |
| Cobertura | JaCoCo (backend), `--coverage` do Jest (frontend) | — | — |

**Por que Maestro e não Detox para E2E:** Maestro usa fluxos declarativos em YAML (sem compilar um app de teste separado), tem manutenção mais barata para um time pequeno, e já é amplamente usado em projetos Expo/EAS — decisão registrada em [`13_ADR_LOG.md`](./13_ADR_LOG.md).

---

## D) Convenção de Nomenclatura de Arquivos de Teste

| Linguagem/Contexto | Convenção | Exemplo |
| :--- | :--- | :--- |
| Java (unitário) | `<Classe>Test.java`, mesmo pacote do alvo em `src/test/java` | `ProgressiveOverloadServiceTest.java` |
| Java (integração) | `<Classe>IT.java` (sufixo `IT`, para diferenciar do `mvn test` padrão e ser pego por um goal de integração separado) | `SessionsControllerIT.java` |
| TypeScript (unitário/componente) | `<Nome>.test.tsx`/`.test.ts`, ao lado do arquivo testado | `SetRow.test.tsx`, `useCountability.test.ts` |
| Maestro (E2E) | `<fluxo>.yaml` em `e2e/flows/` | `registro-login-primeiro-treino.yaml` |

Nome de teste (dentro do arquivo): descreva o comportamento, não a implementação — `deveSugerirAumentoDeCargaQuandoTetoAtingidoEmTodasAsSeries()`, não `testCase3()`.

---

## E) O Que é Unitário vs. Integração vs. E2E Neste Projeto (exemplos concretos)

* **Unitário:** `SetCountabilityRules.isCountable(...)`; `ProgressiveOverloadService.calculateSuggestion(...)` com repositórios mockados retornando os cenários de `06_LOGICA_DE_PROGRESSAO.md` §H; o reducer de `WorkoutSessionContext` (RN) testado isoladamente com `dispatch`.
* **Integração:** `POST /api/v1/sessions/{id}/exercises/{id}/sets` via MockMvc contra um SQLite real, verificando que a constraint `UNIQUE(client_generated_id)` de fato torna o reenvio idempotente; uma `Screen` completa do RN renderizando com `apiClient` mockado, verificando que o estado de `SetRow` transiciona para `OFFLINE_QUEUED` quando a chamada falha.
* **E2E:** o fluxo Maestro citado na Seção B, rodando contra um build real.

---

## F) Casos de Teste Obrigatórios (não-negociáveis)

* Os **4 cenários numéricos completos de `06_LOGICA_DE_PROGRESSAO.md` §H** (H.1-H.4) devem existir **literalmente** como casos de teste de `ProgressiveOverloadServiceTest` — mesmos números de entrada/saída documentados ali, conforme já exigido em `07_ROADMAP_BACKEND.md` §C.4.
* Idempotência de `clientGeneratedId`: um teste de integração que envia a mesma série duas vezes e confirma que apenas um registro existe.
* A transação atômica de `PATCH /cycles/{id}/activate` (`03_CONTRATOS_API.md` §3.7): teste que confirma que nunca existem dois ciclos ativos simultaneamente, mesmo sob falha simulada no meio da transação.

---

## G) Definition of Done — Referência

A cobertura mínima de 80% para classes de regra de negócio pura (já exigida em `00_PRD_IRONTRACK.md` §4.8 e reforçada em `07`/`08`) é medida por linha/instrução via JaCoCo (backend) e pelo relatório nativo do Jest (frontend). PRs que reduzem a cobertura de uma classe já coberta não devem ser aprovados sem justificativa explícita no code review.
