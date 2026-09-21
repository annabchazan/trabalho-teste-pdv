# Casos de teste manual — Venda (abertura, itens e fechamento)

Responsável: **Felipe Martins Bittencourt** · Funcionalidade: **Realizar venda (abrir, adicionar produtos e fechar)**
Módulo: `VendaService` / `VendaController` (`/venda`)

> **Status: projetado, aguardando execução.** A coluna *Resultado obtido* e o campo
> *Status* de cada caso devem ser preenchidos durante a execução. Formato alinhado
> com `CT-NFI-01.md`, `CT-REC-01.md` e `CT-CAIXA-01.md`.

---

## Ambiente

| Item | Valor |
|---|---|
| Subida do sistema | `docker compose up -d` na raiz do projeto |
| URL | http://localhost:8080 |
| Banco | MySQL 8.0 em `localhost:3307` (base `pdv`, root / `123456`) |
| Usuário de teste | `gerente` / `123` |
| Navegador | (preencher na execução) |
| Data da execução | (preencher) |

### Correções de ambiente necessárias para executar

O fechamento de venda **não é executável** no ambiente padrão sem duas correções
de banco, ambas já reportadas como defeito pelo João Portela durante a execução
do CT-REC-01 (Issues [#10](../../issues/10) e [#11](../../issues/11)). Foram
aplicadas no banco local em 21/09/2026 para permitir esta execução;
**nenhum arquivo de `src/main` foi alterado**:

```sql
-- D4: parcela.data_alteracao era TIMESTAMP NOT NULL sem default
ALTER TABLE parcela MODIFY data_alteracao TIMESTAMP NOT NULL
      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE mva     MODIFY data_alteracao TIMESTAMP NOT NULL
      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- D5: trigger de estoque ignorava new.tipo e sempre subtraía
--     (versão corrigida em docs/bugs/defeitos-recebimento.md, defeito D5)
```

Ambos os defeitos foram **reproduzidos de forma independente** nesta execução,
antes da correção:

| Defeito | Evidência coletada nesta execução |
|---|---|
| D5 | Ajuste de entrada de 5 un. do produto 1: `ajuste_produtos.qtd_nova = 5` e `estoque_movimentacao` com `tipo ENTRADA, qtd 5`, mas `produto_estoque.qtd` permaneceu **0**. Fechamento recusado com "O produto de código 1 não tem estoque suficiente, verifique" (`ProdutoService:103`) |
| D4 | Fechamento a prazo → `SQL Error 1364: Field 'data_alteracao' doesn't have a default value`, convertido em `RuntimeException` sem mensagem em `VendaService:273` (ver D-VEN-08) |

Após aplicar as correções, é necessário **refazer o Ajuste de Estoque**: a
movimentação registrada antes da correção não é creditada retroativamente.

## Pré-condições (massa de dados)

Executar na ordem. A migration `V2` já cria as três formas de pagamento usadas
abaixo (`À Vista` = `00`, `A Prazo` = `30`, `Uma entrada + 30` = `00/33`) e os
tipos de título `DIN`, `CARTDEB` e `CARTCRED`.

1. **Empresa** cadastrada (menu *Empresa*).
2. **Cliente** cadastrado em *Pessoas*, com endereço completo.
   Código do cliente: `________`
3. **Título tipo dinheiro** cadastrado em *Títulos* (tipo `Dinheiro`).
   Código: `________`
4. **Máquina de cartão** cadastrada em *Máquina de cartão* (`/maquinacartao`), com
   taxas de débito/crédito e de antecipação preenchidas. Código: `________`
   > Descoberto na execução: sem ela o CT-VENDA-04 quebra com `NullPointerException`
   > em `CartaoLancamentoService:64` (defeito **D-VEN-06**).
5. **Título tipo cartão de crédito** cadastrado em *Títulos* (tipo `Cartão Crédito`),
   **vinculado à máquina da pré-condição 4**, usado em CT-VENDA-04. Código: `________`
6. **Produto** cadastrado em *Produtos*, **vendável**, com **estoque ≥ 10** e preço
   unitário conhecido. Anotar preço: `R$ ________` e estoque inicial: `________`
   > O estoque entra por *Ajuste Estoque* → Inserir → **Processar**, e depende da
   > correção do D5 acima. Atenção: o produto `Sorvete Kg` semeado pelas migrations
   > tem `valor_venda = 0` e balança `SIM` — usá-lo leva a uma venda de total zero.
7. **Caixa aberto** no dia (menu *Caixa* → *Abrir caixa*). Sem caixa aberto o
   fechamento à vista em dinheiro é recusado (ver CT-VENDA-06).
8. **Forma de pagamento `30/60/90`** com 3 parcelas, cadastrada em *Forma de
   pagamento*, usada apenas no CT-VENDA-10. As migrations só criam `00`, `30` e
   `00/33`.

---

## CT-VENDA-01 — Abrir venda, adicionar e remover produtos

**Objetivo:** verificar `abreVenda`, `addProduto` e `removeProduto` — a venda nasce
`ABERTA`, o total de produtos acompanha os itens e a remoção desfaz o item.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Acessar *Vendas* → *Nova venda* (`/venda/form`) | - | Formulário de venda exibido | |
| 2 | Selecionar o cliente e informar uma observação | Cliente da pré-condição · obs.: `venda de teste CT-VENDA-01` | Campos preenchidos | |
| 3 | Salvar a venda | - | Mensagem "Pedido Salvo"; redirecionamento para `/venda/{codigo}`. Anotar o código: `______` | |
| 4 | Conferir a situação da venda | - | Situação **ABERTA**; total de produtos **R$ 0,00**; usuário logado (`gerente`) registrado como responsável | |
| 5 | Adicionar o produto da pré-condição | Produto cadastrado | Item aparece na tabela de produtos da venda; total atualizado para o preço unitário | |
| 6 | Adicionar o mesmo produto mais 2 vezes | - | 3 linhas na tabela; total = 3 × preço unitário | |
| 7 | Remover uma das linhas (link *remover* → confirmar) | - | Restam 2 linhas; total = 2 × preço unitário | |
| 8 | Reabrir a venda em *Vendas* → status *ABERTA* | - | A venda aparece na listagem de abertas, com o cliente e o valor corretos | |
| 9 | Reeditar a venda: alterar a observação e salvar de novo | obs.: `observacao alterada` | Observação persistida; **um novo código de venda NÃO deve ser criado** | |

**Observações da execução:**

_(preencher)_

---

## CT-VENDA-02 — Fechar venda à vista em dinheiro (fluxo principal)

**Objetivo:** verificar `fechaVenda` no caminho à vista/dinheiro: lançamento de
entrada no caixa, geração do *receber*, baixa de estoque e mudança de situação.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição:** venda aberta com **2 itens**, valor total conhecido (`R$ ______`),
caixa aberto e título `DIN` cadastrado. Anotar o **saldo do caixa antes**: `R$ ______`
e o **estoque do produto antes**: `______`.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Na venda aberta, clicar em *Pagamento* | - | Modal de pagamento exibido com o *Valor Total* igual ao total dos produtos | |
| 2 | Selecionar a forma de pagamento | `À Vista` (`00`) | Aparece **1** linha de parcela (valor + título) | |
| 3 | Informar o valor da parcela | Valor **exatamente** igual ao total dos produtos | Campo aceita o valor | |
| 4 | Selecionar o título da parcela | Título `DIN` da pré-condição | Título selecionado | |
| 5 | Deixar *Desc* e *Acre* zerados | `0` / `0` | Campos zerados | |
| 6 | Confirmar o pagamento | - | Alerta **"Venda finalizada com sucesso"** | |
| 7 | Recarregar a venda | - | Situação **FECHADA**; data de finalização preenchida; valor total gravado | |
| 8 | Acessar *Caixa* → lançamentos do caixa aberto | - | Lançamento de **entrada**, tipo RECEBIMENTO, descrição "Recebimento de venda á vista", valor = total da venda | |
| 9 | Acessar *Contas a receber* e filtrar o cliente | Cliente da venda | Existe um *receber* "Recebimento referente a venda {código}" no valor total | |
| 10 | Conferir o estoque do produto em *Produtos* | - | Estoque = estoque anterior **− 2** (saída registrada pela venda) | |
| 11 | Tentar adicionar um produto à venda já fechada | Qualquer produto | Alerta **"Venda Fechada"**; nenhum item é incluído | |

**Observações da execução:**

_(preencher)_

---

## CT-VENDA-03 — Fechar venda a prazo (geração de parcelas)

**Objetivo:** verificar o caminho a prazo de `fechaVenda` — geração de parcela com
vencimento deslocado e vínculo ao *receber* do cliente.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição:** nova venda aberta **com cliente**, 1 item, total conhecido.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento | - | Modal exibido | |
| 2 | Selecionar a forma de pagamento | `A Prazo` (`30`) | Aparece 1 linha de parcela | |
| 3 | Informar o valor da parcela e o título | Valor = total da venda · título `DIN` | Campos preenchidos | |
| 4 | Confirmar o pagamento | - | Alerta "Venda finalizada com sucesso" | |
| 5 | Conferir a venda | - | Situação **FECHADA** | |
| 6 | Acessar *Contas a receber* e filtrar o cliente | Cliente da venda | Parcela em aberto, sequência 1, valor = total da venda | |
| 7 | Conferir o vencimento da parcela | - | Vencimento = **data de hoje + 30 dias** | |
| 8 | Conferir os lançamentos do caixa | - | **Nenhum** lançamento novo (venda a prazo não movimenta caixa) | |

---

## CT-VENDA-04 — Fechar venda à vista no cartão de crédito

**Objetivo:** verificar o desvio de cartão em `fechaVenda` — o valor vai para o
lançamento de cartão e **não** para o caixa.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição:** nova venda aberta com 1 item; título do tipo `Cartão Crédito`
cadastrado (pré-condição 4). Anotar o saldo do caixa antes: `R$ ______`.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento e selecionar a forma | `À Vista` (`00`) | 1 linha de parcela | |
| 2 | Informar o valor e selecionar o título de **cartão de crédito** | Valor = total · título `CARTCRED` | Campos preenchidos | |
| 3 | Confirmar o pagamento | - | Alerta "Venda finalizada com sucesso" | |
| 4 | Conferir a venda | - | Situação **FECHADA** | |
| 5 | Conferir os lançamentos do caixa | - | **Nenhum** lançamento de dinheiro no caixa para esta venda | |
| 6 | Conferir os lançamentos de cartão | - | Lançamento de cartão no valor da parcela, vinculado ao título de crédito | |

---

## CT-VENDA-05 — Fechar venda sem valor de produtos

**Objetivo:** verificar a guarda de valor (`vlprodutos <= 0`) — exercita pela
interface o mesmo ponto coberto por `fechaVenda_deveLancarExcecao_quandoValorDeProdutosNaoEPositivo`.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir uma venda **sem nenhum produto** | - | Venda aberta, total R$ 0,00 | |
| 2 | Abrir o modal de pagamento, selecionar `À Vista` e confirmar | valor da parcela vazio ou `0` | Mensagem **"Venda sem valor, verifique"**; venda continua **ABERTA** | |
| 3 | Conferir os lançamentos do caixa e o estoque | - | Nenhum lançamento e nenhuma baixa de estoque | |

---

## CT-VENDA-06 — Fechar venda à vista em dinheiro sem caixa aberto

**Objetivo:** verificar a guarda `caixas.caixaIsAberto()`.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição:** **fechar o caixa** (menu *Caixa*) antes de executar. Ter uma venda
aberta com 1 item.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento, selecionar `À Vista` | - | 1 linha de parcela | |
| 2 | Informar valor = total e título `DIN`; confirmar | - | Mensagem **"nenhum caixa aberto"** | |
| 3 | Conferir a venda | - | Continua **ABERTA**; estoque inalterado | |
| 4 | Conferir o *receber* do cliente | - | **Ponto de atenção:** o `receber` é cadastrado **antes** da validação do caixa. Registrar se sobrou um recebimento órfão para esta venda | |

---

## CT-VENDA-07 — Fechar venda a prazo sem cliente

**Objetivo:** verificar a guarda "Venda sem cliente" no caminho a prazo.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição:** venda aberta **sem cliente selecionado**, com 1 item.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento e selecionar | `A Prazo` (`30`) | 1 linha de parcela | |
| 2 | Informar valor = total e título `DIN`; confirmar | - | Mensagem **"Venda sem cliente, verifique"** | |
| 3 | Conferir a venda | - | Continua **ABERTA**; nenhuma parcela gerada | |

---

## CT-VENDA-08 — Desconto e acréscimo no fechamento à vista (defeito D-VEN-01)

**Objetivo:** confirmar **pela interface** a inversão de `desconto`/`acréscimo`
caracterizada nos testes `fechaVenda_deveInverterDescontoEAcrescimo_*`:
`avistaDinheiro()` e `aprazo()` são declarados `(..., Double acre, Double desc)` mas
chamados com `(..., desc, acre)`.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição:** caixa aberto; venda aberta com total de **R$ 100,00**. Anotar o
saldo do caixa antes: `R$ ______`.

| # | Passo | Dado de entrada | Resultado esperado (correto) | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento, forma `À Vista`, título `DIN` | - | 1 linha de parcela | |
| 2 | Informar o valor da parcela | `100,00` | Campo aceita | |
| 3 | Informar o desconto | `10,00` | Campo aceita | |
| 4 | Confirmar o pagamento | - | "Venda finalizada com sucesso" | |
| 5 | Conferir o valor gravado na venda | - | Venda fechada por **R$ 90,00** | |
| 6 | Conferir o lançamento no caixa | - | Entrada de **R$ 90,00**. **Defeito esperado: R$ 110,00** (100 + 10) | |
| 7 | Repetir com **acréscimo** de `20,00` e desconto `0` em outra venda de R$ 100,00 | `acre = 20,00` | Venda fechada por R$ 120,00 e caixa recebendo R$ 120,00. **Defeito esperado: caixa recebe R$ 80,00** | |

> Se o passo 6 ou 7 divergir do esperado, abrir issue referenciando
> `VendaService.java` (chamadas de `avistaDinheiro` e `aprazo`).

---

## CT-VENDA-09 — Venda mista (entrada + prazo) com valores válidos (defeito D-VEN-02)

**Objetivo:** confirmar pela interface o defeito caracterizado em
`fechaVenda_deveRecusarVendaMistaValida_porSomarSempreAPrimeiraParcela`: o somatório
de conferência em `avistaDinheiro()` usa `vlParcelas[i]` dentro do laço de índice
`aux`, somando sempre a **mesma** parcela.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição:** caixa aberto; venda aberta **com cliente**, total de **R$ 60,00**.

| # | Passo | Dado de entrada | Resultado esperado (correto) | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento e selecionar | `Uma entrada + 30` (`00/33`) | Aparecem **2** linhas de parcela | |
| 2 | Informar a 1ª parcela (à vista) | `50,00` · título `DIN` | Campo aceita | |
| 3 | Informar a 2ª parcela (a prazo) | `10,00` · título `DIN` | Campo aceita | |
| 4 | Confirmar o pagamento | - | "Venda finalizada com sucesso": entrada de R$ 50,00 no caixa + 1 parcela de R$ 10,00 em aberto. **Defeito esperado: "Valor das parcelas diferente do valor total de produtos, verifique"** (o serviço calcula 2 × 50,00 = 100,00) | |
| 5 | Tentar de novo com as duas parcelas **iguais** | `30,00` e `30,00` | Aceita (2 × 30,00 = 60,00 coincide com o total) — confirma que só passa quando as parcelas são iguais | |
| 6 | Conferir a venda e o caixa após o passo 5 | - | Venda FECHADA; entrada de R$ 30,00 no caixa; 1 parcela de R$ 30,00 em aberto | |

---

## CT-VENDA-10 — Venda a prazo em 3 parcelas (defeito D-VEN-03)

**Objetivo:** confirmar o defeito caracterizado em
`fechaVenda_deveFecharAVendaUmaVezPorFormaDePagamento`: `vendas.fechaVenda()` está
**dentro** do laço de formas de pagamento, disparando um `update` por parcela.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição:** cadastrar em *Formas de pagamento* um plano `30/60/90` com
3 parcelas. Venda aberta **com cliente**, total de **R$ 90,00**.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento e selecionar o plano `30/60/90` | - | Aparecem 3 linhas de parcela | |
| 2 | Informar `30,00` em cada parcela, título `DIN` nas três | - | Campos preenchidos | |
| 3 | Confirmar o pagamento | - | "Venda finalizada com sucesso" | |
| 4 | Conferir *Contas a receber* do cliente | - | **3** parcelas, sequências 1, 2 e 3, vencendo em +30, +60 e +90 dias | |
| 5 | Conferir o log da aplicação (`docker compose logs -f pdv-app`) durante o passo 3 | - | Registrar quantos `update ... venda` o Hibernate emitiu. **Defeito esperado: 3 updates** (os dois últimos sem efeito por causa do `and data_finalizado is null` da query) | |
| 6 | Conferir a venda e o estoque | - | Venda FECHADA uma única vez; baixa de estoque aplicada **uma** vez | |

---

## CT-VENDA-11 — Falha silenciosa na inclusão de produto (defeito D-VEN-04)

**Objetivo:** confirmar que `addProduto` e `removeProduto` engolem exceções
(`catch { e.getStackTrace(); }`) e respondem `"ok"` mesmo quando a operação falha —
caracterizado em `addProduto_deveResponderOk_mesmoQuandoRepositorioFalha` e
`removeProduto_deveResponderOk_quandoVendaNaoExiste`.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

| # | Passo | Dado de entrada | Resultado esperado (correto) | Resultado obtido |
|---|---|---|---|---|
| 1 | Com uma venda aberta, forçar a remoção de um item inexistente (repetir o POST `/venda/removeproduto` com `posicaoPro` inválido pelo console do navegador) | `posicaoPro` = número inexistente | Mensagem de erro ao usuário. **Defeito esperado: resposta `ok`, sem erro visível** | |
| 2 | Repetir o POST `/venda/addproduto` com um código de produto inexistente | `codigoPro` = número inexistente | Mensagem de erro ao usuário. **Defeito esperado: resposta `ok`, nenhum item incluído, nenhum aviso** | |
| 3 | Conferir o log da aplicação durante os passos 1 e 2 | - | Registrar se a exceção sequer chega ao log (o código chama `e.getStackTrace()`, que **não imprime** nada) | |

---

## Rastreabilidade

| Caso manual | Casos unitários relacionados | Defeito relacionado |
|---|---|---|
| CT-VENDA-01 | `abreVenda_deveCadastrarVendaAberta_quandoVendaNova`, `abreVenda_deveApenasAtualizarDados_quandoVendaJaExiste`, `addProduto_deveSalvarProdutoNaVenda_quandoVendaEstaAberta`, `removeProduto_deveRemoverItem_quandoVendaEstaAberta` | — |
| CT-VENDA-02 | `fechaVenda_deveLancarNoCaixaEFecharVenda_quandoAVistaEmDinheiro`, `addProduto_deveRecusar_quandoVendaEstaFechada` | — |
| CT-VENDA-03 | `fechaVenda_deveGerarUmaParcelaPorPrazo_quandoVendaAPrazo` | — |
| CT-VENDA-04 | `fechaVenda_deveLancarNoCartao_quandoAVistaComCartaoDeCredito` | — |
| CT-VENDA-05 | `fechaVenda_deveLancarExcecao_quandoValorDeProdutosNaoEPositivo` | — |
| CT-VENDA-06 | `fechaVenda_deveLancarExcecao_quandoAVistaEmDinheiroSemCaixaAberto` | (receber órfão — a confirmar) |
| CT-VENDA-07 | `fechaVenda_deveLancarExcecao_quandoVendaAPrazoSemCliente` | — |
| CT-VENDA-08 | `fechaVenda_deveInverterDescontoEAcrescimo_noLancamentoDeCaixaAVista`, `fechaVenda_deveInverterDescontoEAcrescimo_noValorDaParcelaAPrazo` | D-VEN-01 |
| CT-VENDA-09 | `fechaVenda_deveRecusarVendaMistaValida_porSomarSempreAPrimeiraParcela` | D-VEN-02 |
| CT-VENDA-10 | `fechaVenda_deveFecharAVendaUmaVezPorFormaDePagamento` | D-VEN-03 |
| CT-VENDA-11 | `addProduto_deveResponderOk_mesmoQuandoRepositorioFalha`, `removeProduto_deveResponderOk_quandoVendaNaoExiste` | D-VEN-04 |

Ao menos um destes cenários deve ser cadastrado no **TestLink**, conforme o enunciado.
Os demais ficam documentados neste arquivo.
