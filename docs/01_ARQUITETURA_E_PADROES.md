# 01_ARQUITETURA_E_PADROES.md - Diretrizes de Arquitetura e Padrões de Código

Este documento serve como a **Constituição Técnica** do projeto **IronTrack**. Ele dita as regras arquiteturais, estruturais e de padronização de código que devem ser seguidas de forma estrita por todos os desenvolvedores e agentes de IA durante a implementação do software. Qualquer desvio destas regras será considerado débito técnico ou falha de aceitação.

---

## 1. Visão Arquitetural de Alto Nível

O IronTrack adota uma arquitetura desacoplada e modular, baseada no modelo **Client-Server**.
* **Backend:** API REST robusta construída em Java com Spring Boot, utilizando persistência relacional com SQLite e seguindo os princípios de Design Orientado a Domínio (DDD) simplificado e Arquitetura em Camadas.
* **Frontend:** Aplicativo nativo móvel (iOS e Android) baseado em **React Native com TypeScript, via Expo**, distribuído pela App Store/Play Store, fornecendo resiliência e funcionalidade offline completa (ver `05_DEVOPS_E_SEGURANCA.md` §A.2 para a esteira de build/distribuição via EAS e `04_FRONTEND_UI_COMPONENTES.md`/`08_ROADMAP_FRONTEND.md` para a especificação completa da UI e do roadmap).

---

## 2. Estrutura de Diretórios - Backend (Spring Boot)

O backend deve seguir uma estrutura de pacotes altamente coesa, organizada por camada lógica dentro do pacote raiz `com.irontrack.api`.

### 2.1. Árvore de Pastas do Backend
```text
com.irontrack.api/
│
├── config/              # Configurações do framework (Security, SQLite, Swagger/OpenAPI)
├── controllers/         # Controladores REST (HTTP Entrypoints)
├── services/            # Camada de lógica de negócios (Serviços e Regras)
│   └── impl/            # Implementações concretas de serviços (se necessário)
├── repositories/        # Interfaces do Spring Data JPA (Acesso ao Banco SQLite)
├── entities/            # Entidades do modelo relacional JPA (Tabelas do Banco)
├── dto/                 # Data Transfer Objects (Comunicação de Entrada/Saída)
│   ├── request/         # DTOs de entrada (Payloads de requisições)
│   └── response/        # DTOs de saída (Payloads de respostas)
├── exceptions/          # Exceções customizadas e Global Exception Handler
├── security/            # Filtros JWT, Provedores de Autenticação e Configurações de Criptografia
└── utils/               # Utilitários, validadores gerais e funções auxiliares
```

### 2.2. Separação de Responsabilidades (Arquitetura em Camadas)

1. **Camada de Apresentação (`controllers`)**:
   * Responsável apenas pelo recebimento das requisições HTTP, validação sintática preliminar (usando anotações do Bean Validation como `@Valid`, `@NotNull`, `@Size`), e mapeamento de retornos.
   * **Regra de Ouro:** Nenhum controlador pode conter lógica de negócios ou acessar a base de dados diretamente. Eles devem delegar chamadas para a camada de serviço.
   * Retornam obrigatoriamente objetos da camada `dto.response` encapsulados em `ResponseEntity`.

2. **Camada de Negócio (`services`)**:
   * Onde reside toda a lógica de domínio, algoritmos de cálculo de progressão de carga e regras operacionais do IronTrack.
   * Controla transações de banco de dados utilizando a anotação `@Transactional` do Spring.
   * Realiza a conversão entre entidades de banco de dados e DTOs de requisição/resposta.

3. **Camada de Persistência (`repositories`)**:
   * Abstração de acesso aos dados via Spring Data JPA.
   * Consultas complexas devem ser definidas declarativamente ou através de consultas JPQL/Nativas com `@Query`.

4. **Uso Obrigatório do Padrão DTO (Data Transfer Object)**:
   * **NUNCA** exponha entidades JPA diretamente para o cliente frontend. Isso evita problemas de segurança (exposição do schema do banco), acoplamento excessivo e recursões infinitas na serialização JSON.
   * Cada endpoint de criação/atualização de dados deve ter seu respectivo `RequestDTO`.
   * Cada endpoint de retorno deve ter seu correspondente `ResponseDTO`.
   * A conversão entre `Entity` <-> `DTO` deve ocorrer exclusivamente dentro da camada de `services`. Recomenda-se o uso de records Java (para DTOs imutáveis) ou classes de mapeamento explícitas para garantir máxima legibilidade e performance.

### 2.3. Injeção de Dependências e Princípios SOLID
* **Injeção de Dependência via Construtor:** O uso de `@Autowired` diretamente em atributos de classe é expressamente proibido. Toda injeção de dependência deve ser realizada por meio de construtores de classe. Isso melhora a testabilidade física e garante a imutabilidade das dependências do componente.
  ```java
  // PADRÃO CORRETO:
  @Service
  public class WorkoutService {
      private final WorkoutRepository workoutRepository;
      private final UserRepository userRepository;

      public WorkoutService(WorkoutRepository workoutRepository, UserRepository userRepository) {
          this.workoutRepository = workoutRepository;
          this.userRepository = userRepository;
      }
  }
  ```
* **Princípio da Responsabilidade Única (SRP):** Cada serviço deve tratar de uma única regra ou entidade de negócio agregada. Classes com mais de 300 linhas devem ser revisadas e candidatas a refatoração.
* **Princípio do Aberto/Fechado (OCP):** Motores de cálculo, como a lógica de progressão, devem ser extensíveis por meio de polimorfismo, evitando cadeias excessivas de condicionais `if-else` para novos tipos de exercícios.

---

## 3. Estrutura de Diretórios - Frontend (React Native + TypeScript)

O frontend deve ser estruturado para suportar o desenvolvimento modular de componentes independentes e de fácil manutenção, promovendo a separação entre UI (interface) e Estado/Lógica. A partir desta revisão, o frontend é um aplicativo **React Native via Expo** (não mais um PWA web) — ver `05_DEVOPS_E_SEGURANCA.md` §A.2 para a esteira de build/distribuição (EAS) e `04_FRONTEND_UI_COMPONENTES.md`/`08_ROADMAP_FRONTEND.md` para a especificação completa da UI e do roadmap.

### 3.1. Árvore de Pastas do Frontend
```text
app/                       # Ou src/, conforme convenção do Expo Router ou navegação manual
│
├── assets/                # Ícones, fontes, imagens, splash screen
├── components/
│   ├── common/            # Componentes atômicos (Button, Input, Card, Modal) — NativeWind
│   └── layout/            # Componentes de estrutura (Header, TabBar customizado)
├── screens/                # Telas (equivalente a "pages" no mundo web)
│   ├── Auth/               # LoginScreen, RegisterScreen, VerifyEmailScreen, etc.
│   ├── Dashboard/
│   ├── WorkoutCycle/
│   └── ActiveWorkout/      # Diário de bordo em tempo real
├── navigation/             # Configuração do React Navigation (stacks, tabs, guards)
├── hooks/                  # Custom hooks globais (useAuth, useNetworkStatus, useInterval)
├── contexts/               # Provedores de estado global (AuthContext, WorkoutSessionContext)
├── services/               # Integração com API e infraestrutura
│   ├── apiClient.ts         # Instância central do Axios com interceptores
│   ├── authService.ts
│   └── storage/             # Integração com AsyncStorage para persistência offline
├── utils/                   # Funções de utilidade geral
├── theme/                   # Configuração do NativeWind/tokens de design
└── types/                   # Tipagem estrita do TypeScript
```

### 3.2. Padrão de Componentização (Lógicos vs. Visuais)

Para maximizar a reutilização e simplificar os testes unitários da interface, o IronTrack adota estritamente a separação entre componentes lógicos (Containers) e componentes visuais (Presentational/Dumb) — esta distinção é independente de plataforma e continua valendo integralmente em React Native.

* **Componentes Lógicos (Containers / Telas):**
  * Responsáveis pela orquestração do estado, chamadas de rede à API, assinaturas de Contextos e gerenciamento de efeitos colaterais (`useEffect`).
  * Não devem possuir regras de estilo detalhadas (`StyleSheet`/classes NativeWind extensas) diretamente em suas declarações; eles delegam a renderização física aos componentes visuais correspondentes.
* **Componentes Visuais (Dumb/Presentational Components):**
  * Responsáveis exclusivamente pela renderização visual (markup e estilo).
  * Devem ser **puros**: recebem dados via `props` e notificam ações por meio de funções de callback passadas por parâmetros.
  * São altamente portáveis e não possuem conhecimento direto sobre APIs, navegação ou estados globais de autenticação.

### 3.3. Gerenciamento de Estado Global e Cache de Requisições
* **Estado Global Leve:** Para dados essenciais de segurança e experiência que necessitam permear múltiplos fluxos (ex: sessão do usuário ativa, preferências visuais), deve-se usar o **React Context API** (continua válido em React Native — não é uma API exclusiva do DOM/navegador) ou uma ferramenta minimalista como **Zustand**.
* **Estado de Sessão de Treino Ativo:** O progresso imediato de uma sessão de treino em execução (cronômetro, séries executadas, temporizador de descanso) deve ser controlado em um Contexto dedicado (`WorkoutSessionContext`) persistido continuamente no armazenamento local para resiliência ao app ser minimizado ou encerrado pelo sistema operacional.
* **Cache de Requisições:** Para chamadas de rede repetitivas, implementa-se um wrapper customizado de cache local com expiração controlada na camada de `services/apiClient.ts` para otimizar consumo de dados e habilitar consultas rápidas no modo offline.

---

## 4. Tratamento de Erros e Exceções

O tratamento de erros deve ser consistente em ambas as pontas do sistema para garantir uma UX fluida e evitar falhas silenciosas que frustram o usuário no meio de sua sessão de treino.

### 4.1. Backend: Handler de Exceções Global

O Spring Boot interceptará todas as falhas por meio de uma classe anotada com `@RestControllerAdvice`.
* **Formato de Payload de Erro Padronizado:** Toda resposta de erro enviada ao frontend deve seguir milimetricamente a estrutura JSON abaixo:
  ```json
  {
    "timestamp": "2026-07-01T15:30:00.123Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Mensagem descritiva detalhada voltada para o usuário final.",
    "path": "/api/v1/auth/register"
  }
  ```
* **Mapeamento de Status HTTP:**
  * `400 Bad Request`: Falhas de validação de dados de entrada (`MethodArgumentNotValidException`).
  * `401 Unauthorized`: Tokens inválidos, ausentes ou expirados.
  * `403 Forbidden`: Usuário autenticado tentando acessar recursos de outros usuários.
  * `404 Not Found`: Entidades inexistentes no banco de dados (`ResourceNotFoundException`).
  * `422 Unprocessable Entity`: Violações de regras de negócio (ex: tentar finalizar um treino que já está finalizado).
  * `500 Internal Server Error`: Erros não previstos do sistema.

### 4.2. Frontend: Interceptors do Axios/Fetch

A comunicação HTTP no frontend deve ser centralizada em uma instância compartilhada que implementa interceptores inteligentes:

1. **Intercepção de Autenticação (Refresh Token Flow):**
   * Ao detectar um erro HTTP `401 Unauthorized`, o interceptor deve paralisar o envio de outras chamadas e efetuar um fluxo silencioso de renovação via chamada de Refresh Token (`POST /api/v1/auth/refresh`).
   * Se a renovação for bem-sucedida, atualiza os tokens em armazenamento seguro e refaz a chamada original automaticamente.
   * Se a renovação falhar, os dados de sessão local devem ser limpos imediatamente e o usuário deve ser navegado de volta para `LoginScreen` (`AuthStack`, `04_FRONTEND_UI_COMPONENTES.md` §A).

2. **Intercepção e Fallback Offline:**
   * O interceptor de rede deve capturar falhas físicas de conexão (falta de sinal, queda de rede).
   * Caso o usuário tente salvar dados cruciais (como o registro de um set de exercício de uma sessão ativa) e a rede esteja indisponível, o interceptor deve salvar a operação na fila de sincronização do armazenamento local (AsyncStorage, `04_FRONTEND_UI_COMPONENTES.md` §E.2), alertando o usuário visivelmente via um banner de "Modo Offline Ativo - Seus dados serão sincronizados automaticamente ao retornar a conexão".

---

## 5. Convenções de Nomenclatura e Clean Code

### 5.1. Nomenclatura Padrão do Código
| Tecnologia | Escopo de Aplicação | Padrão | Exemplo |
| :--- | :--- | :--- | :--- |
| **Java** | Classes, Interfaces, Records, Enums | `PascalCase` | `WorkoutSessionService` |
| **Java** | Métodos, Variáveis, Parâmetros | `camelCase` | `calculateProgressiveOverload` |
| **Java** | Constantes Globais (`public static final`) | `UPPER_SNAKE_CASE`| `MAX_RECOVERY_TIME_MINUTES` |
| **React** | Componentes, Contextos, Páginas | `PascalCase` | `TimerIndicator.tsx` |
| **React** | Hooks Customizados | `camelCase` (Prefixo `use`) | `useActiveWorkout` |
| **React** | Variáveis, Funções e Props | `camelCase` | `onSetComplete` |
| **SQLite** | Nomes de Tabelas | `snake_case` (Plural) | `training_cycles`, `exercise_sets` |
| **SQLite** | Nomes de Colunas | `snake_case` | `hold_time_seconds`, `created_at` |

### 5.2. Padrão de Commits (Conventional Commits)
Todas as alterações no repositório de código do IronTrack devem seguir a especificação de Commits Convencionais, para facilitar a automação de releases e rastreabilidade:

**Estrutura de Mensagem:** `tipo(escopo): descrição concisa em português ou inglês`

* `feat`: Adição de nova funcionalidade (ex: `feat(auth): adiciona fluxo de login com JWT`).
* `fix`: Correção de bug no sistema (ex: `fix(progress): corrige cálculo de volume semanal`).
* `docs`: Alterações exclusivamente em arquivos de documentação (ex: `docs(api): atualiza esquemas de endpoints`).
* `style`: Formatação de código que não altera lógica (espaçamentos, quebras de linha, ponto e vírgula).
* `refactor`: Alteração de código que melhora estrutura sem alterar comportamento visível.
* `test`: Criação ou ajuste de testes automatizados (ex: `test(service): adiciona testes unitários de progresso`).
* `chore`: Mudanças estruturais de build, dependências ou ferramentas de CI.

---

## 6. Preparação para DevOps e Segurança

### 6.1. Segurança contra Vazamento de Credenciais
* **Zero Hardcoded Secrets:** É expressamente proibido inserir senhas de bancos de dados, chaves secretas de assinatura de tokens JWT ou senhas de provedores de e-mail diretamente no código-fonte.
* **Variáveis de Ambiente:** Todas as credenciais devem ser extraídas para variáveis de ambiente e referenciadas via arquivos de configuração seguros.
  * No Spring Boot: Uso do mecanismo `${NOME_DA_VARIAVEL}` no arquivo `application.properties` ou `application.yml`.
  * No React Native (Expo): Uso de variáveis de ambiente com prefixo `EXPO_PUBLIC_` para valores que precisam ser inlined no bundle em build-time (ex: `EXPO_PUBLIC_API_BASE_URL`, `05_DEVOPS_E_SEGURANCA.md` §D), lidas de um arquivo `.env` local não commitado.
* O repositório deve conter um arquivo `.env.example` e um `application-dev.properties` de exemplo com chaves genéricas e seguras para desenvolvimento local. O arquivo `.env` real deve ser ignorado de forma absoluta no `.gitignore`.
* A arquitetura deve garantir que a varredura do **GitGuardian** em pipelines de CI passe limpa sem alertas de falsos positivos de credenciais.

### 6.2. Testabilidade e Modularidade em CI/CD
* O código deve ser isolado para permitir a execução paralela de testes unitários sem dependências de infraestrutura externa (mock de banco de dados e APIs externas).
* Os testes automatizados devem ser fáceis de invocar por comandos diretos nos pipelines do GitLab ou GitHub Actions (ex: `mvn test` para backend, `npm run test` para frontend).

---

## 7. Flexibilidade de Domínio (Requisito Obrigatório)

O núcleo de negócios do IronTrack **não pode ser amarrado exclusivamente ao modelo clássico de musculação** (onde cada série de exercício registra apenas *carga* e *repetições*). O domínio deve suportar nativamente três grandes categorias de evolução física:

### 7.1. Modelagem Genérica e Flexível de Exercícios
Para acomodar a calistenia, o powerlifting tradicional e o treinamento de isometrias complexas, o sistema deve classificar os exercícios sob um modelo tipado e polimórfico:

1. **Treino de Força Tradicional (Musculação/Powerlifting):**
   * Métricas principais: **Carga** (peso em kg ou lbs), **Repetições** alcançadas, **Repetições Alvo** e Técnicas aplicadas (ex: falha, drop-set, rest-pause).
2. **Treino Isométrico (Calistenia/Ginástica):**
   * Métricas principais: **Tempo de Isometria** (segundos mantidos), **Nível de Postura** (ex: tucked, advanced tucked, single leg, full) e **Grau de Esforço** (RPE).
3. **Treino de Habilidade / Progressão de Movimento (Calistenia complexa - Ex: Muscle-Up):**
   * Métricas principais: **Step/Fase de Progressão** (fase incremental de facilitação, ex: Step 1: Barra fixa com impulso, Step 2: Barra fixa explosiva, Step 3: Negativa de Muscle-up, Step 4: Muscle-up completo), **Repetições** e **Nível de Controle**.

### 7.2. Abordagem no Banco de Dados SQLite (Polimorfismo de Séries)
A tabela que registra as séries de uma sessão (`exercise_sets`) deve conter colunas flexíveis e anuláveis, de forma a persistir de forma concisa cada uma dessas categorias sem forçar layouts diferentes ou dados fictícios:

```sql
-- Exemplo conceitual de estrutura flexível para exercise_sets
CREATE TABLE exercise_sets (
    id TEXT PRIMARY KEY,
    session_exercise_id TEXT NOT NULL,
    set_number INTEGER NOT NULL,
    
    -- Campos para Musculação Tradicional
    weight REAL NULL,                   -- Peso utilizado
    reps INTEGER NULL,                  -- Repetições realizadas
    reps_target INTEGER NULL,           -- Repetições planejadas
    
    -- Campos para Calistenia Isométrica
    hold_time_seconds INTEGER NULL,     -- Tempo de isometria mantido
    hold_time_target INTEGER NULL,      -- Tempo de isometria planejado
    
    -- Campos para Progressões de Habilidade
    progression_step_id TEXT NULL,      -- FK para a tabela de passos da progressão do movimento
    
    -- Campo Comum de Esforço
    rpe INTEGER NULL,                   -- Rate of Perceived Exertion (1 a 10)
    notes TEXT NULL,                    -- Técnicas aplicadas ou anotações (ex: "rest-pause")
    
    FOREIGN KEY (session_exercise_id) REFERENCES session_exercises(id),
    FOREIGN KEY (progression_step_id) REFERENCES progression_steps(id)
);
```

### 7.3. Adaptação dos Motores de Progressão
Os serviços responsáveis por sugerir a evolução de treino do usuário (Motor de Sobrecarga Progressiva) devem conter ramificações polimórficas (através de padrões de projeto como *Strategy* ou *Factory*), adaptando-se instantaneamente com base no tipo de exercício registrado:
* **Se o exercício for tradicional:** Sugerir aumento de carga ou repetições.
* **Se o exercício for isométrico:** Sugerir aumento no tempo de sustentação (Hold Time) ou avanço para a postura subsequente de menor facilitação.
* **Se o exercício for de habilidade:** Sugerir evolução para o próximo `progression_step` ou aumento de volume na fase atual.

Esta modularidade garante que a aplicação atenda plenamente aos públicos de musculação tradicional e calistenia avançada.
