# Defeitos encontrados em `RecebimentoService`

Responsável: **João Portela** — Entrega 1, disciplina Qualidade e Teste.

Os três defeitos abaixo foram encontrados durante o projeto dos casos de teste
unitário de `RecebimentoService` (TU-REC-01..23). Cada um tem um teste
correspondente na suíte, escrito com a asserção do **comportamento correto** e
marcado com `@Ignore` para não quebrar o build do grupo. Ao corrigir o defeito,
basta remover a anotação e o teste passa a servir como teste de regressão.

O texto de cada seção está pronto para ser colado como uma GitHub Issue.

---

## D1 — Comparação de `Long` com `!=` rejeita parcelas do próprio cliente

**Arquivo:** `src/main/java/net/originmobi/pdv/service/RecebimentoService.java:72`
**Teste:** TU-REC-06 (`abrirRecebimento_deveAceitarParcela_quandoClienteTemCodigoAcimaDoCacheDeLong`)
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

## Observação de qualidade (não é defeito funcional)

`receber()` recebe `vlacrescimo` e `vldesconto` e os grava no `Recebimento`
(linhas 190-191), mas repassa `0.00` fixo para cada parcela na linha 150
(`parcelas.receber(parcela, vlquitado, 0.00, 0.00)`). Ou seja, acréscimo e
desconto ficam registrados no recebimento mas nunca chegam às parcelas.
Não foi aberta Issue porque não conseguimos determinar, apenas pelo código, se
esse comportamento é intencional. Fica registrado aqui para validação com o
grupo antes da Entrega 2.
