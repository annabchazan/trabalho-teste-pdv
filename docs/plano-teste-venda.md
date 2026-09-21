# Plano de Teste — contribuição de Felipe Martins Bittencourt

Blocos prontos para colar nas seções correspondentes do
**Plano_de_Teste_PDV** (Google Docs). Cada título abaixo indica a seção de
destino no documento do grupo.

Componente: **Venda — `VendaService`**
Funcionalidade testada manualmente: **Realizar venda (abrir, adicionar produtos e fechar)**
Execução em **21/09/2026**, sistema subido com `docker compose up -d`
(aplicação em http://localhost:8080, MySQL 8.0), usuário `gerente`.
Cada resultado foi verificado por **consulta direta ao banco**, e não apenas
pela mensagem exibida na tela.

---

## → Seção 5. Distribuição das Responsabilidades

Linha a acrescentar na tabela:

| Membro | Classe — teste unitário | Funcionalidade — teste manual |
|---|---|---|
| Felipe Martins Bittencourt | `VendaService` | Realizar venda (abrir, adicionar produtos e fechar) |

---

## → Seção 7. Casos de Teste Unitário

### VendaService — Felipe Martins Bittencourt

32 casos implementados com JUnit 4 e Mockito, isolando as 11 dependências da
classe. Arquivo: `src/test/java/net/originmobi/pdv/service/VendaServiceTest.java`

Oito casos são **testes de caracterização**: o defeito foi identificado durante
o projeto dos testes, e a asserção fixa o **comportamento atual** (errado) em vez
do esperado, de modo que a suíte passe e o defeito fique documentado. Estão
marcados abaixo como *Passou (caracteriza defeito)* e devem ser reescritos junto
com a correção, na Entrega 2.

#### Método `abreVenda()`

| ID | Cenário | Resultado esperado | Status |
|---|---|---|---|
| TU-VEN-01 | Venda nova, sem código | Venda salva com situação ABERTA, usuário logado, valor de produtos zerado | Passou |
| TU-VEN-02 | Venda já existente | Apenas atualiza cliente e observação; não cria novo registro | Passou |
| TU-VEN-03 | Falha do repositório ao salvar | Deveria propagar o erro; devolve código nulo silenciosamente | Passou (caracteriza defeito D-VEN-04) |

#### Método `busca()`

| ID | Cenário | Resultado esperado | Status |
|---|---|---|---|
| TU-VEN-04 | Filtro informa o código | Consulta por código, ignorando a situação | Passou |
| TU-VEN-05 | Situação ABERTA, sem código | Consulta as vendas abertas | Passou |
| TU-VEN-06 | Situação diferente de ABERTA | Consulta as vendas fechadas | Passou |

#### Métodos `addProduto()` e `removeProduto()`

| ID | Cenário | Resultado esperado | Status |
|---|---|---|---|
| TU-VEN-07 | Adicionar produto em venda aberta | Produto gravado na venda; retorna "ok" | Passou |
| TU-VEN-08 | Adicionar produto em venda fechada | Retorna "Venda fechada"; nada é gravado | Passou |
| TU-VEN-09 | Falha do repositório ao adicionar | Deveria avisar o usuário; responde "ok" sem gravar | Passou (caracteriza defeito D-VEN-04) |
| TU-VEN-10 | Remover item de venda aberta | Item removido; retorna "ok" | Passou |
| TU-VEN-11 | Remover item de venda fechada | Retorna "Venda fechada"; nada é removido | Passou |
| TU-VEN-12 | Remover item de venda inexistente | Deveria avisar o usuário; `NullPointerException` é engolida e responde "ok" | Passou (caracteriza defeito D-VEN-04) |

#### Métodos `lista()` e `qtdAbertos()`

| ID | Cenário | Resultado esperado | Status |
|---|---|---|---|
| TU-VEN-13 | Listagem de vendas | Delega ao repositório e devolve a lista | Passou |
| TU-VEN-14 | Quantidade de vendas em aberto | Devolve a contagem do repositório | Passou |

#### Método `fechaVenda()` — validações de entrada

| ID | Cenário | Resultado esperado | Status |
|---|---|---|---|
| TU-VEN-15 | Venda já fechada | Exceção "venda fechada" | Passou |
| TU-VEN-16 | Valor de produtos zero ou negativo | Exceção "Venda sem valor, verifique" | Passou |
| TU-VEN-17 | À vista em dinheiro sem caixa aberto | Exceção "nenhum caixa aberto" | Passou |
| TU-VEN-18 | Venda a prazo sem cliente | Exceção "Venda sem cliente, verifique" | Passou |
| TU-VEN-19 | Parcela à vista sem valor | Exceção "Parcela sem valor, verifique" | Passou |
| TU-VEN-20 | Parcela a prazo sem valor | Exceção "valor de recebimento invalido" | Passou |

#### Método `fechaVenda()` — caminhos principais

| ID | Cenário | Resultado esperado | Status |
|---|---|---|---|
| TU-VEN-21 | À vista em dinheiro | Gera lançamento de entrada no caixa, fecha a venda e movimenta o estoque | Passou |
| TU-VEN-22 | À vista no cartão de crédito | Gera lançamento de cartão e **não** movimenta o caixa | Passou |
| TU-VEN-23 | A prazo | Gera uma parcela por prazo, com vencimento deslocado e vinculada ao *receber* | Passou |
| TU-VEN-24 | Pagamento misto (à vista + a prazo) | Combina lançamento de caixa e geração de parcela | Passou |

#### Método `fechaVenda()` — defeitos caracterizados

| ID | Cenário | Resultado esperado | Status |
|---|---|---|---|
| TU-VEN-25 | Desconto de R$ 10,00 numa venda de R$ 100,00, à vista | Caixa deveria receber R$ 90,00; recebe **R$ 110,00** enquanto a venda fecha por R$ 90,00 | Passou (caracteriza defeito D-VEN-01) |
| TU-VEN-26 | Acréscimo de R$ 20,00 numa venda de R$ 100,00, a prazo | Parcela deveria ser R$ 120,00; sai por **R$ 80,00** enquanto a venda fecha por R$ 120,00 | Passou (caracteriza defeito D-VEN-01) |
| TU-VEN-27 | Venda mista válida de R$ 60,00 (R$ 50,00 à vista + R$ 10,00 a prazo) | Deveria ser aceita; é recusada porque o somatório calcula 2 × R$ 50,00 | Passou (caracteriza defeito D-VEN-02) |
| TU-VEN-28 | Venda a prazo em 3 parcelas | Deveria emitir um update de fechamento; emite **três** | Passou (caracteriza defeito D-VEN-03) |

#### Método `fechaVenda()` — falhas de infraestrutura

| ID | Cenário | Resultado esperado | Status |
|---|---|---|---|
| TU-VEN-29 | Falha ao cadastrar o *receber* | Encapsula em "Erro ao fechar a venda, chame o suporte" | Passou |
| TU-VEN-30 | Falha no lançamento de caixa | Mesma mensagem | Passou |
| TU-VEN-31 | Falha ao gerar a parcela | Deveria preservar a causa; lança `RuntimeException` **sem mensagem** | Passou (caracteriza defeito D-VEN-08) |
| TU-VEN-32 | Falha ao atualizar a venda | Encapsula em "Erro ao fechar a venda, chame o suporte" | Passou |

**Resultado da execução:** 32 testes, 0 falhas, 0 erros
(`docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B -Dtest=VendaServiceTest test`).

---

## → Seção 8. Casos de Teste Manual

### Funcionalidade: realizar venda — Felipe Martins Bittencourt

**Pré-condições:** cliente cadastrado; título do tipo Dinheiro; título do tipo
Cartão Crédito **vinculado a uma máquina de cartão**; máquina de cartão vinculada
a um caixa do tipo BANCO; produto vendável com estoque; caixa aberto.

O levantamento dessas pré-condições revelou por si só **três defeitos**
(D-VEN-05, D-VEN-06 e D-VEN-07), além de reproduzir de forma independente os
defeitos **D4** e **D5** já relatados pelo módulo de recebimento — sem a correção
dos dois, nenhuma venda de produto com controle de estoque pode ser fechada.

#### CT-VENDA-01 — Abrir venda, adicionar e remover produtos

**Status: Passou**

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Acessar Vendas → nova venda | — | Formulário exibido | OK |
| 2 | Selecionar o cliente e informar observação | Cliente cadastrado | Campos preenchidos | OK |
| 3 | Salvar a venda | — | Mensagem "Pedido Salvo" e redirecionamento para a venda | OK |
| 4 | Conferir a situação | — | Situação ABERTA, total R$ 0,00, usuário logado registrado | OK |
| 5 | Adicionar o produto | Picolé, R$ 6,50 | Item na tabela; total atualizado | OK |
| 6 | Adicionar o mesmo produto mais 2 vezes | — | 3 linhas; total = 3 × preço | OK |
| 7 | Remover uma das linhas | — | Restam 2 linhas; total recalculado | OK — itens 1 a 3 removidos, 2 mantidos na venda |
| 8 | Conferir na listagem de vendas abertas | — | Venda listada com cliente e valor corretos | OK |
| 9 | Reeditar a observação e salvar | Novo texto | Observação persistida, sem criar nova venda | OK |

#### CT-VENDA-02 — Fechar venda à vista em dinheiro

**Status: Passou**

Executado após a correção de ambiente dos defeitos D4 e D5 (ver seção 10). Antes
dela, o caso falhava de forma reprodutível em `ProdutoService.movimentaEstoque`.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento | — | Valor total igual ao total dos produtos | OK |
| 2 | Selecionar a forma de pagamento | À Vista (`00`) | Uma linha de parcela | OK |
| 3 | Informar o valor da parcela | R$ 13,00 | Campo aceita | OK |
| 4 | Selecionar o título | Dinheiro | Título selecionado | OK |
| 5 | Confirmar o pagamento | — | "Venda finalizada com sucesso" | OK |
| 6 | Conferir a venda | — | Situação FECHADA, data de finalização gravada | OK — venda 1, R$ 13,00, finalizada às 19:14:06 |
| 7 | Conferir os lançamentos do caixa | — | Entrada tipo RECEBIMENTO no valor da venda | OK — "Recebimento de venda á vista", R$ 13,00, RECEBIMENTO / ENTRADA |
| 8 | Conferir o estoque do produto | — | Estoque debitado em 2 unidades | OK — saldo reduzido conforme a venda |

**Evidência no banco:** venda 1 com situação FECHADA e valor R$ 13,00;
lançamento de caixa de R$ 13,00 tipo RECEBIMENTO / ENTRADA; saldo do caixa
atualizado; `produto_estoque` debitado.

#### CT-VENDA-03 — Geração de parcela a prazo

**Status: Passou**

> **Ressalva de execução:** este caso e o CT-VENDA-04 foram exercitados numa
> **única venda com pagamento misto** (forma `00/33`: uma entrada no cartão mais
> uma parcela a prazo), e não em execuções isoladas. Os dois ramos do
> `fechaVenda` foram percorridos, mas as variantes puras — forma `30` sozinha e
> forma `00` sozinha com título de cartão — permanecem projetadas e não executadas.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Selecionar a forma de pagamento | Uma entrada + 30 (`00/33`) | Duas linhas de parcela | OK |
| 2 | Informar os valores e os títulos | R$ 3,25 no cartão + R$ 3,25 a prazo | Campos preenchidos | OK |
| 3 | Confirmar o pagamento | — | "Venda finalizada com sucesso" | OK |
| 4 | Conferir a venda | — | Situação FECHADA | OK — venda 3, R$ 6,50, finalizada às 19:20:20 |
| 5 | Conferir a parcela gerada | — | Uma parcela, sequência 1, vinculada ao cliente | OK — parcela de R$ 3,25, sequência 1 |
| 6 | Conferir o vencimento | — | Data de hoje + 33 dias | OK — vencimento 24/10/2026 (21/09 + 33 dias) |

#### CT-VENDA-04 — Lançamento no cartão de crédito

**Status: Passou** (mesma venda do CT-VENDA-03 — ver ressalva acima)

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Selecionar o título de cartão na parcela à vista | Cartão Crédito | Título selecionado | OK |
| 2 | Confirmar o pagamento | — | "Venda finalizada com sucesso" | OK |
| 3 | Conferir os lançamentos de cartão | — | Lançamento no valor da parcela, tipo CREDITO | OK — R$ 3,25, CREDITO, situação APROCESSAR |
| 4 | Conferir os lançamentos do caixa | — | Nenhum lançamento de dinheiro para esta venda | OK — caixa não foi movimentado |

**Observação:** o lançamento foi gravado com `vl_liq_parcela` **negativo**
(−R$ 0,36), porque a taxa de crédito da máquina foi aceita como 111,11%. Ver
defeito D-VEN-10.

#### CT-VENDA-05 — Fechar venda sem valor de produtos

**Status: Passou**

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir uma venda sem produtos | — | Venda aberta, total R$ 0,00 | OK |
| 2 | Tentar fechar a venda | Valor de parcela vazio | Mensagem "Venda sem valor, verifique" | OK — mensagem exibida ao usuário |
| 3 | Conferir a venda | — | Continua ABERTA | OK |
| 4 | Conferir caixa e estoque | — | Nenhum lançamento, nenhuma baixa | OK |

**Evidência:** a resposta HTTP do endpoint de fechamento traz
`{"status":500,"message":"Venda sem valor, verifique"}`, que o JavaScript da tela
converte no alerta exibido ao usuário. A guarda atua na primeira linha do
`fechaVenda`, antes de qualquer gravação.

#### CT-VENDA-06 — Fechar venda à vista em dinheiro sem caixa aberto

**Status: Passou**

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Sem caixa aberto, tentar fechar a venda à vista | Título Dinheiro | Mensagem "nenhum caixa aberto" | OK — em duas execuções |
| 2 | Conferir a venda | — | Continua ABERTA, estoque inalterado | OK |
| 3 | Conferir se sobrou um *receber* órfão | — | Nenhum registro de recebimento para esta venda | OK — nenhum registro em `receber` |

**Ponto de atenção verificado.** O `Receber` é cadastrado **antes** da validação
do caixa, o que sugeriria um registro órfão. A verificação no banco mostrou que
isso **não ocorre**: o `@Transactional` do `fechaVenda` desfaz a gravação quando
a exceção é lançada. O comportamento transacional está correto, e o caso serve
como evidência de que o fechamento é atômico.

#### CT-VENDA-07 a CT-VENDA-11

**Status: Não executados nesta entrega.** Projetados e documentados em
`docs/testes-manuais/CT-VENDA-01.md`, cobrindo a guarda de venda a prazo sem
cliente e a confirmação, pela interface, dos defeitos D-VEN-01 a D-VEN-04.
Ficam como escopo da Entrega 2.

### Casos de execução exploratória

Os três casos a seguir **não foram projetados previamente**. Eles surgiram
durante o levantamento das pré-condições, quando a tentativa de cadastrar a massa
de dados produziu exceções técnicas na tela. Foram formalizados depois, a partir
do comportamento observado e do registro no log da aplicação, e são os únicos
casos manuais com status **Falhou** nesta entrega — pela razão discutida na
observação ao final desta seção.

#### CT-VENDA-12 — Fechar venda sem título selecionado

**Status: FALHOU** — defeito D-VEN-05. Reproduzido **4 vezes**.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o modal de pagamento numa venda com itens | — | Modal exibido | OK |
| 2 | Selecionar a forma de pagamento | À Vista (`00`) | Linha de parcela com combo de título preenchido | **Combo de título vazio** |
| 3 | Informar o valor e confirmar o pagamento | Valor total da venda | Mensagem de validação amigável, ex.: "Selecione um título" | **`NumberFormatException: Zero length string`** exibida crua ao usuário |
| 4 | Conferir a venda | — | Continua ABERTA | OK — nenhuma gravação |

**Causa raiz:** `VendaService:202` executa `Long.decode(titulos[i])` sem validar a
string. A montante, `venda.js` dispara duas requisições assíncronas
independentes no evento `change` da forma de pagamento — uma que cria as linhas
de parcela e outra que carrega a lista de títulos. Quando a segunda responde
antes da primeira, os `option` são inseridos num seletor que ainda não existe no
DOM e o combo fica vazio, sem qualquer aviso.

#### CT-VENDA-13 — Fechar venda no cartão com título sem máquina vinculada

**Status: FALHOU** — defeito D-VEN-06. Reproduzido **5 vezes**.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Cadastrar título do tipo Cartão Crédito | Descrição e tipo | O sistema deveria exigir a máquina de cartão | **Título salvo sem máquina**, sem aviso |
| 2 | Fechar uma venda usando esse título | Forma À Vista, título de cartão | Lançamento de cartão gerado | **`NullPointerException`** exibida ao usuário |
| 3 | Conferir a venda | — | Continua ABERTA | OK — nenhuma gravação |

**Causa raiz:** `CartaoLancamentoService:64` chama
`titulo.get().getMaquina().getTaxa_credito()` sem verificar se a máquina existe.
O cadastro permite a inconsistência porque `Titulo.maquina` não é obrigatório no
modelo e o campo *Máquina* só é inserido no formulário pelo evento `change` do
combo de tipo — ao **editar** um título existente o campo sequer aparece, já que
o evento não dispara no carregamento da página.

#### CT-VENDA-14 — Adicionar produto com balança sem informar o valor

**Status: FALHOU** — defeito D-VEN-07. Reproduzido **1 vez**.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Numa venda aberta, selecionar um produto com balança | Produto com `balanca = SIM` | Diálogo pede o valor | OK |
| 2 | Cancelar o diálogo ou confirmar em branco | (vazio) | Inclusão cancelada, ou mensagem de validação | **`NumberFormatException: empty String`** |

**Causa raiz:** `VendaController:122` executa
`Double.valueOf(request.get("valorBalanca"))` sem validar. O JavaScript envia
string vazia quando o `prompt` é cancelado ou confirmado em branco.

### Observação sobre a distribuição dos resultados

Os seis casos **projetados** que foram executados (CT-VENDA-01 a 06) passaram, e
os três que falharam são todos **exploratórios**. Isso não indica que o módulo
esteja saudável: os casos projetados para expor defeito são o CT-VENDA-08 ao 11,
escritos especificamente para confirmar, pela interface, os defeitos D-VEN-01 a
D-VEN-04 já caracterizados em teste unitário — e são exatamente os que não foram
executados nesta entrega.

Em outras palavras, a ausência de falhas entre os casos projetados é consequência
do **recorte da execução**, não da qualidade do código. Os quatro casos pendentes
têm resultado esperado já conhecido pelos testes unitários: uma venda de
R$ 100,00 com R$ 10,00 de desconto deve creditar R$ 110,00 no caixa, e uma venda
mista de R$ 50,00 mais R$ 10,00 deve ser recusada indevidamente. Executá-los na
Entrega 2 deve produzir quatro falhas previstas.

Vale registrar ainda que os três defeitos encontrados foram achados **preparando
a massa de dados**, e não executando os casos — o mesmo padrão relatado pelo
módulo de recebimento, cujos defeitos D4, D5 e D6 também surgiram nas
pré-condições. É um indício de que o esforço de teste manual rende mais no
caminho até o cenário do que no cenário em si.

---

## → Seção 10. Defeitos Encontrados

Dez defeitos no módulo de venda. Quatro foram encontrados projetando os testes
unitários e seis executando os testes manuais.

| ID | Defeito | Como foi encontrado | Severidade |
|---|---|---|---|
| D-VEN-01 | `avistaDinheiro()` e `aprazo()` são declarados `(..., Double acre, Double desc)` mas chamados com `(..., desc, acre)`: desconto e acréscimo trocam de papel. Uma venda de R$ 100,00 com R$ 10,00 de desconto fecha por R$ 90,00 e credita R$ 110,00 no caixa | Projeto dos testes unitários | Alta — divergência de caixa em toda venda com desconto ou acréscimo |
| D-VEN-02 | O somatório de conferência em `avistaDinheiro()` usa `vlParcelas[i]` dentro do laço de índice `aux`, somando sempre a **mesma** parcela. Vendas mistas com parcelas de valores diferentes são recusadas | Projeto dos testes unitários | Alta — impede venda mista legítima |
| D-VEN-03 | `vendas.fechaVenda()` está dentro do laço de formas de pagamento: uma venda em 3x dispara 3 updates. Os dois últimos não têm efeito apenas por causa do `and data_finalizado is null` da consulta | Projeto dos testes unitários | Média |
| D-VEN-04 | `abreVenda`, `addProduto` e `removeProduto` capturam a exceção e chamam `e.getStackTrace()`, que **não imprime nada**. A operação falha, o usuário recebe "ok" e nem o log guarda registro | Projeto dos testes unitários | Média — falha silenciosa e sem rastro |
| D-VEN-05 | Fechar a venda sem título selecionado gera `NumberFormatException: Zero length string` (`VendaService:202`, `Long.decode`) exibida crua ao usuário. A causa a montante é uma **condição de corrida** em `venda.js`: as linhas de parcela e a lista de títulos são carregadas por duas requisições assíncronas independentes, e quando a segunda responde primeiro o combo de título fica vazio | Execução do teste manual | Média |
| D-VEN-06 | Título do tipo cartão **sem máquina vinculada** gera `NullPointerException` em `CartaoLancamentoService:64`. A causa raiz é o cadastro: `Titulo.maquina` não é obrigatório e o campo *Máquina* só é inserido no formulário pelo evento `change` do tipo, então o título salva sem máquina e só quebra no fechamento da venda | Execução do teste manual | Alta — impede a venda no cartão |
| D-VEN-07 | `VendaController:122` faz `Double.valueOf(request.get("valorBalanca"))` sem validar. Ao adicionar produto com balança, cancelar ou deixar vazio o diálogo de quantidade gera `NumberFormatException: empty String` | Execução do teste manual | Baixa |
| D-VEN-08 | O `catch` de `aprazo()` executa `throw new RuntimeException()` sem mensagem nem causa. Confirmado na prática: o erro de banco do defeito D4 chegou ao usuário como alerta vazio, com a causa real perdida | Projeto dos testes unitários, confirmado na execução manual | Média — impede o diagnóstico de qualquer falha na geração de parcela |
| D-VEN-09 | O `Receber` é criado incondicionalmente, inclusive em venda à vista, mas só recebe parcela no ramo a prazo. Toda venda à vista deixa uma conta a receber sem parcelas; quando a venda não tem cliente, o registro fica ainda com o cliente nulo | Execução do teste manual | Baixa — sujeira de dados acumulativa |
| D-VEN-10 | A taxa de cartão é aceita sem validação de faixa e o líquido não é conferido. Uma taxa de 111,11% produziu lançamento com valor líquido **negativo** (−R$ 0,36 sobre parcela de R$ 3,25) | Execução do teste manual | Baixa |

### Observações metodológicas

**Testes de caracterização.** Os defeitos D-VEN-01 a D-VEN-04 foram encontrados
projetando os testes unitários, e os casos correspondentes (TU-VEN-03, 09, 12,
25 a 28 e 31) foram escritos afirmando o **comportamento atual, defeituoso**, e
não o esperado. A escolha difere da adotada no módulo de recebimento, que marcou
casos equivalentes com `@Ignore` e a asserção correta. A vantagem aqui é que
qualquer alteração futura no cálculo quebra o teste e força a revisão; a
desvantagem é que a suíte, lida isoladamente, parece aprovar o comportamento
errado — por isso cada caso traz um comentário explicando o defeito. Na Entrega 2
as duas abordagens devem ser uniformizadas.

**O que o teste manual acrescentou.** Os testes unitários de `VendaService`
isolam todas as 11 dependências com objetos simulados e não alcançam nada do que
foi encontrado na execução: D-VEN-05 e D-VEN-07 estão na camada de interface e no
JavaScript, D-VEN-06 depende do estado do cadastro de títulos, D-VEN-09 só é
visível inspecionando o banco após a venda, e D-VEN-10 depende de dados
cadastrados pelo operador. Nenhum deles é alcançável por teste unitário com
mocks — mesmo argumento levantado pelo módulo de recebimento a respeito de
D4 e D5.

**Confirmação independente de D4 e D5.** Os dois defeitos bloqueantes relatados
pelo módulo de recebimento foram reproduzidos nesta execução, num banco criado do
zero, antes de qualquer correção:

- **D5** — um ajuste de entrada de 5 unidades gravou `ajuste_produtos` com
  `qtd_nova = 5` e `estoque_movimentacao` com tipo ENTRADA, mas
  `produto_estoque.qtd` permaneceu **0**. O fechamento da venda foi recusado com
  "O produto de código 1 não tem estoque suficiente, verifique".
- **D4** — o fechamento a prazo falhou com
  `SQL Error 1364: Field 'data_alteracao' doesn't have a default value`.

**Correções de ambiente.** Para tornar o fechamento de venda executável, as
causas de D4 e D5 foram corrigidas **apenas no banco de dados local**, sem
alteração de nenhum arquivo de código de produção, reaplicando o SQL já validado
pelo módulo de recebimento. Após a correção, um novo ajuste de estoque creditou o
saldo corretamente e as vendas subsequentes o debitaram — o que valida a correção
proposta para D5 numa segunda instância, independente da primeira.
