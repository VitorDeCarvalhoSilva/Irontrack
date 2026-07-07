# 06_LOGICA_DE_PROGRESSAO.md - Motor de Sobrecarga Progressiva

Este documento formaliza — de forma **determinística, sem ambiguidade e implementável linha a linha** — o motor de sobrecarga progressiva do **IronTrack**: a regra exata que decide, a cada exercício de cada sessão, se o usuário deve subir carga, subir repetições ou repetir o que fez, além da detecção de estagnação e a definição de recorde pessoal (PR). É este documento que `07_ROADMAP_BACKEND.md` usa para gerar o serviço de domínio real — nenhuma decisão aqui é uma sugestão a ser refinada pela IA de implementação; é a especificação final.

O motor opera exclusivamente sobre dados já contratados em `02_SCHEMA_SQLITE.md` e `03_CONTRATOS_API.md`: nenhuma tabela ou endpoint novo é necessário além do patch de snapshot descrito na Seção 1.

---

## 1. Pré-requisito de Schema: Snapshot Imutável de Meta em `session_exercises`

Ao formalizar o algoritmo da Seção C, identificou-se uma lacuna nos documentos anteriores: `session_exercises` registrava apenas a instância do exercício dentro da sessão (`exercise_id`, `order_index`, `notes`), mas não persistia a meta (`targetSets`/`targetRepsMin`/`targetRepsMax`) copiada do template no momento em que a sessão começa. `POST /sessions/start` apenas *retornava* esses valores uma única vez na response — mas o motor de progressão precisa reconsultá-los sempre que `GET .../suggestion` for chamado durante a sessão, e precisa que esses valores sejam um **snapshot imutável**: se o usuário editar o template do dia (`03_CONTRATOS_API.md` §3.13) no meio de um ciclo, sessões já iniciadas não podem ter sua meta alterada retroativamente.

Este patch já foi aplicado a `02_SCHEMA_SQLITE.md` e `03_CONTRATOS_API.md` como pré-requisito deste documento:

* **`02_SCHEMA_SQLITE.md`:** `session_exercises` ganhou as colunas `training_day_exercise_id` (nullable, `FK ON DELETE SET NULL` — rastreabilidade de origem, vira `NULL` sem quebrar o histórico se o item do template for depois editado/removido), `target_sets`, `target_reps_min` e `target_reps_max` (todas nullable, snapshot congelado no instante da cópia). O diagrama ER (`erDiagram`) e o relacionamento `training_day_exercises ||--o{ session_exercises : "snapshots_into"` já refletem essa mudança.
* **`03_CONTRATOS_API.md`:** a descrição de `POST /sessions/start` (§5.1) já deixa explícito que a cópia do template inclui persistir esses quatro campos em cada linha de `session_exercises` criada — não apenas retorná-los na response. A descrição de `GET .../suggestion` (§6.2) já deixa explícito que a "meta do template" usada no cálculo vem do **snapshot em `session_exercises`**, nunca de uma nova consulta a `training_day_exercises` (que pode já ter mudado desde o início da sessão).

Todo o restante deste documento assume esse snapshot como fonte de verdade da meta durante a sessão.

---

## A) Fundamentação: Método de Dupla Progressão (Double Progression)

O IronTrack implementa o método consagrado de treino conhecido como **dupla progressão** (*double progression*). Para cada exercício, o usuário treina dentro de uma faixa de repetições-alvo (`targetRepsMin`–`targetRepsMax`), definida originalmente no template do dia de treino (`training_day_exercises`, `02_SCHEMA_SQLITE.md`) e congelada como snapshot em `session_exercises` no momento em que cada sessão específica começa (Seção 1 acima). A cada sessão, o usuário tenta adicionar repetições dentro dessa faixa, mantendo a carga estável; ao atingir o topo da faixa (`targetRepsMax`) em **todas** as séries planejadas (`targetSets`) com a mesma carga, a carga sobe (em um incremento definido por exercício) e a meta de repetições volta à base da faixa (`targetRepsMin`). É uma progressão em duas dimensões — carga e repetições — daí o nome: o usuário nunca fica "parado" tentando adicionar carga arbitrariamente sem primeiro esgotar a faixa de repetições disponível, o que produz uma curva de progressão mais sustentável e menos propensa a estagnação prematura por tentativas de saltos de carga grandes demais.

As fontes de dado envolvidas em todo o cálculo formalizado por este documento são exatamente quatro, todas já existentes nos contratos anteriores:

1. **`training_day_exercises`** — a meta planejada original (a "receita" do dia de treino).
2. **`session_exercises`** — o snapshot imutável da meta no momento em que uma sessão específica começou (Seção 1), mais o vínculo com a sessão e o exercício executado.
3. **`exercise_sets` + `exercise_set_techniques`** — a execução real: o que o usuário de fato levantou, quantas repetições, e quais técnicas de intensificação (se alguma) foram aplicadas em cada série.
4. **`exercise_library.load_increment_kg`** — o tamanho do incremento de carga a aplicar quando o teto da faixa de repetições é atingido, configurável por exercício (`02_SCHEMA_SQLITE.md`).

---

## B) Classificação de Séries: "Contável" vs "Não-Contável"

Nem toda série executada é comparável entre sessões para fins de avaliação de progressão. Uma série com técnica de intensificação (ex: um `DROP_SET` que artificialmente estende o número de repetições reduzindo a carga no mesmo fôlego) não reflete a mesma capacidade que uma série direta ("straight set") executada até a falha natural ou dentro da meta planejada. Por isso, toda série é classificada como **contável** ou **não-contável** para fins de cálculo do motor de progressão — essa classificação **nunca** afeta a persistência (todas as séries continuam sendo salvas normalmente no histórico e exibidas ao usuário), apenas o que entra nas fórmulas das Seções C, D e E.

A regra de classificação, por técnica registrada em `exercise_set_techniques.technique`, é a seguinte:

| Técnica (`exercise_set_techniques.technique`) | Contável para avaliação de teto? | Justificativa |
| :--- | :--- | :--- |
| *(nenhuma técnica registrada)* | Sim | Série direta ("straight set"), a unidade padrão de comparação. |
| `FALHA` | Sim | Falha concêntrica natural ao fim de uma série contínua — ainda é uma única série comparável, não estende o esforço além da série. |
| `PAUSA` | Sim | Variação de tempo sob tensão (rep com pausa), não altera o número de repetições úteis executadas de forma contínua. |
| `DROP_SET` | Não | Estende o esforço além de uma única série contínua (redução de carga no mesmo fôlego) — infla artificialmente a percepção de "repetições alcançadas" e não é comparável a uma série direta. |
| `REST_PAUSE` | Não | Mesma razão: mini-pausas dentro da mesma série para somar mais repetições — não é uma medida direta de força na carga registrada. |
| `NEGATIVA_FORCADA` | Não | Depende de assistência de um parceiro na fase excêntrica — a repetição não reflete a capacidade do usuário sozinho. |

**Regra de composição para múltiplas técnicas:** como `exercise_set_techniques` permite uma relação N:N (uma série pode ter mais de uma técnica associada), uma série com múltiplas técnicas é **não-contável se qualquer uma** das técnicas presentes for não-contável — a classificação não-contável sempre "vence" em caso de combinação (ex: uma série marcada como `FALHA` **e** `DROP_SET` simultaneamente é não-contável, porque `DROP_SET` está presente).

---

## C) Algoritmo de Sugestão de Carga

O algoritmo abaixo é executado pelo endpoint `GET /sessions/{sessionId}/exercises/{sessionExerciseId}/suggestion` (`03_CONTRATOS_API.md` §6.2), formalizado como pseudocódigo determinístico — esta é a especificação final, não um esboço:

```
ENTRADA: sessionExerciseId (da sessão ATIVA, IN_PROGRESS)

1. Carregar sessionExercise (target_sets, target_reps_min, target_reps_max
   — snapshot, Seção 1) e exercise_library.load_increment_kg do exercício
   associado.

2. Localizar a sessão anterior de referência:
   - Buscar a sessão mais recente com status = COMPLETED, para o mesmo
     user_id e o mesmo training_day_id (índice idx_sessions_user_day_start),
     que seja ANTERIOR à sessão atual.
   - Dentro dela, localizar o session_exercise cujo exercise_id seja igual
     ao exercise_id do sessionExercise atual (mesma lógica usada por
     lastPerformance em POST /sessions/start).
   - Se não existir sessão anterior, OU o exercício não tiver sido
     executado nela → ir para o CASO 1 (sem histórico).

3. Filtrar as séries (exercise_sets) desse session_exercise anterior:
   manter apenas as CONTÁVEIS (Seção B).

3a. Se o número de séries contáveis for MAIOR que target_sets (séries bônus
    registradas além do planejado), considerar apenas as PRIMEIRAS
    target_sets séries contáveis, em ordem crescente de set_number — séries
    bônus além da meta permanecem no histórico normalmente, mas não
    participam desta avaliação de progressão.

3b. Definir pesoReferencia = peso da primeira série contável considerada
    (após o corte do passo 3a). É o peso-base de comparação para os CASOs
    3 e 4 abaixo. Qualquer série contável considerada cujo peso seja
    DIFERENTE de pesoReferencia nunca pode, sozinha, indicar teto atingido
    — a presença de qualquer variação de peso entre as séries consideradas
    força automaticamente o CASO 4 (nunca o CASO 3), independentemente de
    quantas repetições essa série com peso diferente tenha alcançado.

4. CASO 1 — Sem histórico (nenhuma sessão anterior ou nenhuma série
   contável na sessão anterior):
   targetWeight = null
   targetReps   = target_reps_min (snapshot)
   basis = "Primeira vez executando este exercício (ou sessão anterior sem
            séries válidas para comparação) — registre seu desempenho hoje
            visando {target_reps_min} a {target_reps_max} repetições por
            série para receber sugestões de carga a partir da próxima vez."

5. CASO 2 — Histórico insuficiente (existem séries contáveis, mas em
   quantidade MENOR que target_sets do snapshot ANTERIOR — ou seja, a
   sessão anterior foi interrompida/parcial para este exercício):
   targetWeight = peso da última série contável registrada (mantém)
   targetReps   = target_reps_max da sessão anterior (tenta completar a
                  faixa que não foi concluída)
   basis = "Na sessão de {data}, você completou apenas {N} de {M} séries
            planejadas para este exercício. Repita {targetWeight}kg e
            complete as {target_sets} séries dentro da faixa de
            {target_reps_min}-{target_reps_max} repetições."

6. CASO 3 — Teto atingido (há exatamente target_sets séries contáveis
   consideradas — após os cortes dos passos 3a/3b —, TODAS com peso ==
   pesoReferencia, E TODAS com reps >= target_reps_max):
   incremento = exercise_library.load_increment_kg
   targetWeight = arredondar(pesoReferencia + incremento, para o múltiplo
                  de 1.25 mais próximo — ver Seção C.1 abaixo)
   targetReps   = target_reps_min (snapshot ATUAL, reinicia a faixa)
   basis = "Na sessão de {data}, você atingiu o teto de {target_reps_max}
            repetições em todas as {target_sets} séries com
            {pesoReferencia}kg. Para manter a sobrecarga progressiva, o
            sistema sugere {targetWeight}kg (incremento de {incremento}kg)
            buscando {target_reps_min} repetições."

7. CASO 4 — Teto não atingido (há >= target_sets séries contáveis
   originalmente, mas a condição do CASO 3 não foi satisfeita — seja
   porque ao menos uma série considerada com peso == pesoReferencia tem
   reps < target_reps_max, seja porque há variação de peso entre as
   séries consideradas, seja porque, após o corte do passo 3a, sobrou
   menos que target_sets séries com peso == pesoReferencia):
   targetWeight = pesoReferencia (mantém — séries com peso diferente do
                  de referência são tratadas como tentativas à parte, não
                  contam para esta comparação)
   menorReps    = a MENOR quantidade de reps entre as séries consideradas
                  (passo 3a) cujo peso == pesoReferencia (séries com peso
                  diferente são ignoradas nesta subcomparação — não são
                  comparáveis entre si)
   targetReps   = min(menorReps + 1, target_reps_max)
   basis = "Na sessão de {data}, você alcançou {menorReps} repetições na
            série mais fraca com {pesoReferencia}kg (faixa alvo:
            {target_reps_min}-{target_reps_max}). Mantenha o peso e busque
            {targetReps} repetições em todas as séries hoje."

8. Retornar { sessionExerciseId, exerciseId, targetWeight, targetReps,
   basis } conforme o contrato de 03_CONTRATOS_API.md §6.2.
```

Este cálculo é **determinístico e idempotente**: chamado múltiplas vezes para o mesmo `sessionExerciseId` sem novas séries registradas na sessão anterior, sempre retorna o mesmo resultado — por isso o frontend pode cachear a resposta livremente (`04_FRONTEND_UI_COMPONENTES.md` já trata `ExerciseCardProps.suggestion` como um valor cacheável). A sugestão é calculada **uma vez por exercício por sessão**, com base apenas na sessão *anterior* concluída — séries já registradas *nesta mesma sessão em andamento* não realimentam o cálculo: a meta exibida para "hoje" é fixa do início ao fim do exercício dentro da sessão atual, mesmo que o usuário já tenha registrado 2 das 3 séries planejadas quando consultar novamente a sugestão.

### C.1. Arredondamento de Carga Sugerida

Todo `targetWeight` sugerido pelo CASO 3 é arredondado para o **múltiplo de 1,25 kg mais próximo** — a menor fração de anilha padrão disponível na maioria das academias — usando arredondamento **para cima** em caso de empate exato entre dois múltiplos (ex: um incremento que resultasse em exatamente `x,625` arredondaria para cima, até o próximo múltiplo de `1,25`, nunca para baixo). Essa regra garante que toda sugestão de carga seja **fisicamente montável** com anilhas padrão de academia, independentemente do valor bruto resultante de somar `load_increment_kg` ao peso base — evita que o sistema sugira, por exemplo, `82,3kg`, um valor impossível de montar com anilhas convencionais.

---

## D) Detecção de Estagnação

A regra de negócio que define o que conta como "exercício estagnado" é formalizada de forma definitiva neste documento. A **materialização** disso como alerta persistente (tabela `stagnation_alerts`, endpoints `GET /alerts` e `POST /alerts/{alertId}/snooze`) já foi implementada em revisão posterior — ver `02_SCHEMA_SQLITE.md` e `03_CONTRATOS_API.md` §7.1-§7.2 — mas a lógica de detecção em si (o que conta como estagnado) continua definida exclusivamente aqui, não deve ser reinterpretada em `02`/`03`:

> Um exercício é considerado **estagnado** quando, nas últimas **3 sessões concluídas consecutivas** em que ele foi executado (dentro do mesmo `training_day_id`, espaçadas por pelo menos 3 semanas corridas entre a primeira e a última dessas 3 ocorrências — alinhado ao critério de "3 semanas sem progressão" do `00_PRD_IRONTRACK.md` §4.6), **nenhuma** das duas condições abaixo ocorreu:
> 1. Aumento do peso de referência (o CASO 3 do algoritmo da Seção C foi disparado em pelo menos uma dessas sessões); OU
> 2. Aumento da menor repetição contável registrada em relação à sessão imediatamente anterior (o usuário avançou dentro da faixa, CASO 4).
>
> Se ambas as condições permanecerem estáveis (ou regredirem) pelas 3 ocorrências consecutivas, o exercício deve ser sinalizado como estagnado.

Essa avaliação é naturalmente derivável **reaplicando o algoritmo da Seção C** às últimas 3 ocorrências e comparando os resultados entre si — não exige nenhum dado adicional além do que já existe em `exercise_sets`/`session_exercises`. Não há necessidade de uma tabela de histórico de "eventos de progressão" separada: o próprio histórico de sessões já contém tudo o que a detecção de estagnação precisa recalcular sob demanda — o gatilho síncrono ao final de `PATCH /sessions/{id}/finish` (`03_CONTRATOS_API.md` §7.1) apenas persiste o *resultado* dessa reavaliação em `stagnation_alerts`, não um log de eventos intermediários. A regra de "o que conta como estagnado" está definida aqui de forma definitiva e não deve ser reinterpretada em `02`/`03`/`07` sem uma decisão de produto explícita.

---

## E) Definição de Recorde Pessoal (PR)

Dois tipos de PR são formalizados aqui — o mesmo conceito que `04_FRONTEND_UI_COMPONENTES.md` já implementa parcialmente na UI (`isPersonalRecord` em `SetRowProps`/`ExerciseCardProps`) e que o endpoint `GET /metrics/exercises/{exerciseId}/pr` (`03_CONTRATOS_API.md` §6.3) expõe:

1. **PR de Carga:** o maior `weight` já registrado em qualquer série **contável** (Seção B) daquele `exercise_id`, para aquele usuário, independentemente do número de repetições (desde que `reps >= 1`). Um PR de Carga é, por definição, sobre uma única série — não uma soma.
2. **PR de Volume (sessão única):** o maior valor de `Σ (weight × reps)` somado entre todas as séries **contáveis** de um mesmo `exercise_id` dentro de uma única sessão `COMPLETED`. Ao contrário do PR de Carga, este é cumulativo dentro de uma sessão inteira, não de uma série isolada.

**Regra de exibição em tempo real (frontend):** ao confirmar uma série (`onConfirm` em `SetRow`), o frontend marca a série recém-confirmada com `isPersonalRecord = true` se o `weight` sozinho já superar o PR de Carga anterior daquele exercício para o usuário. A verificação de PR de Volume só é relevante no resumo pós-sessão — exibida após `PATCH /sessions/{id}/finish` (`03_CONTRATOS_API.md` §5.5), nunca série a série — porque volume é uma soma acumulada ao longo de toda a sessão, e só está completa quando a sessão é finalizada.

**Séries não-contáveis (Seção B) nunca contam para PR de nenhum tipo** — nem de Carga, nem de Volume. Essa restrição evita que um `DROP_SET` ou `REST_PAUSE`, que naturalmente permitem acumular mais peso×repetições através de artifícios de execução, "inflem" artificialmente um recorde que não reflete um ganho real de força ou capacidade de trabalho.

---

## F) Camada de Implementação Recomendada (Backend)

Alinhado aos princípios já fixados em `01_ARQUITETURA_E_PADROES.md` §2.3 (injeção de dependência via construtor, Princípio da Responsabilidade Única, Princípio Aberto/Fechado para motores de cálculo), a implementação recomendada é um **serviço de domínio dedicado**, ex. `ProgressiveOverloadService`, com dois métodos públicos:

* `calculateSuggestion(sessionExerciseId): SuggestionResult` — implementa exatamente o algoritmo da Seção C (incluindo o arredondamento da Seção C.1), sem lógica adicional além do que está formalizado ali.
* `detectStagnation(userId, trainingDayId, exerciseId): boolean` — implementa a regra da Seção D, reaplicando `calculateSuggestion`-equivalente sobre as últimas 3 ocorrências para verificar as duas condições de não-estagnação.

A classificação "contável vs não-contável" (Seção B) deve virar um **método utilitário puro e testável isoladamente**, ex. `SetCountabilityRules.isCountable(techniques: Set<Technique>): boolean` — por ser uma regra pura sem I/O (recebe um conjunto de técnicas, retorna um booleano), é trivialmente coberta por testes unitários sem exigir banco de dados em memória, contribuindo diretamente para a cobertura mínima de 80% exigida pela Definition of Done (`00_PRD_IRONTRACK.md` §4.8).

A consulta descrita no passo 2 da Seção C (localizar a sessão anterior de referência) deve reutilizar o índice `idx_sessions_user_day_start` (`02_SCHEMA_SQLITE.md`), já existente para exatamente esse propósito — nenhum novo índice é necessário para este motor.

---

## G) Casos de Borda Adicionais

Cada caso de borda abaixo tem uma regra definitiva — nenhum é deixado como "a definir":

* **Sessão anterior `CANCELLED`:** nunca é usada como referência — o algoritmo (Seção C, passo 2) só considera sessões `COMPLETED`. Uma sessão `CANCELLED` é tratada como se não existisse para fins de progressão.
* **Exercício trocado no template entre sessões:** a busca por "sessão anterior" (Seção C, passo 2) compara por `exercise_id` explicitamente — se o dia de treino trocou de exercício (ex: Supino Reto → Supino Inclinado) entre uma sessão e outra, não há continuidade de progressão entre eles; o novo exercício cai no CASO 1 (sem histórico) na primeira vez que for executado, mesmo que o `training_day_id` seja o mesmo.
* **Faixa de repetições do template alterada entre sessões:** irrelevante para o cálculo — a Seção C sempre usa o **snapshot** de `session_exercises` (`target_reps_min`/`target_reps_max` no momento em que cada sessão específica começou), nunca o template "atual" no momento da consulta. Isso é exatamente o que a Seção 1 deste documento (patch de schema) garante.
* **Múltiplos `session_exercises` para o mesmo `exercise_id` na mesma sessão (bi-set/superset deliberado):** o algoritmo trata cada `sessionExerciseId` de forma independente — a "sessão anterior de referência" (Seção C, passo 2) busca por `exercise_id` na sessão anterior sem se importar com qual `session_exercise` específico da sessão atual está pedindo a sugestão; se houver ambiguidade (o exercício apareceu mais de uma vez na sessão anterior também), usa-se a primeira ocorrência por `order_index`.
* **Unidade de peso:** todo o algoritmo assume quilogramas (`kg`), consistente com `weight REAL` em `02_SCHEMA_SQLITE.md` e os exemplos de `03_CONTRATOS_API.md` — não há suporte a libras (`lb`) nesta versão do produto; não deve ser introduzida conversão de unidades.
* **RPE não participa do cálculo automático:** o campo `rpe` é armazenado e exibido ao usuário como informação de apoio (inclusive usado pela UI para feedback visual, `04_FRONTEND_UI_COMPONENTES.md` §C.3), mas **não** entra em nenhuma fórmula da Seção C nesta versão — é uma decisão deliberada para manter o motor determinístico e simples de testar no MVP. Fica registrado como uma nota de possível refinamento futuro (auto-regulação por RPE), mas nada além de manter o campo disponível deve ser implementado agora.
* **Séries contáveis com pesos diferentes entre si (ex: usuário reduz a carga na última série por fadiga, sem marcar nenhuma técnica de intensificação):** qualquer variação de peso entre as séries consideradas (Seção C, passo 3b) impede o CASO 3 — a presença de peso diferente do `pesoReferencia` (o peso da primeira série contável) automaticamente qualifica a avaliação para o CASO 4, mesmo que a série com peso diferente tenha atingido `target_reps_max`. Isso evita que uma variação não-intencional de carga seja interpretada como "teto atingido".
* **Séries bônus além de `targetSets`:** quando o usuário registra mais séries contáveis do que o planejado, apenas as primeiras `targetSets` séries contáveis (por `set_number` crescente) participam da avaliação de progressão (Seção C, passo 3a) — séries extras ficam no histórico normalmente, mas nunca influenciam a sugestão da próxima sessão, positiva ou negativamente.

---

## H) Exemplos Numéricos Completos

Os quatro cenários abaixo cobrem os casos mais relevantes do algoritmo da Seção C e servem como casos de teste de referência para a implementação do `ProgressiveOverloadService` (Seção F).

### H.1. Cenário 1 — Teto Atingido (CASO 3)

**Entrada:** `sessionExercise` com meta (snapshot) `targetSets = 3`, `targetRepsMin = 8`, `targetRepsMax = 12`. Na sessão `COMPLETED` anterior, o mesmo exercício teve 3 séries contáveis, todas a `80kg`: `12`, `12` e `12` repetições. `exercise_library.load_increment_kg = 2.5`.

**Avaliação:** 3 séries contáveis `>= targetSets` (3), e todas com `reps >= targetRepsMax` (12) ao mesmo peso (80kg) → **CASO 3 (teto atingido)**.

**Cálculo:** `targetWeight = arredondar(80 + 2.5, múltiplo de 1.25) = 82.5`. `targetReps = targetRepsMin (snapshot atual) = 8`.

**Response `GET .../suggestion`:**
```json
{
  "sessionExerciseId": "sexe-e101",
  "exerciseId": "exe-0001-bench",
  "targetWeight": 82.5,
  "targetReps": 8,
  "basis": "Na sessão de 2026-06-24, você atingiu o teto de 12 repetições em todas as 3 séries com 80kg. Para manter a sobrecarga progressiva, o sistema sugere 82.5kg (incremento de 2.5kg) buscando 8 repetições."
}
```

### H.2. Cenário 2 — Teto Não Atingido (CASO 4)

**Entrada:** mesma meta (`targetSets = 3`, `targetRepsMin = 8`, `targetRepsMax = 12`). Na sessão anterior, 3 séries contáveis a `80kg`: `12`, `10` e `9` repetições.

**Avaliação:** 3 séries contáveis `>= targetSets` (3), mas nem todas atingiram `targetRepsMax` (12) — a série de `9` reps não atingiu o teto → **CASO 4 (teto não atingido)**.

**Cálculo:** `targetWeight = 80` (mantém). `menorReps = 9` (a menor entre 12, 10, 9). `targetReps = min(9 + 1, 12) = 10`.

**Response `GET .../suggestion`:**
```json
{
  "sessionExerciseId": "sexe-e101",
  "exerciseId": "exe-0001-bench",
  "targetWeight": 80,
  "targetReps": 10,
  "basis": "Na sessão de 2026-06-24, você alcançou 9 repetições na série mais fraca com 80kg (faixa alvo: 8-12). Mantenha o peso e busque 10 repetições em todas as séries hoje."
}
```

### H.3. Cenário 3 — Sessão Anterior com Técnica Não-Contável (CASO 2, demonstrando a Seção B em ação)

**Entrada:** mesma meta (`targetSets = 3`, `targetRepsMin = 8`, `targetRepsMax = 12`). Na sessão anterior, 3 séries foram registradas a `80kg`: série 1 com `12` repetições (sem técnica), série 2 com `11` repetições (sem técnica), e série 3 com `15` repetições marcada com a técnica `DROP_SET`.

**Avaliação:** aplicando a Seção B, a série 3 (`DROP_SET`) é **não-contável** e é excluída da avaliação, restando apenas 2 séries contáveis (série 1 e série 2). Como `2 < targetSets (3)`, o histórico é insuficiente → **CASO 2**, mesmo que a série excluída sozinha (`15` reps, acima do teto de `12`) pudesse sugerir erroneamente que o teto foi atingido caso fosse contabilizada. Isso demonstra por que a classificação da Seção B é aplicada **antes** de qualquer outra avaliação do algoritmo.

**Cálculo:** `targetWeight = 80` (peso da última série contável, a série 2). `targetReps = targetRepsMax da sessão anterior = 12` (tenta completar a faixa que não foi concluída com número pleno de séries contáveis).

**Response `GET .../suggestion`:**
```json
{
  "sessionExerciseId": "sexe-e101",
  "exerciseId": "exe-0001-bench",
  "targetWeight": 80,
  "targetReps": 12,
  "basis": "Na sessão de 2026-06-24, você completou apenas 2 de 3 séries planejadas para este exercício. Repita 80kg e complete as 3 séries dentro da faixa de 8-12 repetições."
}
```

### H.4. Cenário 4 — Peso Heterogêneo entre Séries Contáveis (CASO 4, demonstrando a Seção C, passo 3b)

**Entrada:** meta `targetSets = 3`, `targetRepsMin = 8`, `targetRepsMax = 12`. Sessão anterior: 3 séries contáveis — série 1 a `82.5kg` com `8` reps, série 2 a `80kg` com `10` reps, série 3 a `80kg` com `10` reps.

**Avaliação:** `pesoReferencia = 82.5kg` (peso da primeira série contável). As séries 2 e 3 têm peso diferente (`80kg ≠ 82.5kg`) → força CASO 4, independentemente de quantas repetições cada uma alcançou.

**Cálculo:** `targetWeight = 82.5` (mantém `pesoReferencia`). Considerando apenas séries com peso `== pesoReferencia` (apenas a série 1): `menorReps = 8`. `targetReps = min(8 + 1, 12) = 9`.

**Response `GET .../suggestion`:**
```json
{
  "sessionExerciseId": "sexe-e101",
  "exerciseId": "exe-0001-bench",
  "targetWeight": 82.5,
  "targetReps": 9,
  "basis": "Na sessão de 2026-06-24, você alcançou 8 repetições na série mais fraca com 82.5kg (faixa alvo: 8-12). Mantenha o peso e busque 9 repetições em todas as séries hoje."
}
```
Este cenário demonstra por que a Seção C exige peso idêntico entre todas as séries consideradas para o CASO 3 — uma única série com peso diferente, mesmo que tenha atingido o teto de repetições, não é suficiente para justificar o aumento de carga.
