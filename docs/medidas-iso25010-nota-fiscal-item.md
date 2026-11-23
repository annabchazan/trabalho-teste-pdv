# Medidas de qualidade ISO/IEC 25010:2023 — `NotaFiscalItemService`

**Responsável:** Anna Beatriz Chaboudet Chazan
**Entrega 2.** Segue o modelo e a escala combinados pelo grupo, os mesmos das
seções de `VendaService`, `RecebimentoService` e `CaixaService` no Google Docs.

> A versão oficial está no Google Docs do grupo, aba "Entrega 2".

## Modelo e escala

Foram usadas as 9 características da ISO/IEC 25010:2023. Nenhum teste novo foi
feito para esta avaliação: todos os valores vêm dos testes e das análises da
Entrega 2. As metas foram definidas pelo grupo, não pela norma.

A nota é dada pela condição mais grave que aparece na característica. Cada
defeito conta na característica que ele afeta.

- **5 – excelente:** todas as metas atingidas, sem ressalvas;
- **4 – bom:** metas atingidas ou a 80% ou mais, só com ressalvas menores;
- **3 – aceitável com ressalvas:** alguma medida abaixo de 80% da meta, ou defeito de severidade média;
- **2 – insuficiente:** defeito de severidade alta, ou a maioria das medidas fora da meta;
- **1 – inaceitável:** a função principal não funciona.

## Avaliação

| Característica | Medida e resultado | Nota | Por que essa nota |
|---|---|---|---|
| Adequação funcional | Casos funcionais e de sistema com o resultado do requisito: 12 de 15 (80%) — TU-NFI-15 a 24 e TS-NFI-01 a 05 | 2 | Os 80% dariam 4, mas uma das falhas é a Issue #2, de severidade alta: produto sem Modalidade BC ICMS não pode ser incluído |
| Eficiência de desempenho | Inclusões de item atualizadas na tela em até 2 s: 44 de 50 (88%), mediana de 830 ms (TS-NFI-06) | 4 | Acima de 80% da meta, mas 6 medições passaram de 2 s, uma delas com 8,1 s |
| Compatibilidade | Operações de item que funcionam no MySQL 8 do projeto, sem ajuste manual: 3 de 3 (incluir, somar quantidade e remover; TI-NFI-02 a 04 e TS-NFI-01, 03 e 04) | 4 | Meta atingida; a convivência com os outros serviços foi avaliada só de forma qualitativa |
| Capacidade de interação | Entradas inválidas que viram mensagem compreensível: 8 de 11 classes inválidas (73%) | 2 | Produto sem Modalidade BC ICMS mostra o alerta técnico "No value present" (Issue #2, alta); quantidade 0 ou negativa é aceita sem aviso (Issue #35); nota inexistente na remoção gera erro técnico |
| Confiabilidade | Falhas do repositório que viram mensagem: 2 de 2 (TU-NFI-11 e 13); defeitos na classe: 2 | 2 | A inclusão é interrompida por exceção não tratada quando falta a Modalidade BC ICMS (Issue #2, alta) |
| Segurança | Acesso sem login redirecionado para o login: 1 de 1 (TS-NFI-05); vulnerabilidades e pontos sensíveis no Sonar: 0 e 0 | 4 | Metas atingidas, mas não houve teste de permissões por grupo de usuário |
| Manutenibilidade | Arestas 100% (40/40); mutação 89% (33/37, os 4 restantes equivalentes); Sonar 5 → 0; maior método com complexidade 14 (meta ≤ 10); 5 dependências (meta ≤ 7) | 3 | `verificaRegraDeTributacao` fica abaixo de 80% da meta de complexidade; as demais metas foram atingidas |
| Flexibilidade | 1 comando para subir (`docker compose up -d`); compila só com Java 8 | 3 | Não compila com a versão atual do Java, e o Selenium exige Java 11 ou mais |
| Segurança física (safety) | Valores inválidos gravados na nota: quantidade 0 ou negativa aceita (TU-NFI-21 e 22) | 3 | O item é gravado com valor total zero ou negativo, e os totais da nota são recalculados com ele (Issue #35, média) |

**Média: 3,0 de 5** nas 9 características. `NotaFiscalItemService`
ficou bem testado (100% de arestas e todos os mutantes não equivalentes mortos),
sem problemas no Sonar e com poucas dependências. O que puxa as notas para baixo
são os dois defeitos abertos: a Issue #2 (alta), que afeta a adequação
funcional, a capacidade de interação e a confiabilidade, e a Issue #35 (média).

## Como cada medida foi obtida

- **Adequação funcional:** dos casos derivados das tabelas de partição e valor
  limite (TU-NFI-15 a 24), 8 passam; TU-NFI-21 e 22 mostram a Issue #35. Dos
  testes de sistema, 4 passam; TS-NFI-02 mostra a Issue #2. Os casos TU-NFI-01
  a 14, da Entrega 1, e os testes de integração não entram na conta.
- **Capacidade de interação:** classes inválidas I1 a I11 do projeto funcional.
  Viram mensagem compreensível: I1, I2, I3, I4, I6, I8, I9 e I10. Não viram:
  I5 (Issue #2), I7 (Issue #35) e I11 (nota inexistente na remoção, que lança
  `NoSuchElementException`).
- **Manutenibilidade:** JaCoCo e PIT sobre `NotaFiscalItemServiceTest`;
  complexidade ciclomática por método no JaCoCo; Sonar sobre
  `NotaFiscalItemService.java`. Os 4 mutantes equivalentes estão nas linhas
  115 e 133 (remoção do `System.out.println` dos `catch`) e 165 e 174
  (`aux + 1` trocado por `aux - 1` num contador que só é comparado com 0).
- **Eficiência de desempenho:** TS-NFI-06, em 5 execuções de 10 medições
  cada, depois de uma inclusão de aquecimento. O teste fica com `@Ignore`
  porque o limite é violado de forma intermitente. Tempos em
  `docs/evidencias/entrega2/sistema/TS-NFI-06-tempos.txt`.
- **Compatibilidade:** testes de integração com o MySQL 8 do docker-compose e
  o esquema criado pelo Flyway, e testes de sistema com a aplicação no
  docker-compose.

Evidências em `docs/evidencias/entrega2/`.

## Defeitos considerados

| Issue | Defeito | Severidade | Evidência |
|---|---|---|---|
| [#2](https://github.com/annabchazan/trabalho-teste-pdv/issues/2) | `NoSuchElementException` ao inserir item quando o produto está sem Modalidade BC ICMS (linha 72) | Alta | CT-NFI-02; TU-NFI-14, TI-NFI-05 e TS-NFI-02 (`@Ignore`) |
| [#35](https://github.com/annabchazan/trabalho-teste-pdv/issues/35) | `insere()` aceita quantidade 0 ou negativa e grava o item com valor total zero ou negativo | Média | TU-NFI-21 e 22 (`@Ignore`) |

Nenhum dos dois foi corrigido nesta entrega. A Issue
[#1](https://github.com/annabchazan/trabalho-teste-pdv/issues/1) (cadastro de
Empresa) foi achada ao preparar os casos de nota fiscal, mas fica fora da
classe e não entra na avaliação.
