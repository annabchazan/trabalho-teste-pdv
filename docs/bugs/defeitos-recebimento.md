# Defeitos encontrados no módulo de Recebimento

Responsável: **João Portela** — Entrega 1, disciplina Qualidade e Teste.

Seis defeitos encontrados a partir do trabalho sobre `RecebimentoService`. Os que
estão na própria classe (D1, D2, D3 e D6) têm um teste correspondente na suíte
`RecebimentoServiceTest`, escrito com a asserção do **comportamento correto** e
marcado com `@Ignore` para não quebrar o build do grupo — ao corrigir o defeito,
basta remover a anotação e o teste passa a servir como teste de regressão.

D4 e D5 estão fora de `RecebimentoService` (no schema do banco e num trigger),
mas foram encontrados aqui porque **bloqueiam completamente** o módulo de
recebimento: sem eles corrigidos, não existe parcela para receber.

| ID | Onde | Como foi encontrado |
|---|---|---|
| D1 | `RecebimentoService:72` | projeto dos testes unitários |
| D2 | `RecebimentoService:108` | projeto dos testes unitários |
| D3 | `RecebimentoService:106,115` | projeto dos testes unitários |
| D4 | `V1__cria_estrutura_inicial.sql:882` (schema) | execução do teste manual |
| D5 | `V1__cria_estrutura_inicial.sql:1506` (trigger) | execução do teste manual |
| D6 | `RecebimentoService:67` + `ReceberController:97` | execução do teste manual |

Os seis estão registrados como GitHub Issues: [#4](https://github.com/annabchazan/trabalho-teste-pdv/issues/4) (D1), [#5](https://github.com/annabchazan/trabalho-teste-pdv/issues/5) (D2), [#6](https://github.com/annabchazan/trabalho-teste-pdv/issues/6) (D3), [#10](https://github.com/annabchazan/trabalho-teste-pdv/issues/10) (D4), [#11](https://github.com/annabchazan/trabalho-teste-pdv/issues/11) (D5) e [#12](https://github.com/annabchazan/trabalho-teste-pdv/issues/12) (D6).

D1 a D3 foram encontrados projetando os testes unitários; **D4 a D6 foram encontrados executando o teste manual CT-REC-01** — ver `docs/testes-manuais/CT-REC-01.md`.

---

## D1 — Comparação de `Long` com `!=` rejeita parcelas do próprio cliente

**Arquivo:** `src/main/java/net/originmobi/pdv/service/RecebimentoService.java:72`
**Teste:** TU-REC-06 (`abrirRecebimento_deveAceitarParcela_quandoClienteTemCodigoAcimaDoCacheDeLong`)
**Issue:** [#4](https://github.com/annabchazan/trabalho-teste-pdv/issues/4)
**Severidade:** Alta — impede o recebimento na operação real.

### Descrição

```java
if (parcela.getReceber().getPessoa().getCodigo() != codpes)
    throw new RuntimeException("A parcela " + parcela.getCodigo() + " não pertence ao cliente selecionado");
```

`getCodigo()` devolve `Long` e `codpes` é `Long`. O operador `!=` compara
**referências**, não valores. A comparação só funciona por acidente: o
`Long.valueOf()` mantém um cache de instâncias para a faixa **-128 a 127**, então
clientes com código baixo passam. A partir do código **128** cada chamada devolve
um objeto novo e a comparação é sempre verdadeira, bloqueando o recebimento de
parcelas que pertencem legitimamente ao cliente.

### Passos para reproduzir

1. Cadastrar clientes até que algum receba código maior que 127.
2. Gerar uma venda a prazo para esse cliente (cria parcelas em `receber`).
3. Em **Contas a receber**, selecionar a parcela do cliente e clicar em *Receber*.
4. O sistema acusa "A parcela X não pertence ao cliente selecionado".

### Resultado esperado

O recebimento é aberto normalmente, pois a parcela pertence ao cliente.

### Resultado obtido

`RuntimeException: A parcela X não pertence ao cliente selecionado`.

### Correção sugerida

```java
if (!parcela.getReceber().getPessoa().getCodigo().equals(codpes))
```

---

## D2 — `NullPointerException` quando o título não é informado

**Arquivo:** `src/main/java/net/originmobi/pdv/service/RecebimentoService.java:108`
**Teste:** TU-REC-19 (`receber_deveLancarMensagemAmigavel_quandoCodigoDoTituloENulo`)
**Issue:** [#5](https://github.com/annabchazan/trabalho-teste-pdv/issues/5)
**Severidade:** Média — validação existe, mas é inalcançável.

### Descrição

```java
if (codtitulo == 0 || codtitulo == null)
    throw new RuntimeException("Selecione um título para realizar o recebimento");
```

`codtitulo` é `Long`. Em `codtitulo == 0` o compilador faz **unboxing** para
comparar com o literal `int`. Se `codtitulo` for `null`, o unboxing estoura um
`NullPointerException` **antes** de a segunda condição ser avaliada — ou seja,
`codtitulo == null` é código morto e a validação nunca protege o caso que ela
pretende proteger.

Hoje o `RecebimentoController` converte título vazio em `0L`, o que mascara o
defeito na interface web. Qualquer outro chamador do serviço (um teste, uma API
futura, uma chamada interna) recebe o NPE.

### Resultado esperado

`RuntimeException: Selecione um título para realizar o recebimento`

### Resultado obtido

`NullPointerException`

### Correção sugerida

Inverter a ordem das condições — `if (codtitulo == null || codtitulo == 0)` — ou
usar `Long.valueOf(0L).equals(codtitulo)`.

---

## D3 — `NoSuchElementException` ("No value present") com título inexistente

**Arquivo:** `src/main/java/net/originmobi/pdv/service/RecebimentoService.java:106` e `:115`
**Teste:** TU-REC-20 (`receber_deveLancarMensagemAmigavel_quandoTituloNaoExiste`)
**Issue:** [#6](https://github.com/annabchazan/trabalho-teste-pdv/issues/6)
**Severidade:** Média — erro técnico vazando para o usuário.

### Descrição

```java
Optional<Titulo> titulo = titulos.busca(codtitulo);   // linha 106
...
recebimento.get().setTitulo(titulo.get());            // linha 115
```

O `Optional` é consumido com `.get()` sem nenhuma verificação de presença. Se o
código de título informado não existir no banco (registro removido, requisição
adulterada, dado inconsistente), o usuário recebe o alerta técnico
`No value present` em vez de uma mensagem de validação. O mesmo vale para
`recebimentos.findById(codreceber)`, também consumido com `.get()`.

Vale registrar que **este é o mesmo padrão de defeito** que a Anna Chazan
documentou em `NotaFiscalItemService.java:67`
(`produto.map(Produto::getModBcIcms).get().getTipo()`). Não é um erro pontual: é
um padrão recorrente no projeto — `Optional` usado como açúcar sintático e
resolvido com `.get()` sem checagem. Sugerimos tratar como item de inspeção de
código para a Entrega 2 (Sonar), e não apenas como dois bugs isolados.

### Resultado esperado

Mensagem de validação amigável, por exemplo
"Título não encontrado, favor verifique".

### Resultado obtido

`NoSuchElementException: No value present`

### Correção sugerida

```java
if (!titulo.isPresent())
    throw new RuntimeException("Título não encontrado, favor verifique");
```

---

## D4 — Venda a prazo impossível: `parcela.data_alteracao` sem valor default

**Arquivo:** `src/main/resources/db/migration/V1__cria_estrutura_inicial.sql:882`
**Encontrado em:** execução do CT-REC-01, pré-condição 5 (3 tentativas)
**Issue:** [#10](https://github.com/annabchazan/trabalho-teste-pdv/issues/10)
**Severidade:** Bloqueante — inviabiliza todo o módulo de contas a receber.

### Descrição

A migration declara, para a tabela `parcela`:

```sql
`data_alteracao` TIMESTAMP NOT NULL,
```

**sem `DEFAULT CURRENT_TIMESTAMP`**, ao contrário de todas as tabelas irmãs da
mesma migration (ex. `parcela_pagar` na linha 509, que tem o default). Em MySQL
5.x isso funcionava por acidente: com `explicit_defaults_for_timestamp = OFF`, a
primeira coluna `TIMESTAMP` da tabela recebia `DEFAULT CURRENT_TIMESTAMP ON
UPDATE CURRENT_TIMESTAMP` implicitamente.

O `docker-compose.yml` do grupo fixa `mysql:8.0`, onde
`explicit_defaults_for_timestamp` é **ON** por padrão (confirmado no container:
valor `1`, MySQL 8.0.46). O default implícito deixou de existir, e como o
`sql_mode` inclui `STRICT_TRANS_TABLES`, a omissão virou erro em vez de warning.

`ParcelaRepository.gerarparcela` não inclui `data_alteracao` no INSERT:

```java
@Query(value = "insert into parcela (valor_total, valor_desconto, valor_acrescimo, valor_recebido, "
        + "valor_restante, receber_codigo, quitado, sequencia, data_cadastro, data_vencimento) values (...)",
        nativeQuery = true)
```

→ `SQL Error 1364: Field 'data_alteracao' doesn't have a default value`.

### Por que o erro era indiagnosticável pela tela

O `catch` de `VendaService.aprazo()` (linha 271-274) descarta o erro real:

```java
} catch (Exception e) {
    e.getMessage();
    throw new RuntimeException();   // sem mensagem, sem causa
}
```

O usuário recebe um alerta vazio. Esse é exatamente o defeito nº 4 que o
@felipembits caracterizou no teste
`fechaVenda_devePerderAMensagemDeErro_quandoFalhaAoGerarParcela`, que assere
`e.getMessage() == null` — o teste dele previu em laboratório o que aconteceu na
execução real.

### Impacto

Nenhuma venda a prazo pode ser fechada → nenhuma parcela é criada → os módulos
*Contas a receber* e *Recebimento* ficam **inexecutáveis**.

### Correção sugerida

Nova migration:

```sql
ALTER TABLE parcela MODIFY data_alteracao TIMESTAMP NOT NULL
      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE mva     MODIFY data_alteracao TIMESTAMP NOT NULL
      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
```

São as duas únicas colunas do schema nessa condição (verificado via
`information_schema.columns`).

---

## D5 — Trigger de estoque sempre subtrai: entrada de estoque nunca credita

**Arquivo:** `src/main/resources/db/migration/V1__cria_estrutura_inicial.sql:1506`
**Encontrado em:** execução do CT-REC-01, pré-condição 4
**Issue:** [#11](https://github.com/annabchazan/trabalho-teste-pdv/issues/11)
**Severidade:** Bloqueante — nenhum produto com controle de estoque pode ser vendido.

### Descrição

```sql
CREATE TRIGGER `atualiza_produto_estoque_AFTER_INSERT`
AFTER INSERT ON `estoque_movimentacao` FOR EACH ROW
BEGIN
    SET @codprod = new.produto_codigo;
    SET @qtd = new.qtd;
    select coalesce(qtd, 0) INTO @qtd_estoque from produto_estoque where produto_codigo = @codprod;
    IF(@qtd <= @qtd_estoque) THEN
        SET @novo_estoque = (@qtd_estoque - @qtd);
        update produto_estoque set qtd = @novo_estoque where produto_codigo = @codprod;
    end if;
END
```

Dois problemas encadeados:

1. **O trigger ignora `new.tipo`.** Não distingue `ENTRADA` de `SAIDA` — sempre
   subtrai. Uma entrada de estoque jamais credita o saldo.
2. **A guarda `@qtd <= @qtd_estoque` impede a movimentação inicial.** Com saldo
   0 e entrada de 50, `50 <= 0` é falso e o `update` não executa.

### Evidência da execução

Após processar um ajuste de entrada de 50 unidades do produto 1 pela interface:

```
ajuste_produtos:      produto 1 | estoque_atual 0 | qtd_alteracao 50 | qtd_nova 50   (OK)
estoque_movimentacao: produto 1 | tipo ENTRADA    | qtd 50                            (OK)
produto_estoque:      produto 1 | qtd 0                                               (ERRADO)
```

O razão de estoque foi gravado, o saldo não. Em seguida, o fechamento da venda
falha em `ProdutoService.movimentaEstoque` com "O produto de código 1 não tem
estoque suficiente, verifique" — mensagem correta, sintoma de um saldo que
nunca poderia existir.

### Impacto

Os dois produtos semeados pelas migrations têm `controla_estoque = SIM`. Como
nenhum deles pode receber saldo, **nenhuma venda de produto com controle de
estoque é possível** — o fluxo central do sistema.

### Correção sugerida (validada na prática)

```sql
DROP TRIGGER IF EXISTS `atualiza_produto_estoque_AFTER_INSERT`;

CREATE TRIGGER `atualiza_produto_estoque_AFTER_INSERT`
AFTER INSERT ON `estoque_movimentacao` FOR EACH ROW
BEGIN
    DECLARE v_estoque INT;
    SELECT COALESCE(qtd, 0) INTO v_estoque
      FROM produto_estoque WHERE produto_codigo = NEW.produto_codigo;

    IF NEW.tipo = 'ENTRADA' THEN
        UPDATE produto_estoque SET qtd = v_estoque + NEW.qtd
         WHERE produto_codigo = NEW.produto_codigo;
    ELSE
        IF NEW.qtd <= v_estoque THEN
            UPDATE produto_estoque SET qtd = v_estoque - NEW.qtd
             WHERE produto_codigo = NEW.produto_codigo;
        ELSE
            SIGNAL SQLSTATE '45000'
               SET MESSAGE_TEXT = 'Estoque insuficiente para a saida de estoque';
        END IF;
    END IF;
END
```

**Correção verificada:** após aplicá-la, um novo ajuste de entrada de 50 un.
levou o saldo a 50, e as duas vendas subsequentes (2 e 3 unidades) o deixaram em
**45** — entrada creditando e saídas debitando corretamente.

### Observação para a Entrega 2

Nenhum dos quatro conjuntos de testes unitários do grupo poderia encontrar D4 ou
D5: todos nós isolamos os repositórios com Mockito, e a lógica defeituosa está
no schema e num trigger do banco. Os dois defeitos só aparecem com o sistema em
execução. É o argumento mais concreto que temos para o valor do teste manual, e
justifica priorizar **testes de integração com banco real** na Entrega 2.

---

## D6 — `NumberFormatException` ao receber sem selecionar parcelas

**Arquivo:** `src/main/java/net/originmobi/pdv/service/RecebimentoService.java:67`
e `src/main/java/net/originmobi/pdv/controller/ReceberController.java:97`
**Teste:** TU-REC-25 (`abrirRecebimento_deveLancarMensagemAmigavel_quandoNenhumaParcelaFoiSelecionada`)
**Encontrado em:** execução do CT-REC-01, passo 5
**Issue:** [#12](https://github.com/annabchazan/trabalho-teste-pdv/issues/12)
**Severidade:** Média — erro técnico cru exibido ao usuário.

### Descrição

`ReceberController:97` monta o array de parcelas a partir do parâmetro da
requisição:

```java
String[] arrayParcelas = request.get("parcelas").replace(", ", " ").split(" ");
```

Quando nenhuma parcela é marcada, o parâmetro chega vazio e `"".split(" ")`
devolve **`[""]`** — um array com um elemento vazio, e não um array vazio. O
laço de `abrirRecebimento` então executa:

```java
Parcela parcela = parcelas.busca(Long.decode(arrayParcelas[i]));   // linha 67
```

→ `java.lang.NumberFormatException: Zero length string`, exibido cru na tela.

### Passos para reproduzir

1. Acessar *Receber* e filtrar um cliente com parcelas em aberto.
2. **Sem marcar nenhum checkbox**, clicar no ícone de receber.

### Resultado esperado

Mensagem de validação, por exemplo "Selecione ao menos uma parcela para receber".

### Resultado obtido

`java.lang.NumberFormatException: Zero length string`

### Correção sugerida

Validar no início de `abrirRecebimento`, tratando também o elemento vazio:

```java
if (arrayParcelas == null || arrayParcelas.length == 0
        || (arrayParcelas.length == 1 && arrayParcelas[0].trim().isEmpty()))
    throw new RuntimeException("Selecione ao menos uma parcela para receber");
```

### Nota de autocrítica

Este defeito é um **caso ausente na minha própria suíte de testes unitários**.
Os 24 casos originais cobriam parcela já quitada, parcela de outro cliente,
cliente inexistente e falha do repositório — mas nenhum exercitava a lista de
parcelas vazia. O caso **TU-REC-25** foi acrescentado depois da execução manual,
e o registro dessa descoberta está em
[`docs/ai/revisao-recebimento-service.md`](../ai/revisao-recebimento-service.md).

---

## Observação de qualidade (não é defeito funcional)

`receber()` recebe `vlacrescimo` e `vldesconto` e os grava no `Recebimento`
(linhas 190-191), mas repassa `0.00` fixo para cada parcela na linha 150
(`parcelas.receber(parcela, vlquitado, 0.00, 0.00)`). Ou seja, acréscimo e
desconto ficam registrados no recebimento mas nunca chegam às parcelas.
Não foi aberta Issue porque não conseguimos determinar, apenas pelo código, se
esse comportamento é intencional. Fica registrado aqui para validação com o
grupo antes da Entrega 2.
