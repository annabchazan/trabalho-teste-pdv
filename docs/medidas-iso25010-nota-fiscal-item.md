# Medidas de qualidade ISO/IEC 25010 — `NotaFiscalItemService` e a inclusão de item em nota fiscal

**Responsável:** Anna Beatriz Chaboudet Chazan
**Entrega 2.** Segue o mesmo método e a mesma escala de
[`medidas-iso25010-venda.md`](medidas-iso25010-venda.md).

> Este é o rascunho. A versão oficial vai para o Google Docs do grupo, no
> Relatório de Execução.

## Como esta avaliação foi feita

O modelo de qualidade de produto da ISO/IEC 25010 divide a qualidade em
características e subcaracterísticas. Para cada subcaracterística escolhida,
definimos uma **medida** no estilo da ISO/IEC 25023: um nome, uma fórmula,
uma meta e a evidência de onde o valor saiu.

Nenhum teste novo foi feito nesta etapa. Todos os valores vêm dos testes e
das análises da Entrega 2, já documentados e commitados. A meta de cada
medida foi definida pelo grupo para este trabalho; não é um valor imposto
pela norma.

Foram consideradas as 8 características da ISO/IEC 25010:2011, a versão
mais usada na literatura. A revisão de 2023 tem 9 características.

### Escala de avaliação

Cada medida e cada característica recebem um dos três níveis abaixo. A escala
é ordinal: ✅ é melhor que ⚠️, e ⚠️ é melhor que ❌.

| Nível | Significado | Critério para uma medida | Critério para uma característica |
|---|---|---|---|
| ✅ | Atende | O valor atinge a meta | Todas as medidas atendem |
| ⚠️ | Atende parcialmente | O valor fica perto da meta (no limite, ou com falha isolada), ou a avaliação é só qualitativa ou parcial | Há medidas ⚠️, ou medidas ✅ e ❌ misturadas |
| ❌ | Não atende | O valor fica longe da meta, ou há defeito de severidade alta ligado à medida | A maior parte das medidas é ❌ |

Uma medida marcada como "informativo" não entra na nota da característica,
porque não tem meta.

## Resumo

| Característica | Subcaracterística | Medida | Valor | Meta | Avaliação |
|---|---|---|---|---|---|
| Adequação funcional | Correção funcional | M1. Casos com resultado igual ao requisito | 12 de 15 (80%) | 100% | ❌ |
| Confiabilidade | Maturidade | M2. Densidade de defeitos | 2 defeitos; 15,0 por mil linhas de código | o menor possível; nenhum de severidade alta | ❌ |
| Confiabilidade | Maturidade | M3. Defeitos corrigidos | 0 de 2 | — | informativo |
| Confiabilidade | Tolerância a falhas | M4. Caminhos de gravação que escondem a falha do usuário | 0 de 2 | 0 | ✅ |
| Eficiência de desempenho | Comportamento temporal | M5. Tempo de inclusão de item | não medido | — | não avaliada |
| Manutenibilidade | Testabilidade | M6. Cobertura de arestas | 100% (40/40) (antes: 87%) | ≥ 80% | ✅ |
| Manutenibilidade | Testabilidade | M7. Escore de mutação | 89% (33/37); 100% sem os 4 equivalentes (antes: 84%) | ≥ 80% | ✅ |
| Manutenibilidade | Analisabilidade e modificabilidade | M8. Problemas do Sonar | 0 (antes: 5) | 0 | ✅ |
| Manutenibilidade | Analisabilidade | M9. Complexidade ciclomática do maior método | `verificaRegraDeTributacao`: 14 | ≤ 10 | ❌ |
| Manutenibilidade | Modularidade | M10. Dependências da classe | 5 | ≤ 7 | ✅ |
| Usabilidade | Proteção contra erro do usuário | M11. Avaliação qualitativa | 2 problemas observados | — | ⚠️ |
| Segurança | — | M12. Vulnerabilidades e pontos sensíveis no Sonar | 0 e 0 | 0 | ✅ (só análise estática) |
| Segurança | Autenticidade | M13. Acessos sem login redirecionados para o login | 1 de 1 | 100% | ✅ |
| Compatibilidade | Interoperabilidade | M14. Operações de item que funcionam no banco padrão do projeto | 3 de 3 (incluir, somar quantidade e remover) | 3 de 3 | ✅ |
| Compatibilidade | Coexistência | M15. Avaliação qualitativa | roda ao lado do banco e do SonarQube; o PIT precisou de mais tempo | — | ⚠️ |
| Portabilidade | Instalabilidade | M16. Comandos para subir aplicação e banco do zero | 1 (`docker compose up -d`) | ≤ 3 | ✅ |
| Portabilidade | Adaptabilidade | M17. Versões de Java com que o projeto compila e testa | só Java 8; os testes Selenium exigem Java 11 ou mais | uma versão de Java com suporte atual | ❌ |

### Avaliação por característica

| Característica | Medidas | Avaliação | Justificativa |
|---|---|---|---|
| Adequação funcional | M1 | ❌ | 80% dos casos dão o resultado do requisito, mas a falha é ligada a um defeito de severidade alta (Issue #2) |
| Confiabilidade | M2 a M4 | ⚠️ | Não esconde falhas do repositório, mas tem um defeito de severidade alta aberto |
| Eficiência de desempenho | M5 | não avaliada | Não houve medição de tempo |
| Manutenibilidade | M6 a M10 | ⚠️ | Testabilidade e inspeção excelentes e poucas dependências; `verificaRegraDeTributacao` acima do limite de complexidade |
| Usabilidade | M11 | ⚠️ | Só avaliação qualitativa; mensagem técnica na tela e quantidade inválida aceita sem aviso |
| Segurança | M12 e M13 | ✅ | Nada apontado pela análise estática e acesso sem login bloqueado; não houve teste de permissões |
| Compatibilidade | M14 e M15 | ⚠️ | Funciona no banco padrão sem ajuste; a coexistência foi avaliada só de forma qualitativa |
| Portabilidade | M16 e M17 | ⚠️ | Fácil de instalar com Docker, mas preso ao Java 8 |

**Conclusão em uma frase:** `NotaFiscalItemService` agora está bem testado,
sem problemas no Sonar e com poucas dependências, mas ainda mostra um erro
técnico quando o produto não tem Modalidade BC ICMS e aceita quantidade zero
ou negativa; o desempenho não foi medido, e o projeto depende do Java 8.

---

## 1. Adequação funcional

### M1. Correção funcional — casos com resultado igual ao requisito

- **Fórmula:** X = A ÷ B, em que A é o número de casos cujo resultado obtido
  é igual ao esperado pelo requisito e B é o número de casos com resultado
  esperado definido pelo requisito.
- **Casos considerados:**
  - casos derivados das tabelas de partição e valor limite, TU-NFI-15 a 24:
    **8 passam**. TU-NFI-21 (qtd = 0) e TU-NFI-22 (qtd = −1) mostram que a
    quantidade inválida é aceita;
  - teste de sistema TS-NFI-01 a 05: **4 passam** e o TS-NFI-02 mostra o
    defeito da Issue #2.
- **Valor:** X = 12 ÷ 15 = **80%**. **Meta:** 100%. **Avaliação:** ❌, porque
  uma das falhas é ligada a defeito de severidade alta (Issue #2).
- **Evidências:** `NotaFiscalItemServiceTest.java` e
  `NotaFiscalItemSistemaIT.java`; tabela "Casos novos derivados das tabelas"
  no Plano de Teste.

Os casos TU-NFI-01 a 14, da Entrega 1, não entram nesta conta, para seguir o
mesmo critério do `VendaService`: só os casos do projeto funcional e de
sistema da Entrega 2. Os testes de integração também ficam de fora, porque
verificam a gravação no banco, e não o requisito.

## 2. Confiabilidade

### M2. Maturidade — densidade de defeitos

- **Fórmula:** X = número de defeitos ÷ tamanho em mil linhas de código. O
  tamanho é o do Sonar depois das correções: 133 linhas de código (0,133 mil).
- **Defeitos contados** (lista completa na seção 9):
  - Issue #2, da Entrega 1: `NoSuchElementException` quando o produto não tem
    Modalidade BC ICMS. Severidade alta;
  - novo, achado no projeto dos TU-NFI-21 e 22: `insere()` aceita quantidade
    0 ou negativa.
- **Valor:** 2 ÷ 0,133 = **15,0 defeitos por mil linhas**.
- **Avaliação:** ❌. A meta era não ter defeitos de severidade alta.

A Issue #1 (cadastro de Empresa) foi achada ao preparar os casos de nota
fiscal, mas não está em `NotaFiscalItemService` nem na inclusão de item, e
por isso não entra na conta.

### M3. Maturidade — defeitos corrigidos

- **Fórmula:** X = defeitos corrigidos ÷ defeitos encontrados.
- **Valor:** 0 ÷ 2 = **0%**.
- **Avaliação:** informativo. Nenhum defeito foi corrigido nesta entrega. Os
  testes que mostram cada defeito estão escritos com o comportamento correto
  e marcados com `@Ignore`, apontando o defeito: basta remover a anotação
  depois da correção.

### M4. Tolerância a falhas — caminhos de gravação que escondem a falha do usuário

- **Fórmula:** X = caminhos de gravação que, quando o repositório falha,
  respondem como se tivessem dado certo ÷ caminhos de gravação.
- **Operações que gravam:** `insere` e `remove`.
- **Valor:** **0 de 2.** Nos dois casos a exceção do repositório vira uma
  mensagem para o usuário ("Erro ao salvar item na nota, chame o suporte" e
  "Erro ao tentar remover o item da nota, chame o suporte").
- **Meta:** 0. **Avaliação:** ✅
- **Evidências:** TU-NFI-11 e TU-NFI-13 em `NotaFiscalItemServiceTest`.

## 3. Eficiência de desempenho

### M5. Comportamento temporal — tempo de inclusão de item

Não houve medição de tempo nesta entrega: os testes de sistema TS-NFI
verificam o resultado na tela, mas não o tempo de resposta.

**Avaliação:** não avaliada.

## 4. Manutenibilidade

### M6. Testabilidade — cobertura de arestas

- **Fórmula:** X = ramos percorridos ÷ ramos, medidos pelo JaCoCo. Com 100% das
  linhas, isso equivale ao critério todas-arestas.
- **Valor:** **100% (40/40)**, com 100% das linhas (85/85). Antes da
  Entrega 2 eram 87% (35/40), e `buscaItensNota` não tinha teste.
- **Meta:** ≥ 80%. **Avaliação:** ✅
- **Evidência:** `docs/evidencias/entrega2/depois-testes/jacoco-NotaFiscalItemService-metodos.png`.

### M7. Testabilidade — escore de mutação

- **Fórmula:** X = mutantes mortos ÷ mutantes gerados, medidos pelo PIT com os
  operadores padrão.
- **Valor:** **89% (33/37).** Antes da Entrega 2: 84% (31/37). Os 4 mutantes
  que sobreviveram são equivalentes: dois removem o `System.out.println` dos
  `catch` (linhas 115 e 133) e dois trocam `aux + 1` por `aux - 1` num contador
  que só é comparado com 0 (linhas 165 e 174). Sem eles, o escore é 100%.
- **Meta:** ≥ 80%. **Avaliação:** ✅
- **Evidência:** `docs/evidencias/entrega2/depois-testes/pit-NotaFiscalItemService.png`
  e `pit-NotaFiscalItemService-mutantes.png`.

### M8. Analisabilidade e modificabilidade — problemas do Sonar

- **Fórmula:** número de problemas abertos no arquivo
  `NotaFiscalItemService.java`.
- **Valor:** **0.** Antes: 5 problemas de severidade média, todos da regra
  de injeção no campo (um por dependência com `@Autowired` no atributo). A
  correção foi receber as dependências pelo construtor, sem mudar o
  comportamento.
- **Meta:** 0. **Avaliação:** ✅
- **Evidência:** `docs/evidencias/entrega2/depois-testes/sonar-NotaFiscalItemService-issues.png`
  e `sonar-notafiscal-metricas.png`.

### M9. Analisabilidade — complexidade do maior método

- **Fórmula:** complexidade ciclomática do método mais complexo, medida pelo
  JaCoCo.
- **Valor:** `verificaRegraDeTributacao`, com complexidade **14** e 26 linhas.
  O segundo é `insere`, com 8. A classe soma 25.
- **Meta:** ≤ 10 por método, um limite usual de mercado. **Avaliação:** ❌.
  O método junta cinco validações do produto com dois laços quase iguais,
  um para nota de saída e outro para nota de entrada.

### M10. Modularidade — dependências da classe

- **Fórmula:** número de outras classes do sistema injetadas em
  `NotaFiscalItemService`. Depois da correção do Sonar, todas aparecem no
  construtor.
- **Valor:** **5** (1 repositório e 4 serviços).
- **Meta:** ≤ 7, um limite usual para uma classe com uma só responsabilidade.
  **Avaliação:** ✅

## 5. Usabilidade

### M11. Proteção contra erro do usuário — avaliação qualitativa

Nesta etapa não há medida numérica de usabilidade; ela exigiria testes com
usuários. Ficam registradas duas observações dos testes:

1. Ao incluir um produto sem Modalidade BC ICMS, a tela mostra o alerta
   técnico "No value present", em vez de uma mensagem de validação
   (TS-NFI-02, Issue #2).
2. A quantidade 0 ou negativa é aceita sem aviso, e o item é gravado com
   valor total zero ou negativo (TU-NFI-21 e 22).

**Avaliação:** ⚠️

## 6. Segurança

### M12. Vulnerabilidades e pontos sensíveis no Sonar

- **Valor:** 0 vulnerabilidades e 0 pontos sensíveis (*security hotspots*) em
  `NotaFiscalItemService.java`, antes e depois das correções.
- **Avaliação:** ✅, mas só quanto à análise estática.

### M13. Autenticidade — acessos sem login redirecionados para o login

- **Fórmula:** X = páginas da nota fiscal acessadas sem login que redirecionam
  para a tela de login ÷ páginas testadas.
- **Valor:** **1 de 1.** Acessar `/notafiscal/form` sem estar logado
  redireciona para `/login` (TS-NFI-05, o caso não funcional).
- **Meta:** 100%. **Avaliação:** ✅. Não houve teste de permissões por grupo
  de usuário.

## 7. Compatibilidade

Não houve teste dedicado a compatibilidade. As duas medidas usam o que
apareceu nas outras etapas.

### M14. Interoperabilidade — operações de item que funcionam no banco padrão

- **Fórmula:** X = operações sobre itens da nota (incluir, somar quantidade
  de um produto já incluído e remover) que funcionam no banco definido no
  `docker-compose.yml` (MySQL 8.0, esquema criado pelo Flyway) sem ajuste
  manual ÷ operações.
- **Valor:** **3 de 3.** Os testes de integração TI-NFI-02 a 04 gravam e
  conferem item, imposto e totais no banco, e os testes de sistema TS-NFI-01,
  03 e 04 fazem as mesmas operações pela tela.
- **Meta:** 3 de 3. **Avaliação:** ✅

### M15. Coexistência — avaliação qualitativa

A aplicação, o banco e o SonarQube rodaram ao mesmo tempo, em containers
separados, sem conflito de porta ou de recurso. O único efeito observado foi
lentidão: com o limite de tempo padrão, o PIT marcava mutantes como
`TIMED_OUT` só por demora do container, e foi preciso usar
`timeoutConstant` de 30 s.

**Avaliação:** ⚠️, porque a avaliação é só qualitativa.

## 8. Portabilidade

### M16. Instalabilidade — comandos para subir aplicação e banco do zero

- **Fórmula:** número de comandos necessários, numa máquina com Docker, para
  ter a aplicação e o banco funcionando.
- **Valor:** **1** (`docker compose up -d`). O Flyway cria o esquema e os dados
  iniciais na primeira subida.
- **Meta:** ≤ 3. **Avaliação:** ✅. Sem Docker, o README pede criar o banco e
  configurar o `application.properties` à mão.

### M17. Adaptabilidade — versões de Java com que o projeto compila e testa

- **Fórmula:** versões de Java com que o projeto compila e roda a suíte de
  testes completa.
- **Valor:** o projeto está configurado para Java 8 (`java.version` 1.8 no
  `pom.xml`), e o README avisa que ele não funciona com JDKs recentes. Já os
  testes de sistema usam o Selenium 4.13, que exige Java 11 ou mais novo. A
  análise do Sonar ainda exige Java 21, num container separado.
- **Meta:** uma versão de Java com suporte atual que compile e teste tudo.
  **Avaliação:** ❌.

## 9. Situação final dos defeitos

Nenhum defeito foi corrigido na Entrega 2 (M3). Todos estão registrados em
teste com `@Ignore`.

| ID | Defeito | Severidade | Onde está | Evidência | Issue |
|---|---|---|---|---|---|
| Issue #2 | `NoSuchElementException` ao inserir item quando o produto está sem Modalidade BC ICMS (linha 72) | Alta | `NotaFiscalItemService` | TU-NFI-14, TI-NFI-05, TS-NFI-02 (`@Ignore`); CT-NFI-02 | [#2](https://github.com/annabchazan/trabalho-teste-pdv/issues/2) |
| — | `insere()` aceita quantidade 0 ou negativa e grava o item com valor total zero ou negativo | Não classificada | `NotaFiscalItemService` | TU-NFI-21 e 22 (`@Ignore`) | ainda sem Issue |

Fora da classe, a Issue [#1](https://github.com/annabchazan/trabalho-teste-pdv/issues/1)
(cadastro de Empresa expõe exceção técnica do Hibernate) foi achada ao
preparar os casos de nota fiscal e continua aberta.

## 10. Limitações

- **Desempenho:** não houve medição de tempo, então a eficiência de
  desempenho não foi avaliada.
- **Metas definidas pelo grupo:** as metas são escolhas deste trabalho,
  baseadas em limites usuais de mercado (cobertura e mutação ≥ 80%,
  complexidade ≤ 10), e não valores da norma.
- **Usabilidade e segurança:** a usabilidade foi avaliada só de forma
  qualitativa, e a segurança pela análise estática e por um único caso de
  acesso sem login.
- **Compatibilidade e portabilidade:** não houve teste dedicado. As medidas
  M14 a M17 usam o que apareceu nas outras etapas e no README, e por isso são
  menos profundas que as demais.
- **Escopo:** as medidas valem para `NotaFiscalItemService` e a inclusão de
  item em nota fiscal, não para o sistema inteiro.
