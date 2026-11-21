# Entrega 2 — RecebimentoService (João Portela)

Seção do João Portela no Plano de Teste e no Relatório da Entrega 2. Este arquivo é a
fonte do texto colado no Google Docs do grupo.

**Classe sob teste:** `RecebimentoService`, do módulo de contas a receber. Ela abre um
recebimento com as parcelas em aberto de um cliente, recebe o valor rateando-o entre as
parcelas e lançando no caixa ou no cartão, e remove recebimentos ainda não processados.

**Branch:** `test/recebimento-service-entrega2`.

---

## 1. Projeto de teste funcional

Os casos foram derivados dos requisitos e do comportamento esperado pela tela, sem olhar
a implementação. A cobertura do código é medida depois, na técnica estrutural (seção 3).

### Requisitos funcionais considerados

- **RF-REC-01:** o usuário escolhe uma ou mais parcelas em aberto de um cliente e abre
  um recebimento. O total é a soma do que falta pagar em cada parcela.
- **RF-REC-02:** só é possível receber parcelas que pertencem ao cliente selecionado e
  ainda não estão quitadas.
- **RF-REC-03:** ao receber, o usuário informa o título (dinheiro, cartão de débito,
  cartão de crédito) e o valor, que não pode ser maior que o total nem menor ou igual a
  zero.
- **RF-REC-04:** o valor recebido é distribuído entre as parcelas na ordem em que
  aparecem: cada uma recebe até o que falta pagar nela, e o que sobra passa para a
  seguinte.
- **RF-REC-05:** recebimento em dinheiro gera uma entrada no caixa aberto. Recebimento
  no cartão gera um lançamento de cartão e não movimenta o caixa.
- **RF-REC-06:** um recebimento já processado não pode ser recebido de novo nem
  removido.
- **RF-REC-07:** qualquer erro de entrada deve chegar ao usuário como mensagem de
  validação, não como erro técnico.

### 1.1 Classes de equivalência

**abrirRecebimento (RF-REC-01, 02 e 07)**

| Condição de entrada | Classes válidas | Classes inválidas |
|---|---|---|
| E1 Parcelas selecionadas | V1: uma parcela · V2: várias parcelas | I1: nenhuma (lista vazia ou texto em branco) |
| E2 Situação da parcela | V3: em aberto | I2: já quitada |
| E3 Dono da parcela | V4: o cliente selecionado | I3: outro cliente |
| E4 Código do cliente | V5: até 127 · V6: 128 ou mais | — |
| E5 Cliente | V7: existe | I4: não existe |

V5 e V6 são a mesma classe pela especificação: o código do cliente não deveria mudar o
resultado. Foram separadas porque o valor 127 é a fronteira do cache de `Long` do Java,
e é exatamente onde o defeito D1 aparece.

**receber (RF-REC-03 a 07)**

| Condição de entrada | Classes válidas | Classes inválidas |
|---|---|---|
| E6 Título | V8: dinheiro · V9: cartão de débito · V10: cartão de crédito | I5: não informado (0) · I6: nulo · I7: inexistente |
| E7 Situação do recebimento | V11: em aberto | I8: já processado · I9: inexistente |
| E8 Valor recebido | V12: maior que 0 e até o total | I10: zero ou negativo · I11: maior que o total |
| E9 Parcelas do recebimento | V13: uma ou mais | I12: nenhuma |
| E10 Caixa, para título em dinheiro | V14: aberto | I13: nenhum caixa aberto |

**remover (RF-REC-06)**

| Condição de entrada | Classes válidas | Classes inválidas |
|---|---|---|
| E11 Situação do recebimento | V15: em aberto | I14: já processado |

### 1.2 Valores-limite

| Grandeza | Fronteira | Valores testados |
|---|---|---|
| Valor recebido (total R$ 100,00) | 0 e o total | −0,01 · **0,00** · 0,01 · 99,99 · **100,00** · 100,01 |
| Quantidade de parcelas | 0 e 1 | **0** · 1 · 2 · 3 |
| Código do cliente | 127 | **127** · 128 |
| Rateio: valor em relação à 1ª parcela (R$ 100,00) | valor da parcela | 99,99 · **100,00** · 100,01 |

### 1.3 Tabela de decisão — o que acontece em `receber`

As regras são avaliadas na ordem. "—" indica condição irrelevante.

| Condições / Ações | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 | R9 |
|---|---|---|---|---|---|---|---|---|---|
| Título informado e existente? | N | S | S | S | S | S | S | S | S |
| Recebimento em aberto? | — | N | S | S | S | S | S | S | S |
| Valor ≤ total? | — | — | N | S | S | S | S | S | S |
| Tem parcelas? | — | — | — | N | S | S | S | S | S |
| Valor > 0? | — | — | — | — | N | S | S | S | S |
| Tipo do título | — | — | — | — | — | dinheiro | dinheiro | débito | crédito |
| Caixa aberto? | — | — | — | — | — | S | N | — | — |
| Quita parcelas (rateio) | | | | | | X | | X | X |
| Lança no caixa | | | | | | X | | | |
| Lança no cartão | | | | | | | | X | X |
| Grava data de processamento | | | | | | X | | X | X |
| Mensagem de validação | X | X | X | X | X | | X | | |

R7 não estava prevista no código original. A tabela mostrou que faltava essa regra:
com título em dinheiro e nenhum caixa aberto, o serviço chamava `Optional.get()` num
`Optional` vazio (ver seção de defeitos).

---

## 2. Testes unitários com dependências isoladas

Classe de teste: [`RecebimentoServiceTest.java`](../src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java).
As 9 dependências de `RecebimentoService` (1 repositório e 8 serviços) são simuladas
com Mockito (`@Mock` e `@InjectMocks`). Nenhum teste acessa banco, rede ou interface.

### 2.1 Falhas de isolamento encontradas e corrigidas

- **I-1 — relógio real.** O serviço grava `new DataAtual().dataAtualTimeStamp()`, que
  usa o relógio do sistema, e o TU-REC-07 só conferia que a data não era nula. Os
  casos TU-REC-29 e TU-REC-39 passaram a conferir que a data gravada cai dentro da
  janela de execução do próprio teste.
- **I-2 — usuário logado.** `receber()` cria `new Aplicacao()`, que lê o usuário do
  `SecurityContextHolder` estático. O `@Before` define o usuário e o `@After` limpa o
  contexto, para o resultado não depender da ordem dos testes nem de outras classes.
- **I-3 — vínculos não verificados.** O PIT mostrou que apagar
  `recebimento.setTitulo(...)` ou `lancamento.setRecebimento(...)` não quebrava
  nenhum teste. O TU-REC-38 passou a conferir o título, o caixa, o usuário e o
  recebimento gravados no lançamento.

### 2.2 Casos novos (TU-REC-26 a 43)

| ID | Origem no projeto funcional | Cenário |
|---|---|---|
| TU-REC-26 | E4, limite 127 | Cliente com código 127 |
| TU-REC-27 | E4, limite 128 (D1) | Cliente com código 128 |
| TU-REC-28 | E1, limite 0 (D6) | Lista de parcelas vazia |
| TU-REC-43 | E1 (D6) | Lista de parcelas nula, chamada direta ao serviço |
| TU-REC-29 | E1, limite 1 | Uma parcela: total, cliente, data e parcela gravados |
| TU-REC-30 | E1, limite 3 | Três parcelas, soma e ordem |
| TU-REC-31 | E8, limite 0,01 | Menor valor válido |
| TU-REC-32 | E8, limite −0,01 | Valor negativo |
| TU-REC-33 | E8, limite total + 0,01 | Um centavo acima do total |
| TU-REC-34 | E8, limite total − 0,01 | Recebimento parcial |
| TU-REC-35 | Rateio, limite 100,01 | Um centavo passa para a segunda parcela |
| TU-REC-36 | E8, arredondamento | Total com três casas, valor arredondado aceito |
| TU-REC-37 | E8, arredondamento | Um centavo acima do total arredondado |
| TU-REC-38 | RF-REC-05 (I-3) | Vínculos do título e do lançamento |
| TU-REC-39 | RF-REC-06 (I-1) | Data de processamento do momento do recebimento |
| TU-REC-40 | E7, I9 (D3) | Recebimento inexistente |
| TU-REC-41 | Tabela, R7 (D7) | Dinheiro sem caixa aberto |
| TU-REC-42 | E8 (D8) | Parcela paga pela outra tela depois de abrir o recebimento |

Os 4 casos que estavam com `@Ignore` na Entrega 1 (TU-REC-06, 19, 20 e 25) voltaram
a rodar depois das correções da seção 6. O TU-REC-16 ganhou a simulação de caixa
aberto, porque o caixa passou a ser validado antes de quitar as parcelas.

**Resultado:** 43 casos, 43 passam, nenhum ignorado. Executada contra o código da
Entrega 1, a mesma suíte tem **10 falhas** — exatamente os casos dos defeitos D1,
D2, D3, D6, D7 e D8.

---

## 3. Teste estrutural (JaCoCo, critério todas-arestas)

| Métrica | Antes (Entrega 1, 25 casos) | Depois (43 casos) |
|---|---|---|
| Arestas | 32/34 (94,1%) | **48/48 (100%)** |
| Linhas | 88/91 | 111/111 |
| Instruções | 450/461 (97%) | 532/532 (100%) |

As 2 arestas que faltavam eram inalcançáveis no código antigo; uma delas era o
teste de nulidade do título, código morto por causa do defeito D2. Com a correção,
o teste passou a ser alcançável e é coberto pelo TU-REC-19. O número de arestas
subiu de 34 para 48 porque as correções acrescentaram validações.

Prints: [`antes/jacoco-RecebimentoService-metodos.png`](evidencias/entrega2/recebimento/antes/jacoco-RecebimentoService-metodos.png) e
[`depois/jacoco-RecebimentoService-metodos.png`](evidencias/entrega2/recebimento/depois/jacoco-RecebimentoService-metodos.png).

---

## 4. Teste baseado em defeitos (PIT, mutadores padrão)

| Métrica | Antes | Depois |
|---|---|---|
| Mutantes mortos (escore) | 33/39 (85%) | **44/44 (100%)** |
| Força dos testes | 33/39 (85%) | 44/44 (100%) |

Os 6 sobreviventes da medição inicial e o que aconteceu com cada um:

| Linha (código antigo) | Mutação | Destino |
|---|---|---|
| 115 | Remove `recebimento.setTitulo(...)` | Morto pelo TU-REC-38 |
| 175 | Remove `lancamento.setRecebimento(...)` | Morto pelo TU-REC-38 |
| 136 | `i < size` vira `i <= size` no laço do rateio | Deixou de existir: o rateio foi reescrito com `for-each` e `Math.min` |
| 140 | `vlsobra < 0` vira `<= 0` | Era equivalente (com sobra 0 o resultado é 0 nos dois casos); deixou de existir na reescrita |
| 143 | `vlquitado < 0` vira `<= 0` | Era equivalente na prática (−0,0 e 0,0); deixou de existir na reescrita |
| 212 | Remove `System.out.println(e)` | Deixou de existir: a exceção passou a ser encadeada como causa |

A reescrita do rateio não mudou o comportamento: os casos de rateio da Entrega 1
(TU-REC-13 e 24) e os novos (TU-REC-34 e 35) passam com os mesmos valores.

Prints: [`antes/pit-RecebimentoService.png`](evidencias/entrega2/recebimento/antes/pit-RecebimentoService.png),
[`depois/pit-RecebimentoService.png`](evidencias/entrega2/recebimento/depois/pit-RecebimentoService.png)
e o detalhe por linha em `pit-RecebimentoService-mutantes.png` nas duas pastas.

---

## 5. Inspeção com o SonarQube

SonarQube Community 26.11 em Docker, análise do projeto inteiro com a cobertura do JaCoCo.

| Métrica de `RecebimentoService` | Antes | Depois |
|---|---|---|
| Problemas abertos | 10 | **0** |
| — injeção por atributo (`java:S6813`) | 9 | 0 |
| — `Long` comparado com `!=` (`java:S4973`, é o defeito D1) | 1 | 0 |
| Nota de confiabilidade | C | A |
| Nota de manutenibilidade | A | A |
| Nota de segurança | A | A |
| Esforço estimado para corrigir os problemas | 50 min | 0 |
| Cobertura (Sonar) | 96,0% | 100% |
| Linhas de código | 153 | 174 |
| Complexidade ciclomática / cognitiva | 20 / 35 | 33 / 38 |

As 9 dependências passaram a ser recebidas pelo construtor, e o D1 foi corrigido.
A complexidade subiu porque as correções acrescentaram validações (D2, D3, D6, D7 e
D8) e três métodos auxiliares; a complexidade cognitiva, que mede a dificuldade de
leitura, subiu pouco (35 → 38).

O Sonar não aponta D2, D3, D6, D7 nem D8: eles só apareceram pelo projeto dos testes.

Prints: `sonar-RecebimentoService-issues.png` e `sonar-RecebimentoService-metricas.png` em
[`antes/`](evidencias/entrega2/recebimento/antes/) e [`depois/`](evidencias/entrega2/recebimento/depois/).

---

## 6. Defeitos corrigidos e encontrados

| ID | Defeito | Como foi encontrado | Situação |
|---|---|---|---|
| D1 (#4) | `Long` comparado com `!=`: cliente com código acima de 127 não consegue receber | Entrega 1, projeto unitário | Corrigido |
| D2 (#5) | Nulo testado depois do unboxing | Entrega 1, projeto unitário | Corrigido |
| D3 (#6) | Título inexistente gera "No value present" | Entrega 1, projeto unitário | Corrigido, junto com a variante do recebimento inexistente |
| D6 (#12) | Receber sem marcar parcela gera `NumberFormatException` | Entrega 1, teste manual | Corrigido |
| D7 | Dinheiro sem caixa aberto gera "No value present" | Entrega 2, tabela de decisão | Corrigido |
| D8 | Parcela paga pela outra tela faz o caixa receber dinheiro que não entrou | Entrega 2, classes de equivalência | Corrigido |
| D9 | Usuário sem permissão consegue receber chamando o endereço direto | Entrega 2, teste de sistema de segurança | Aberto (`SecurityConfig`, fora da classe) |

Descrição completa, com passos e correção: [`docs/bugs/defeitos-recebimento.md`](bugs/defeitos-recebimento.md).

**O peso do D1 só apareceu com o banco real.** Os testes de integração usam um
cliente de código 9201. Executados contra o código da Entrega 1, **os 7 falham**
com "A parcela 9201 não pertence ao cliente selecionado": nenhum recebimento pode
nem ser aberto para clientes a partir do código 128. No teste unitário isso parecia
um caso de borda; no banco real, bloqueia o módulo para quase todos os clientes.

---

## 7. Testes de integração

Classe: [`RecebimentoServiceIT.java`](../src/test/java/net/originmobi/pdv/service/RecebimentoServiceIT.java).
JUnit 4 com `SpringRunner`, executados pelo maven-failsafe. Contexto Spring e o
MySQL 8 do docker-compose, com o esquema criado pelo Flyway: o serviço, os serviços
que ele chama, os repositórios JPA, o `@Transactional` e o gatilho do banco que
atualiza o saldo do caixa rodam de verdade. A massa de dados é própria (códigos
92xx), apagada antes e depois de cada caso, e cada resultado é conferido por
consulta ao banco.

| ID | Cenário | Resultado |
|---|---|---|
| TI-REC-01 | Abrir e receber R$ 150,00 em dinheiro: parcelas quitadas, lançamento no caixa com usuário e recebimento, saldo do caixa atualizado pelo gatilho | Passou |
| TI-REC-02 | Receber R$ 120,00 de R$ 150,00: primeira parcela quitada, R$ 30,00 restando na segunda | Passou |
| TI-REC-03 | Cartão de crédito: `cartao_lancamento` com taxa de 3% e líquido de R$ 145,50; caixa não movimentado | Passou |
| TI-REC-04 | Remover recebimento em aberto (e os vínculos com as parcelas); recusar a remoção de um processado | Passou |
| TI-REC-05 | Falha no lançamento de caixa depois de quitar as parcelas: o `@Transactional` desfaz a quitação | Passou |
| TI-REC-06 | D8 no banco: parcela paga pela tela de contas a receber; valor acima do que falta é recusado, e o que falta é recebido | Passou |
| TI-REC-07 | D7 no banco: caixa fechado; mensagem de validação e nenhuma parcela quitada | Passou |

**Resultado:** 7 casos, 7 passaram. Contra o código da Entrega 1: 7 falhas, todas pelo D1.

---

## 8. Testes de sistema (Selenium) e requisitos não funcionais

Classe: [`RecebimentoSistemaIT.java`](../src/test/java/net/originmobi/pdv/service/RecebimentoSistemaIT.java).
Selenium 4.13 com Chrome sem janela, contra a aplicação no docker-compose,
recompilada com as correções. Massa de dados própria (códigos 93xx), criada e
apagada direto no MySQL.

| ID | Cenário | Resultado |
|---|---|---|
| TS-REC-01 (CT-REC-01) | Marcar as duas parcelas, receber R$ 150,00 em dinheiro; a tela não mostra mais parcelas em aberto e o banco confirma quitação e caixa | Passou |
| TS-REC-02 (CT-REC-02) | Confirmar sem título: "Selecione um título para realizar o recebimento" | Passou |
| TS-REC-03 (CT-REC-03) | Reabilitar os campos por JavaScript e reenviar um recebimento processado: o servidor recusa com "Recebimento já esta fechado" e o caixa recebe uma vez só | Passou |
| TS-REC-04 (D6) | Acionar o recebimento sem marcar parcela: "Selecione ao menos uma parcela para receber" | Passou |
| TS-REC-05 (RNF, segurança) | `/receber` e `/recebimento/1` sem login redirecionam para `/login` | Passou |
| TS-REC-06 (RNF, desempenho) | Do clique em "Receber" até a confirmação, em até 2 s, em 5 recebimentos seguidos | Passou |
| TS-REC-07 (RNF, segurança) | Usuário sem `REALIZAR_RECEBIMENTO` não pode receber pelo endereço direto | Ignorado: falha pelo D9 (403 esperado, 200 obtido) |

O **CT-REC-03**, que na Entrega 1 ficou "Não executado" porque a tela desabilita os
campos, foi executado pelo TS-REC-03.

**Desempenho (TS-REC-06):** duas execuções, 10 medições, entre **592 e 672 ms**
(média de cerca de 620 ms), contra a meta de 2 s. Ressalva: medido numa máquina de
desenvolvimento, com um usuário e banco local; não é um teste de carga.

**Resultado:** 7 casos, 6 passaram e 1 está ignorado pelo D9.

---

## 9. Medidas de qualidade — ISO/IEC 25010

**Escala:** 1 — inaceitável (a falha impede o uso) · 2 — insuficiente · 3 — aceitável
com ressalvas · 4 — bom (atende, com problemas menores conhecidos) · 5 — excelente
(atende plenamente, sem problema conhecido). "Antes" é o código da Entrega 1;
"depois", o desta entrega. As medidas são do módulo de recebimento, e cada nota
vem de uma evidência das seções anteriores.

| Característica | Medida | Antes | Depois | Nota antes → depois |
|---|---|---|---|---|
| Adequação funcional | Casos unitários derivados da especificação que passam | 33/43 (77%) | 43/43 (100%) | 2 → 4 |
| Eficiência de desempenho | Tempo do clique à confirmação do recebimento (meta 2 s) | — | 592 a 672 ms em 10 medições | — → 5 |
| Compatibilidade | Integração com caixa, cartão e contas a receber verificada no banco | Inconsistente com a tela de contas a receber (D8) | 7/7 casos de integração | 2 → 3 |
| Capacidade de interação | Entradas inválidas que geram mensagem compreensível | 9/14 (64%) | 14/14 (100%) | 3 → 4 |
| Confiabilidade | Defeitos conhecidos na classe; reversão em falha | 6 abertos; reversão OK | 0 abertos; reversão OK (TI-REC-05) | 2 → 4 |
| Segurança | Controles de acesso verificados | Login exigido 2/2; permissão 0/1 | Igual (D9 aberto) | 2 → 2 |
| Manutenibilidade | Problemas do Sonar; arestas; escore de mutação | 10; 94,1%; 85% | 0; 100%; 100% | 3 → 4 |
| Flexibilidade | Ambientes em que o módulo roda sem ajuste | Só Java 8 e MySQL ajustado à mão | Igual | 2 → 2 |
| Segurança de uso (*safety*) | — | Não se aplica | Não se aplica | — |

**Justificativas**

- **Adequação funcional (4).** Todos os casos derivados da especificação passam e os
  6 defeitos da classe foram corrigidos. Não é 5 porque D4 e D5, no esquema do banco,
  impedem que uma instalação nova do MySQL 8 feche vendas a prazo, que são as que
  geram as parcelas; sem parcelas não há o que receber. A medida antes caía para 2 por causa do D1: com o banco real, nenhum
  cliente a partir do código 128 conseguia receber.
- **Eficiência de desempenho (5).** A maior das 10 medições (672 ms) fica com folga
  abaixo da meta de 2 s. A medida vale para uso de um operador por vez, que é o
  cenário de uma loja; não houve teste de carga.
- **Compatibilidade (3).** O recebimento convive com o caixa (pelo gatilho do
  banco), com o cartão e com a tela de contas a receber, e os testes de integração
  confirmam os três. Antes, o D8 mostrava que as duas telas que recebem a mesma
  parcela não conversavam. Não passa de 3 pela dependência de gatilhos e de um
  esquema que não roda sem ajuste no MySQL 8 (D4).
- **Capacidade de interação (4).** Das 14 classes inválidas do projeto funcional,
  5 mostravam erro técnico ao usuário ("No value present", `NumberFormatException`)
  e hoje todas mostram mensagem de validação. A tela também impede reenviar um
  recebimento processado (TS-REC-03). Não é 5 porque as mensagens herdadas têm
  erros de português ("já esta fechado", "não possue parcelas").
- **Confiabilidade (4).** Uma falha no meio do recebimento não deixa dado pela
  metade: o `@Transactional` desfaz a quitação (TI-REC-05), e o caixa fechado
  passou a ser detectado antes de qualquer gravação (TI-REC-07). Não é 5 porque a
  disponibilidade do módulo ainda depende de D4 e D5.
- **Segurança (2).** O login é exigido (TS-REC-05) e cada lançamento de caixa
  registra o usuário que o fez (TI-REC-01). Mas a permissão de receber só esconde o
  botão: qualquer usuário logado recebe dinheiro pelo endereço direto (D9,
  TS-REC-07). Pesa também a proteção CSRF desligada no `SecurityConfig`. Pela
  leitura do código, o login busca o usuário com `findByUserContaining` (busca por
  trecho do nome), o que merece investigação; não foi testado nesta entrega.
- **Manutenibilidade (4).** O Sonar não aponta mais nada na classe e a suíte cobre
  100% das arestas e mata 100% dos mutantes, o que deixa seguro alterá-la. Não é 5
  porque a classe depende de 9 outras e a complexidade ciclomática subiu para 33
  com as validações novas.
- **Flexibilidade (2).** O projeto só compila com Java 8: no JDK 26 da máquina de
  desenvolvimento, o build falha ao ler um jar antigo do AspectJ, e os testes de
  Selenium precisaram ser compilados no container. O esquema depende de gatilhos do
  MySQL e não roda sem ajuste no MySQL 8 (D4). Nada disso mudou nesta entrega.
- **Segurança de uso.** O recebimento não controla equipamento nem processo físico;
  nenhuma falha põe em risco pessoas ou o ambiente. O dano possível é financeiro e
  já está coberto por adequação funcional e segurança.

---

## 10. Como executar

```sh
# testes unitários + JaCoCo (target/site/jacoco)
docker compose up -d pdv-db
docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B clean test

# PIT só em RecebimentoService (target/pit-reports)
docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B test-compile \
  org.pitest:pitest-maven:mutationCoverage -DtimeoutConstant=30000 \
  -DtargetClasses=net.originmobi.pdv.service.RecebimentoService \
  -DtargetTests=net.originmobi.pdv.service.RecebimentoServiceTest

# testes de integração (precisam do pdv-db)
docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B verify \
  -Dtest=RecebimentoServiceTest -Dit.test=RecebimentoServiceIT

# testes de sistema (precisam da aplicação no ar e do Chrome na máquina)
docker compose up -d
docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B test-compile
./mvnw failsafe:integration-test failsafe:verify -Dit.test=RecebimentoSistemaIT
```

O último comando roda os testes de sistema na máquina, onde está o Chrome, sem
recompilar. Com um JDK 8 instalado, `./mvnw verify -Dit.test=RecebimentoSistemaIT`
também funciona.
