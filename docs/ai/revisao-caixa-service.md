# Revisão da solução gerada com IA — `CaixaServiceTest` e `CaixaServiceIT`

Responsável: **João Pedro G. Valadares** · Data: 2026-11-22 · Ferramenta: Claude Code (Claude Opus 5.5)

Este documento atende ao trecho do enunciado que pede, sempre que a IA for usada de
forma relevante na geração de testes, a preservação de:

| Artefato | Onde está |
|---|---|
| Solução inicial produzida com IA (testes unitários) | [`snapshots/CaixaServiceTest.v1-ia.java`](snapshots/CaixaServiceTest.v1-ia.java) |
| Solução inicial produzida com IA (testes de integração) | [`snapshots/CaixaServiceIT.v1-ia.java`](snapshots/CaixaServiceIT.v1-ia.java) |
| Solução final após revisão | [`CaixaServiceTest.java`](../../src/test/java/net/originmobi/pdv/service/CaixaServiceTest.java) e [`CaixaServiceIT.java`](../../src/test/java/net/originmobi/pdv/service/CaixaServiceIT.java) |
| Descrição das alterações realizadas | este arquivo |

O relatório completo da Entrega 2 de `CaixaService` (projeto funcional, todas-arestas,
mutação, integração e prints) está no documento da entrega, com link no README.

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
