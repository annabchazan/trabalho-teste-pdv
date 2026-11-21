# pdv
Sistema de ERP web desenvolvido em Java com Spring Framework 

# Recursos
- Cadastro produtos/clientes/fornecedor
- Controle de estoque
- Gerenciar comandas
- Realizar venda
- Controle de fluxo de caixa
- Controle de pagar e receber
- Venda com cartões
- Gerenciar permissões de usuários por grupos
- Cadastrar novas formas de pagamentos
- Relatórios

# Instalação
Para instalar o sistema, você deve criar o banco de dado "pdv" no mysql e configurar o arquivo application.properties
com os dados do seu usuário root do mysql e rodar o projeto pelo Eclipse ou gerar o jar do mesmo e execultar.

# Logando no sistema
Para logar no sistema, use o usuário "gerente" e a senha "123".

# Tecnologias utilizadas
- Spring Framework 5
- Thymeleaf 3
- MySQL
- Hibernate
- FlyWay

# Execução com Docker
Para executar a aplicação utilizando o docker, utilize o seguinte comando na raiz do projeto:
```sh
docker compose up -d
```

---

# Trabalho Acadêmico — Qualidade e Teste

Fork do software original ([repo-software-testing-courses/pdv](https://github.com/repo-software-testing-courses/pdv))
usado como alvo de teste na disciplina Qualidade e Teste.

## Artefatos das Entregas 1 e 2

- **Plano de Teste:** [docs/plano-teste.md](docs/plano-teste.md) (versão final colaborativa: [Google Docs](https://docs.google.com/document/d/1oVK7AhjF8N6JUp6qeUy8XOYfglWvrWK11OmjJYEYRDw/edit?tab=t.0))
- **Registro de uso de IA:** [docs/ai/AI-LOG.md](docs/ai/AI-LOG.md)
- **Código-fonte original:** `src/main/java/net/originmobi/pdv/`
- **Testes unitários:** `src/test/java/net/originmobi/pdv/`
- **Casos de teste manual (TestLink):** [instância da disciplina](http://vania.ic.uff.br/testlink/) — projeto `projeto_teste_anna`, caso executado CT-NFI-01 (evidência em [`docs/testes-manuais/evidencias/`](docs/testes-manuais/evidencias/))
- **Relatório de bugs:** [Issues deste repositório](../../issues)
- **Slides/apresentação:** [preencher diretório indicado pela professora]

### Artefatos por integrante

**Felipe Martins Bittencourt — `VendaService`**

- Testes unitários (32 cenários): [`VendaServiceTest.java`](src/test/java/net/originmobi/pdv/service/VendaServiceTest.java)
- Casos de teste manual CT-VENDA-01..11: [`docs/testes-manuais/CT-VENDA-01.md`](docs/testes-manuais/CT-VENDA-01.md)

**Anna Beatriz Chaboudet Chazan — `NotaFiscalItemService`**

- Testes unitários TU-NFI-01..24: [`NotaFiscalItemServiceTest.java`](src/test/java/net/originmobi/pdv/service/notafiscal/NotaFiscalItemServiceTest.java)
- Testes de integração TI-NFI-01..05: [`NotaFiscalItemServiceIT.java`](src/test/java/net/originmobi/pdv/service/notafiscal/NotaFiscalItemServiceIT.java)
- Testes de sistema (Selenium) TS-NFI-01..05: [`NotaFiscalItemSistemaIT.java`](src/test/java/net/originmobi/pdv/service/notafiscal/NotaFiscalItemSistemaIT.java)
- Evidências de JaCoCo, PIT e SonarQube, antes e depois dos testes novos: [`docs/evidencias/entrega2/`](docs/evidencias/entrega2/)
- Casos de teste manual CT-NFI-01/02: [`docs/testes-manuais/CT-NFI-01.md`](docs/testes-manuais/CT-NFI-01.md)
- CT-NFI-01 executado no TestLink (Passou): [`docs/testes-manuais/evidencias/`](docs/testes-manuais/evidencias/)
- Defeitos encontrados: Issues [#1](../../issues/1), [#2](../../issues/2)

**João Portela — `RecebimentoService`**

- **Relatório da Entrega 2** (projeto funcional, JaCoCo, PIT, Sonar, integração, Selenium e ISO 25010): [`docs/entrega2-recebimento.md`](docs/entrega2-recebimento.md)
- Testes unitários TU-REC-01..43: [`RecebimentoServiceTest.java`](src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java)
- Testes de integração TI-REC-01..07: [`RecebimentoServiceIT.java`](src/test/java/net/originmobi/pdv/service/RecebimentoServiceIT.java)
- Testes de sistema (Selenium) TS-REC-01..07: [`RecebimentoSistemaIT.java`](src/test/java/net/originmobi/pdv/service/RecebimentoSistemaIT.java)
- Evidências de JaCoCo, PIT e SonarQube, antes e depois: [`docs/evidencias/entrega2/recebimento/`](docs/evidencias/entrega2/recebimento/)
- Casos de teste manual CT-REC-01..03 — **executados**, com resultados obtidos: [`docs/testes-manuais/CT-REC-01.md`](docs/testes-manuais/CT-REC-01.md)
- Defeitos encontrados (D1 a D9; D1, D2, D3, D6, D7 e D8 corrigidos na Entrega 2): [`docs/bugs/defeitos-recebimento.md`](docs/bugs/defeitos-recebimento.md) — Issues [#4](../../issues/4), [#5](../../issues/5), [#6](../../issues/6), [#10](../../issues/10), [#11](../../issues/11), [#12](../../issues/12)
- Soluções iniciais geradas com IA, preservadas: [`v1`](docs/ai/snapshots/RecebimentoServiceTest.v1-ia.java) (Entrega 1) e [`v2`](docs/ai/snapshots/RecebimentoServiceTest.v2-ia.java) (Entrega 2)
- Revisão das soluções geradas com IA: [`docs/ai/revisao-recebimento-service.md`](docs/ai/revisao-recebimento-service.md)

**João Pedro G. Valadares — `CaixaService`**

- Testes unitários TU-CX-01..11: [`CaixaServiceTest.java`](src/test/java/net/originmobi/pdv/service/CaixaServiceTest.java)
- Casos manuais CT-CX-01/02, ainda planejados para execução: [`docs/testes-manuais/CT-CAIXA-01.md`](docs/testes-manuais/CT-CAIXA-01.md)

## Como executar os testes

O projeto tem `source/target 1.8`, incompatível com JDKs recentes. A forma
garantida de rodar a suíte é pelo container Java 8 do `docker-compose.yml`:

```sh
docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B test
```

Relatório de cobertura JaCoCo gerado em `target/site/jacoco/index.html`.

Com um JDK 8 instalado localmente, `./mvnw test` também funciona.

### Teste de mutação (PIT)

O plugin já está configurado com as 4 classes do grupo. Para rodar só a sua classe:

```sh
mvn test-compile org.pitest:pitest-maven:mutationCoverage -DtargetClasses=net.originmobi.pdv.service.notafiscal.NotaFiscalItemService -DtargetTests=net.originmobi.pdv.service.notafiscal.NotaFiscalItemServiceTest
```

Sem os parâmetros `-D`, roda nas 4 classes. Relatório em `target/pit-reports/index.html`.

### Testes de integração e de sistema (Selenium)

Classes com sufixo `IT` (ex.: `NotaFiscalItemServiceIT.java`) não rodam no `mvn test`; rodam com `mvn verify`.
O Selenium 4.13 baixa o ChromeDriver automaticamente (precisa do Chrome instalado e da aplicação rodando em `http://localhost:8080`).

### Inspeção de código (SonarQube)

O scanner exige Java 21, então a análise roda em um container separado, depois do `mvn test` com Java 8:

```sh
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
```

Acesse `http://localhost:9000` (usuário `admin`, senha `admin`), gere um token em *My Account > Security* e rode:

```sh
docker run --rm --network host -v "$PWD":/app -w /app -v pdv-m2:/root/.m2 maven:3.9-eclipse-temurin-21 mvn -B sonar:sonar -Dsonar.token=SEU_TOKEN
```

No Windows/macOS, troque `--network host` por `-Dsonar.host.url=http://host.docker.internal:9000`.

## Responsabilidades por membro

| Membro | Classe sob teste unitário | Funcionalidade testada manualmente |
|--------|---------------------------|-------------------------------------|
| Felipe Martins Bittencourt | `VendaService` | Realizar venda (abrir, adicionar produtos e fechar) |
| Anna Beatriz Chaboudet Chazan | `NotaFiscalItemService` | Inclusão de item em nota fiscal |
| João Portela | `RecebimentoService` | Receber parcelas em aberto de um cliente |
| João Pedro G. Valadares | `CaixaService` | Abertura e fechamento de caixa |

