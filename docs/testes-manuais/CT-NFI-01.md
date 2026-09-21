# Casos de teste manual — Nota Fiscal (item)

Responsável: **Anna Beatriz Chaboudet Chazan** · Funcionalidade: **Inclusão de item em nota fiscal**
Módulo: `NotaFiscalItemService` / `NotaFiscalItemController`

> **Status: executado.** Casos originalmente registrados no Plano de Teste
> (Google Docs) e formalizados aqui. Evidência de execução no TestLink em
> [`evidencias/`](evidencias/).

---

## Ambiente

| Item | Valor |
|---|---|
| Subida do sistema | `docker compose up -d` na raiz do projeto |
| URL | http://localhost:8080 |
| Banco | MySQL 8.0 em `localhost:3307` (base `pdv`, root / `123456`) |
| Usuário de teste | `gerente` / `123` |
| Data da execução | 17/09/2026 |

## Pré-condições (massa de dados)

1. **Empresa** cadastrada no sistema.
2. **Cliente/destinatário** cadastrado, com endereço completo.
3. **Produto** "Picolé" (cód. 1) cadastrado com tributação completa: NCM, CST/CSOSN,
   CFOP, unidade e Modalidade BC ICMS.
4. Para o CT-NFI-02: um segundo produto cadastrado **sem** Modalidade BC ICMS.

---

## CT-NFI-01 — Inclusão de item com dados válidos

**Objetivo:** verificar que a inclusão de um item em uma Nota Fiscal, com produto
corretamente cadastrado (tributação, NCM, CFOP, unidade e Modalidade BC ICMS),
calcula e exibe corretamente os valores do item e recalcula o total da nota.

**Status:** ( x ) Passou ( ) Falhou

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Acessar "NF-e" > "Nova Nota" (`/notafiscal/form`) | - | Formulário exibido, aba "Destinatário" ativa | OK |
| 2 | Preencher "Natureza operação" | Venda de mercadoria | Campo aceita o texto | OK |
| 3 | Selecionar o destinatário cadastrado | Nome do cliente | Cliente selecionado, endereço/UF preenchidos automaticamente | OK |
| 4 | Selecionar "E/S" | SAIDA | Opção selecionada | OK |
| 5 | Clicar em "Criar Nota" | - | Nota salva; redireciona para `/notafiscal/{codigo}`; abas "Produtos" e "Frete" disponíveis | OK |
| 6 | Clicar na aba "Produtos" | - | Tela de busca de produto e tabela de itens (vazia) | OK |
| 7 | Selecionar o produto cadastrado | COD: 1 - Picolé | Produto selecionado no combo | OK |
| 8 | Clicar em "Inserir" | - | Diálogo pede "Informe a quantidade" | OK |
| 9 | Informar a quantidade e confirmar | 3 | Requisição enviada, sem erros | OK |
| 10 | Observar a tabela de itens | - | Produto com NCM, CST, CFOP, unidade, Qtd, Vl. Unitário, Vl. Total, BC/Vl. ICMS calculados | Picolé, NCM 21069090, CFOP 5102, Qtd 3, Vl. Unit. R$ 6,50, Vl. Total R$ 19,50 |
| 11 | Observar a aba "Totais" | - | Total da nota recalculado | Total Nota R$ 19,50 |

**Evidência TestLink:**
[`evidencias/testlink-ct-nfi-01-design.pdf`](evidencias/testlink-ct-nfi-01-design.pdf)
(especificação do caso, Suite `NotaFiscal`) e
[`evidencias/testlink-ct-nfi-01-execucao.pdf`](evidencias/testlink-ct-nfi-01-execucao.pdf)
(execução: **Passou**, 21/09/2026 14:25, testador `chazananna`, Build `Entrega 1`,
Plano de Teste `Entrega 1 - PDV`).

---

## CT-NFI-02 — Item com produto sem Modalidade BC ICMS

**Objetivo:** verificar o comportamento do sistema ao inserir, na nota, um produto
cadastrado sem Modalidade BC ICMS.

**Status:** ( ) Passou ( x ) Falhou — bug real encontrado
**Issue vinculada:** [#2 — NullPointerException/NoSuchElementException ao inserir item na nota fiscal quando produto está sem "Modalidade BC ICMS" cadastrada](../../../issues/2)

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Na aba "Produtos", selecionar o produto sem Modalidade BC ICMS | - | Produto selecionado no combo | OK |
| 2 | Clicar em "Inserir" | - | Diálogo pede quantidade | OK |
| 3 | Informar quantidade e confirmar | 3 | Mensagem de validação amigável (ex.: "Produto sem Modalidade BC ICMS, favor verifique") | Alerta técnico "No value present" (`NoSuchElementException`), sem indicar o campo problemático |

**Causa raiz (código-fonte):** `NotaFiscalItemService.java`, linha 67:

```java
int modBcIcms = produto.map(Produto::getModBcIcms).get().getTipo();
```

Chamado sem validar antes se o produto tem Modalidade BC ICMS — diferente de NCM,
unidade e CEST, que são validados em `verificaRegraDeTributacao()`.

**Observação adicional (mesma sessão, causa semelhante):** durante o cadastro das
pré-condições, o mesmo padrão de falha (entrada inválida/vazia gerando exceção
técnica em vez de validação amigável) apareceu também em Empresa e Regra de
Tributação — registrado como Issue separada
([#1](../../../issues/1)), fora do escopo desta funcionalidade.

---

## Rastreabilidade

| Caso manual | Casos unitários relacionados | Defeito relacionado |
|---|---|---|
| CT-NFI-01 | TU-NFI-01, TU-NFI-02 | — |
| CT-NFI-02 | TU-NFI-04 (cenário análogo coberto no unitário, mas com mensagem correta via mock) | Issue #2 |

Cenário registrado no **TestLink** (instância da disciplina, projeto
`projeto_teste_anna`, Suite `NotaFiscal`): CT-NFI-01, conforme o enunciado.
