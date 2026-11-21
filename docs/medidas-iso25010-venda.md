# Medidas de qualidade ISO/IEC 25010 — `VendaService` e a funcionalidade de venda

**Responsável:** Felipe Martins Bittencourt
**Entrega 2, Etapa 8.** Decisões registradas em
[`docs/ai/decisoes-entrega2-venda.md`](ai/decisoes-entrega2-venda.md)
(D-32 em diante).

> Este é o rascunho. A versão oficial vai para o Google Docs do grupo, na
> seção de medidas de qualidade.

## Como esta avaliação foi feita

O modelo de qualidade de produto da ISO/IEC 25010 divide a qualidade em
características e subcaracterísticas. Para cada subcaracterística escolhida,
definimos uma **medida** no estilo da ISO/IEC 25023: um nome, uma fórmula,
uma meta e a evidência de onde o valor saiu.

Nenhum teste novo foi feito nesta etapa. Todos os valores vêm das Etapas 1
a 7, já documentadas e commitadas. A meta de cada medida foi definida pelo
grupo para este trabalho; não é um valor imposto pela norma.

**Avaliação:** ✅ atende · ⚠️ atende parcialmente · ❌ não atende

## Resumo

| Característica | Subcaracterística | Medida | Valor | Meta | Avaliação |
|---|---|---|---|---|---|
| Adequação funcional | Correção funcional | M1. Casos com resultado igual ao requisito | 9 de 16 (56%) | 100% | ❌ |
| Confiabilidade | Maturidade | M2. Densidade de defeitos | 19 defeitos; 84,8 por mil linhas de código | o menor possível; nenhum de severidade alta | ❌ |
| Confiabilidade | Maturidade | M3. Defeitos corrigidos | 0 de 19 | — (fora do escopo, por decisão) | informativo |
| Confiabilidade | Tolerância a falhas | M4. Falhas no fechamento que desfazem tudo no banco | 2 de 2 (100%) | 100% | ✅ |
| Confiabilidade | Tolerância a falhas | M5. Caminhos de gravação que escondem a falha do usuário | 4 de 5 | 0 | ❌ |
| Eficiência de desempenho | Comportamento temporal | M6. Fechamentos em até 2 s | 24 de 25 (96%); mediana 576 ms | 100% | ⚠️ |
| Manutenibilidade | Testabilidade | M7. Cobertura de arestas | 100% (36/36) | ≥ 80% | ✅ |
| Manutenibilidade | Testabilidade | M8. Escore de mutação | 100% (60/60) | ≥ 80% | ✅ |
| Manutenibilidade | Analisabilidade e modificabilidade | M9. Problemas e dívida técnica do Sonar | 0 problemas; 0 min (antes: 11; 55 min) | 0 | ✅ |
| Manutenibilidade | Analisabilidade | M10. Complexidade ciclomática do maior método | `fechaVenda`: 10 | ≤ 10 | ⚠️ no limite |
| Manutenibilidade | Modularidade | M11. Dependências da classe | 11 | ≤ 7 | ❌ |
| Usabilidade | Proteção contra erro do usuário | M12. Avaliação qualitativa | 2 problemas observados | — | ⚠️ |
| Segurança | — | M13. Vulnerabilidades e pontos sensíveis no Sonar | 0 e 0 | 0 | ✅ (só análise estática) |

**Conclusão em uma frase:** `VendaService` agora está bem testado e fácil de
analisar (manutenibilidade boa), mas faz contas erradas e esconde falhas
(adequação funcional e confiabilidade ruins). O tempo de resposta atende,
com uma falha isolada a investigar.

---

## 1. Adequação funcional

### M1. Correção funcional — casos com resultado igual ao requisito

- **Fórmula:** X = A ÷ B, em que A é o número de casos cujo resultado obtido
  é igual ao esperado pelo requisito e B é o número de casos com resultado
  esperado definido pelo requisito.
- **Casos considerados:**
  - teste funcional TF-VEN-01 a 12 (Etapa 2): 10 casos têm resultado definido
    pelo requisito. Os casos TF-VEN-08 (prazo `0`) e TF-VEN-11 (situação em
    minúsculas) ficam de fora, porque o requisito não diz o que deveria
    acontecer. Dos 10, **4 passam** e 6 mostram defeito;
  - teste de sistema TS-VEN-01 a 06 (Etapa 6): **5 passam** e o TS-VEN-06
    mostra o defeito D-VEN-01.
- **Valor:** X = 9 ÷ 16 = **56%**. **Meta:** 100%. **Avaliação:** ❌
- **Evidências:** [`teste-funcional-venda.md`](teste-funcional-venda.md),
  seção 4; [`teste-sistema-venda.md`](teste-sistema-venda.md), seção 2.

Os testes unitários da Etapa 1 não entram nesta conta, porque foram escritos
a partir do código. Os casos de caracterização passam por definição: eles
confirmam o comportamento atual, mesmo quando está errado.

## 2. Confiabilidade

### M2. Maturidade — densidade de defeitos

- **Fórmula:** X = número de defeitos ÷ tamanho em mil linhas de código. O
  tamanho é o do Sonar depois da Etapa 7: 224 linhas de código (0,224 mil).
- **Defeitos contados** (lista completa na seção 7):
  - 10 da Entrega 1 (D-VEN-01 a 10);
  - 3 novos achados pelos testes unitários da Etapa 1 (T-3, T-9a, T-9b);
  - 6 novos achados pelo teste funcional da Etapa 2.
- **Valor:** 19 ÷ 0,224 = **84,8 defeitos por mil linhas**. Só os da Entrega 1
  dariam 44,6. Três defeitos são de severidade alta (D-VEN-01, 02 e 06).
- **Avaliação:** ❌. A meta era não ter defeitos de severidade alta.

Parte dos 19 defeitos é da funcionalidade de venda, mas não está dentro de
`VendaService`: D-VEN-05 (em `venda.js`), D-VEN-06 (cadastro de título e
`CartaoLancamentoService`) e D-VEN-07 (`VendaController`). Mesmo sem eles, a
densidade passaria de 70 por mil linhas.

### M3. Maturidade — defeitos corrigidos

- **Fórmula:** X = defeitos corrigidos ÷ defeitos encontrados.
- **Valor:** 0 ÷ 19 = **0%**.
- **Avaliação:** informativo. Corrigir defeitos ficou fora do escopo da
  Entrega 2 por decisão (D-10, D-13 e D-27): os testes registram o
  comportamento atual para que a correção seja feita depois, com segurança.

### M4. Tolerância a falhas — falhas no fechamento que desfazem tudo no banco

- **Fórmula:** X = falhas provocadas no meio do fechamento que não deixam
  nenhum registro parcial ÷ falhas provocadas.
- **Valor:** **2 ÷ 2 = 100%**. No TI-VEN-06 (sem caixa aberto) e no TI-VEN-07
  (sem estoque), o log de SQL mostra que o recebimento, a atualização da
  venda e o lançamento no caixa chegaram a ser gravados antes do erro. Depois
  do erro, nada disso ficou no banco.
- **Avaliação:** ✅
- **Evidência:** [`teste-integracao-venda.md`](teste-integracao-venda.md),
  seção 3.

### M5. Tolerância a falhas — caminhos de gravação que escondem a falha do usuário

- **Fórmula:** X = caminhos de gravação que, quando o repositório falha,
  respondem como se tivessem dado certo ÷ caminhos de gravação.
- **Operações que gravam:** `abreVenda` (nova e alteração), `addProduto`,
  `removeProduto` e `fechaVenda`. São 5 caminhos, contando os dois de
  `abreVenda`.
- **Valor:** **4 de 5.** `abreVenda` (nos dois caminhos), `addProduto` e
  `removeProduto` capturam a exceção e chamam `e.getStackTrace()`, que não
  registra nada (D-VEN-04). Contando por método, são 3 dos 4. `fechaVenda` avisa o
  usuário, mas em parte dos casos com mensagem vazia (D-VEN-08) ou com a
  exceção técnica crua (TF-VEN-09, T-9a, T-9b).
- **Meta:** 0. **Avaliação:** ❌
- **Evidências:** testes TU-VEN-03, 09 e 12 e T-1 em `VendaServiceTest`.

## 3. Eficiência de desempenho

### M6. Comportamento temporal — fechamentos em até 2 segundos

- **Requisito (TS-VEN-07):** fechar uma venda pela tela leva no máximo 2 s,
  do clique em "Pagar" até a mensagem de confirmação.
- **Fórmula:** X = fechamentos medidos em até 2.000 ms ÷ fechamentos medidos.
- **Medições:** 5 execuções do TS-VEN-07, com 5 fechamentos cada. Quatro são
  da Etapa 6 e uma é da Etapa 7, já com o código corrigido.

  | Execução | Tempos (ms) |
  |---|---|
  | Etapa 6, 1ª | 879 · 709 · **5.339** · 733 · 738 |
  | Etapa 6, 2ª | 569 · 552 · 570 · 666 · 632 |
  | Etapa 6, 3ª | 604 · 565 · 607 · 576 · 603 |
  | Etapa 6, 4ª | 573 · 570 · 584 · 556 · 555 |
  | Etapa 7 | 557 · 571 · 551 · 576 · 562 |

- **Valor:** **24 ÷ 25 = 96%.** Mediana de 576 ms; média de 611 ms sem a
  medição fora do limite e de 800 ms com ela.
- **Meta:** 100%. **Avaliação:** ⚠️. O tempo típico fica em menos de um terço
  do limite, mas houve uma violação de 5,3 s. O log mostrou uma pausa de
  3,8 s no servidor, logo depois de o Hibernate apagar a tabela temporária do
  `update` em massa da venda (D-24).
- **Evidência:** [`teste-sistema-venda.md`](teste-sistema-venda.md), seção 3,
  e D-28.

## 4. Manutenibilidade

### M7. Testabilidade — cobertura de arestas

- **Fórmula:** X = ramos percorridos ÷ ramos, medidos pelo JaCoCo. Com 100% das
  linhas, isso equivale ao critério todas-arestas (ver Etapa 3).
- **Valor:** **100% (36/36)**, com 100% das linhas (130/130). Antes da
  Entrega 2 eram 97% (37/38).
- **Meta:** ≥ 80%. **Avaliação:** ✅
- **Evidência:** `docs/evidencias/entrega2/depois-sonar/jacoco-VendaService-metodos.png`.

### M8. Testabilidade — escore de mutação

- **Fórmula:** X = mutantes mortos ÷ mutantes gerados, medidos pelo PIT com os
  operadores padrão.
- **Valor:** **100% (60/60).** Antes da Entrega 2: 79% (52/66). Depois das
  Etapas 1 e 2: 91% (60/66), com 6 equivalentes. Esses 6 sumiram quando o
  código morto foi removido na Etapa 7.
- **Meta:** ≥ 80%. **Avaliação:** ✅
- **Evidência:** `docs/evidencias/entrega2/depois-sonar/pit-VendaService.png`.

### M9. Analisabilidade e modificabilidade — problemas e dívida técnica do Sonar

- **Fórmula:** número de problemas abertos e esforço estimado para resolvê-los,
  no arquivo `VendaService.java`.
- **Valor:** **0 problemas e 0 min.** Antes da Etapa 7: 11 problemas (regra
  `java:S6813`, injeção no campo) e 55 min. A nota de manutenibilidade
  continuou A.
- **Meta:** 0. **Avaliação:** ✅
- **Evidência:** [`inspecao-sonar-venda.md`](inspecao-sonar-venda.md).

### M10. Analisabilidade — complexidade do maior método

- **Fórmula:** complexidade ciclomática do método mais complexo, medida pelo
  JaCoCo.
- **Valor:** `fechaVenda`, com complexidade **10** e 42 linhas. A classe soma
  29 de complexidade ciclomática e 43 de complexidade cognitiva (Sonar).
- **Meta:** ≤ 10 por método, um limite usual de mercado. **Avaliação:** ⚠️ no
  limite. Na Etapa 7 a complexidade caiu de 11 para 10, porque o laço do
  `modeloPagar` foi removido. `fechaVenda` ainda junta validação, cálculo,
  recebimento, lançamento, parcelas, fechamento e estoque. Foi nesse método
  que apareceram os defeitos mais graves (D-VEN-01, 02 e 03).

### M11. Modularidade — dependências da classe

- **Fórmula:** número de outras classes do sistema injetadas em
  `VendaService`. Desde a Etapa 7, todas aparecem no construtor.
- **Valor:** **11** (1 repositório e 10 serviços).
- **Meta:** ≤ 7, um limite usual para uma classe com uma só responsabilidade.
  **Avaliação:** ❌. A classe cuida de venda, caixa, contas a receber, cartão
  e estoque. Por isso, um teste unitário de `fechaVenda` precisa simular 10
  das 11 dependências.

## 5. Usabilidade

### M12. Proteção contra erro do usuário — avaliação qualitativa

Nesta etapa não há medida numérica de usabilidade; ela exigiria testes com
usuários. Ficam registradas duas observações dos testes de sistema e dos
testes manuais:

1. Na venda fechada, o "x" de remover produto continua aparecendo. O sistema
   recusa a remoção, mas o botão não deveria estar disponível (Etapa 6).
2. Vários erros chegam ao usuário como texto técnico ou vazio:
   `NumberFormatException` ao fechar sem título (D-VEN-05) ou ao cancelar o
   valor da balança (D-VEN-07), e um alerta vazio quando a parcela não é
   gerada (D-VEN-08).

As duas observações estão nas Issues [#21](https://github.com/annabchazan/trabalho-teste-pdv/issues/21) e [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17).

**Avaliação:** ⚠️

## 6. Segurança

### M13. Vulnerabilidades e pontos sensíveis no Sonar

- **Valor:** 0 vulnerabilidades e 0 pontos sensíveis (*security hotspots*) em
  `VendaService.java`, antes e depois da Etapa 7. Nota de segurança A.
- **Avaliação:** ✅, mas só quanto à análise estática. A segurança da
  aplicação (autenticação e permissões) foi testada pela Anna, em
  `NotaFiscalItemService`, e não faz parte desta avaliação.

## 7. Situação final dos defeitos

Nenhum defeito foi corrigido na Entrega 2 (M3). Todos estão registrados em
teste ou em caso manual. Cada um tem uma Issue no GitHub; alguns defeitos parecidos foram
reunidos na mesma Issue.

| ID | Defeito | Severidade | Onde está | Evidência | Issue |
|---|---|---|---|---|---|
| D-VEN-01 | Desconto e acréscimo trocados no caixa e nas parcelas | Alta | `VendaService` | T-6, T-7, TS-VEN-06 (`@Ignore`) | [#7](https://github.com/annabchazan/trabalho-teste-pdv/issues/7) |
| D-VEN-02 | Conferência das parcelas à vista soma sempre a mesma parcela; venda mista é recusada | Alta | `VendaService` | TU-VEN-27 | [#8](https://github.com/annabchazan/trabalho-teste-pdv/issues/8) |
| D-VEN-03 | Um update de fechamento por forma de pagamento | Média | `VendaService` | TU-VEN-28 | [#18](https://github.com/annabchazan/trabalho-teste-pdv/issues/18) |
| D-VEN-04 | Falhas engolidas em `abreVenda`, `addProduto` e `removeProduto` | Média | `VendaService` | TU-VEN-03, 09, 12; T-1 | [#15](https://github.com/annabchazan/trabalho-teste-pdv/issues/15) |
| D-VEN-05 | Fechar sem título gera `NumberFormatException` (condição de corrida em `venda.js`) | Média | `venda.js` e `VendaService` | CT-VENDA (manual) | [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17) |
| D-VEN-06 | Título de cartão sem máquina gera `NullPointerException` | Alta | cadastro de título e `CartaoLancamentoService` | CT-VENDA (manual) | [#14](https://github.com/annabchazan/trabalho-teste-pdv/issues/14) |
| D-VEN-07 | Cancelar o valor da balança gera `NumberFormatException` | Baixa | `VendaController` | CT-VENDA (manual) | [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17) |
| D-VEN-08 | Erro na geração de parcela sobe sem mensagem | Média | `VendaService` | TU-VEN-31 | [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17) |
| D-VEN-09 | Venda à vista cria conta a receber sem parcelas | Baixa | `VendaService` | CT-VENDA (manual) | [#19](https://github.com/annabchazan/trabalho-teste-pdv/issues/19) |
| D-VEN-10 | Taxa de cartão sem validação; líquido negativo | Baixa | cartão | CT-VENDA (manual) | [#20](https://github.com/annabchazan/trabalho-teste-pdv/issues/20) |
| T-3 | Título à vista que não é dinheiro nem cartão fecha a venda sem registrar o recebimento | Média | `VendaService` | T-3 | [#22](https://github.com/annabchazan/trabalho-teste-pdv/issues/22) |
| T-9a | Título inexistente gera erro técnico (`NoSuchElementException`) | Média | `VendaService` | T-9a | [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17) |
| T-9b | Venda inexistente gera erro técnico (`NullPointerException`) | Média | `VendaService` | T-9b | [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17) |
| TF-VEN-05 | Desconto igual ao total fecha a venda por R$ 0,00 | Média | `VendaService` | TF-VEN-05 | [#16](https://github.com/annabchazan/trabalho-teste-pdv/issues/16) |
| TF-VEN-06 | Desconto maior que o total fecha a venda com valor negativo | Média | `VendaService` | TF-VEN-06 | [#16](https://github.com/annabchazan/trabalho-teste-pdv/issues/16) |
| TF-VEN-07 | Acréscimo negativo é aceito e vira desconto | Média | `VendaService` | TF-VEN-07 | [#16](https://github.com/annabchazan/trabalho-teste-pdv/issues/16) |
| TF-VEN-09 | Faltar título gera `ArrayIndexOutOfBoundsException` depois da 1ª parcela | Média | `VendaService` | TF-VEN-09 | [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17) |
| TF-VEN-10 | Incluir produto em venda inexistente gera `NullPointerException` | Média | `VendaService` | TF-VEN-10 | [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17) |
| TF-VEN-12 | Situação nula na busca gera `NullPointerException` | Média | `VendaService` | TF-VEN-12 | [#17](https://github.com/annabchazan/trabalho-teste-pdv/issues/17) |

As severidades da Entrega 1 vêm do
[`plano-teste-venda.md`](plano-teste-venda.md). Os 9 defeitos achados na
Entrega 2 foram classificados como de severidade média na abertura das Issues.

## 8. Limitações

- **Amostra pequena de desempenho:** 25 medições, numa máquina de
  desenvolvimento com a memória quase cheia (94% durante a Etapa 7). Não há
  teste de carga com vários usuários ao mesmo tempo.
- **Metas definidas pelo grupo:** as metas são escolhas deste trabalho,
  baseadas em limites usuais de mercado (cobertura e mutação ≥ 80%,
  complexidade ≤ 10), e não valores da norma.
- **Usabilidade e segurança:** a usabilidade foi avaliada só de forma
  qualitativa, e a segurança só pela análise estática.
- **Escopo:** as medidas valem para `VendaService` e a funcionalidade de
  venda, não para o sistema inteiro.
