# Revisão da solução gerada com IA — `RecebimentoServiceTest`

Responsável: **João Portela** · Data: 2026-09-20 · Ferramenta: Claude Code (Claude Opus 5)

Este documento atende ao trecho do enunciado que pede, sempre que a IA for usada de
forma relevante na geração de testes, a preservação de:

| Artefato | Onde está |
|---|---|
| Solução inicialmente produzida com auxílio da IA | [`snapshots/RecebimentoServiceTest.v1-ia.java`](snapshots/RecebimentoServiceTest.v1-ia.java) |
| Solução final após revisão | [`src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java`](../../src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java) |
| Descrição das alterações realizadas | este arquivo |

---

## 1. Como a solução foi produzida

O prompt inicial não pediu "escreva testes para esta classe". Antes de gerar código,
a IA foi orientada a ler `RecebimentoService.java` e as classes de que ela depende
(`ParcelaService`, `RecebimentoParcelaService`, `TituloService`,
`CartaoLancamentoService`, `CaixaService`, os models `Parcela`, `Recebimento`,
`Titulo`, `TituloTipo`) e a **mapear as armadilhas de testabilidade antes de escrever
qualquer asserção**. Esse mapeamento virou parte do plano e é o que explica por que a
v1 já nasceu com as decisões da seção 2.

Também foi imposta uma restrição explícita: **nenhum arquivo de `src/main` pode ser
alterado**. Defeitos encontrados viram Issue, não correção silenciosa — do contrário
o teste "passaria" às custas de mascarar o problema que ele deveria revelar.

## 2. Decisões de projeto já presentes na v1 (não são correções)

Para não inflar artificialmente esta seção: a v1 **compilou e passou na primeira
execução** (23 testes, 0 falhas, 3 ignorados). Os três pontos abaixo não foram
"consertados depois" — eles já estavam na v1 porque foram levantados na leitura
prévia do código. Ficam registrados porque são exatamente o tipo de erro que uma
geração de teste sem leitura do código cometeria:

1. **`doAnswer` simulando o `@GeneratedValue`.** `abrirRecebimento()` termina com
   `recebimento.getCodigo().toString()`. Com `RecebimentoRepository` mockado, o
   `save()` não atribui a chave primária e o código fica `null` — o caminho feliz
   estouraria um `NullPointerException` **causado pelo teste, não pelo sistema**.
   O `doAnswer` em `simulaGeracaoDeCodigoNoSave()` reproduz o que o JPA faria.
2. **`SecurityContextHolder` populado no `@Before`.** `receber()` faz
   `new Aplicacao()` (linha 159), e o construtor de `Aplicacao` lê
   `SecurityContextHolder.getContext().getAuthentication().getName()`. Sem
   autenticação no contexto, todo teste de `receber()` falharia com NPE dentro do
   Spring Security. O `@After` limpa o contexto para não vazar entre classes de teste.
3. **Ordem das guardas respeitada em cada teste.** Em `receber()` as validações não
   são independentes: a linha 115 (`titulo.get()`) executa **antes** das checagens de
   valor. Por isso TU-REC-10 e TU-REC-12 precisam de um título presente no mock,
   mesmo não sendo esse o objeto do teste. Testes escritos "por dedução da mensagem
   de erro" falhariam aqui.

## 3. Alterações reais feitas na revisão (v1 → final)

### 3.1 Evidência insuficiente no teste do defeito D2

Na v1, TU-REC-19 (`receber` com `codtitulo` nulo) falhava com:

```
expected:<Selecione um título para realizar o recebimento> but was:<null>
```

O relatório do Surefire mostrava `null`, mas **não dizia que a causa era um
`NullPointerException`** — quem lê o relatório não consegue distinguir "o sistema
lançou NPE" de "o sistema lançou uma exceção sem mensagem". Foi acrescentada uma
asserção que nomeia o tipo da exceção:

```java
assertFalse("Esperava validação de negócio, veio " + e.getClass().getName(),
        e instanceof NullPointerException);
```

### 3.2 Caso de teste ausente, identificado pelo relatório de cobertura

A v1 foi aceita como "completa" com 91,2% de cobertura de arestas. A leitura do
relatório JaCoCo (`target/site/jacoco/jacoco.xml`) mostrou que o **ramo falso do
`if (vlrecebido > 0)` na linha 138 nunca era exercitado**: nenhum teste da v1 tinha
mais parcelas do que o valor recebido conseguia cobrir.

Foi acrescentado **TU-REC-24** — R$ 100,00 recebidos sobre três parcelas de R$ 100,00
— que verifica que as duas parcelas seguintes não recebem um lançamento de R$ 0,00.
Não é cobertura pela cobertura: é uma regra de negócio real que estava sem teste.

Cobertura de arestas de `RecebimentoService`: **91,2% → 94,1%** (32 de 34).

### 3.3 Caso ausente descoberto pela execução manual (TU-REC-25)

Este é o achado mais importante desta revisão, e vale registrar com honestidade:
**a suíte de 24 casos foi dada como completa, com 94,1% de cobertura de arestas,
e ainda assim tinha um buraco que só a execução manual expôs.**

Durante o CT-REC-01, ao clicar no botão de receber sem marcar nenhuma parcela, o
sistema devolveu `NumberFormatException: Zero length string` cru na tela. A causa
é `ReceberController:97` (`"".split(" ")` devolve `[""]`, um array com um elemento
vazio, não um array vazio) combinada com `RecebimentoService:67`
(`Long.decode("")`).

Os 24 casos originais cobriam, em `abrirRecebimento`, parcela já quitada, parcela
de outro cliente, cliente inexistente, falha do repositório e o caso do cache de
`Long` — mas **nenhum exercitava a lista de parcelas vazia**. Foi acrescentado
TU-REC-25, e o defeito virou a Issue #12 (D6).

A lição metodológica: cobertura de arestas alta não implica cobertura de
**classes de equivalência de entrada**. Os 94,1% foram atingidos sem nunca testar
a fronteira "coleção vazia", porque o laço `for` da linha 66 é percorrido de
qualquer forma — com um elemento inválido. Para a Entrega 2, isso reforça a
necessidade da técnica funcional (análise de valor limite) ao lado da estrutural.

## 4. Como o resultado da IA foi verificado

1. **Execução real, em Java 8.** O host tem apenas JDK 26, incompatível com o
   `source/target 1.8` do projeto e com o Mockito 2.15. A suíte foi executada dentro
   do container `eclipse-temurin:8-jdk` do `docker-compose.yml`:
   `docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B test`.
   Resultado: **37 testes, 0 falhas, 3 ignorados** (24 desta classe + 13 da Anna).
2. **Prova de que os 3 testes ignorados realmente falham.** Um `@Ignore` sem prova
   não vale nada — poderia estar escondendo um teste mal escrito em vez de um defeito
   do sistema. As anotações foram removidas temporariamente e a suíte reexecutada. As
   três falhas confirmaram, uma a uma, o defeito que cada teste afirma:

   | Teste | Falha observada | Defeito |
   |---|---|---|
   | TU-REC-06 | `RuntimeException: A parcela 10 não pertence ao cliente selecionado` (com a parcela pertencendo ao cliente) | D1 |
   | TU-REC-19 | exceção com mensagem `null` (NPE) | D2 |
   | TU-REC-20 | `ComparisonFailure: expected:<Título não encontrado...> but was:<No value present>` | D3 |

   Os `@Ignore` foram então restaurados para manter o build do grupo verde.
3. **Conferência literal das mensagens.** Cada `assertEquals` foi comparado com a
   string exata do `RecebimentoService.java`. As mensagens do sistema têm erros de
   português e uma delas está errada de fato — `remover()` devolve "Erro ao remover
   **orçamento**, chame o suporte" dentro do serviço de recebimento. Os testes
   reproduzem o texto como ele é hoje; corrigir o texto é mudança de `src/main` e
   fica para a Entrega 2.
4. **Análise do que ficou descoberto**, em vez de aceitar o número da cobertura:
   - linhas 80-82 — `catch (Exception e)` dentro do laço de `abrirRecebimento()`.
     O `try` só contém `lista.add()` e uma soma; **o bloco é inalcançável**. Não é
     falta de teste, é código morto.
   - linha 108, ramo `codtitulo == null` — inalcançável por causa do próprio
     **defeito D2** (o unboxing acontece antes). O ramo só passa a ser exercitável
     depois da correção.
   - linha 143, ramo falso de `vlquitado < 0` — só alcançável com uma parcela de
     `valor_restante = 0,00`, estado que não deveria existir (parcela sem saldo é
     parcela quitada). Não foi escrito teste para não fabricar um cenário impossível
     só para subir o percentual.

## 5. O que a IA não decidiu

A escolha da classe, o tratamento dado aos testes que expõem defeito (`@Ignore` +
Issue, em vez de ajustar a asserção para o comportamento errado), o recorte do que
é defeito e do que é observação de qualidade, e a decisão de não alterar `src/main`
nesta entrega foram do responsável. A IA executou, apontou causas raiz e foi
confrontada com a execução real a cada passo.
