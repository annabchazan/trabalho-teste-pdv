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

<!--
Modelo de entrada para novos registros:

### AAAA-MM-DD — Título curto

- **Ferramenta:**
- **Membro responsável:**
- **Contexto/Prompt (resumo):**
- **Artefatos afetados:**
- **Validação realizada:**
-->
