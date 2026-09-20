# Casos de teste manual — Recebimento de parcelas

Responsável: **João Portela** · Funcionalidade: **Receber parcelas em aberto de um cliente**
Módulo: `RecebimentoService` / `ReceberController` / `RecebimentoController`

> **Status: projetado, aguardando execução.** A coluna *Resultado obtido* deve ser
> preenchida durante a execução, junto com o campo *Status* de cada caso. Este
> arquivo segue o mesmo formato usado pela Anna Chazan nos casos CT-NFI-01/02 do
> Plano de Teste.

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

## Pré-condições (massa de dados)

Executar na ordem. Sem isso, o fluxo de recebimento não chega a ser exibido.

1. **Empresa** cadastrada (menu *Empresa*) — necessária para o sistema operar.
2. **Cliente** cadastrado em *Pessoas*, com endereço completo.
   Anotar o **código do cliente** gerado: `________`
3. **Título** do tipo dinheiro cadastrado em *Títulos*, com sigla `DIN`.
   Anotar o código: `________`
4. **Caixa aberto** no dia (menu *Caixa* → *Abrir caixa*). Sem caixa aberto, o
   lançamento do recebimento falha.
5. **Parcelas em aberto para o cliente**: realizar uma venda e fechá-la **a prazo**
   (forma de pagamento com prazo, ex.: `30/60`), gerando ao menos **duas parcelas**.
   Anotar valor de cada parcela: `________` e `________`

---

## CT-REC-01 — Recebimento total de parcelas em aberto

**Objetivo:** verificar que o recebimento soma corretamente as parcelas selecionadas,
quita cada uma e gera o lançamento de entrada no caixa.

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Acessar *Contas a receber* (`/receber`) | - | Tela de listagem exibida com o filtro de cliente | |
| 2 | Selecionar o cliente no filtro e pesquisar | Nome do cliente da pré-condição | Lista exibe as parcelas em aberto do cliente e o total a receber | |
| 3 | Conferir o total exibido | - | Total = soma dos valores restantes das parcelas em aberto | |
| 4 | Marcar as duas parcelas em aberto | - | Parcelas marcadas | |
| 5 | Clicar em *Receber* | - | Sistema abre o recebimento (`/recebimento/{codigo}`) com o valor total preenchido | |
| 6 | Conferir o campo *Total* | - | Igual à soma das parcelas marcadas (passo 3) | |
| 7 | Selecionar o título | Título `DIN` da pré-condição | Título selecionado no combo | |
| 8 | Informar o valor recebido | Valor total das parcelas | Campo aceita o valor | |
| 9 | Clicar em *Receber* | - | Alerta "Recebimento realizado com sucesso" | |
| 10 | Recarregar a tela do recebimento | - | Campos *Desconto*, *Acréscimo*, *Valor recebido* e *Título* desabilitados (recebimento processado) | |
| 11 | Voltar a *Contas a receber* e filtrar o mesmo cliente | Nome do cliente | As parcelas recebidas não aparecem mais como em aberto | |
| 12 | Acessar *Caixa* → lançamentos do caixa aberto | - | Existe lançamento de **entrada**, tipo RECEBIMENTO, com a descrição "Referente ao recebimento {código}" e o valor recebido | |

**Observações da execução:**

_(preencher)_

---

## CT-REC-02 — Confirmar recebimento sem selecionar o título

**Objetivo:** verificar o tratamento de entrada inválida no campo obrigatório
*Título*. Este caso exercita, pela interface, o mesmo ponto do código coberto pelos
testes unitários TU-REC-08 / TU-REC-19 (defeito **D2** em
[`docs/bugs/defeitos-recebimento.md`](../bugs/defeitos-recebimento.md)).

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

**Pré-condição adicional:** repetir os passos 1 a 6 de CT-REC-01 com outro conjunto de
parcelas em aberto, de modo a ter um recebimento ainda **não processado**.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Na tela do recebimento, deixar o combo *Título* na opção vazia | (nenhum) | Combo permanece sem seleção | |
| 2 | Informar o valor recebido | Valor total das parcelas | Campo aceita o valor | |
| 3 | Clicar em *Receber* | - | Mensagem de validação amigável: "Selecione um título para realizar o recebimento" | |
| 4 | Conferir a lista de parcelas | - | Nenhuma parcela foi quitada | |
| 5 | Conferir os lançamentos do caixa | - | Nenhum lançamento novo foi gerado | |

**Observação sobre o defeito D2:** o `RecebimentoController` converte o título vazio
em `0L` antes de chamar o serviço, então pela interface web o passo 3 tende a exibir a
mensagem correta. O defeito D2 está na guarda do serviço
(`RecebimentoService.java:108`), que faz o unboxing de `codtitulo` antes de testar se
ele é nulo — qualquer chamador que passe `null` recebe `NullPointerException` em vez
da validação. A execução deste caso serve para confirmar **em que camada** a validação
está de fato acontecendo hoje.

---

## CT-REC-03 — Tentar receber um recebimento já processado

**Objetivo:** verificar a guarda de reprocessamento (TU-REC-09).

**Status:** ( ) Passou ( ) Falhou — _preencher na execução_

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Abrir o recebimento já processado em CT-REC-01 | URL `/recebimento/{codigo}` | Tela exibida com os campos desabilitados | |
| 2 | Reenviar a confirmação de recebimento (ex.: reabilitando o botão pelo console do navegador ou repetindo o POST) | mesmos dados de CT-REC-01 | Mensagem "Recebimento já esta fechado"; nenhum lançamento duplicado no caixa | |
| 3 | Conferir os lançamentos do caixa | - | Continua existindo **apenas um** lançamento para esse recebimento | |

---

## Rastreabilidade

| Caso manual | Casos unitários relacionados | Defeito relacionado |
|---|---|---|
| CT-REC-01 | TU-REC-01, TU-REC-07, TU-REC-13 | — |
| CT-REC-02 | TU-REC-08, TU-REC-19 | D2 |
| CT-REC-03 | TU-REC-09 | — |

Ao menos um destes cenários deve ser cadastrado no **TestLink**, conforme o enunciado
(link a preencher no `README.md`). Os demais ficam documentados neste arquivo.
