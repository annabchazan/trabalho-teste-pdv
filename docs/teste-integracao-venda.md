# Testes de integração — `VendaService`

**Responsável:** Felipe Martins Bittencourt
**Entrega 2, Etapa 5.** Decisões registradas em
[`docs/ai/decisoes-entrega2-venda.md`](ai/decisoes-entrega2-venda.md)
(D-19 a D-21).

> Este é o rascunho. A versão oficial vai para o Google Docs do grupo, na
> seção de testes de integração.

## Resumo

| Item | Resultado |
|---|---|
| Classe de teste | [`VendaServiceIT.java`](../src/test/java/net/originmobi/pdv/service/VendaServiceIT.java) |
| Casos | TI-VEN-01 a 08 (9 métodos de teste) |
| Execução | 9 passaram, 0 falhas, 0 erros |
| Defeitos novos | nenhum |
| Evidência | [`docs/evidencias/entrega2/integracao/failsafe-VendaServiceIT.png`](evidencias/entrega2/integracao/failsafe-VendaServiceIT.png) |

## 1. O que os testes unitários não cobrem

Nos testes unitários (`VendaServiceTest`), todas as 11 dependências de
`VendaService` são simuladas com o Mockito. Eles mostram que o serviço chama
cada dependência com os valores certos, mas não mostram o que acontece de fato
no banco. Os testes de integração cobrem três pontos:

1. **Consultas do `VendaRepository`.** Exemplos: `fechaVenda`, que só altera
   vendas sem `data_finalizado`; `findBySituacaoEquals`; `qtdVendasEmAberto`.
2. **A transação do `fechaVenda`** (`@Transactional`). O método grava o
   recebimento antes de verificar o caixa e atualiza a venda antes de baixar o
   estoque. Se a transação não for desfeita quando um erro acontece depois,
   ficam gravados no banco um recebimento sem venda fechada ou uma venda
   fechada sem baixa de estoque.
3. **Colaboração com os serviços e os triggers reais.** `CaixaService`,
   `CaixaLancamentoService`, `ReceberService`, `ParcelaService`,
   `ProdutoService` e `TituloService` rodam de verdade, junto com os triggers
   do MySQL que atualizam o valor da venda, o total do caixa e o estoque.

## 2. Ambiente

- `@SpringBootTest` sem servidor web, com o contexto completo da aplicação.
- Banco MySQL 8 do `docker-compose` (`pdv-db`), com o esquema, os dados
  iniciais e os triggers criados pelo Flyway. É o mesmo padrão de
  `NotaFiscalItemServiceIT`.
- O usuário logado é simulado como `gerente` pelo `SecurityContextHolder`. O
  singleton `Aplicacao` é zerado antes e depois de cada caso.
- A classe de teste **não** usa `@Transactional`. Assim, a transação de
  `fechaVenda` é a real e pode ser confirmada ou desfeita.

### Massa de dados

| Dado | Código | Observação |
|---|---|---|
| Produto "Produto IT" | 9001 | valor 10,00, controla estoque; o saldo é ajustado em cada caso |
| Título "Dinheiro IT" | 9001 | tipo Dinheiro (`DIN`) |
| Forma de pagamento à vista | 9001 | `00` |
| Forma de pagamento a prazo | 9002 | `30` |
| Caixa "Caixa IT" | 9001 | criado só nos casos que precisam de caixa aberto |
| Vendas | gerados pelo banco | identificadas pela observação "Venda IT" |
| Usuário e cliente | 1 | `gerente` e o cliente carregados pelo Flyway, só leitura |

Tudo é apagado antes e depois de cada caso. O banco local já tinha o
"Caixa01" aberto, dos testes manuais da Entrega 1. Ele é marcado como fechado
durante o caso e restaurado no fim, sem receber lançamentos (D-20). Depois da
execução, o banco foi conferido: o Caixa01 voltou como estava e não sobrou
nenhum registro dos testes.

## 3. Casos de teste

| ID | Requisito / regra | Cenário | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| TI-VEN-01 | RF-01 | `abreVenda` de uma venda nova | Venda gravada `ABERTA`, ligada ao `gerente`, valor dos produtos 0 e sem data de finalização | igual ✅ |
| TI-VEN-02a | RF-02 · E11 V15 | `addProduto` em venda aberta | Item gravado em `venda_produtos`; o trigger soma 10,00 ao valor da venda | igual ✅ |
| TI-VEN-02b | RF-02 · E11 I15 | `addProduto` em venda fechada | Resposta "Venda fechada"; nenhum item gravado | igual ✅ |
| TI-VEN-03 | RF-03 | Duas vendas abertas e uma fechada; `busca` por situação e por código; `qtdAbertos` | Cada busca devolve só as vendas certas; `qtdAbertos` aumenta em 2 | igual ✅ |
| TI-VEN-04 | RF-04 · R1 | Fechar à vista em dinheiro, com caixa aberto e estoque 5 | Venda `FECHADA` com total 10,00 e data de finalização; forma de pagamento gravada; recebimento de 10,00; lançamento `RECEBIMENTO`/`ENTRADA` de 10,00 no caixa; total do caixa 10,00 pelo trigger; estoque 4 e movimentação `SAIDA` "Venda N" | igual ✅ |
| TI-VEN-05 | RF-04 · R6 | Fechar a prazo (`30`), com cliente | Venda `FECHADA`; recebimento com 1 parcela de 10,00, não quitada, sequência 1, vencimento daqui a 30 dias; nenhum lançamento no caixa | igual ✅ |
| TI-VEN-06 | RF-04 · R2 | Fechar à vista em dinheiro **sem caixa aberto** | Erro "nenhum caixa aberto" e **transação desfeita**: o recebimento, já gravado antes do erro, some; a venda continua `ABERTA`; o estoque não muda | igual ✅ |
| TI-VEN-07 | RF-04 | Fechar com **estoque insuficiente** (saldo 0) | Erro "...não tem estoque suficiente..." **depois** do update da venda e do lançamento no caixa; tudo é desfeito: a venda continua `ABERTA`, sem total, sem recebimento e sem lançamento, e o total do caixa continua 0 | igual ✅ |
| TI-VEN-08 | E1 I1 | Fechar venda já fechada; chamar direto o `update` do repositório | Erro "venda fechada"; o `update` do repositório não altera a venda, por causa da condição `data_finalizado is null` | igual ✅ |

### Como saber que a transação foi realmente desfeita

Os casos TI-VEN-06 e 07 só provam alguma coisa se as gravações chegaram a
acontecer antes do erro. Para conferir, os dois casos foram rodados de novo
com o log de SQL do Hibernate ligado
(`logging.level.org.hibernate.SQL=debug`, já configurado no projeto):

- nos dois casos aparece o `insert into receber`;
- no TI-VEN-07 também aparecem o `update venda set situacao...` e o
  `insert into caixa_lancamento`.

Depois do erro, nenhum desses registros ficou no banco. Logo, a transação foi
desfeita.

## 4. Conclusão

- **Não apareceram defeitos novos.** As consultas, os triggers e a transação
  de `fechaVenda` funcionam como o requisito pede.
- **O resultado positivo é a contribuição destes testes:** os testes
  unitários não conseguiam mostrar que a transação é desfeita, e agora isso
  está demonstrado.
- **Os defeitos já registrados continuam válidos.** Venda de valor zero ou
  negativo, acréscimo negativo e título à vista de outro tipo dependem só da
  lógica do serviço. Eles estão documentados em
  [`teste-funcional-venda.md`](teste-funcional-venda.md) e não foram
  repetidos aqui.
- **Uma observação para a Etapa 7.** A verificação de estoque acontece duas
  vezes: no `ProdutoService` e no trigger `atualiza_produto_estoque`, que
  dispara um erro de banco. Hoje a verificação do serviço vem antes e é a que
  aparece para o usuário.

## 5. Como reproduzir

Suba o banco:

```sh
docker compose up -d pdv-db
```

Rode só a `VendaServiceIT` num container Java 8 ligado à rede do `pdv-db`. O
parâmetro `-Dtest=NenhumTeste` pula os testes unitários:

```sh
docker run --rm --network trabalho-teste-pdv_default -v "$PWD":/app -w /app -v pdv-m2:/root/.m2 trabalho-teste-pdv-pdv-app \
  mvn -B verify org.apache.maven.plugins:maven-surefire-report-plugin:3.2.5:failsafe-report-only \
  -Dtest=NenhumTeste -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=VendaServiceIT -Djacoco.skip=true
```

O relatório fica em `target/site/failsafe-report.html`. Para ver o SQL de um
caso, rode só ele com `-Dit.test=VendaServiceIT#nomeDoMetodo` e procure por
`insert into` no log.
