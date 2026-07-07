# 12_GLOSSARIO_DE_DOMINIO.md - Glossário de Domínio

Termos de negócio centrais ao **IronTrack**, centralizados para consulta
rápida — a fonte de verdade de cada um continua sendo o documento
referenciado; este glossário é um atalho, não uma redefinição.

| Termo | Definição | Fonte |
| :--- | :--- | :--- |
| **Dupla Progressão** (*Double Progression*) | Método de treino em que o usuário progride primeiro em repetições dentro de uma faixa-alvo, e só aumenta a carga ao esgotar essa faixa em todas as séries planejadas. | `06_LOGICA_DE_PROGRESSAO.md` §A |
| **Série Contável** | Uma série cuja execução é comparável entre sessões para fins de avaliação de progressão — nenhuma técnica de intensificação registrada, ou apenas `FALHA`/`PAUSA`. | `06_LOGICA_DE_PROGRESSAO.md` §B |
| **Série Não-Contável** | Uma série que usou `DROP_SET`, `REST_PAUSE` ou `NEGATIVA_FORCADA` — excluída do cálculo de sugestão de carga, PR e detecção de estagnação (mas continua salva no histórico normalmente). | `06_LOGICA_DE_PROGRESSAO.md` §B |
| **`pesoReferencia`** | O peso da primeira série contável considerada em uma avaliação de progressão — base de comparação para decidir se o teto da faixa de repetições foi atingido. Qualquer série com peso diferente automaticamente impede o CASO 3 (teto atingido). | `06_LOGICA_DE_PROGRESSAO.md` §C, passo 3b |
| **Teto de Repetições** | O valor `targetRepsMax` da faixa-alvo de um exercício — quando atingido em todas as séries planejadas com o mesmo peso, dispara o aumento de carga (CASO 3). | `06_LOGICA_DE_PROGRESSAO.md` §C |
| **CASO 1 / CASO 2 / CASO 3 / CASO 4** | Os 4 ramos do algoritmo de sugestão de carga: sem histórico / histórico insuficiente / teto atingido / teto não atingido. | `06_LOGICA_DE_PROGRESSAO.md` §C |
| **PR de Carga** | O maior peso já registrado em qualquer série contável de um exercício, para um usuário, com pelo menos 1 repetição. | `06_LOGICA_DE_PROGRESSAO.md` §E |
| **PR de Volume** | O maior valor de Σ(peso × repetições) entre séries contáveis, somado dentro de uma única sessão `COMPLETED`, para um exercício. | `06_LOGICA_DE_PROGRESSAO.md` §E |
| **Estagnação** | Um exercício é estagnado quando, nas últimas 3 sessões consecutivas em que foi executado, nem o peso de referência nem a menor repetição contável avançaram. | `06_LOGICA_DE_PROGRESSAO.md` §D |
| **Snapshot de Meta** | A cópia congelada de `targetSets`/`targetRepsMin`/`targetRepsMax` do template do dia (`training_day_exercises`) para `session_exercises`, no instante em que uma sessão começa — imutável mesmo que o template seja editado depois. | `06_LOGICA_DE_PROGRESSAO.md` §1 |
| **Técnica de Intensificação** | `FALHA`, `DROP_SET`, `REST_PAUSE`, `PAUSA` ou `NEGATIVA_FORCADA` — associadas a uma série via `exercise_set_techniques` (N:N). | `02_SCHEMA_SQLITE.md`, `06_LOGICA_DE_PROGRESSAO.md` §B |
| **`clientGeneratedId`** | UUID gerado no cliente no momento do registro de uma série, usado para tornar reenvios de rede idempotentes (a mesma série nunca duplica). | `03_CONTRATOS_API.md` §5.2, `04_FRONTEND_UI_COMPONENTES.md` §E |
| **Ciclo de Treino** (`training_cycle`) | Um programa de treino nomeado (ex: "ABC", "Push/Pull/Legs") com data de início/fim, do qual apenas um pode estar ativo por usuário por vez. | `02_SCHEMA_SQLITE.md`, `03_CONTRATOS_API.md` §3 |
| **Dia de Treino** (`training_day`) | Uma divisão dentro de um ciclo (ex: "Dia A - Push") — um container nomeado e ordenado, não uma sessão executada. | `02_SCHEMA_SQLITE.md` |
| **Template do Dia** (`training_day_exercises`) | A composição planejada de exercícios de um dia de treino — séries/faixa de reps alvo — copiada para `session_exercises` sempre que uma sessão é iniciada a partir daquele dia. | `02_SCHEMA_SQLITE.md`, `03_CONTRATOS_API.md` §3.12 |
| **Sessão de Treino** (`training_session`) | Uma execução real e datada de um dia de treino — do início (`IN_PROGRESS`) à finalização (`COMPLETED`) ou cancelamento (`CANCELLED`). | `02_SCHEMA_SQLITE.md`, `03_CONTRATOS_API.md` §5 |
| **`sessionExercise`** | A instância de um exercício dentro de uma sessão específica — carrega o snapshot de meta e é o que o path param `{sessionExerciseId}` referencia (nunca `{exerciseId}`, que seria o catálogo genérico). | `02_SCHEMA_SQLITE.md`, `03_CONTRATOS_API.md` §5.2 |
| **RPE** (*Rate of Perceived Exertion*) | Escala de 1 a 10 de percepção de esforço de uma série. Armazenado e exibido, mas não participa do cálculo automático de sugestão de carga (decisão deliberada, `06` §G). | `02_SCHEMA_SQLITE.md`, `06_LOGICA_DE_PROGRESSAO.md` §G |
| **Tipo A / Tipo B** | Classificação de conduta para agentes de IA: Tipo A é mecânico/inequívoco (corrige direto); Tipo B é decisão de produto/arquitetura (relata, não decide). | `AGENTS.md` §3 |
