# 05_DEVOPS_E_SEGURANCA.md - Pipeline, Containerização e Segurança Operacional

Este documento detalha a camada operacional completa do **IronTrack**: containerização, pipelines de CI/CD, gestão de segredos em produção e endurecimento (*hardening*) de segurança da aplicação em execução. Ele **expande** — sem duplicar — as regras já fixadas em [`01_ARQUITETURA_E_PADROES.md`](./01_ARQUITETURA_E_PADROES.md), **Seção 6 (Preparação para DevOps e Segurança)**: Zero Hardcoded Secrets, uso de `.env.example`/`application-dev.properties`, escaneamento GitGuardian e testabilidade isolada em CI. Qualquer regra já definida naquela seção é referenciada aqui por nome, nunca reescrita.

O escopo de infraestrutura segue estritamente a stack definida em [`00_PRD_IRONTRACK.md`](./00_PRD_IRONTRACK.md) §2: Spring Boot + SQLite no backend (deploy em instância única de nuvem, ex: Oracle Cloud) e React Native (Expo) no frontend, distribuído como aplicativo nativo via App Store/Play Store — a containerização desta seção (Docker/docker-compose) aplica-se exclusivamente ao **backend**; o frontend nativo usa uma esteira de build/distribuição própria (EAS, Seção A.2). **Não há Kubernetes, PostgreSQL ou Redis nesta arquitetura** — o SQLite não suporta múltiplas instâncias de escrita concorrentes, o que torna deploy multi-réplica/orquestração complexa não apenas desnecessária, mas contraindicada sem uma migração de banco de dados fora do escopo atual do produto.

> **Nota sobre "Cache" em `00_PRD_IRONTRACK.md` §4.2 (Sprint 0):** o roadmap menciona genericamente "Docker Compose (API + Banco + Cache)". Não há nenhum serviço de cache backend (Redis ou similar) definido em `01_ARQUITETURA_E_PADROES.md`. O único mecanismo de cache do produto é o **wrapper customizado de cache local no frontend** (`01_ARQUITETURA_E_PADROES.md` §3.3, camada `services/apiClient.ts`), que roda inteiramente no navegador/IndexedDB — não é um serviço de infraestrutura. Por isso, o `docker-compose.yml` desta seção **não inclui um serviço de cache dedicado**; caso um cache de servidor venha a ser justificado no futuro, ele deve ser adicionado como uma extensão documentada e aprovada, não assumido por padrão.

---

## A) Containerização (Docker)

### A.1. `Dockerfile` do Backend (Spring Boot)

Build multi-stage: estágio de build com Maven + JDK completo, estágio final com apenas o JRE mínimo necessário para execução.

```dockerfile
# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cliente sqlite3 (CLI) para permitir `docker exec` executando `.backup` (Seção E.4) —
# eclipse-temurin:21-jre-alpine não inclui esse binário por padrão.
RUN apk add --no-cache sqlite

# Cria o usuário não-root antes do COPY, mas só troca para ele (USER) depois —
# WORKDIR /app é criado pelo root com 755, então o COPY abaixo ainda roda como
# root e usa --chown para entregar o .jar já com o dono correto; só então o
# processo passa a rodar como irontrack (hardening básico de container).
RUN addgroup -S irontrack && adduser -S irontrack -G irontrack

COPY --from=build --chown=irontrack:irontrack /app/target/*.jar app.jar

USER irontrack

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

* O estágio `build` nunca é publicado nem promovido — apenas o `.jar` final é copiado para o estágio `runtime`, reduzindo drasticamente a superfície de ataque e o tamanho da imagem (sem Maven, sem código-fonte, sem JDK completo na imagem final).
* Nenhuma variável de ambiente sensível é declarada via `ENV` neste `Dockerfile` — todas são injetadas em tempo de execução (Seção A.3 e D).

### A.2. Build e Distribuição Mobile (EAS)

O frontend é um aplicativo **React Native via Expo** — não é servido por um container web, não há `Dockerfile` nem Nginx de frontend, e não há imagem de frontend em nenhum registry de containers. O build e a distribuição são feitos inteiramente pelo **EAS (Expo Application Services)**, a esteira de CI/CD gerenciada da Expo.

* **`eas.json`** define três perfis de build, na raiz do projeto frontend:
  ```json
  {
    "build": {
      "development": {
        "developmentClient": true,
        "distribution": "internal"
      },
      "preview": {
        "distribution": "internal",
        "channel": "preview"
      },
      "production": {
        "autoIncrement": true,
        "channel": "production"
      }
    },
    "submit": {
      "production": {}
    }
  }
  ```
  * **`development`** — build com o Dev Client da Expo, usado no dia a dia de desenvolvimento local (hot reload completo).
  * **`preview`** — build instalável (`.apk`/`.ipa` ad-hoc ou interno) gerado automaticamente para cada Pull Request (Seção B), permitindo que revisores instalem e testem a mudança em um dispositivo real sem precisar de ambiente de desenvolvimento local.
  * **`production`** — build final, submetido às lojas via `eas submit`, sempre com gate humano (Seção C.2).
* **`app.json`/`app.config.ts`** concentra a identidade do app: nome, ícone, splash screen, `bundleIdentifier` (iOS) e `package` (Android), e a configuração do `expo-notifications` (Seção E, referenciada por `04_FRONTEND_UI_COMPONENTES.md`).
* **Atualizações OTA (`eas update`):** mudanças que tocam **apenas** código JavaScript/TypeScript (sem alterar módulos nativos) podem ser publicadas diretamente aos usuários via `eas update`, sem passar pela revisão de loja — usado no fluxo de staging (Seção C.1). Mudanças que exigem novo binário nativo (nova dependência nativa, mudança de permissão do SO, etc.) exigem um novo ciclo completo de `eas build` + `eas submit`.

### A.3. `docker-compose.yml` de Desenvolvimento Local

```yaml
services:
  api:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    env_file:
      - .env
    volumes:
      - sqlite_data:/data
    restart: unless-stopped

volumes:
  sqlite_data:
    driver: local
```

* **Sem serviço `frontend`:** o app React Native/Expo não roda em container — em desenvolvimento local, ele é executado via Expo Go ou um build de desenvolvimento (`eas build --profile development`) apontando para a API exposta por este `docker-compose.yml` em `http://localhost:8080` (ou o IP da máquina de desenvolvimento na rede local, para testar em dispositivo físico).
* **Regra de persistência do SQLite:** o arquivo de banco **nunca** é montado via *bind mount* direto de um `.db` do host (`./data/irontrack.db:/data/irontrack.db`). Esse padrão é proibido porque bind mounts diretos de um único arquivo SQLite entre host e container expõem o banco a condições de corrida de I/O (o driver de sistema de arquivos do host e do container podem não compartilhar o mesmo mecanismo de *locking* de forma confiável, especialmente em Docker Desktop/WSL2 no Windows). Em vez disso, usa-se um **volume nomeado** (`sqlite_data`) gerenciado inteiramente pelo Docker, montado em `/data` dentro do container, com `DATABASE_PATH=/data/irontrack.db` (Seção D) apontando para dentro desse volume.
* **Regra de segredos:** nenhuma variável sensível (`JWT_SECRET_KEY`, credenciais de e-mail, etc.) é declarada diretamente no `docker-compose.yml`. Todas vêm exclusivamente de `env_file: .env`, arquivo que **nunca é commitado** (já coberto por `.gitignore`, conforme `01_ARQUITETURA_E_PADROES.md` §6.1). O `docker-compose.yml` em si é seguro para versionamento porque não contém nenhum valor real — apenas a referência ao arquivo externo.

---

## B) Pipeline de CI (Integração Contínua)

Workflow do GitHub Actions em `.github/workflows/ci.yml`, disparado em `push` e `pull_request` visando `main` e `develop`, conforme roadmap de `00_PRD_IRONTRACK.md` §4.2 (Sprint 0).

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  lint-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Checkstyle
        run: mvn -f backend/pom.xml checkstyle:check

  lint-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci --prefix frontend
      - run: npm run lint --prefix frontend
      - run: npm run format:check --prefix frontend
      - name: Type check (TypeScript)
        run: npm run typecheck --prefix frontend

  test-backend:
    needs: lint-backend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Testes unitários + cobertura (JaCoCo)
        run: mvn -f backend/pom.xml test jacoco:report jacoco:check
        # jacoco:check está configurado no pom.xml com regra de linha/instrução
        # mínima de 80%, conforme Definition of Done (00_PRD_IRONTRACK.md §4.8).
        # O build falha automaticamente (BUILD FAILURE) se a cobertura cair
        # abaixo do limiar — não é uma checagem manual.

  test-frontend:
    needs: lint-frontend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci --prefix frontend
      - name: Testes unitários e de componente (Jest + React Native Testing Library)
        run: npm run test --prefix frontend -- --coverage

  docker-build-validation:
    needs: test-backend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build imagem backend (validação, sem push)
        run: docker build -t irontrack-api:ci-validation ./backend
        # Apenas o backend é containerizado — o frontend (React Native/Expo)
        # não gera imagem Docker; sua validação de build é o job
        # eas-build-preview abaixo.

  eas-build-preview:
    needs: test-frontend
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci --prefix frontend
      - name: EAS Build (perfil preview)
        working-directory: frontend
        run: npx eas-cli build --profile preview --non-interactive --no-wait
        env:
          EXPO_TOKEN: ${{ secrets.EXPO_TOKEN }}
        # Gera um build instalável (.apk / .ipa ad-hoc) para o PR, permitindo
        # que o revisor teste a mudança em um dispositivo real. --no-wait não
        # bloqueia o pipeline esperando o build terminar na nuvem da Expo —
        # o link de instalação fica disponível no dashboard do EAS.

  secret-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: GitGuardian Scan (ggshield)
        uses: GitGuardian/ggshield-action@v1
        env:
          GITGUARDIAN_API_KEY: ${{ secrets.GITGUARDIAN_API_KEY }}
        # Escaneia o diff do PR (ou o push). Qualquer segredo detectado
        # falha o job sem exceção configurada por padrão — nenhuma allowlist
        # de segredo é aceita sem revisão explícita de um mantenedor sênior.
```

* **Jobs paralelos vs. sequenciais:** `lint-backend` e `lint-frontend` rodam em paralelo (não dependem um do outro); `test-backend`/`test-frontend` dependem apenas do lint da própria stack; `docker-build-validation` só roda após `test-backend` passar; `eas-build-preview` só roda após `test-frontend` passar, e apenas em eventos de Pull Request (`if: github.event_name == 'pull_request'` — não roda em push direto); `secret-scan` roda de forma independente e paralela a todo o resto, pois não depende de build/testes.
* **Critério de bloqueio de merge:** as *branch protection rules* de `main` e `develop` exigem que **todos** os jobs acima (`lint-backend`, `lint-frontend`, `test-backend`, `test-frontend`, `docker-build-validation`, `eas-build-preview` em PRs, `secret-scan`) estejam com status `success` como *required status checks* antes de permitir o merge de um Pull Request. Nenhum job é opcional; não há *merge override* padrão para administradores fora de um incidente documentado.

---

## C) Pipeline de CD (Entrega Contínua)

Dado que a infraestrutura-alvo é uma instância única de nuvem (ex: Oracle Cloud, conforme `00_PRD_IRONTRACK.md` §2) rodando SQLite, o CD utiliza **push de imagem para um registry + pull remoto via SSH**, sem orquestrador — a abordagem mais simples que atende ao requisito de instância única (implantar Kubernetes/Swarm para uma única réplica de escrita seria complexidade sem benefício real, já que o SQLite proíbe múltiplas instâncias de escrita concorrente).

### C.1. Deploy Automático em Staging

```yaml
name: CD - Staging

on:
  push:
    branches: [develop]

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Build & push imagem da API
        run: |
          docker build -t ghcr.io/irontrack/api:staging-${{ github.sha }} -t ghcr.io/irontrack/api:staging-latest ./backend
          docker push ghcr.io/irontrack/api:staging-${{ github.sha }}
          docker push ghcr.io/irontrack/api:staging-latest

  deploy-api:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy via SSH na instância de staging
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.STAGING_HOST }}
          username: ${{ secrets.STAGING_SSH_USER }}
          key: ${{ secrets.STAGING_SSH_PRIVATE_KEY }}
          script: |
            cd /opt/irontrack
            export IMAGE_TAG=staging-${{ github.sha }}
            docker compose pull
            docker compose up -d

  deploy-mobile-ota:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci --prefix frontend
      - name: EAS Update (canal preview — OTA, apenas mudanças JS)
        working-directory: frontend
        run: npx eas-cli update --channel preview --non-interactive --message "${{ github.event.head_commit.message }}"
        env:
          EXPO_TOKEN: ${{ secrets.EXPO_TOKEN }}
```

* Disparado automaticamente a cada merge em `develop`, sem gate humano — conforme `00_PRD_IRONTRACK.md` §4.2 ("pipeline de CD para staging automático após merge em `develop`"). O deploy do frontend em staging é uma atualização **OTA** (`eas update`, canal `preview`) — não gera um novo binário nem passa por revisão de loja; mudanças que exigem binário nativo novo exigem um `eas build --profile preview` manual (Seção A.2) antes de poderem ser testadas.

### C.2. Deploy com Gate Humano em Produção

```yaml
name: CD - Production

on:
  push:
    branches: [main]

jobs:
  build-and-push-api:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      # ... mesmos passos de build/push de C.1, com tags `production-${{ github.sha }}`

  deploy-api:
    needs: build-and-push-api
    runs-on: ubuntu-latest
    environment:
      name: production   # Environment protegido no GitHub — exige aprovação manual
    steps:
      - name: Deploy via SSH na instância de produção
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.PRODUCTION_HOST }}
          username: ${{ secrets.PRODUCTION_SSH_USER }}
          key: ${{ secrets.PRODUCTION_SSH_PRIVATE_KEY }}
          script: |
            cd /opt/irontrack
            export IMAGE_TAG=production-${{ github.sha }}
            docker compose pull
            docker compose up -d

  build-and-submit-mobile:
    runs-on: ubuntu-latest
    environment:
      name: production   # mesmo Environment protegido — mesma aprovação manual do backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci --prefix frontend
      - name: EAS Build (perfil production)
        working-directory: frontend
        run: npx eas-cli build --profile production --non-interactive --auto-submit
        env:
          EXPO_TOKEN: ${{ secrets.EXPO_TOKEN }}
          APPLE_APP_SPECIFIC_PASSWORD: ${{ secrets.APPLE_APP_SPECIFIC_PASSWORD }}
          ASC_API_KEY: ${{ secrets.ASC_API_KEY }}
          GOOGLE_SERVICE_ACCOUNT_JSON: ${{ secrets.GOOGLE_SERVICE_ACCOUNT_JSON }}
        # --auto-submit encadeia `eas submit` automaticamente ao final do build,
        # publicando nas filas de revisão da App Store/Play Store.
```

* Os jobs `deploy-api` e `build-and-submit-mobile` referenciam o mesmo **GitHub Environment** chamado `production`, configurado com *required reviewers* — cada workflow fica pausado em "Waiting for review" até que um mantenedor autorizado aprove manualmente a promoção, mesmo que o build já tenha sido concluído com sucesso. Não existe caminho automático de `main` direto para produção, nem para o backend nem para o app mobile — a submissão às lojas (`eas submit`) é sempre um passo deliberado, nunca automático a partir de um merge.

### C.3. Estratégia de Rollback

* Toda imagem publicada carrega duas tags: uma imutável por commit (`production-<sha>`) e uma móvel (`production-latest`). O arquivo `docker-compose.prod.yml` na instância referencia a imagem via variável `${IMAGE_TAG}`, nunca hardcoded.
* Rollback consiste em reexecutar o deploy apontando `IMAGE_TAG` para o SHA da última versão estável conhecida:
  ```bash
  # Na instância de produção (ou reexecutando o job de deploy manualmente com um SHA anterior)
  export IMAGE_TAG=production-<sha-anterior-estavel>
  docker compose pull
  docker compose up -d
  ```
* **Restrição crítica do SQLite em rollback:** como o banco é um único arquivo compartilhado (não versionado por deploy), o rollback de código **não reverte o schema do banco**. Toda migration aplicada por uma versão mais nova deve ser escrita de forma retrocompatível com a versão anterior (padrão *expand/contract*: adicionar colunas nulas/novas tabelas é seguro para rollback; remover ou renomear colunas usadas pela versão anterior não é). Antes de qualquer rollback, deve-se confirmar que a versão-alvo do rollback ainda é compatível com o schema atual do banco.
* Um backup do SQLite (Seção E.4) é sempre executado imediatamente **antes** de qualquer deploy em produção, como salvaguarda adicional caso o rollback de código não seja suficiente.

---

## D) Gestão de Segredos e Variáveis de Ambiente (Detalhamento)

Complementa `01_ARQUITETURA_E_PADROES.md` §6.1 (Zero Hardcoded Secrets) com a lista exaustiva de variáveis necessárias em produção.

| Variável | Consumidor | Descrição |
| :--- | :--- | :--- |
| `DATABASE_PATH` | Backend (`application.properties`: `spring.datasource.url=jdbc:sqlite:${DATABASE_PATH}`) | Caminho absoluto do arquivo SQLite dentro do volume montado (ex: `/data/irontrack.db`). |
| `JWT_SECRET_KEY` | Backend | Chave simétrica usada para assinar e validar os tokens JWT (`access` e `refresh`). Mínimo de 256 bits, gerada por gerador criptográfico seguro — nunca uma string memorável. |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | Backend | Tempo de vida do access token em milissegundos (ex: `900000` = 15 minutos). |
| `JWT_REFRESH_TOKEN_EXPIRATION_MS` | Backend | Tempo de vida do refresh token em milissegundos (ex: `604800000` = 7 dias). |
| `CORS_ALLOWED_ORIGINS` | Backend | Origem(ns) HTTPS autorizada(s) a consumir a API (ver Seção E.3). Em produção, um único valor — nunca `*`. |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USER` / `SMTP_PASSWORD` | Backend | Credenciais do provedor de e-mail transacional, usadas nos fluxos de verificação de cadastro e recuperação de senha (`00_PRD_IRONTRACK.md` EP-01, Sprint 1). |
| `MAIL_FROM_ADDRESS` | Backend | Endereço de remetente exibido nos e-mails transacionais. |
| `GITGUARDIAN_API_KEY` | CI (GitHub Actions Secret) | Chave de autenticação do `ggshield` no pipeline (Seção B) — nunca exposta em log de build. |
| `EXPO_PUBLIC_API_BASE_URL` | Frontend (build-time) | URL base da API consumida pelo `apiClient.ts` (ex: `https://api.irontrack.app/api/v1`). Variáveis com o prefixo `EXPO_PUBLIC_` são inlined no bundle JS pelo Metro bundler do Expo em tempo de build (equivalente ao prefixo `VITE_` do Vite) — uma mudança na URL da API exige novo build/OTA update, não apenas reinício do processo. |
| `EXPO_TOKEN` | CI (GitHub Actions Secret) | Token de autenticação da conta Expo/EAS, usado por `eas-cli` nos jobs de CI/CD (Seções B e C) para `eas build`/`eas update`/`eas submit` sem login interativo. |
| `APPLE_APP_SPECIFIC_PASSWORD` / `ASC_API_KEY` | CI (GitHub Actions Secret, Environment `production`) | Credenciais para submissão automatizada à App Store via `eas submit` (`05` §C.2) — senha de app específico da conta Apple Developer ou chave de API do App Store Connect. |
| `GOOGLE_SERVICE_ACCOUNT_JSON` | CI (GitHub Actions Secret, Environment `production`) | Credencial de conta de serviço do Google Play Console, usada por `eas submit` para publicar automaticamente na Play Store (`05` §C.2). |

### D.1. Rotação de `JWT_SECRET_KEY`

* Rotacionar `JWT_SECRET_KEY` invalida **imediatamente todos os tokens já emitidos** — tanto `accessToken` quanto `refreshToken` deixam de validar contra a assinatura HMAC, pois o backend usa uma única chave simétrica ativa por vez (sem suporte a múltiplas chaves ou `kid` no cabeçalho do JWT nesta versão do produto).
* **Impacto assumido:** rotação de chave é tratada como um evento de **invalidação em massa deliberada** (força logout de todos os usuários simultaneamente), não como uma operação silenciosa. Por isso, rotação de `JWT_SECRET_KEY` deve ser: (1) planejada fora de horário de pico de uso (academia, tipicamente noite/manhã), (2) comunicada como manutenção esperada, e (3) executada via atualização do segredo no cofre (Seção D.2) seguida de reinício controlado do serviço backend.
* **Gatilhos obrigatórios de rotação:** vazamento suspeito ou confirmado da chave (ex: alerta do GitGuardian em um commit histórico), rotação periódica preventiva (recomendado: a cada 90 dias) e desligamento de um mantenedor com acesso ao cofre de produção.
* Um período de graça com duas chaves simultâneas (chave atual + anterior) não é implementado nesta versão do produto por adicionar complexidade ao `JwtTokenProvider` sem justificativa dado o tempo de vida curto do access token (15 min) — a decisão consciente é aceitar o impacto de logout em massa em troca de simplicidade de implementação.

### D.2. Onde os Segredos de Produção Residem

* **Nunca** em arquivo versionado no repositório, criptografado ou não — reforça `01_ARQUITETURA_E_PADROES.md` §6.1.
* Segredos consumidos pelos workflows de CI/CD (`GITGUARDIAN_API_KEY`, `STAGING_SSH_PRIVATE_KEY`, `PRODUCTION_SSH_PRIVATE_KEY`, `STAGING_HOST`, `PRODUCTION_HOST`, etc.) residem exclusivamente em **GitHub Actions Secrets**, escopados por *Environment* (`staging` vs. `production`) para que uma aprovação de deploy de staging não exponha automaticamente credenciais de produção.
* Segredos consumidos em runtime pelo container backend (`JWT_SECRET_KEY`, `SMTP_PASSWORD`, etc.) residem no arquivo `.env` local **apenas em desenvolvimento**. Em produção, são injetados diretamente como variáveis de ambiente do processo Docker na instância de nuvem (via `docker compose --env-file /etc/irontrack/prod.env`, arquivo com permissão restrita de leitura `600`, gerenciado fora do repositório Git) — ou, quando o provedor de nuvem oferecer um cofre de segredos gerenciado (ex: Oracle Cloud Vault), este é o mecanismo preferencial, evitando qualquer arquivo de texto plano persistido em disco na instância.

---

## E) Segurança Adicional de Aplicação

### E.1. Cabeçalhos de Segurança HTTP

Configurados no backend Spring Boot via `HttpSecurity.headers()` (Spring Security), aplicados a todas as respostas da API:

| Cabeçalho | Valor | Justificativa |
| :--- | :--- | :--- |
| `X-Content-Type-Options` | `nosniff` | Impede que o navegador infira um `Content-Type` diferente do declarado, mitigando ataques de MIME-sniffing. |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Força HTTPS em todas as requisições subsequentes — aplicado apenas quando o backend está atrás de TLS (produção/staging; omitido em desenvolvimento local sem certificado). |
| `X-Frame-Options` | `DENY` | Previne que a API seja embutida em `iframe` de terceiros (defesa complementar contra clickjacking, ainda que a API não sirva HTML). |
| `Content-Security-Policy` | `default-src 'none'` | Como a API é *API-only* (nunca serve HTML renderizável), a política mais restritiva possível é aplicada — não há necessidade de liberar scripts, estilos ou origens de conteúdo. |

* O frontend é um aplicativo nativo (React Native/Expo, Seção A.2), não uma página servida por um navegador — `Content-Security-Policy` e os demais cabeçalhos acima **não se aplicam** ao cliente mobile, apenas às respostas HTTP da API em si.

### E.2. Rate Limiting em `POST /auth/login`

Reforça a regra de negócio já definida em `00_PRD_IRONTRACK.md` §4.3 (bloqueio temporário após 5 falhas consecutivas de login).

* **Camada de implementação — bloqueio por conta (regra de negócio):** um filtro do Spring Security (`AuthenticationFailureBadCredentialsEvent` listener) mantém um contador de tentativas falhas por `email`, usando um cache em memória (`Caffeine`, sem dependência externa) com expiração de 15 minutos por entrada. Ao atingir 5 falhas, o próximo `POST /auth/login` para aquele `email` retorna `429 Too Many Requests` mesmo com credenciais corretas, até o cooldown expirar.
  > **Limitação assumida (instância única):** por não haver Redis nesta arquitetura, o contador reside apenas na memória do processo backend — um reinício do serviço (ex: durante um deploy) zera os contadores. Dado que o deploy é infrequente e a instância é única (Seção C), esse *trade-off* é aceito conscientemente em vez de introduzir uma dependência de infraestrutura adicional só para persistência de contadores de rate limit.
* **Camada de implementação — throttling por IP (defesa complementar, coarse-grained):** um Nginx dedicado como proxy reverso à frente da API em produção (o frontend nativo não usa mais Nginx, Seção A.2) aplica `limit_req_zone` sobre o path `/api/v1/auth/login`, limitando requisições por IP de origem independentemente do `email` usado, mitigando *credential stuffing* distribuído entre múltiplas contas:
  ```nginx
  limit_req_zone $binary_remote_addr zone=login_limit:10m rate=10r/m;

  location /api/v1/auth/login {
      limit_req zone=login_limit burst=3 nodelay;
      proxy_pass http://api:8080;
  }
  ```

### E.3. Política de CORS

* `CORS_ALLOWED_ORIGINS` (Seção D) é lido pelo `CorsConfigurationSource` do Spring Security e configura uma **lista explícita de origem única** em produção (ex: `https://app.irontrack.com`) — o wildcard `*` é proibido em qualquer ambiente que não seja desenvolvimento local, pois os endpoints protegidos usam `Authorization: Bearer <JWT>` (`03_CONTRATOS_API.md` §1.3): combinar `Access-Control-Allow-Origin: *` com credenciais/tokens Bearer amplia desnecessariamente a superfície de exfiltração de tokens caso exista qualquer vulnerabilidade de XSS em domínios não controlados.
* Ambientes de staging e produção usam valores distintos de `CORS_ALLOWED_ORIGINS`, nunca compartilhando a mesma configuração.

### E.4. Estratégia de Backup do SQLite

Como o banco de dados é um único arquivo sem replicação nativa (diferente de um SGBD cliente-servidor), o backup é uma responsabilidade operacional explícita, não delegável ao SQLite em si.

* **Mecanismo — execução via `docker exec` (não um script de host com acesso direto ao arquivo):** o `cron` roda no **host** da instância de produção, mas delega a geração do backup ao **container `api` em execução**, via `docker exec` — reaproveitando o binário `sqlite3` já instalado na imagem de runtime (`apk add sqlite`, Seção A.1) e a variável `$DATABASE_PATH` tal como resolvida dentro do container (Seção D), sem que o host precise conhecer ou inspecionar o `Mountpoint` real do volume nomeado `sqlite_data`. O arquivo gerado é então extraído do container para o host com `docker cp`, e só a partir daí manipulado como um arquivo comum do host (retenção, upload externo):
  ```bash
  #!/bin/sh
  # /opt/irontrack/scripts/backup-sqlite.sh — executado por cron NO HOST.
  # $DATABASE_PATH abaixo NÃO é uma variável do ambiente do host: ela é
  # resolvida dentro do container `api` (nome do serviço no docker-compose.prod.yml,
  # ex: irontrack-api-1) via `docker exec`, que herda o ambiente do processo do
  # container — o mesmo $DATABASE_PATH definido na Seção D.
  CONTAINER_NAME="irontrack-api-1"
  TIMESTAMP=$(date +%Y%m%d-%H%M%S)
  BACKUP_FILE="irontrack-${TIMESTAMP}.db"

  # 1. Gera o backup consistente DENTRO do container, em um caminho temporário do container
  docker exec "$CONTAINER_NAME" sh -c 'sqlite3 "$DATABASE_PATH" ".backup /tmp/'"$BACKUP_FILE"'"'

  # 2. Copia o arquivo gerado de dentro do container para o diretório do HOST
  docker cp "$CONTAINER_NAME:/tmp/${BACKUP_FILE}" "/backups/${BACKUP_FILE}"

  # 3. Remove o arquivo temporário de dentro do container (não deixa cópia residual na imagem em execução)
  docker exec "$CONTAINER_NAME" rm "/tmp/${BACKUP_FILE}"

  # Retenção local: mantém apenas os últimos 14 backups diários
  find /backups -name "irontrack-*.db" -mtime +14 -delete
  ```
* **Periodicidade:** a cada 6 horas, mais um backup imediatamente antes de todo deploy em produção (Seção C.3).
* **Armazenamento externo:** os backups em `/backups` — um diretório comum do **host**, fora de qualquer volume Docker do container ativo (distinto do volume nomeado `sqlite_data` usado pelo `api`, Seção A.3), para que uma falha isolada nesse volume não destrua simultaneamente o banco e seus backups — são replicados para um object storage externo ao final de cada execução do cron (ex: `oci os object put` se a instância-alvo for Oracle Cloud, ou o CLI equivalente do provedor efetivamente contratado) — evitando que a perda da instância de produção implique perda total de dados históricos de treino dos usuários.
* Backups nunca incluem o `.env`/segredos junto ao arquivo `.db` no mesmo destino de armazenamento sem controle de acesso equivalente — o bucket/object storage de backups do banco tem sua própria política de acesso restrita, distinta do cofre de segredos (Seção D.2).

---

## F) Checklist de Segurança Pré-Deploy

Gate manual obrigatório antes de qualquer promoção para o ambiente de **produção** (complementar à aprovação humana do GitHub Environment, Seção C.2):

- [ ] Todos os jobs obrigatórios do CI (`lint-backend`, `lint-frontend`, `test-backend`, `test-frontend`, `docker-build-validation`, `eas-build-preview` em PRs, `secret-scan`) estão verdes na última execução da branch `main`.
- [ ] Cobertura de testes unitários do backend permanece `>= 80%` (relatório JaCoCo do último `test-backend`).
- [ ] Nenhum segredo real está presente em `docker-compose.yml`, `Dockerfile`, código-fonte ou histórico de commits do PR (confirmado pelo job `secret-scan`, sem *allowlist* aplicada sem revisão).
- [ ] `CORS_ALLOWED_ORIGINS` em produção aponta exclusivamente para o domínio real do frontend — nunca `*`, nunca um domínio de staging.
- [ ] `JWT_SECRET_KEY` de produção é distinto do valor usado em desenvolvimento/staging, com no mínimo 256 bits de entropia.
- [ ] Backup do SQLite executado e verificado (arquivo íntegro, tamanho consistente com o esperado) imediatamente antes do deploy.
- [ ] Tag de imagem (`production-<sha>`) documentada em algum canal de comunicação da equipe, para permitir rollback rápido e rastreável (Seção C.3).
- [ ] Cabeçalhos de segurança HTTP (`E.1`) validados em staging via inspeção de resposta (`curl -I`) antes da promoção para produção.
- [ ] Rate limiting de `POST /auth/login` (`E.2`) testado manualmente em staging (5 tentativas falhas geram `429`).
- [ ] Nenhuma variável de ambiente de produção está com valor placeholder/exemplo copiado de `.env.example` ou `application-dev.properties` (`01_ARQUITETURA_E_PADROES.md` §6.1).
