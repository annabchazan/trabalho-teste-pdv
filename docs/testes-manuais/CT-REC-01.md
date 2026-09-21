# Casos de teste manual — Recebimento de parcelas

Responsável: **João Portela** · Funcionalidade: **Receber parcelas em aberto de um cliente**
Módulo: `RecebimentoService` / `ReceberController` / `RecebimentoController`

> **Status: EXECUTADO em 21/09/2026.** CT-REC-01 e CT-REC-02 passaram; CT-REC-03
> não foi executado (ver justificativa na própria seção). A execução revelou
> **três defeitos novos** que não haviam sido encontrados pelos testes unitários
> (D4, D5 e D6 em [`docs/bugs/defeitos-recebimento.md`](../bugs/defeitos-recebimento.md)).

---

## Ambiente de execução

| Item | Valor |
|---|---|
| Subida do sistema | `docker compose up -d` na raiz do projeto |
| URL | http://localhost:8080 |
| Banco | MySQL **8.0.46** em `localhost:3307` (base `pdv`, root / `123456`) |
| Usuário | `gerente` / `123` |
| Data da execução | 21/09/2026, 17:03 às 18:01 |
| Verificação dos resultados | Consulta direta ao MySQL após cada passo, além da observação na tela |

### Correções de ambiente necessárias para executar

O fluxo **não é executável** no ambiente documentado pelo grupo sem duas
correções, ambas reportadas como defeito. Elas foram aplicadas no banco local
para permitir a execução; **nenhum arquivo de `src/main` foi alterado**:

```sql
-- D4: parcela.data_alteracao era TIMESTAMP NOT NULL sem default
ALTER TABLE parcela MODIFY data_alteracao TIMESTAMP NOT NULL
      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE mva     MODIFY data_alteracao TIMESTAMP NOT NULL
      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- D5: trigger de estoque ignorava o tipo do movimento e sempre subtraía
--     (versão corrigida em docs/bugs/defeitos-recebimento.md, defeito D5)
```

## Pré-condições

A execução mostrou que a lista de pré-condições projetada estava **errada**:
empresa e cadastro de cliente não são necessários, e faltavam duas que só
apareceram ao rodar o sistema (título e estoque). Lista corrigida:

| # | Pré-condição | Como | Situação na execução |
|---|---|---|---|
| 1 | Usuário, cliente, produto e formas de pagamento | já vêm das migrations do Flyway | Cliente `João Rafael Mendes Nogueira` (cód. 1), produto `Picolé` R$ 6,50 (cód. 1) |
| 2 | **Título** do tipo Dinheiro | menu *Titulos* | Criado: `Dinheiro`, tipo DIN (cód. 1) |
| 3 | **Caixa aberto** | menu *Caixa / Cofre* | Criado: `Caixa diário`, tipo CAIXA, abertura R$ 0,00 |
| 4 | **Estoque do produto** | menu *Ajuste Estoque* → Inserir → **Processar** | Entrada de 50 un. do Picolé — **falhou na 1ª tentativa por causa do defeito D5** |
| 5 | **Duas vendas fechadas a prazo** para o mesmo cliente | menu *Pedidos* | Venda 1: Picolé × 2 = R$ 13,00 · Venda 2: Picolé × 3 = R$ 19,50 — **falharam nas 3 primeiras tentativas por causa do defeito D4** |

Estado final das pré-condições, conferido no banco: estoque do Picolé em **45 un.**
(`50 − 2 − 3`), duas vendas FECHADAS e **2 parcelas em aberto** de R$ 13,00 e
R$ 19,50, totalizando **R$ 32,50** para o cliente 1.

---

## CT-REC-01 — Recebimento total de parcelas em aberto

**Objetivo:** verificar que o recebimento soma corretamente as parcelas
selecionadas, quita cada uma e gera o lançamento de entrada no caixa.

**Status: PASSOU**

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Acessar *Receber* (`/receber`) | - | Tela de listagem com filtro de cliente | OK |
| 2 | Filtrar pelo cliente e pesquisar | `João Rafael Mendes Nogueira` | Lista as parcelas em aberto e o total | OK — parcelas 3 e 4 listadas |
| 3 | Conferir o total exibido | - | Soma dos valores restantes | **R$ 32,50** (13,00 + 19,50) |
| 4 | Marcar as duas parcelas | - | Parcelas marcadas | OK |
| 5 | Clicar no ícone de receber | - | Abre `/recebimento/{codigo}` | OK — recebimento **1** criado, com os 2 vínculos em `recebimento_parcelas` |
| 6 | Conferir o campo *Total* | - | Igual ao total do passo 3 | **R$ 32,50** (`recebimento.valor_total = 32.5`) |
| 7 | Selecionar o título | `Dinheiro` | Título selecionado | OK |
| 8 | Informar o valor recebido | `32,50` | Campo aceita o valor | OK |
| 9 | Clicar em *Receber* | - | "Recebimento realizado com sucesso" | **Mensagem exibida conforme esperado** |
| 10 | Recarregar a tela | - | Campos Desc/Acres/Valor recebido/Título desabilitados | OK — `data_processamento` preenchida (18:00:37) dispara o bloqueio no JS |
| 11 | Voltar a *Receber* e filtrar o cliente | - | Parcelas não aparecem mais em aberto | OK — 0 parcelas em aberto para o cliente 1 |
| 12 | *Caixa / Cofre* → lançamentos | - | Entrada de R$ 32,50, tipo RECEBIMENTO | OK — ver evidência abaixo |

### Evidência coletada no banco após o passo 9

```
recebimento:  codigo 1 | valor_total 32.5 | valor_recebido 32.5
              valor_acrescimo 0 | valor_desconto 0
              data_processamento 2026-09-21 18:00:37 | titulo_codigo 1

parcela 3:    valor_total 13.0  | valor_recebido 13.0  | valor_restante 0 | quitado 1
parcela 4:    valor_total 19.5  | valor_recebido 19.5  | valor_restante 0 | quitado 1

caixa_lancamento: "Referente ao recebimento 1" | valor 32.5
                  tipo RECEBIMENTO | estilo ENTRADA
                  caixa_codigo 1 | recebimento_codigo 1 | usuario_codigo 1

caixa:        valor_abertura 0.00 -> valor_total 32.50
```

### Correspondência com os testes unitários

Dois resultados confirmam, no sistema real, o que os testes unitários asseriam
sobre mocks:

- **TU-REC-13 (rateio):** o teste assere que R$ 150 sobre parcelas de 100 + 100
  gera lançamentos de 100 e 50, na ordem. Na execução real, R$ 32,50 sobre
  13,00 + 19,50 gerou `valor_recebido` 13,00 e 19,50 — cada parcela recebeu
  exatamente seu valor restante, na ordem da lista.
- **TU-REC-07 (lançamento de caixa):** o teste assere `valor`,
  `TipoLancamento.RECEBIMENTO` e `EstiloLancamento.ENTRADA` via `argThat`. O
  banco gravou os três campos idênticos, mais a descrição
  `"Referente ao recebimento 1"`.

---

## CT-REC-02 — Confirmar recebimento sem selecionar o título

**Objetivo:** verificar o tratamento do campo obrigatório *Título*. Exercita
pela interface o mesmo ponto coberto por TU-REC-08 / TU-REC-19 (defeito **D2**,
Issue #5).

**Status: PASSOU**

Executado sobre o recebimento 1 ainda em aberto, **antes** do passo 7 do
CT-REC-01 — a validação ocorre antes de qualquer escrita, então o caso não
consome o recebimento nem exige massa de dados própria.

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Na tela do recebimento, deixar o combo *Titulo* vazio | (nenhum) | Combo sem seleção | OK |
| 2 | Informar o valor recebido | `32,50` | Campo aceita o valor | OK |
| 3 | Clicar em *Receber* | - | Validação amigável | **"Selecione um título para realizar o recebimento"** |
| 4 | Conferir as parcelas | - | Nenhuma parcela quitada | OK — `quitado = 0` nas duas |
| 5 | Conferir os lançamentos do caixa | - | Nenhum lançamento novo | OK — tabela ainda vazia |

### Conclusão sobre o defeito D2

O caso **passou**, e isso **reduz a severidade do D2** em relação ao que eu
havia documentado. O `RecebimentoController` converte título vazio em `0L`
antes de chamar o serviço, então a guarda `codtitulo == 0` é acionada e o
usuário recebe a mensagem correta. O defeito real — unboxing de `null` antes do
teste de nulidade em `RecebimentoService:108` — **não é alcançável pela
interface web**; afeta apenas chamadores que passem `null` diretamente ao
serviço. A Issue #5 foi atualizada com essa conclusão.

---

## CT-REC-03 — Tentar receber um recebimento já processado

**Status: NÃO EXECUTADO**

O caso foi projetado para verificar a guarda "Recebimento já esta fechado"
(coberta por TU-REC-09). Após o processamento, o `recebimento.js` desabilita os
campos *Desc*, *Acres*, *Valor recebido* e *Título* quando detecta
`data_processamento` preenchida — verificado no passo 10 do CT-REC-01. Ou seja,
**um usuário não consegue reenviar o recebimento pela interface**, e o caso não
é executável como teste manual sem contornar o controle do cliente
(reabilitando os campos pelo console do navegador ou repetindo o POST fora da
tela).

Isso é um resultado positivo de defesa em profundidade e não uma lacuna: a
proteção existe nas duas camadas — no cliente (campos desabilitados) e no
serviço (`RecebimentoService:111`, coberto por TU-REC-09). Fica registrado como
candidato a **teste de sistema na Entrega 2**, quando houver Selenium e for
possível manipular o DOM de forma controlada e reproduzível.

---

## Defeitos encontrados durante a execução

Todos detalhados em [`docs/bugs/defeitos-recebimento.md`](../bugs/defeitos-recebimento.md):

| ID | Defeito | Onde apareceu |
|---|---|---|
| **D4** | `parcela.data_alteracao TIMESTAMP NOT NULL` sem default → SQL 1364 em MySQL 8 → nenhuma venda a prazo funciona | Pré-condição 5, 3 tentativas |
| **D5** | Trigger `atualiza_produto_estoque_AFTER_INSERT` ignora `tipo` e sempre subtrai → produto com estoque controlado nunca pode ser vendido | Pré-condição 4 |
| **D6** | `abrirRecebimento` com nenhuma parcela marcada → `NumberFormatException: Zero length string` cru na tela | Passo 5, ao clicar no ícone sem marcar |

**D6 é um caso ausente na minha própria suíte unitária.** Os 24 testes cobriam
parcela quitada, parcela de outro cliente, cliente inexistente e falha do
repositório — mas nunca a lista vazia. Foi acrescentado o caso **TU-REC-25**
para cobrir a lacuna.

## Comportamentos corretos observados

Vale registrar o que funcionou, por contraste:

- **Rollback transacional do `fechaVenda`**: nas 3 falhas da pré-condição 5, o
  `receber` e a `parcela` inseridos foram desfeitos — 0 órfãos no banco. Os
  códigos gerados depois (receber 6 e 7, parcela 3 e 4) confirmam que os IDs
  1-5 foram consumidos e revertidos.
- **Guardas com mensagem clara**: "O produto de código 1 não tem estoque
  suficiente, verifique", "Ajuste já processado" e "Selecione um título para
  realizar o recebimento" todas apareceram tratadas, sem vazar exceção técnica.
- **Trigger de saldo de caixa** (`tr_atualizaValoresCaixa_AFTER_INSERT`):
  atualizou o `valor_total` do caixa de 0,00 para 32,50 automaticamente.

## Rastreabilidade

| Caso manual | Casos unitários relacionados | Defeito relacionado | Status |
|---|---|---|---|
| CT-REC-01 | TU-REC-01, TU-REC-07, TU-REC-13 | D4, D5, D6 (na preparação/execução) | Passou |
| CT-REC-02 | TU-REC-08, TU-REC-19 | D2 (severidade revista) | Passou |
| CT-REC-03 | TU-REC-09 | — | Não executado |

O CT-REC-01 é o cenário cadastrado no **TestLink** (link a preencher no
`README.md`).
