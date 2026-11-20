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
