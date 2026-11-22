# Revisão da solução gerada com IA — testes e correção de `CaixaService`

Responsável: **João Pedro G. Valadares** · Data: 2026-11-22 · Ferramenta: Claude Code (Claude Opus 5.5)

Este documento atende ao trecho do enunciado que pede, sempre que a IA for usada de
forma relevante na geração de testes, a preservação de:

| Artefato | Onde está |
|---|---|
| Solução inicial produzida com IA (testes unitários) | [`snapshots/CaixaServiceTest.v1-ia.java`](snapshots/CaixaServiceTest.v1-ia.java) |
| Solução inicial produzida com IA (testes de integração) | [`snapshots/CaixaServiceIT.v1-ia.java`](snapshots/CaixaServiceIT.v1-ia.java) |
| Solução inicial produzida com IA (testes de sistema) | igual à final: [`CaixaSistemaIT.java`](../../src/test/java/net/originmobi/pdv/service/CaixaSistemaIT.java) passou na primeira execução sem alteração |
| Solução inicial produzida com IA (correção do Sonar) | igual à final: [`CaixaService.java`](../../src/main/java/net/originmobi/pdv/service/CaixaService.java) no commit 7a3ed1f; a classe antes da correção está no commit 40606df |
| Solução final após revisão | [`CaixaServiceTest.java`](../../src/test/java/net/originmobi/pdv/service/CaixaServiceTest.java), [`CaixaServiceIT.java`](../../src/test/java/net/originmobi/pdv/service/CaixaServiceIT.java), [`CaixaSistemaIT.java`](../../src/test/java/net/originmobi/pdv/service/CaixaSistemaIT.java) e [`CaixaService.java`](../../src/main/java/net/originmobi/pdv/service/CaixaService.java) |
| Descrição das alterações realizadas | este arquivo |

O relatório completo da Entrega 2 de `CaixaService` (projeto funcional, todas-arestas,
mutação, integração, sistema, Sonar, ISO/IEC 25010 e prints) está no documento da entrega, com link no README.

## Como a versão inicial foi produzida

A IA leu `CaixaService`, `CaixaRepository`, `CaixaController`, a tela de busca de caixa
e os testes da Entrega 1. A partir disso propôs as classes de equivalência e os
valores-limite (casos TF-CX-01 a 30) e gerou a primeira versão de `CaixaServiceTest`
com 33 testes. Depois gerou a primeira versão de `CaixaServiceIT` com 10 testes contra
o MySQL do `docker-compose`.

## Como a versão inicial foi verificada

1. Compilação e execução no container Java 8 (`mvn clean test` e `mvn verify`).
2. Cobertura de arestas com o JaCoCo e leitura das arestas não cobertas.
3. Análise de mutantes com o PIT e leitura de cada mutante sobrevivente.
4. Conferência dos identificadores dos testes contra a tabela do projeto funcional.
5. Conferência do banco depois dos testes de integração (0 caixas e 0 lançamentos
   sobrando).

## Alterações da versão inicial para a final

| # | Arquivo | Problema na versão da IA | Como foi encontrado | Alteração |
|---|---|---|---|---|
| 1 | `CaixaServiceTest` | não compilava: `lancamento.getCaixa()` devolve `Optional<Caixa>`, e o teste comparava direto com o caixa | compilação | passou a comparar com `getCaixa().get()` |
| 2 | `CaixaServiceTest` | os números TF-CX dos comentários não batiam com a tabela do projeto funcional (TF-CX-20 a 28 repetidos ou trocados) e o defeito da data aparecia como D-CX-05 | conferência contra a tabela | comentários renumerados para TF-CX-22 a 30; defeito renomeado para D-CX-04 |
| 3 | `CaixaServiceTest` | faltava o filtro de banco com data nula: a aresta falsa da linha 195 ficou sem teste (42 de 44 arestas) | relatório do JaCoCo | incluído o teste TE-CX-01 (43 de 44; a aresta que sobra é não executável) |
| 4 | `CaixaServiceIT` | TI-CX-10 supunha que `listaBancos` devolvia números no lugar de caixas e que o erro seria `ClassCastException` ao ler um item | execução contra o MySQL | o erro real acontece na própria consulta: o Hibernate procura a coluna `descricao` e lança `InvalidDataAccessResourceUsageException`. O teste e a descrição do defeito D-CX-06 foram corrigidos |

## O que foi mantido sem alteração

- As classes de equivalência, os valores-limite e os 30 casos funcionais.
- Os testes TI-CX-01 a 09, que passaram na primeira execução.
- A decisão de não matar os dois mutantes sobreviventes (`println` das linhas 73 e
  74), tratados como equivalentes porque a saída no console não faz parte do
  comportamento da classe.

## O que foi rejeitado

- Testar a saída do console para matar os mutantes dos `println`: prenderia o teste
  a um código de depuração que deve sair na correção apontada pelo Sonar.

## Testes de sistema (`CaixaSistemaIT`)

A IA leu as telas de caixa (`caixa/form`, `caixa/list`, `caixa/gerenciar` e o modal de
fechamento), o `caixa.js` e o `CaixaController`, e gerou os 8 casos TS-CX-01 a 08 com Selenium 4.
Para o requisito não funcional, propôs segurança (login obrigatório e senha de
fechamento protegida), porque o grupo já tinha usado desempenho em `VendaSistemaIT`.

Verificação: execução com a aplicação no ar, leitura dos prints de cada caso e
conferência do banco depois da execução (0 caixas e 0 lançamentos sobrando).
Resultado: 8 testes, 0 falhas na primeira execução, por isso a versão final é igual à
inicial. O caso TS-CX-08 mostrou um defeito novo, a senha de fechamento na URL
(D-CX-07).

O único ajuste foi de ambiente, não de código: o Selenium não abre o Chrome dentro do
terminal da IA (falha de conexão local do Java), então os testes de sistema foram
rodados no terminal do Windows, com o comando registrado no documento da entrega.

## Correção dos apontamentos do Sonar (`CaixaService.java`)

O responsável rodou o SonarQube for IDE e tirou o print com 21 apontamentos. A IA
propôs e fez a correção com uma regra: mudar a estrutura sem mudar o comportamento.
As mensagens ao usuário ficaram iguais e os defeitos D-CX-01 a 07 não foram
corrigidos.

Principais mudanças: injeção pelo construtor, `Clock` injetado para as datas,
`CaixaException` no lugar de `RuntimeException` genérica (guardando a causa), tabelas
`EnumMap` para descrição e observação padrão, divisão de `cadastro` em métodos menores
e remoção dos `println` e dos campos sem uso.

Verificação: os 52 testes das etapas anteriores serviram de rede de segurança. Nos
testes unitários mudou só a criação do serviço, que passou a usar o construtor com
`Clock`, e as datas passaram a ser conferidas com valor exato. Nenhum resultado
esperado mudou. Depois da correção:
34 testes unitários, 10 de integração e 8 de sistema passando; 28 de 28 arestas; 49
de 49 mutantes mortos. O responsável tirou o print novo: 1 apontamento (java:S2143,
informativo), mantido com justificativa no documento da entrega.

O que foi rejeitado: trocar os tipos `java.sql.Date` e `Timestamp` da entidade `Caixa`
para zerar o S2143. A mudança atingiria `CaixaLancamentoService`, as consultas de
`CaixaRepository`, as telas e o relatório `caixa.jrxml`, fora do escopo da classe.

## Medidas ISO/IEC 25010

A IA propôs as 19 medidas e a avaliação de cada característica, usando a mesma escala
do grupo. Os valores vêm das etapas anteriores, exceto o tempo de resposta, medido com
100 requisições à tela de caixas com a aplicação no ar. A severidade dos defeitos
(alta, média e baixa) foi definida com critério escrito no documento da entrega.
