# Registro de Uso de IA (AI-LOG)

Registro das interações com ferramentas de IA Generativa que contribuíram
substancialmente para os artefatos deste trabalho (disciplina Qualidade e Teste).
Correções ortográficas e configurações pontuais de IDE não são registradas aqui.

## Entradas

### 2026-09-21 - Ampliação dos testes e documentação de CaixaService

- **Ferramenta:** Codex (OpenAI)
- **Membro responsável:** João Pedro G. Valadares
- **Contexto/Prompt (resumo):** Pedido para ampliar moderadamente os testes
  unitários de `CaixaService`, e para documentar a responsabilidade individual no plano de teste. A instrução
  também determinou que os casos manuais fossem apenas planejados, sem registrar
  execução, TestLink ou Issue nesta etapa.
- **Artefatos afetados:**
  `src/test/java/net/originmobi/pdv/service/CaixaServiceTest.java`, `docs/plano-teste.md`, `README.md` e
  este registro.
- **Decisão:** foram incluídos cenários de abertura e fechamento: caixa anterior aberto, valor nulo, valor
  positivo, valor negativo, senha vazia, senha incorreta e senha válida.
- **Validação realizada:** os testes foram revisados contra `CaixaService.java`
  e executados no container Java 8 do projeto com
  `docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B
  -Dtest=CaixaServiceTest test`: 11 testes, 0 falhas e 0 erros.

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

### 2026-09-21 — Execução manual do CT-REC-01 e diagnóstico de 3 defeitos bloqueantes

- **Ferramenta:** Claude Code (Anthropic, modelo Opus 5)
- **Membro responsável:** João Portela
- **Contexto/Prompt (resumo):** Pedido de apoio para **executar** os casos de
  teste manual do módulo de recebimento, que estavam apenas projetados. A divisão
  de trabalho foi decidida antes de começar: **eu cadastrei as pré-condições e
  operei o sistema pela interface**; a IA ficou responsável por preparar o
  ambiente, verificar o estado real no banco após cada passo e diagnosticar a
  causa raiz dos erros observados, consultando o código-fonte e os logs do
  servidor em vez de propor correções por tentativa e erro.
- **Artefatos afetados:**
  - `docs/testes-manuais/CT-REC-01.md` (resultados obtidos preenchidos;
    pré-condições corrigidas; CT-REC-03 justificado como não executado)
  - `docs/bugs/defeitos-recebimento.md` (defeitos D4, D5 e D6)
  - `src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java`
    (caso TU-REC-25)
  - `docs/ai/revisao-recebimento-service.md` (seção 3.3)
- **Resultado da IA (resumo):** três diagnósticos de causa raiz, cada um
  confirmado contra o log do servidor e o estado do banco:
  1. **D4** — `parcela.data_alteracao TIMESTAMP NOT NULL` sem default na
     migration, inconsistente com as tabelas irmãs. Em MySQL 8
     (`explicit_defaults_for_timestamp = ON`) o default implícito do MySQL 5.x
     deixou de existir → `SQL 1364` → nenhuma venda a prazo fecha.
  2. **D5** — o trigger `atualiza_produto_estoque_AFTER_INSERT` ignora
     `new.tipo` e sempre subtrai, com guarda que impede a movimentação inicial →
     nenhum produto com controle de estoque pode ser vendido.
  3. **D6** — `abrirRecebimento` com nenhuma parcela marcada estoura
     `NumberFormatException` cru, por causa de `"".split(" ") == [""]`.
- **Decisão:** aceitos os três diagnósticos após verificação (abaixo). Duas
  correções de ambiente foram aplicadas **apenas no banco local**, com meu aval
  explícito, para tornar o fluxo executável — **nenhum arquivo de `src/main` foi
  alterado**; os defeitos foram reportados, não corrigidos no código. Uma
  sugestão da IA foi **rejeitada e corrigida por mim**: ela inicialmente
  afirmou que o `ProdutoRepository.movimentaEstoque` "esquecia" de atualizar o
  saldo; ao checar, existe um trigger justamente para isso, e a IA teve que
  refazer o diagnóstico — o defeito estava na lógica do trigger, não na ausência
  dele.
- **Validação realizada:**
  1. Cada causa raiz foi confirmada contra a **stack trace real** do servidor
     (ex.: `at net.originmobi.pdv.service.RecebimentoService.abrirRecebimento(RecebimentoService.java:67)`)
     e contra consultas ao MySQL, não por inferência.
  2. A correção do D5 foi **provada na prática**: após o ajuste do trigger, uma
     entrada de 50 unidades levou o saldo de 0 para 50, e as duas vendas
     seguintes (2 e 3 un.) o deixaram em 45 — entrada creditando e saídas
     debitando. Antes, o saldo ficava em 0.
  3. Todo resultado do CT-REC-01 foi conferido no banco, campo por campo
     (`recebimento`, `parcela`, `caixa_lancamento`, `caixa`), e não apenas pela
     mensagem na tela.
  4. O TU-REC-25 teve o `@Ignore` removido temporariamente para provar que falha
     pelo defeito D6 (`Esperava validação de negócio, veio
     java.lang.NumberFormatException`) e não por erro de escrita.
  5. Suíte completa do grupo reexecutada ao final: **74 testes, 0 falhas, 4
     ignorados** (RecebimentoService 25, VendaService 32, NotaFiscalItem 13,
     Caixa 4).
- **Observação metodológica:** D4 e D5 são invisíveis para os quatro conjuntos
  de testes unitários do grupo, porque todos isolam os repositórios com Mockito e
  a lógica defeituosa está no schema e num trigger do banco. D6 era um caso de
  entrada ausente na minha própria suíte, apesar dos 94,1% de cobertura de
  arestas. Os três reforçam, com evidência própria, por que a Entrega 2 precisa de
  testes de integração com banco real e da técnica funcional de valor limite.

### 2026-09-21 — Criação de testes unitários de VendaService

- **Ferramenta:** Claude Code (Anthropic, modelo Opus 5)
- **Membro responsável:** Felipe Martins Bittencourt
- **Contexto/Prompt (resumo):** Pedido para implementar os testes unitários de
  `VendaService` seguindo o padrão já adotado em `CaixaServiceTest` e
  `NotaFiscalItemServiceTest` (JUnit 4 + `MockitoJUnitRunner`, mocks para todas
  as dependências, nomes de método no formato
  `metodo_deveComportamento_quandoCondicao`). A IA leu `VendaService` e os
  colaboradores (`VendaRepository`, `PagamentoTipoService`, `TituloService`,
  `ParcelaService`, `CaixaLancamentoService`, `CartaoLancamentoService`,
  `ProdutoService`), mapeou os fluxos de `abreVenda`, `busca`, `addProduto`,
  `removeProduto`, `lista`, `qtdAbertos` e `fechaVenda` (à vista em dinheiro,
  à vista no cartão, a prazo e pagamento misto) e propôs os cenários.
- **Artefatos afetados:**
  `src/test/java/net/originmobi/pdv/service/VendaServiceTest.java`,
  `docs/ai/AI-LOG.md` e `README.md`.
- **Validação realizada:** 32 cenários executados localmente com
  `mvnw.cmd -Dtest=VendaServiceTest test` (32 testes, 0 falhas, 0 erros) e a
  suíte completa com `mvnw.cmd -Dtest=VendaServiceTest,CaixaServiceTest,NotaFiscalItemServiceTest test`
  (49 testes, 0 falhas, 0 erros). Cada asserção foi conferida manualmente
  contra o código de produção. Seis cenários são testes de caracterização de
contra o código de produção. Oito cenários são testes de caracterização de
  código de teste): troca dos parâmetros `acre`/`desc` nas chamadas de
  `avistaDinheiro()` e `aprazo()`; somatório de conferência usando
  `vlParcelas[i]` dentro do laço de índice `aux`; `vendas.fechaVenda()` dentro
  do laço de formas de pagamento; `throw new RuntimeException()` sem mensagem
  em `aprazo()`; e exceções silenciadas em `abreVenda`, `addProduto` e
  `removeProduto`. Esses testes fixam o comportamento atual e devem ser
  ajustados junto com a correção dos defeitos na Entrega 2.

<!--
Modelo de entrada para novos registros:

### AAAA-MM-DD — Título curto

- **Ferramenta:**
- **Membro responsável:**
- **Contexto/Prompt (resumo):**
- **Artefatos afetados:**
- **Validação realizada:**
-->
