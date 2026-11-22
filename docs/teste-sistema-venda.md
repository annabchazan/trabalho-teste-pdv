# Testes de sistema (Selenium) e requisito não funcional — Venda

**Responsável:** Felipe Martins Bittencourt
**Entrega 2, Etapa 6.** Decisões registradas em
[`docs/ai/decisoes-entrega2-venda.md`](ai/decisoes-entrega2-venda.md)
(D-23 a D-25).

> Este é o rascunho. A versão oficial vai para o Google Docs do grupo, na
> seção de testes de sistema.

## Resumo

| Item | Resultado |
|---|---|
| Classe de teste | [`VendaSistemaIT.java`](../src/test/java/net/originmobi/pdv/service/VendaSistemaIT.java) |
| Casos funcionais | TS-VEN-01 a 06: 5 passaram; o TS-VEN-06 fica com `@Ignore` por causa do defeito D-VEN-01, que foi confirmado pela tela |
| Requisito não funcional | TS-VEN-07, desempenho: atendido em 3 das 4 execuções; uma falha intermitente foi registrada |
| Evidências | [`docs/evidencias/entrega2/sistema/`](evidencias/entrega2/sistema/) |

## 1. Abordagem

Os testes de sistema usam a aplicação completa, no ar, pela interface web. O
Selenium WebDriver controla o Chrome como um usuário faria: entra com o login
`gerente`, abre a tela da venda, inclui produtos, abre o modal de pagamento,
escolhe a forma de pagamento e o título e clica em "Pagar". As verificações
olham o que aparece na tela (alertas, itens, total, botões desabilitados) e
conferem o banco quando é preciso.

Os casos automatizam os testes manuais da Entrega 1
([`docs/testes-manuais/CT-VENDA-01.md`](testes-manuais/CT-VENDA-01.md)).

### Ambiente

- Aplicação: `docker compose up -d`, em `http://localhost:8080` (Java 8,
  Spring Boot 2.0.2, MySQL 8 do `pdv-db`).
- Testes: rodam no Windows, com Maven 3.9 e Java 17, Selenium 4.13 e Chrome
  sem janela. O ChromeDriver é resolvido pelo próprio Selenium.
- Massa de dados criada direto no MySQL (porta 3307):

  | Dado | Código |
  |---|---|
  | produto "Produto Selenium", R$ 10,00, estoque 100 | 9201 |
  | título "Dinheiro Selenium" | 9201 |
  | formas de pagamento `00` (à vista) e `30` (a prazo) | 9201 e 9202 |
  | caixa "Caixa Selenium" | 9201 |
  | vendas | observação "Venda Selenium" |

  Tudo é apagado antes e depois de cada caso. O Caixa01, que já existia no
  banco, fica marcado como fechado durante o caso e volta como estava no fim
  (D-20). Isso foi conferido depois das execuções.

### Pré-condição: defeito D4 (Issue #10)

Num banco recém-criado pelo `docker-compose`, a coluna
`parcela.data_alteracao` é obrigatória e não tem valor padrão. Com isso, o
MySQL 8 recusa o insert da parcela e todo fechamento a prazo falha. É o
defeito D4, registrado na Issue #10. Ele não é de `VendaService`.

O teste TS-VEN-03 (venda a prazo) confere a coluna antes de rodar. Se ela não tiver valor padrão,
o teste aparece como **pulado**, com uma mensagem que aponta a Issue #10, em
vez de falhar. Para que ele rode, aplique antes no banco:

```sql
alter table parcela modify data_alteracao timestamp not null default current_timestamp;
```

Os resultados registrados neste documento foram obtidos num banco com esse
ajuste, aplicado à mão na Entrega 1 (ver `docs/testes-manuais/CT-VENDA-01.md`).
Quando a Issue #10 for corrigida, o teste volta a rodar sozinho.

## 2. Casos funcionais

| ID | Teste manual | Passos na tela | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| TS-VEN-01 | CT-VENDA-01 | Nova venda: escolher o cliente, informar a observação e salvar; inserir o produto pelo combo de busca; remover pelo "x" e confirmar | A venda é criada `ABERTA`; o item aparece na tabela e o total vai a R$ 10,00; depois de remover, a tabela fica vazia e o total volta a R$ 0,00 | igual ✅ |
| TS-VEN-02 | CT-VENDA-02 | Venda com 1 item e caixa aberto; "Gerar Venda", forma "à vista", título dinheiro, "Pagar" | Alerta "Venda finalizada com sucesso"; venda `FECHADA`; lançamento de R$ 10,00 no caixa; ao recarregar, "Gerar Venda" e o combo de produtos ficam desabilitados | igual ✅ |
| TS-VEN-03 | CT-VENDA-03 | Venda com cliente; forma "a prazo" (`30`) | Alerta de sucesso; venda `FECHADA`; parcela com vencimento daqui a 30 dias | igual ✅ |
| TS-VEN-04 | CT-VENDA-06 | Sem caixa aberto; forma "à vista", dinheiro | Alerta "nenhum caixa aberto"; venda continua `ABERTA`; nenhum recebimento gravado | igual ✅ |
| TS-VEN-05 | CT-VENDA-07 | Venda sem cliente; forma "a prazo" | Alerta "Venda sem cliente, verifique"; venda continua `ABERTA` | igual ✅ |
| TS-VEN-06 | CT-VENDA-08 | Caixa aberto; à vista em dinheiro com desconto de R$ 2,00 | Venda com total R$ 8,00 e lançamento de **R$ 8,00** no caixa | **lança R$ 12,00** ❌ DEFEITO D-VEN-01 |

**TS-VEN-06.** O teste espera o comportamento correto. Ele foi rodado uma vez
sem o `@Ignore` e falhou com `expected:<8.0> but was:<12.0>`: a venda é
gravada com total de R$ 8,00, mas o caixa recebe R$ 12,00. Isso confirma pela
interface o defeito D-VEN-01, já caracterizado nos testes unitários:
`avistaDinheiro` recebe o desconto e o acréscimo com os parâmetros trocados.
Seguindo o padrão do grupo, o teste fica com
`@Ignore("D-VEN-01: ...")` até a correção. Basta remover a anotação para
reproduzir a falha.

**Observação de interface (fora dos casos).** Na tela da venda fechada, o "x"
para remover o produto continua aparecendo. O serviço recusa a remoção
("Venda fechada"), mas o botão não deveria estar disponível. Fica registrado
para a avaliação de usabilidade.

## 3. Requisito não funcional: desempenho (TS-VEN-07)

| Item | Definição |
|---|---|
| Característica (ISO/IEC 25010) | Eficiência de desempenho, no aspecto do tempo de resposta |
| Requisito | Fechar uma venda pela tela leva **no máximo 2 s**, do clique em "Pagar" até a mensagem de confirmação |
| Justificativa | O fechamento acontece com o cliente esperando no caixa. Esperas de mais de 2 s passam a sensação de sistema travado e provocam cliques repetidos |
| Medição | 1 fechamento de aquecimento, que não entra na conta, e mais 5 fechamentos medidos, cada um com uma venda nova, à vista em dinheiro. O tempo vai do clique até o alerta aparecer, medido no próprio teste com `System.nanoTime()` |
| Critério de aceitação | O **maior** dos 5 tempos é de no máximo 2.000 ms |

### Resultados

| Execução | Tempos (ms) | Média | Maior | Resultado |
|---|---|---|---|---|
| 1ª (suíte completa) | 879 · 709 · **5.339** · 733 · 738 | 1.680 | 5.339 | ❌ falhou |
| 2ª (só o TS-VEN-07) | 569 · 552 · 570 · 666 · 632 | 598 | 666 | ✅ |
| 3ª (só o TS-VEN-07) | 604 · 565 · 607 · 576 · 603 | 591 | 607 | ✅ |
| 4ª (suíte completa, final) | 573 · 570 · 584 · 556 · 555 | 568 | 584 | ✅ |

### Análise da falha da 1ª execução

O log de SQL da aplicação (Hibernate em nível debug) mostra que o atraso
ficou no servidor, dentro do fechamento. Ao atualizar a venda, o Hibernate
cria uma tabela temporária (`HT_venda`), porque a forma de pagamento da venda
fica numa tabela de ligação. No fechamento lento, houve uma pausa de **3,8 s**
logo depois do `drop temporary table HT_venda`, antes do comando seguinte (a
baixa de estoque). Nos outros 8 fechamentos bem-sucedidos da mesma execução, esse
mesmo passo levou de 2 a 80 ms.

O teste não identifica a causa exata. As hipóteses são uma espera do MySQL ao
apagar a tabela temporária ou uma pausa da JVM. Mas o teste localizou onde
está o risco.

### Conclusão

O requisito é atendido na maior parte das execuções: a média ficou perto de
0,6 s, bem abaixo do limite. Ainda assim, houve **uma violação intermitente**
(5,3 s em 20 medições, cerca de 5%).

Por decisão do responsável (D-24), o critério continua o aprovado. A violação
fica registrada como não conformidade intermitente, a ser investigada. Uma
possível melhoria é trocar o `update` em massa do `VendaRepository.fechaVenda`,
que gera a tabela temporária, por uma atualização da entidade já carregada.
Isso pode ser considerado na Etapa 7.

Como o critério é o maior tempo, o teste pode falhar de novo de vez em
quando. Se isso acontecer, a mensagem da falha traz os 5 tempos medidos.

## 4. Evidências

Em [`docs/evidencias/entrega2/sistema/`](evidencias/entrega2/sistema/):

| Arquivo | Conteúdo |
|---|---|
| `failsafe-VendaSistemaIT.png` | Relatório da execução final: 7 testes, 6 passaram, 1 pulado (TS-VEN-06) |
| `TS-VEN-02-modal-pagamento.png` | Modal de pagamento preenchido pelo Selenium, logo antes do clique em "Pagar" |
| `TS-VEN-02-venda-fechada.png` | Tela da venda depois do fechamento: situação `FECHADA` e botões desabilitados |

Os prints de tela são gravados pelo próprio teste em
`target/selenium-prints/`.

## 5. Como reproduzir

Suba a aplicação e o banco:

```sh
docker compose up -d
```

Rode no computador onde o Chrome está instalado, com Java 11 ou mais novo
(o Selenium 4.13 não roda em Java 8):

```sh
mvn verify -Dtest=NenhumTeste -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=VendaSistemaIT -Djacoco.skip=true
```

Para ver o navegador, acrescente `-Dselenium.headless=false`. Os tempos do
TS-VEN-07 aparecem na saída como
`TS-VEN-07 tempos de fechamento (ms): [...]`.
