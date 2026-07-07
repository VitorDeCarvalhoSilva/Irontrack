# 11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md - Privacidade e Retenção de Dados (LGPD)

> **Nota de escopo:** este documento estabelece uma política **técnica**
> padrão e defensável para orientar a implementação, com base no direito de
> eliminação previsto na LGPD (Lei 13.709/2018, art. 18, VI). Ele **não
> substitui validação jurídica formal** antes do lançamento em produção —
> trate os prazos e mecanismos abaixo como o padrão de engenharia a
> implementar, sujeito a ajuste por orientação legal antes do go-live.

---

## A) Dados Pessoais Coletados

O IronTrack coleta e processa: nome, e-mail, senha (hash, nunca em texto
plano), e um histórico detalhado de desempenho físico (ciclos de treino,
sessões, séries executadas — carga, repetições, RPE, técnicas aplicadas).
Este último é dado sensível o suficiente para merecer o mesmo rigor de
proteção que dado de saúde, mesmo não sendo formalmente classificado como
"dado sensível" pela LGPD (art. 5º, II) — é dado pessoal detalhado sobre o
corpo e desempenho físico do titular.

## B) Direito de Eliminação — Fluxo de Exclusão de Conta

1. **Solicitação:** o usuário solicita exclusão via `DELETE /api/v1/users/me`
   (novo endpoint, ver patch na Seção D).
2. **Soft-delete imediato (Período de Carência de 30 dias):** a conta é
   marcada com `deletion_requested_at = now()` (nova coluna em `users`, ver
   Seção D). A partir desse instante: login é bloqueado imediatamente
   (mesmo efeito de `403 Forbidden` do e-mail não verificado); todos os
   `refresh_tokens` e `push_subscriptions` do usuário são revogados/removidos
   de imediato. Os dados **não são apagados ainda** — o período de carência
   existe para cobrir o caso de solicitação acidental ou conta comprometida
   por terceiro mal-intencionado.
3. **Cancelamento da exclusão:** dentro dos 30 dias, o usuário pode
   solicitar a reativação (via um fluxo equivalente ao "esqueci minha
   senha" — comprovação de posse do e-mail cadastrado) para reverter
   `deletion_requested_at` para `NULL`.
4. **Exclusão física (hard delete) após 30 dias:** um job agendado
   (`@Scheduled`, diário) varre `users` com `deletion_requested_at` mais
   antigo que 30 dias e executa `DELETE FROM users WHERE id = ...`. Graças
   às restrições `ON DELETE CASCADE` já definidas em `02_SCHEMA_SQLITE.md`
   para praticamente todas as tabelas dependentes de `users` (`training_cycles`,
   `training_sessions`, `refresh_tokens`, `stagnation_alerts`,
   `push_subscriptions`), a exclusão em cascata remove automaticamente todo
   o histórico de treino associado — carga, repetições, séries, técnicas.
   `exercise_library.user_id` usa `ON DELETE SET NULL` deliberadamente
   (Seção C abaixo explica por quê).
5. **Sem anonimização/retenção parcial:** este projeto **não** opta por
   anonimizar e reter dados agregados para fins estatísticos — a exclusão é
   completa. Justificativa: manter qualquer vínculo, mesmo anonimizado,
   adiciona complexidade de conformidade (é preciso provar que a
   anonimização é irreversível) sem benefício de produto suficiente no
   estágio atual do IronTrack.

## C) Exceção: Exercícios Customizados Criados pelo Usuário Excluído

`exercise_library.user_id` usa `ON DELETE SET NULL` (não `CASCADE`) porque
um exercício customizado pode ter sido referenciado por `session_exercises`
de **sessões já registradas** (o próprio histórico do usuário que está
sendo apagado, então não é um problema de terceiros neste caso — mas o
design da coluna é compartilhado). Ao excluir o usuário, seus exercícios
customizados deixam de ter `user_id` (viram órfãos, `isCustom` continua
`true` mas sem dono) — como as sessões que os referenciam já foram
apagadas em cascata pelo passo anterior, na prática esses exercícios órfãos
ficam inacessíveis a qualquer usuário e podem ser removidos por uma rotina
de limpeza periódica (não crítica, não bloqueia o fluxo principal de
exclusão).

## D) Patch Necessário (aplicado por este documento)

### `02_SCHEMA_SQLITE.md`
```sql
ALTER TABLE users ADD COLUMN deletion_requested_at TEXT;
```
Nullable — `NULL` significa conta ativa normalmente; preenchido significa
"em período de carência de exclusão".

### `03_CONTRATOS_API.md`
Novo endpoint, Seção 2 (Autenticação e Usuários):
```
DELETE /api/v1/users/me
POST   /api/v1/auth/cancel-deletion
```
* **`DELETE /users/me`** — Request: `{ "password": "SenhaAtual123!" }`
  (reconfirma a senha atual como proteção contra sequestro de sessão ativa
  sem o usuário presente). Seta `deletion_requested_at = now()`, revoga
  todos os `refresh_tokens` e remove `push_subscriptions`. Response `202
  Accepted` com `{ "deletionScheduledFor": "2026-08-06T00:00:00.000Z" }`
  (data = agora + 30 dias).
* **`POST /auth/cancel-deletion`** — mesmo mecanismo de prova de posse
  de `POST /auth/forgot-password` (link por e-mail). Seta
  `deletion_requested_at = NULL`. Response `200 OK`.
* Login (`POST /auth/login`, §2.2) passa a retornar `403 Forbidden` também
  quando `deletion_requested_at IS NOT NULL` (mesma família de erro do
  e-mail não verificado — conta indisponível para uso).

## E) Backups e o Direito de Eliminação

O backup do SQLite (`05_DEVOPS_E_SEGURANCA.md` §E.4) roda a cada 6 horas com
retenção local de 14 dias e replicação externa. Isso significa que os dados
de um usuário excluído podem persistir em **backups** por até
aproximadamente 14 dias após a exclusão física do banco principal — isso é
uma prática amplamente aceita de mercado (a alternativa, expurgar
retroativamente de cada snapshot de backup individualmente, tem custo de
engenharia desproporcional ao risco para um produto neste estágio), desde
que **divulgada** ao usuário. O texto de confirmação de exclusão (passo B.4)
deve informar isso explicitamente: *"Seus dados serão removidos do sistema
principal em até 30 dias após a solicitação. Backups de segurança podem
reter uma cópia por até 14 dias adicionais, após os quais são
sobrescritos."*

## F) Retenção de Contas Inativas (sem ação do usuário)

O IronTrack **não** exclui automaticamente contas por inatividade — a
exclusão só ocorre mediante solicitação explícita do usuário (Seção B). Os
dados de treino são o núcleo do valor do produto para o próprio usuário
(histórico de longo prazo é o diferencial do app), então retenção
indefinida de uma conta inativa, sem solicitação de exclusão, é consistente
com a finalidade declarada do tratamento de dados.

## G) Consentimento e Transparência

O fluxo de `POST /auth/register` (`03_CONTRATOS_API.md` §2.1) deve
apresentar um link para uma futura Política de Privacidade voltada ao
usuário final (documento de produto/legal, distinto deste documento técnico
— fora do escopo de `docs/`, que é documentação interna de engenharia) antes
da criação da conta.
