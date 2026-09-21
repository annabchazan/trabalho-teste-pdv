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

### 2026-09-20 — Criação de testes unitários de CaixaService

- **Ferramenta:** GitHub Copilot
- **Membro responsável:** João Pedro G. Valadares
- **Contexto/Prompt (resumo):** Apoio na continuidade da Entrega 1, com
  implementação de testes unitários isolados para `CaixaService`, uma classe
  com regras de abertura, fechamento, validação e filtragem de caixas.
- **Artefatos afetados:**
  `src/test/java/net/originmobi/pdv/service/CaixaServiceTest.java` e
  `pom.xml`.
- **Validação realizada:** Foram projetados quatro cenários com JUnit e
  Mockito, cobrindo caixa aberto encontrado, caixa aberto ausente, filtragem
  por data e listagem sem data.

### 2026-09-20 — Correção do ambiente de testes Maven

- **Ferramenta:** GitHub Copilot
- **Membro responsável:** João Pedro G. Valadares
- **Contexto/Prompt (resumo):** Apoio na configuração do ambiente para permitir
  a execução dos testes unitários do projeto com o Maven Wrapper e Java 19.
- **Artefatos afetados:** `.mvn/wrapper/maven-wrapper.jar`,
  `.mvn/wrapper/maven-wrapper.properties` e `pom.xml`.
- **Validação realizada:** O Maven Wrapper foi configurado com Maven 3.9.9,
  as dependências de teste foram alinhadas ao Java 19 e o Surefire foi
  atualizado. O comando `mvnw.cmd -Dtest=CaixaServiceTest test` foi executado
  com sucesso, com 4 testes executados, 0 falhas e 0 erros.

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
