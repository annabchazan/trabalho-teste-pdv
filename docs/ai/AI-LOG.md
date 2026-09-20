# Registro de Uso de IA (AI-LOG)

Registro das interações com ferramentas de IA Generativa que contribuíram
substancialmente para os artefatos deste trabalho (disciplina Qualidade e Teste).
Correções ortográficas e configurações pontuais de IDE não são registradas aqui.

## Entradas

### 2026-09-11 — Mapeamento de classes para a Entrega 1

- **Ferramenta:** Claude Code (Anthropic, modelo Sonnet 5)
- **Membro responsável:** Anna Chazan
- **Contexto/Prompt (resumo):** Pedido de apoio para analisar a estrutura do
  software-alvo (`repo-software-testing-courses/pdv`) e identificar classes
  candidatas a "alta complexidade" (não-CRUD), para apoiar a distribuição
  entre os 4 integrantes.
- **Artefatos afetados:** Nenhum arquivo do repositório foi gerado ou alterado
  pela IA; uso restrito ao mapeamento/listagem de classes.
- **Validação realizada:** Ranking de complexidade foi feito por contagem
  automatizada de pontos de decisão (`if/for/while/case/catch/&&/||`) via script
  de linha de comando — não substitui medição formal de complexidade
  ciclomática (a ser feita com ferramenta dedicada, ex. `checkstyle`/`PMD`/
  plugin de cobertura, na Entrega 2).

### 2026-09-17 — Apoio na execução manual dos casos de teste CT-NF-01 e CT-NF-02

- **Ferramenta:** Claude Code (Anthropic, modelo Sonnet 5)
- **Membro responsável:** Anna Chazan
- **Contexto/Prompt (resumo):** Pedido de apoio para executar manualmente os
  casos de teste CT-NF-01 e CT-NF-02. A IA investigou o banco de dados e o
  código-fonte (controllers, templates, JS) para levantar os pré-requisitos de
  dados necessários (Empresa, Tributação, Regra fiscal, Produto configurado)
  antes da execução. Quando surgiram erros inesperados durante a execução
  (CNPJ inválido, campo sem cidade, alíquota de PIS vazia, Modalidade BC ICMS
  faltando), a IA consultou os logs do servidor para identificar a causa raiz
  técnica de cada erro, em vez de propor correções por tentativa e erro.
- **Artefatos afetados:** Nenhum código de produção foi alterado; uso restrito
  à preparação de massa de dados e ao diagnóstico de erros observados durante
  a execução manual dos casos de teste.
- **Validação realizada:** Cada causa raiz apontada pela IA foi confirmada
  contra os logs do servidor e o estado real do banco de dados antes de ser
  aceita; os testes CT-NF-01 e CT-NF-02 foram executados manualmente pelo
  responsável após o ajuste da massa de dados.

### 2026-09-17 — Criação e correção de testes unitários de NotaFiscalItemService

- **Ferramenta:** Claude Code (Anthropic, modelo Sonnet 5)
- **Membro responsável:** Anna Chazan
- **Contexto/Prompt (resumo):** Apoio na criação de testes unitários em
  `NotaFiscalItemServiceTest.java`, incluindo configuração de mocks, usuário
  autenticado e asserções. A IA também revisou se `NotaFiscalItemServiceTest.java`
  cobria os pontos importantes de `NotaFiscalItemService.java`, e ajudou a
  corrigir os testes criados no mesmo dia.
- **Artefatos afetados:**
  `src/test/java/net/originmobi/pdv/service/notafiscal/NotaFiscalItemServiceTest.java`.
- **Validação realizada:** Cobertura dos cenários de
  `NotaFiscalItemService.java` foi revisada manualmente pelo responsável;
  suíte de testes executada localmente para confirmar que os testes criados e
  corrigidos passam.

### 2026-09-20 — Testes unitários de RecebimentoService e relatório de defeitos

- **Ferramenta:** Claude Code (Anthropic, modelo Opus 5)
- **Membro responsável:** João Portela
- **Contexto/Prompt (resumo):** Pedido de apoio para analisar o estado do
  repositório e o Plano de Teste do grupo, identificar qual classe ainda não
  tinha dono (`RecebimentoService`) e projetar a suíte de testes unitários dessa
  classe. A instrução dada à IA foi explícita em dois pontos: (1) **ler
  `RecebimentoService.java` e todas as suas dependências antes de escrever
  qualquer teste**, mapeando as armadilhas de testabilidade; e (2) **não alterar
  nenhum arquivo de `src/main`** — defeitos encontrados deveriam virar Issue, e
  não correção silenciosa que faria o teste passar escondendo o problema.
- **Artefatos afetados:**
  - `src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java`
    (24 casos, TU-REC-01 a TU-REC-24)
  - `docs/bugs/defeitos-recebimento.md` (defeitos D1, D2 e D3)
  - `docs/testes-manuais/CT-REC-01.md` (CT-REC-01 a CT-REC-03, projetados)
  - `docs/ai/snapshots/RecebimentoServiceTest.v1-ia.java` (versão inicial preservada)
  - `docs/ai/revisao-recebimento-service.md` (descrição das alterações v1 → final)
- **Resultado da IA (resumo):** suíte cobrindo os três métodos públicos, incluindo
  o laço de rateio do valor entre parcelas e a bifurcação cartão/caixa; três
  defeitos apontados com causa raiz e número de linha; roteiros de teste manual.
- **Decisão:** aceito na maior parte. Foram rejeitadas duas coisas: deixar os
  testes que expõem defeito com a asserção do comportamento errado (optou-se por
  `@Ignore` + Issue, mantendo a asserção correta) e qualquer alteração em
  `src/main`. Foram acrescentados na revisão: uma asserção que nomeia o
  `NullPointerException` no relatório do D2 e o caso TU-REC-24, ausente na
  primeira versão.
- **Validação realizada:**
  1. Suíte executada de fato, dentro do container `eclipse-temurin:8-jdk` do
     `docker-compose.yml` (o host só tem JDK 26, incompatível com o `target 1.8`
     e com o Mockito 2.15): **37 testes, 0 falhas, 3 ignorados** — 24 desta
     classe e 13 da suíte da Anna, que não foi afetada.
  2. Os 3 testes marcados com `@Ignore` tiveram a anotação **removida
     temporariamente** e a suíte foi reexecutada, para provar que eles falham
     pelo defeito que afirmam e não por erro de escrita. As três falhas foram
     confirmadas uma a uma (registro em `docs/ai/revisao-recebimento-service.md`,
     seção 4).
  3. Relatório JaCoCo conferido: **94,1% de cobertura de arestas** (32/34) e
     96,7% de linhas em `RecebimentoService`. As arestas restantes foram
     analisadas individualmente e são inalcançáveis (uma delas por causa do
     próprio defeito D2) — não foram "cobertas" com cenários artificiais.
  4. Cada `assertEquals` foi conferido contra a string literal do código-fonte,
     inclusive as mensagens com erro de português do sistema original.

<!--
Modelo de entrada para novos registros:

### AAAA-MM-DD — Título curto

- **Ferramenta:**
- **Membro responsável:**
- **Contexto/Prompt (resumo):**
- **Artefatos afetados:**
- **Validação realizada:**
-->
