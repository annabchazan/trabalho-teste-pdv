# Registro de decisões — Entrega 2 (`VendaService`)

**Responsável:** Felipe Martins Bittencourt
**Apoio de IA:** Claude Code (Anthropic, modelo Opus 5.5)

Regra de trabalho combinada: a IA **não executa nenhuma etapa sem aprovação
explícita** do responsável. Cada proposta e a decisão tomada ficam registradas
aqui, em ordem cronológica. Os commits são de autoria do responsável.

Formato de cada decisão: proposta da IA, alternativas apresentadas, decisão do
responsável e efeito no repositório.

---

## 2026-11-20 — Planejamento da Entrega 2

### D-01 — Divisão da entrega em etapas

- **Proposta:** dividir a Entrega 2 em 9 etapas, nesta ordem:
  0. Preparação
  1. Testes unitários com dependências isoladas
  2. Teste funcional
  3. Teste estrutural (todas-arestas, ≥ 80%)
  4. Teste de mutação (≥ 80%)
  5. Testes de integração
  6. Testes de sistema (Selenium + 1 requisito não funcional)
  7. Inspeção com Sonar
  8. Medidas ISO 25010 e fechamento

  Justificativa da ordem: o teste funcional vem antes; cobertura e mutação
  mostram as lacunas que sobraram; a ISO 25010 usa as evidências de todas as
  etapas anteriores.
- **Proposta complementar:** um único Google Docs "Relatório da Entrega 2",
  com uma seção por item do enunciado e um registro de decisões.
- **Decisão:** aprovado; começar pela Etapa 0.
- **Efeito:** nenhum arquivo alterado.

## 2026-11-20 — Etapa 0: Preparação

**Levantamento feito antes das decisões:** a Anna Chazan já tinha configurado
no `pom.xml` (commit `a51da9e`) as ferramentas JaCoCo 0.8.11, PIT 1.15.8 (com
`VendaService` entre as classes-alvo), Selenium e failsafe para classes `*IT`.
O README já documenta como rodar o SonarQube em container. Por isso, a Etapa 0
de `VendaService` não precisa alterar o build.

### D-02 — Classe sob teste

- **Proposta:** manter `VendaService`.
- **Justificativa:** é a classe de maior complexidade entre as quatro do grupo.
  A contagem de pontos de decisão (`if/for/while/case/catch/&&/||`) dá 27 em
  `VendaService`, contra 23 em `NotaFiscalItemService` e 19 em
  `RecebimentoService` e `CaixaService`. Só o método `fechaVenda` tem
  complexidade ciclomática estimada de cerca de 14. Além disso, não é CRUD: tem
  regras de venda, pagamento à vista e a prazo, parcelas e caixa. Essa contagem
  é uma estimativa; o valor oficial virá do Sonar.
- **Alternativa apresentada:** trocar de classe.
- **Decisão:** manter `VendaService`.

### D-03 — Forma de registro das decisões

- **Proposta:** este arquivo, com uma entrada resumida no
  [`AI-LOG.md`](AI-LOG.md) apontando para ele.
- **Alternativa apresentada:** registrar cada decisão diretamente no AI-LOG.
- **Decisão:** arquivo próprio + AI-LOG.
- **Efeito:** criados este arquivo e a entrada de 2026-11-20 no AI-LOG.

### D-04 — Branch de trabalho

- **Proposta:** branch `test/venda-service-entrega2`, criada a partir do
  `master` atualizado (`325d7ac`). O merge ou PR só acontece quando o
  responsável pedir.
- **Alternativa apresentada:** commitar direto no `master`, como na parte da
  Anna.
- **Decisão:** branch nova.
- **Efeito:** branch criada localmente; ainda não publicada no remoto.

### D-05 — Linha de base antes de mexer nos testes

- **Proposta:** medir JaCoCo, PIT e Sonar de `VendaService` antes de qualquer
  alteração nos testes. Os prints ficam em `docs/evidencias/entrega2/baseline/`,
  seguindo o padrão da Anna, e servem de "antes" no relatório.
- **Alternativa apresentada:** adiar a medição.
- **Decisão:** medir agora.
- **Efeito:** a medição exigiu antes resolver o problema do Docker (D-06).
  Resultados de JaCoCo e PIT em D-07. O Sonar ficou pendente (D-08).

### D-06 — Docker Desktop não iniciava

- **Contexto:** a máquina não tem JDK 8 (só 17 e 25) e o projeto só compila em
  Java 8, então a suíte precisa rodar no container do `docker-compose.yml`.
  O Docker Desktop (4.58) estava parado.
- **Proposta 1:** iniciar o Docker Desktop. **Decisão:** aprovado.
  **Resultado:** erro na inicialização, porque ele não conseguiu remover o
  socket `AppData\Local\Docker\run\dockerInference`.
- **Proposta 2:** apagar os dois sockets antigos de 24/10 nessa pasta.
  **Alternativa apresentada:** o próprio responsável resolver.
  **Decisão:** aprovado, sem usar "Reset to factory defaults", que apagaria os
  volumes do projeto. **Resultado:** o Windows não apagou os arquivos (*"Não é
  possível o acesso ao arquivo pelo sistema"*). Eles só foram apagados pelo
  Linux do WSL (`wsl -d Ubuntu -- rm`). O Docker recriou o socket e falhou de
  novo.
- **Proposta 3:** desligar o Docker AI (`EnableDockerAI` em
  `settings-store.json`, com backup). **Alternativa apresentada:** usar o JDK 17
  local. **Decisão:** aprovado. **Resultado:** não resolveu. O log mostrou a
  mesma falha em outro socket antigo,
  `AppData\Local\docker-secrets-engine\engine.sock`.
- **Diagnóstico:** este Windows (build 26300, de teste) não consegue apagar
  arquivos de socket Unix, e o Docker Desktop falha sempre que encontra um
  socket deixado pela execução anterior.
- **Proposta 4:** desfazer a mudança do Docker AI, apagar todos os sockets
  antigos pelo WSL e reiniciar. **Alternativa apresentada:** instalar o JDK 8.
  **Decisão:** aprovado. **Resultado:** o Docker subiu (Engine 29.1.5) e a
  configuração voltou ao original.
- **Para as próximas vezes:** se o erro voltar, encerre o Docker e rode:
  `wsl -d Ubuntu -- bash -c 'find /mnt/c/Users/fmart/AppData/Local/Docker /mnt/c/Users/fmart/AppData/Local/docker-secrets-engine -maxdepth 3 -type s -delete'`.

### D-07 — Resultado da linha de base (JaCoCo e PIT)

- **Execução da suíte:** 93 testes, 0 falhas, 1 erro e 7 ignorados. Os 32
  testes de `VendaServiceTest` passaram. O erro é do
  `PdvApplicationTests.contextLoads`, que precisa do MySQL, e o comando do
  README usa `--no-deps`, sem banco. Esse erro interrompia o build antes do
  relatório do JaCoCo. **Decisão operacional da IA:** rodar com
  `-Dmaven.test.failure.ignore=true`. O erro continua sendo reportado, mas o
  relatório é gerado. Nenhum arquivo do projeto mudou.
- **JaCoCo — `VendaService` (antes):**

  | Métrica | Valor |
  |---|---|
  | Instruções | 99% (4 de 584 não cobertas) |
  | Ramos (todas-arestas) | **97%** (1 de 38 não coberto, em `fechaVenda`) |
  | Linhas | 124 de 126 (2 não cobertas, em `abreVenda`) |
  | Complexidade ciclomática | 30 (`fechaVenda` = 11) |

  A meta de 80% de todas-arestas já é atendida pelos testes da Entrega 1.
- **PIT — `VendaService` (antes):** 66 mutantes, 52 mortos, **79% de escore**,
  um ponto abaixo da meta de 80%. Os 14 sobreviventes:
  - 7 aritméticos (`MathMutator`): `fechaVenda` L169, L197 e L198;
    `avistaDinheiro` L290 e L309; `aprazo` L266. Indicam asserções fracas sobre
    os valores calculados.
  - 2 condicionais negadas (`NegateConditionalsMutator`): `fechaVenda` L179 e
    L217.
  - 1 chamada removida a `Venda.setPagamentotipo` (L184). O teste não verifica
    o tipo de pagamento gravado.
  - 1 retorno trocado por 0 em `avistaDinheiro` (L319).
  - 3 remoções de `System.out.println` (L193, L242, L316). São candidatos a
    mutantes equivalentes, e o Sonar tende a apontar esses `println` como
    problema.

### D-08 — Linha de base do Sonar

- **Proposta:** baixar as imagens Docker `sonarqube:community` e
  `maven:3.9-eclipse-temurin-21` (o scanner do README exige Java 21) e subir o
  SonarQube local. **Alternativa apresentada:** deixar o Sonar para depois.
  **Decisão:** aprovado.
- **Credenciais:** o responsável entrou no SonarQube, trocou a senha padrão e
  gerou o token na própria conta. A IA não criou nem viu credenciais. O token
  foi lido de um arquivo temporário só durante o comando e apagado em seguida.
  Não está no repositório nem no chat.
- **Execução:** `mvn sonar:sonar` no container Java 21, usando as classes e o
  `jacoco.xml` gerados pela execução em Java 8 (D-07).
- **Resultado — projeto inteiro:** 10.910 linhas, cobertura de 17,6%, 8 bugs,
  29 vulnerabilidades, 176 code smells. Notas: confiabilidade E,
  segurança D, manutenibilidade A.
- **Resultado — `VendaService` (antes):**

  | Métrica | Valor |
  |---|---|
  | Linhas de código | 225 |
  | Complexidade ciclomática | 29 |
  | Complexidade cognitiva | 44 |
  | Cobertura / ramos | 98,2% / 97,4% |
  | Bugs / vulnerabilidades | 0 / 0 |
  | Code smells | **11** (dívida técnica de 55 min) |
  | Duplicação | 0% |
  | Notas (confiab. / seg. / manut.) | A / A / A |

  Os 11 problemas são da mesma regra, `java:S6813` (MAJOR): injeção por campo
  com `@Autowired` nas linhas 40 a 70, nas 11 dependências da classe. O Sonar
  recomenda injeção pelo construtor. A Anna fez essa mesma correção em
  `NotaFiscalItemService` (commit `5aa1420`). Essa correção também facilita
  isolar as dependências nos testes (Etapa 1). As 3 chamadas a
  `System.out.println` não foram apontadas pelo perfil padrão do Sonar.
- **Prints:** a tirar pelo responsável e salvar em
  `docs/evidencias/entrega2/baseline/`.

### D-09 — Commit da Etapa 0

- **Proposta:** commitar o registro de decisões e o AI-LOG na branch
  `test/venda-service-entrega2`, em nome do responsável e sem coautoria da IA.
- **Alternativa apresentada:** esperar os prints e commitar tudo junto.
- **Decisão:** commitar agora. Os prints da linha de base irão em um commit
  separado.
- **Efeito:** commit local, ainda não enviado ao remoto.

## 2026-11-20 — Etapa 1: Testes unitários com dependências isoladas

### D-10 — Escopo da Etapa 1

- **Diagnóstico da IA:** o `VendaServiceTest` já simula com Mockito as 11
  dependências da classe e não acessa o banco. Há duas falhas de isolamento:
  - **I-1:** o singleton `Aplicacao` guarda numa variável estática o usuário
    lido do `SecurityContextHolder` e só o lê uma vez. O resultado depende da
    ordem dos testes e de outras classes de teste da mesma execução.
  - **I-2:** `fechaVenda` cria `new DataAtual()`, que usa o relógio real, e os
    testes aceitavam qualquer data de vencimento (`any(Date.class)`).
- **Proposta:**
  1. **Isolamento só no código de teste:** zerar `Aplicacao.aplicacao` com
     `ReflectionTestUtils` antes e depois de cada teste (I-1). Conferir os
     vencimentos em relação à data de hoje (I-2). Sem alterar o `pom.xml` nem a
     produção.
  2. **Defeitos:** manter os testes "DEFEITO" documentando o comportamento
     atual, sem corrigir os bugs nesta etapa.
  3. **9 testes novos (T-1 a T-9)**, voltados ao ramo sem cobertura, às linhas
     sem cobertura e aos mutantes sobreviventes que podem ser mortos.
- **Alternativas apresentadas:** mudar a produção agora (injeção por construtor
  e relógio como dependência, o que adiantaria a Etapa 7); corrigir os
  defeitos já nesta etapa; mudar a lista de testes.
- **Decisão:** aprovado como proposto.

### D-11 — Resultado da Etapa 1

- **Alterações em `VendaServiceTest`:** só código de teste. A produção e o
  `pom.xml` não mudaram.
  - I-1: `ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null)` no
    `@Before` e no `@After`.
  - I-2 / T-8: o teste a prazo "30/60" confere os vencimentos `hoje + 30` e
    `hoje + 60` (helper `hojeMais`), em vez de `any(Date.class)`.
  - 9 métodos de teste novos:

    | ID | Método | Tipo |
    |---|---|---|
    | T-1 | `abreVenda_deveEngolirExcecao_quandoRepositorioFalhaAoAtualizarVendaExistente` | DEFEITO |
    | T-2 | `fechaVenda_deveLancarNoCartao_quandoAVistaComCartaoDeDebito` | sucesso |
    | T-3 | `fechaVenda_deveFecharSemRegistrarRecebimento_quandoTituloAVistaNaoEDinheiroNemCartao` | DEFEITO |
    | T-4 | `fechaVenda_deveRegistrarOReceberComDescontoEAcrescimo` | sucesso |
    | T-5 | `fechaVenda_deveGravarAFormaDePagamentoNaVenda` | sucesso |
    | T-6 | `fechaVenda_deveRatearDescontoEAcrescimoComSinaisTrocados_nasParcelasAPrazo` | DEFEITO |
    | T-7 | `fechaVenda_deveSubtrairOAcrescimo_noLancamentoDeCaixaAVista` | DEFEITO |
    | T-9a | `fechaVenda_deveLancarErroTecnico_quandoTituloNaoExiste` | DEFEITO |
    | T-9b | `fechaVenda_deveLancarErroTecnico_quandoVendaNaoExiste` | DEFEITO |

- **Execução** (container Java 8): `VendaServiceTest` com 41 testes, 0 falhas
  e 0 erros. Suíte completa: 102 testes, com o mesmo único erro preexistente
  de `PdvApplicationTests`, que exige MySQL.
- **Medições:**

  | Métrica | Antes (D-07) | Depois |
  |---|---|---|
  | Testes em `VendaServiceTest` | 32 | 41 |
  | JaCoCo, instruções | 99% | **100%** |
  | JaCoCo, ramos (todas-arestas) | 97% (37/38) | **100%** (38/38) |
  | JaCoCo, linhas | 124/126 | **126/126** |
  | PIT, escore de mutação | 79% (52/66) | **91%** (60/66) |

- **Os 6 mutantes sobreviventes são equivalentes:** nenhum teste consegue
  distingui-los do código original, porque não mudam nenhum comportamento
  observável. A justificativa detalhada fica para a Etapa 4:
  - L193, L242 e L316 removem `System.out.println(e)` dentro de `catch`. Só
    afetam o console, e a exceção relançada é a mesma.
  - L290 (`qtdVezes - 1` → `+ 1`) e L319 (retorno trocado por 0) alteram
    `qtdVezes`, que é repassado a `aprazo()` mas nunca é lido.
  - L179 nega a condição do laço que preenche `modeloPagar`, um mapa que nunca
    é consultado.

### D-12 — Commit da Etapa 1

- **Proposta:** commitar `VendaServiceTest.java` e este registro na branch
  `test/venda-service-entrega2`, em nome do responsável e sem coautoria da IA.
- **Alternativa apresentada:** revisar as alterações antes.
- **Decisão:** commitar.
- **Efeito:** commit local `467a5b0`, ainda não enviado ao remoto.

## 2026-11-20 — Etapa 2: Teste funcional

### D-13 — Técnicas, escopo, documentação e defeitos

- **Proposta:** derivar os casos a partir dos requisitos RF-01 a RF-04, sem
  olhar o código, usando:
  - classes de equivalência;
  - valores-limite;
  - tabela de decisão por parcela, com as regras R1 a R7.

  O resultado são 12 testes novos (TF-VEN-01 a TF-VEN-12). Os testes que já
  existiam passam a ter a referência da regra que cobrem.
- **Decisões, uma por item:**
  1. **Técnicas:** classes de equivalência, valores-limite e tabela de
     decisão. Alternativa recusada: incluir o grafo de causa e efeito, que
     repetiria a tabela de decisão.
  2. **Escopo:** foco em `fechaVenda`, mais os limites de RF-02
     (`addProduto`) e RF-03 (`busca`). Alternativa recusada: só `fechaVenda`.
  3. **Documentação:** a IA escreve o projeto de teste em
     [`docs/teste-funcional-venda.md`](../teste-funcional-venda.md) e o
     responsável passa o texto para o Google Docs com a própria conta, para a
     autoria ficar registrada lá. Alternativa recusada: o responsável escrever
     direto no Docs.
  4. **Defeitos:** mesmo critério de D-10. Os testes registram o comportamento
     atual marcado como DEFEITO, sem corrigir a produção.
- **Decisão:** aprovado como proposto.

### D-14 — Resultado da Etapa 2

- **Artefatos:**
  - [`docs/teste-funcional-venda.md`](../teste-funcional-venda.md): requisitos,
    13 grupos de classes de equivalência, valores-limite, tabela de decisão
    R1 a R7 e rastreabilidade de cada classe, limite e regra até o teste que a
    cobre.
  - `VendaServiceTest.java`: 12 testes novos (TF-VEN-01 a 12) e um comentário
    `// Teste funcional:` em 8 testes que já existiam, indicando a regra ou o
    limite que cobrem.
- **Ajuste de implementação, decidido pela IA:** no TF-VEN-06 (desconto de
  100,01 sobre 100,00), o total calculado não é exatamente −0,01 em `double`.
  O teste compara com tolerância de 0,000001 (`doubleThat`), e a observação
  ficou registrada no projeto de teste.
- **Defeitos novos encontrados pelo teste funcional (6):**
  - desconto igual ao total fecha a venda por 0,00 (TF-VEN-05);
  - desconto maior que o total fecha a venda com valor negativo (TF-VEN-06);
  - acréscimo negativo é aceito (TF-VEN-07);
  - faltar título para uma forma de pagamento gera
    `ArrayIndexOutOfBoundsException` depois de a 1ª parcela já ter sido gerada
    e a venda marcada como fechada (TF-VEN-09);
  - incluir produto em venda inexistente gera `NullPointerException`
    (TF-VEN-10);
  - situação nula na busca gera `NullPointerException` (TF-VEN-12).

  Há ainda 2 comportamentos que o requisito não define e que ficaram apenas
  registrados: o prazo `0` é tratado como venda a prazo (TF-VEN-08), e a busca
  por situação diferencia maiúsculas de minúsculas (TF-VEN-11).
- **Execução** (container Java 8): `VendaServiceTest` com 53 testes, 0 falhas
  e 0 erros. Suíte completa: 114 testes, com o mesmo único erro preexistente
  de `PdvApplicationTests`.
- **Medições:** JaCoCo continua em 100% de instruções, ramos e linhas. O PIT
  continua em 91% (60 de 66), com os mesmos 6 mutantes equivalentes de D-11.
  Como esperado, os testes funcionais não mataram mutantes novos, porque os
  testes estruturais da Etapa 1 já cobriam o código. O ganho deles está nos
  defeitos encontrados a partir dos requisitos.
- **Pendente para o responsável:** passar o conteúdo de
  `docs/teste-funcional-venda.md` para o Google Docs com a própria conta.

### D-15 — Commit da Etapa 2

- **Proposta:** commitar o projeto de teste funcional, `VendaServiceTest.java`
  e este registro na branch `test/venda-service-entrega2`, em nome do
  responsável e sem coautoria da IA.
- **Alternativa apresentada:** revisar as alterações antes.
- **Decisão:** commitar.
- **Efeito:** commit local `838c831`, ainda não enviado ao remoto.

## 2026-11-20 — Etapas 3 e 4: Teste estrutural e teste de mutação

### D-16 — Escopo da documentação e das evidências

- **Contexto:** as metas já tinham sido atingidas nas Etapas 1 e 2, com 100%
  de ramos e 91% de escore de mutação. As Etapas 3 e 4 ficam para documentar
  e justificar esses números, com evidências do antes e do depois.
- **Proposta, uma decisão por item:**
  1. **Grafo de fluxo de controle** de `fechaVenda`, `avistaDinheiro` e
     `aprazo`, com cada aresta ligada ao teste que passa por ela. Alternativa
     recusada: fazer o grafo de todos os 11 métodos.
  2. **Prints** gerados automaticamente com o Chrome sem janela, a partir dos
     relatórios HTML. O "antes" vem dos testes da Entrega 1 (commit `325d7ac`),
     rodados numa cópia temporária fora do repositório. Alternativa recusada:
     o responsável tirar os prints manualmente. O Sonar continua manual,
     porque exige login.
  3. **Operadores de mutação:** o conjunto padrão do PIT (DEFAULTS),
     configurado pelo grupo, para os escores serem comparáveis entre as
     classes. Alternativa recusada: o conjunto STRONGER.
  4. **Documento:** um arquivo só, `docs/teste-estrutural-mutacao-venda.md`,
     para as duas etapas. Alternativa recusada: um arquivo por etapa.
- **Decisão:** aprovado como proposto.

### D-17 — Resultado das Etapas 3 e 4

- **Linha de base reproduzida:** os testes do commit `325d7ac` foram extraídos
  com `git archive` para a pasta temporária da sessão, fora do repositório, e
  rodados no mesmo container Java 8. Os números bateram com D-07: 97% de ramos
  e 79% de mutação. A branch de trabalho não foi alterada.
- **Depois:** PIT rodado de novo com `-DfullMutationMatrix=true` (opção de
  linha de comando, sem mudar o `pom.xml`) para obter os testes que matam cada
  mutante. Resultado: 91% (60/66).
- **Evidências:** 8 prints gerados com o Chrome sem janela a partir dos
  relatórios HTML, recortados com PIL (Python), em
  `docs/evidencias/entrega2/baseline/` e `depois-testes/`:
  - `jacoco-VendaService-metodos`;
  - `jacoco-VendaService-codigo`, que mostra `fechaVenda` com o losango
    amarelo da linha 218 no "antes";
  - `pit-VendaService` (resumo);
  - `pit-VendaService-mutantes` (lista de mutantes).

  **Ajuste da IA:** a rolagem por âncora não funcionou no Chrome sem janela.
  Por isso as páginas foram capturadas inteiras e recortadas no trecho
  relevante.
- **Documento:** [`docs/teste-estrutural-mutacao-venda.md`](../teste-estrutural-mutacao-venda.md),
  com:
  - **Teste estrutural:** justificativa de por que ramos + linhas do JaCoCo
    equivalem a todas-arestas; grafo de fluxo de controle de `fechaVenda` (25
    nós, 30 arestas, diagrama Mermaid) e de `avistaDinheiro` e `aprazo`; cada
    aresta ligada ao teste que a percorre. A única aresta sem teste no "antes"
    era a **e24** (título à vista de outro tipo), coberta pelo T-3.
  - **Teste de mutação:** escore bruto 79% → **91%**; escore efetivo, sem os
    equivalentes, 87% → **100%**. A matriz traz os 66 mutantes com antes e
    depois. Os 8 mutantes mortos na Entrega 2 estão explicados um a um, e a
    equivalência dos 6 vivos está justificada.
- **Correção feita pela IA durante a revisão:** a primeira versão do texto
  dizia que o JaCoCo conta os `catch` na complexidade, o que está errado. O
  texto foi corrigido: pelo grafo a complexidade é 13 (12 decisões, incluindo
  as 2 saídas por exceção), e o JaCoCo informa 11 porque conta só as 10
  condições do bytecode.
- **Observação para a Etapa 7:** os 6 equivalentes vêm de código morto (os
  `println` dos `catch`, o parâmetro `qtdVezes` sem uso e o mapa `modeloPagar`
  nunca lido). São candidatos naturais à limpeza na inspeção com o Sonar.
- **Pendente para o responsável:** passar o documento para o Google Docs. O
  diagrama Mermaid aparece desenhado no GitHub; no Docs pode entrar como print.

### D-18 — Commit das Etapas 3 e 4

- **Proposta:** commitar o documento de teste estrutural e de mutação, os 8
  prints de evidência e este registro na branch `test/venda-service-entrega2`,
  em nome do responsável e sem coautoria da IA.
- **Alternativa apresentada:** revisar as alterações antes.
- **Decisão:** commitar.
- **Efeito:** commit local, ainda não enviado ao remoto.

## 2026-11-20 — Etapa 5: Testes de integração

### D-19 — Escopo dos testes de integração

- **Contexto:** nos testes unitários todas as dependências de `VendaService`
  são simuladas. Ficam sem verificação as consultas reais do
  `VendaRepository`, a transação do `fechaVenda` (`@Transactional`) e a
  colaboração com os serviços reais de caixa, títulos, recebimento, parcelas e
  estoque.
- **Proposta:**
  1. Classe `VendaServiceIT.java` no padrão já usado pelo grupo
     (`NotaFiscalItemServiceIT`): `@SpringBootTest` sem servidor web, MySQL do
     `docker-compose` (`pdv-db`) com o esquema do Flyway, massa de dados com
     códigos a partir de 9001 apagada antes e depois de cada caso, execução
     por `mvn verify`. Usuário logado simulado como `gerente`.
  2. Casos TI-VEN-01 a 08: abrir venda; incluir produto em venda aberta e
     fechada; busca e `qtdAbertos`; fechar à vista em dinheiro; fechar a
     prazo; fechar sem caixa aberto (verificar se a transação é desfeita);
     fechar com estoque insuficiente (verificar se a venda volta a ficar
     aberta); fechar venda já fechada.
  3. Comportamento divergente do requisito é registrado como DEFEITO, sem
     alterar o código de produção.
  4. Ações de ambiente: subir só o container `pdv-db` e rodar o Maven num
     container na mesma rede, executando apenas a `VendaServiceIT`.
  5. Entregáveis: `VendaServiceIT.java`, `docs/teste-integracao-venda.md`,
     print do relatório de execução e registro das decisões.
- **Decisão:** aprovado como proposto.

### D-20 — Caixa aberto existente no banco local

- **Contexto:** o banco local (`pdv-db`) tem o "Caixa01" (código 1), aberto
  desde os testes manuais da Entrega 1. O sistema só aceita um caixa do tipo
  `CAIXA` aberto por vez. O TI-VEN-06 precisa que não haja caixa aberto, e um
  lançamento no caixa altera os totais dele por trigger.
- **Opções apresentadas:** (1) guardar os dados do Caixa01, marcá-lo como
  fechado durante cada caso e restaurá-lo depois, usando um caixa próprio
  (9001) nos testes; (2) lançar no próprio Caixa01 e desfazer os totais na
  limpeza; (3) criar um banco separado só para as ITs.
- **Decisão:** opção 1, fechar e restaurar. O Caixa01 nunca recebe
  lançamentos dos testes.

### D-21 — Resultado da Etapa 5

- **Execução:** `VendaServiceIT` com 9 métodos (TI-VEN-01 a 08, sendo o 02
  dividido em venda aberta e fechada). Rodou num container Java 8 ligado à
  rede do `pdv-db`: 9 passaram, 0 falhas. Relatório gerado com o
  `maven-surefire-report-plugin` 3.2.5, pela linha de comando e sem mudar o
  `pom.xml`. O print está em
  `docs/evidencias/entrega2/integracao/failsafe-VendaServiceIT.png`.
- **Transação de `fechaVenda`:** os casos TI-VEN-06 (sem caixa aberto) e
  TI-VEN-07 (estoque insuficiente) mostram que ela é desfeita. Para provar que
  as gravações aconteceram antes do erro, a IA rodou os dois casos de novo e
  conferiu o log de SQL: `insert into receber` nos dois casos, e
  `update venda` e `insert into caixa_lancamento` no 07. Nenhum desses
  registros ficou no banco.
- **Defeitos novos:** nenhum.
- **Banco local:** depois da execução, o Caixa01 voltou como estava (aberto,
  com a mesma `data_alteracao`) e não sobraram registros dos testes.
- **Documento:** [`docs/teste-integracao-venda.md`](../teste-integracao-venda.md).
- **Pendente para o responsável:** passar o documento para o Google Docs.

### D-22 — Commit da Etapa 5

- **Proposta:** commitar `VendaServiceIT.java`, o documento de testes de
  integração, o print do relatório e este registro na branch
  `test/venda-service-entrega2`, em nome do responsável e sem coautoria da IA.
- **Alternativa apresentada:** revisar as alterações antes.
- **Decisão:** commitar.
- **Efeito:** commit local, ainda não enviado ao remoto.

## 2026-11-20 — Etapa 6: Testes de sistema (Selenium) e requisito não funcional

### D-23 — Escopo dos testes de sistema

- **Proposta:**
  1. Classe `VendaSistemaIT.java` no padrão de `NotaFiscalItemSistemaIT`:
     Chrome sem janela por padrão, login como `gerente`, massa de dados
     criada direto no MySQL (porta 3307) com códigos a partir de 9201 e
     apagada antes e depois de cada caso. O Caixa01 é tratado como na D-20.
  2. Casos funcionais, que automatizam os testes manuais da Entrega 1:
     TS-VEN-01 (CT-VENDA-01) abrir venda, incluir e remover produto;
     TS-VEN-02 (CT-VENDA-02) fechar à vista em dinheiro; TS-VEN-03
     (CT-VENDA-03) fechar a prazo; TS-VEN-04 (CT-VENDA-06) fechar sem caixa
     aberto; TS-VEN-05 (CT-VENDA-07) fechar a prazo sem cliente; TS-VEN-06
     (CT-VENDA-08) desconto e acréscimo, que espera o comportamento correto e
     fica com `@Ignore` por causa do defeito D-VEN-01.
  3. Requisito não funcional TS-VEN-07, de desempenho (ISO 25010: eficiência
     de desempenho, tempo de resposta): fechar uma venda pela tela leva no
     máximo 2 s, do clique em confirmar até a mensagem. Medição de 5
     fechamentos depois de um de aquecimento; critério: o maior tempo. A
     segurança não foi escolhida porque já foi testada pela Anna.
  4. Ambiente: aplicação com `docker compose up -d pdv-app`; testes rodando
     no Windows, com Maven e Java 17, por causa do Chrome.
  5. Entregáveis: `VendaSistemaIT.java`, `docs/teste-sistema-venda.md`,
     prints e registro das decisões.
- **Decisão:** aprovado como proposto.

### D-24 — Falha intermitente no requisito de desempenho

- **Contexto:** na primeira execução da suíte, o TS-VEN-07 falhou: tempos de
  879, 709, 5339, 733 e 738 ms, contra o limite de 2000 ms para o maior. No
  log de SQL da aplicação, a IA achou uma pausa de 3,8 s no servidor logo
  depois do `drop temporary table HT_venda`, que o Hibernate executa no
  update em massa da venda. Duas novas execuções só do TS-VEN-07 passaram
  (maior tempo de 666 e 607 ms).
- **Opções apresentadas:** (1) manter o critério e registrar a falha
  intermitente com a causa achada; (2) mudar o critério (por exemplo,
  mediana de até 2 s); (3) ampliar a amostra para 20 medições.
- **Decisão:** opção 1, manter e registrar.

### D-25 — Resultado da Etapa 6

- **Execução final** (suíte completa, no Windows, com Java 17): 7 testes, 6
  passaram, 1 pulado (TS-VEN-06), 0 falhas. Tempos do TS-VEN-07: 573, 570,
  584, 556 e 555 ms.
- **TS-VEN-06:** rodado uma vez sem o `@Ignore` para confirmar o defeito.
  Falhou com `expected:<8.0> but was:<12.0>` (lançamento de caixa), o que
  confirma o D-VEN-01 pela tela. A anotação foi recolocada logo depois.
- **Prints:** a pedido do escopo, o TS-VEN-02 grava 2 prints de tela (modal
  de pagamento e venda fechada). Ao todo são 3 arquivos em
  `docs/evidencias/entrega2/sistema/`, contando o relatório. **Ajuste da
  IA:** o primeiro relatório incluiu o resultado antigo da `VendaServiceIT`
  que estava em `target`. O relatório foi gerado de novo só com a
  `VendaSistemaIT`.
- **Ambiente:** o plugin de relatório do Maven foi baixado da Maven Central
  para o repositório local do Windows. O Caixa01 voltou como estava e não
  sobraram registros dos testes.
- **Observação de interface:** na venda fechada, o "x" de remover produto
  continua aparecendo. Fica para a avaliação de usabilidade.
- **Documento:** [`docs/teste-sistema-venda.md`](../teste-sistema-venda.md).
- **Pendente para o responsável:** passar o documento para o Google Docs.

### D-26 — Commit da Etapa 6

- **Proposta:** commitar `VendaSistemaIT.java`, o documento de testes de
  sistema, os 3 prints e este registro na branch `test/venda-service-entrega2`,
  em nome do responsável e sem coautoria da IA.
- **Alternativa apresentada:** revisar as alterações antes.
- **Decisão:** commitar.
- **Efeito:** commit local, ainda não enviado ao remoto.
