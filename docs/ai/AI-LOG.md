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

<!--
Modelo de entrada para novos registros:

### AAAA-MM-DD — Título curto

- **Ferramenta:**
- **Membro responsável:**
- **Contexto/Prompt (resumo):**
- **Artefatos afetados:**
- **Validação realizada:**
-->
