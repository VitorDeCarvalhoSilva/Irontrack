# 🏋️ IronTrack

**Diário de bordo inteligente para musculação clássica e hipertrofia.**

IronTrack não é só um app para "dar check" nos exercícios do dia. Ele
registra carga, repetições, séries e percepção de esforço (RPE) de cada
treino e, com base nesse histórico, aplica o princípio da **sobrecarga
progressiva** via um método de **dupla progressão** — sugerindo, a cada
sessão, se você deve subir carga, subir repetições, ou repetir o que fez.

> **Status atual:** Sprint 0 e Sprint 1 (`00_PRD_IRONTRACK.md` §4.2/§4.3)
> **concluídas** em backend e frontend — autenticação (cadastro, login/logout,
> recuperação de senha, exclusão de conta com carência de 30 dias), perfil de
> usuário e a identidade visual completa do app (`docs/15`) já funcionam de
> ponta a ponta. Sprints 2-5 (ciclos de treino, registro de sessões, métricas
> e offline-first) ainda não foram implementadas. Toda a especificação —
> schema, contratos de API, motor de progressão, roadmaps — está em
> [`docs/`](docs/) e é a fonte de verdade do projeto.

---

## Stack Tecnológico

| Camada | Tecnologia |
| :--- | :--- |
| **Backend** | Java + Spring Boot (API RESTful) |
| **Banco de Dados** | SQLite |
| **Frontend** | React Native (via Expo) — app nativo iOS/Android, offline-first |
| **Infra/Deploy** | Docker (backend), EAS Build/Submit/Update (frontend), GitHub Actions, GitGuardian |

Nenhuma outra peça de infraestrutura é usada por decisão deliberada — sem
Redis, sem PostgreSQL, sem Kubernetes. As razões de cada escolha estão
registradas em [`docs/13_ADR_LOG.md`](docs/13_ADR_LOG.md).

---

## Documentação

Toda a arquitetura, regras de negócio e roadmap do projeto vivem em
[`docs/`](docs/), numerados na ordem em que devem ser lidos — cada
documento assume as decisões dos anteriores como já resolvidas.

| # | Documento | Conteúdo |
| :--- | :--- | :--- |
| 00 | [PRD_IRONTRACK](docs/00_PRD_IRONTRACK.md) | Visão de produto, stack, roadmap ágil (épicos e sprints) |
| 01 | [ARQUITETURA_E_PADROES](docs/01_ARQUITETURA_E_PADROES.md) | Regras de arquitetura, SOLID, estrutura de pastas, convenções de nomenclatura |
| 02 | [SCHEMA_SQLITE](docs/02_SCHEMA_SQLITE.md) | Modelagem de dados, diagrama ER, DDL completo |
| 03 | [CONTRATOS_API](docs/03_CONTRATOS_API.md) | Especificação exata de todos os endpoints REST (requests/responses) |
| 04 | [FRONTEND_UI_COMPONENTES](docs/04_FRONTEND_UI_COMPONENTES.md) | Navegação, árvore de componentes, UX mobile, arquitetura offline-first |
| 05 | [DEVOPS_E_SEGURANCA](docs/05_DEVOPS_E_SEGURANCA.md) | CI/CD, containerização, build/distribuição mobile (EAS), segurança e segredos |
| 06 | [LOGICA_DE_PROGRESSAO](docs/06_LOGICA_DE_PROGRESSAO.md) | O motor de sobrecarga progressiva — algoritmo determinístico, detecção de estagnação, recordes pessoais |
| 07 | [ROADMAP_BACKEND](docs/07_ROADMAP_BACKEND.md) | Roadmap de implementação do backend, sprint a sprint |
| 08 | [ROADMAP_FRONTEND](docs/08_ROADMAP_FRONTEND.md) | Roadmap de implementação do frontend, sprint a sprint |
| 09 | [RELATORIO_DE_INSPECAO_INICIAL](docs/09_RELATORIO_DE_INSPECAO_INICIAL.md) | Auditoria completa da documentação — correções aplicadas e pontos em aberto |
| 10 | [ESTRATEGIA_DE_TESTES](docs/10_ESTRATEGIA_DE_TESTES.md) | Pirâmide de testes, TDD, ferramentas por camada, casos de teste obrigatórios |
| 11 | [POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS](docs/11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md) | Exclusão de conta, retenção de dados, conformidade com a LGPD |
| 12 | [GLOSSARIO_DE_DOMINIO](docs/12_GLOSSARIO_DE_DOMINIO.md) | Termos de negócio centrais do produto, centralizados para consulta rápida |
| 13 | [ADR_LOG](docs/13_ADR_LOG.md) | Histórico de decisões arquiteturais — o quê foi decidido, alternativas consideradas, e por quê |
| 14 | [CATALOGO_DE_ERROS_DE_NEGOCIO](docs/14_CATALOGO_DE_ERROS_DE_NEGOCIO.md) | Índice único de todas as condições de erro de negócio da API |
| 15 | [DESIGN_SYSTEM_UI_UX](docs/15_DESIGN_SYSTEM_UI_UX.md) | Sistema de design (paleta, tipografia, ícones, motion) e planejamento de UI tela a tela |

### Governança de Agentes de IA

Este projeto é construído com apoio de agentes de IA. Dois arquivos
na raiz do repositório governam esse trabalho:

* **[`AGENTS.md`](AGENTS.md)** — leitura obrigatória para qualquer agente
  (Claude, Gemini, Cursor, etc.) antes de tocar em qualquer arquivo: ordem
  de leitura, regras de conduta, pipeline de execução, permissões e
  restrições, padrões de código, mandato de TDD, e anti-padrões proibidos.
* **[`AGENT_LOG.md`](AGENT_LOG.md)** — log cumulativo (append-only) de tudo
  que já foi feito no projeto por qualquer agente, em qualquer sessão.

---

## Como Rodar Localmente

### Backend

Requer Java 21. Se não tiver Java/Maven instalados globalmente, use o
Maven Wrapper já incluso (`backend/mvnw`/`mvnw.cmd`).

```bash
cd backend
cp .env.example .env   # ajuste os valores antes de subir em produção; o
                        # perfil "dev" (application-dev.properties) já traz
                        # valores seguros de exemplo para rodar local
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A API sobe em `http://localhost:8080/api/v1` (Swagger/OpenAPI em
`/swagger-ui.html`, `05_DEVOPS_E_SEGURANCA.md`). O banco SQLite de
desenvolvimento é criado automaticamente em `backend/data/irontrack-dev.db`
(migrado via Flyway) — nenhum banco externo é necessário.

### Frontend

Requer Node.js e o app **Expo Go** no celular (ou um emulador Android/iOS),
Expo SDK 54 travado (`13_ADR_LOG.md` ADR-017).

```bash
cd frontend
npm install
cp .env.example .env   # ajuste EXPO_PUBLIC_API_BASE_URL se o backend não
                        # estiver em localhost:8080 (ex: IP da máquina na
                        # rede local, para testar em dispositivo físico)
npx expo start
```

Escaneie o QR code com o Expo Go (Android) ou a câmera (iOS). Scripts úteis:
`npm run lint`, `npm run typecheck`, `npm run test`.

### Testes e Verificação

* Backend: `./mvnw test` (cobertura mínima de 80%, gate JaCoCo —
  `00_PRD_IRONTRACK.md` §4.8).
* Frontend: `npm run test` (Jest + React Native Testing Library).
* Ambos os passos acima, mais lint/typecheck limpos, são pré-requisito de
  Definition of Done para qualquer sprint (`07`/`08`, Seção D de cada um).

---

## Licença

A definir.
