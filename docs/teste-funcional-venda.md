# Projeto de teste funcional — `VendaService`

**Responsável:** Felipe Martins Bittencourt
**Entrega 2, Etapa 2.** Decisões registradas em
[`docs/ai/decisoes-entrega2-venda.md`](ai/decisoes-entrega2-venda.md) (D-13).

> Este é o rascunho do projeto de teste. A versão oficial vai para o Google
> Docs do grupo, na seção de teste funcional.

Neste teste os casos são derivados dos **requisitos**, sem olhar a
implementação. A cobertura do código é medida depois, na técnica estrutural
(Etapa 3). Foram usadas três técnicas:

1. **Classes de equivalência:** cada entrada é dividida em grupos de valores
   que o sistema deve tratar da mesma forma. Basta testar um representante de
   cada grupo.
2. **Valores-limite:** testa os valores imediatamente abaixo, em cima e acima
   de cada fronteira entre classes, onde os erros costumam aparecer.
3. **Tabela de decisão:** combina as condições que decidem o que acontece com
   cada parcela no fechamento da venda.

Todos os casos foram implementados como testes unitários em
[`VendaServiceTest.java`](../src/test/java/net/originmobi/pdv/service/VendaServiceTest.java),
com as dependências simuladas pelo Mockito. Cada teste traz um comentário com o
ID do caso (`TF-VEN-xx`) ou da regra (`R1` a `R7`).

## Requisitos funcionais considerados

Fontes: README do sistema, plano de teste da Entrega 1
([`docs/plano-teste-venda.md`](plano-teste-venda.md)) e casos manuais
CT-VENDA-01 a 14.

| ID | Requisito |
|---|---|
| RF-01 | O usuário abre uma venda, que nasce aberta e vinculada ao usuário logado. |
| RF-02 | Produtos só podem ser incluídos ou removidos enquanto a venda está aberta. |
| RF-03 | As vendas podem ser buscadas por código ou por situação (aberta ou fechada). |
| RF-04 | No fechamento, a venda é paga conforme a forma de pagamento escolhida. Cada parcela é à vista (`00`), com lançamento no caixa ou no cartão, ou a prazo (`30`, `60`…), com geração de parcela a receber. O valor final é produtos + acréscimo − desconto. |

## 1. Classes de equivalência

### `fechaVenda` (RF-04)

| # | Entrada | Classes válidas | Classes inválidas |
|---|---|---|---|
| E1 | Situação da venda | V1: aberta | I1: fechada · I2: inexistente |
| E2 | Valor dos produtos | V2: maior que 0 | I3: 0 ou menos |
| E3 | Desconto | V3: de 0 até menos que (produtos + acréscimo) | I4: igual ou maior que o total · I5: negativo |
| E4 | Acréscimo | V4: 0 ou mais | I6: negativo |
| E5 | Prazo de cada parcela | V5: `00` (à vista) · V6: `30`, `30/60`… (a prazo) · V7: misto `00/30` | I7: `0` (ambíguo entre à vista e prazo de 0 dias) |
| E6 | Soma das parcelas à vista em dinheiro | V8: igual ao valor dos produtos | I8: diferente · I9: parcela vazia |
| E7 | Tipo do título | V9: dinheiro · V10: cartão de débito · V11: cartão de crédito | I10: outro tipo · I11: título inexistente |
| E8 | Caixa, para parcela à vista em dinheiro | V12: aberto | I12: fechado |
| E9 | Cliente, para parcela a prazo | V13: informado | I13: ausente |
| E10 | Quantidade de títulos e de formas de pagamento | V14: iguais | I14: menos títulos que formas |

### `addProduto` / `removeProduto` (RF-02) e `busca` (RF-03)

| # | Entrada | Classes válidas | Classes inválidas |
|---|---|---|---|
| E11 | Situação da venda ao incluir ou remover produto | V15: aberta | I15: fechada · I16: inexistente |
| E12 | Situação informada na busca | V16: `"ABERTA"` · V17: `"FECHADA"` | I17: outra grafia (ex.: `"aberta"`) · I18: nula |
| E13 | Código no filtro da busca | V18: informado · V19: vazio | — |

## 2. Valores-limite

| Grandeza | Fronteira | Valores testados |
|---|---|---|
| Valor dos produtos | 0 | −0,01 · **0,00** · 0,01 |
| Soma das parcelas à vista, para produtos de 100,00 | 100,00 | 99,99 · **100,00** · 100,01 |
| Desconto em relação ao total, para produtos de 100,00 | 100,00 | **100,00** (total 0) · 100,01 (total −0,01) |
| Número de parcelas | 1 | 1 · 2 · 3 |

Os valores em **negrito** ficam exatamente na fronteira.

**Observação técnica:** 100 − 100,01 não é exatamente −0,01 em `double`. Por
isso o teste do desconto acima do total compara o valor com tolerância de
0,000001.

## 3. Tabela de decisão: tratamento de cada parcela no fechamento

| Condições / Ações | R1 | R2 | R3 | R4 | R5 | R6 | R7 |
|---|---|---|---|---|---|---|---|
| Parcela à vista (`00`)? | S | S | S | S | S | N | N |
| Tipo do título | dinheiro | dinheiro | débito | crédito | outro | — | — |
| Caixa aberto? | S | N | — | — | — | — | — |
| Cliente informado? | — | — | — | — | — | S | N |
| **Lança no caixa** | X | | | | | | |
| **Lança no cartão** | | | X | X | | | |
| **Gera parcela a receber** | | | | | | X | |
| **Erro "nenhum caixa aberto"** | | X | | | | | |
| **Erro "Venda sem cliente, verifique"** | | | | | | | X |
| **Erro de tipo de pagamento inválido** | | | | | X* | | |

\* Resultado esperado pelo requisito. Hoje o sistema fecha a venda **sem
registrar o recebimento** (defeito, teste T-3).

## 4. Casos de teste e rastreabilidade

### Casos novos da Etapa 2

| ID | Técnica | Classe / limite | Entrada principal | Resultado esperado (requisito) | Resultado obtido |
|---|---|---|---|---|---|
| TF-VEN-01 | limite | E2, −0,01 | produtos = −0,01 | erro "Venda sem valor" | igual ✅ |
| TF-VEN-02 | limite | E2, 0,01 | produtos = 0,01, cartão | venda fechada por 0,01 | igual ✅ |
| TF-VEN-03 | limite | E6, 99,99 | parcela 99,99 para 100,00 | erro "Valor das parcelas diferente…" | igual ✅ |
| TF-VEN-04 | limite | E6, 100,01 | parcela 100,01 para 100,00 | erro "Valor das parcelas diferente…" | igual ✅ |
| TF-VEN-05 | limite | E3, I4 na fronteira | desconto = 100,00 | recusar venda de valor zero | **fecha por 0,00** ❌ DEFEITO |
| TF-VEN-06 | limite | E3, I4 acima | desconto = 100,01 | recusar venda de valor negativo | **fecha por −0,01** ❌ DEFEITO |
| TF-VEN-07 | classe | E4, I6 | acréscimo = −10,00 | recusar acréscimo negativo | **aceita; funciona como desconto** ❌ DEFEITO |
| TF-VEN-08 | classe | E5, I7 | prazo `0`, sem cliente | indefinido no requisito | tratado como a prazo; exige cliente (comportamento atual registrado) |
| TF-VEN-09 | classe | E10, I14 | 2 formas e 1 título | mensagem de erro de validação | **`ArrayIndexOutOfBoundsException`** depois de gerar a 1ª parcela ❌ DEFEITO |
| TF-VEN-10 | classe | E11, I16 | incluir produto em venda inexistente | mensagem "venda não encontrada" | **`NullPointerException`** ❌ DEFEITO |
| TF-VEN-11 | classe | E12, I17 | situação `"aberta"` | buscar vendas abertas | **busca as fechadas** (comportamento atual registrado) |
| TF-VEN-12 | classe | E12, I18 | situação nula | mensagem de erro ou busca padrão | **`NullPointerException`** ❌ DEFEITO |

### Classes, limites e regras já cobertos por testes existentes

| Classe / limite / regra | Teste |
|---|---|
| E1 V1 · R1 · E6 V8 (100,00) · E8 V12 | `fechaVenda_deveLancarNoCaixaEFecharVenda_quandoAVistaEmDinheiro` |
| E1 I1 | `fechaVenda_deveLancarExcecao_quandoVendaJaEstaFechada` |
| E1 I2 | `fechaVenda_deveLancarErroTecnico_quandoVendaNaoExiste` (T-9b) |
| E2 I3 (0,00) | `fechaVenda_deveLancarExcecao_quandoValorDeProdutosNaoEPositivo` |
| E3 V3 · E4 V4 | `fechaVenda_deveRegistrarOReceberComDescontoEAcrescimo` (T-4) |
| E5 V6 · R6 · 2 parcelas | `fechaVenda_deveGerarUmaParcelaPorPrazo_quandoVendaAPrazo` |
| E5 V7 | `fechaVenda_deveCombinarCaixaEParcela_quandoPagamentoMistoAVistaEAPrazo` |
| 3 parcelas | `fechaVenda_deveFecharAVendaUmaVezPorFormaDePagamento` |
| E6 I9 | `fechaVenda_deveLancarExcecao_quandoParcelaAVistaVemSemValor` |
| E7 I11 | `fechaVenda_deveLancarErroTecnico_quandoTituloNaoExiste` (T-9a) |
| R2 · E8 I12 | `fechaVenda_deveLancarExcecao_quandoAVistaEmDinheiroSemCaixaAberto` |
| R3 · E7 V10 | `fechaVenda_deveLancarNoCartao_quandoAVistaComCartaoDeDebito` (T-2) |
| R4 · E7 V11 | `fechaVenda_deveLancarNoCartao_quandoAVistaComCartaoDeCredito` |
| R5 · E7 I10 | `fechaVenda_deveFecharSemRegistrarRecebimento_quandoTituloAVistaNaoEDinheiroNemCartao` (T-3) |
| R7 · E9 I13 | `fechaVenda_deveLancarExcecao_quandoVendaAPrazoSemCliente` |
| E11 V15 | `addProduto_deveSalvarProdutoNaVenda_quandoVendaEstaAberta` · `removeProduto_deveRemoverItem_quandoVendaEstaAberta` |
| E11 I15 | `addProduto_deveRecusar_quandoVendaEstaFechada` · `removeProduto_deveRecusar_quandoVendaEstaFechada` |
| E11 I16 (remover) | `removeProduto_deveResponderOk_quandoVendaNaoExiste` |
| E12 V16 · E13 V19 | `busca_deveConsultarVendasAbertas_quandoSituacaoAbertaESemCodigo` |
| E12 V17 | `busca_deveConsultarVendasFechadas_quandoSituacaoDiferenteDeAberta` |
| E13 V18 | `busca_deveConsultarPorCodigo_quandoFiltroInformaCodigo` |

Com os casos novos, todas as classes, limites e regras listados acima têm pelo
menos um teste.
