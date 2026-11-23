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

- Testes unitários e funcionais (53 testes, dependências isoladas com Mockito; TF-VEN-01..12): [`VendaServiceTest.java`](src/test/java/net/originmobi/pdv/service/VendaServiceTest.java)
- Testes de integração TI-VEN-01..08: [`VendaServiceIT.java`](src/test/java/net/originmobi/pdv/service/VendaServiceIT.java)
- Testes de sistema (Selenium) TS-VEN-01..07, com o requisito não funcional de desempenho: [`VendaSistemaIT.java`](src/test/java/net/originmobi/pdv/service/VendaSistemaIT.java)
- Casos de teste manual CT-VENDA-01..11: [`docs/testes-manuais/CT-VENDA-01.md`](docs/testes-manuais/CT-VENDA-01.md)
- Entrega 2, documentos (rascunhos; versão oficial no [Google Docs](https://docs.google.com/document/d/1oVK7AhjF8N6JUp6qeUy8XOYfglWvrWK11OmjJYEYRDw/edit?tab=t.0)):
  - Teste funcional: [`docs/teste-funcional-venda.md`](docs/teste-funcional-venda.md)
  - Teste estrutural (todas-arestas) e de mutação: [`docs/teste-estrutural-mutacao-venda.md`](docs/teste-estrutural-mutacao-venda.md)
  - Testes de integração: [`docs/teste-integracao-venda.md`](docs/teste-integracao-venda.md)
  - Testes de sistema e requisito não funcional: [`docs/teste-sistema-venda.md`](docs/teste-sistema-venda.md)
  - Inspeção com o SonarQube e correções: [`docs/inspecao-sonar-venda.md`](docs/inspecao-sonar-venda.md)
  - Medidas ISO/IEC 25010: [`docs/medidas-iso25010-venda.md`](docs/medidas-iso25010-venda.md)
- Resultados: 100% de arestas e de mutação (60/60); Sonar de 11 problemas para 0; 19 defeitos registrados, nenhum corrigido por decisão
- Defeitos encontrados (19, agrupados em 10 Issues) e 1 problema de usabilidade: Issues [#7](../../issues/7), [#8](../../issues/8), [#14](../../issues/14), [#15](../../issues/15), [#16](../../issues/16), [#17](../../issues/17), [#18](../../issues/18), [#19](../../issues/19), [#20](../../issues/20), [#22](../../issues/22); usabilidade: [#21](../../issues/21)
- Evidências de JaCoCo, PIT, SonarQube, integração e Selenium: [`docs/evidencias/entrega2/`](docs/evidencias/entrega2/)
- Registro das decisões e do uso de IA na Entrega 2: [`docs/ai/decisoes-entrega2-venda.md`](docs/ai/decisoes-entrega2-venda.md)

**Anna Beatriz Chaboudet Chazan — `NotaFiscalItemService`**

- Testes unitários TU-NFI-01..24: [`NotaFiscalItemServiceTest.java`](src/test/java/net/originmobi/pdv/service/notafiscal/NotaFiscalItemServiceTest.java)
- Testes de integração TI-NFI-01..05: [`NotaFiscalItemServiceIT.java`](src/test/java/net/originmobi/pdv/service/notafiscal/NotaFiscalItemServiceIT.java)
- Testes de sistema (Selenium) TS-NFI-01..06, com os requisitos não funcionais de segurança e de desempenho: [`NotaFiscalItemSistemaIT.java`](src/test/java/net/originmobi/pdv/service/notafiscal/NotaFiscalItemSistemaIT.java)
- Evidências de JaCoCo, PIT e SonarQube, antes e depois dos testes novos: [`docs/evidencias/entrega2/`](docs/evidencias/entrega2/)
- Medidas ISO/IEC 25010 (rascunho; versão oficial no [Google Docs](https://docs.google.com/document/d/1oVK7AhjF8N6JUp6qeUy8XOYfglWvrWK11OmjJYEYRDw/edit?tab=t.0)): [`docs/medidas-iso25010-nota-fiscal-item.md`](docs/medidas-iso25010-nota-fiscal-item.md)
- Resultados: 100% de arestas e 89% de mutação (33/37, os 4 restantes equivalentes); Sonar de 5 problemas para 0; 2 defeitos na classe, nenhum corrigido
- Casos de teste manual CT-NFI-01/02: [`docs/testes-manuais/CT-NFI-01.md`](docs/testes-manuais/CT-NFI-01.md)
- CT-NFI-01 executado no TestLink (Passou): [`docs/testes-manuais/evidencias/`](docs/testes-manuais/evidencias/)
- Defeitos encontrados: Issues [#1](../../issues/1), [#2](../../issues/2), [#35](../../issues/35)

**João Portela — `RecebimentoService`**

- **Relatório da Entrega 2** (projeto funcional, JaCoCo, PIT, Sonar, integração, Selenium e ISO 25010): [`docs/entrega2-recebimento.md`](docs/entrega2-recebimento.md)
- Testes unitários TU-REC-01..43: [`RecebimentoServiceTest.java`](src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java)
- Testes de integração TI-REC-01..07: [`RecebimentoServiceIT.java`](src/test/java/net/originmobi/pdv/service/RecebimentoServiceIT.java)
- Testes de sistema (Selenium) TS-REC-01..07: [`RecebimentoSistemaIT.java`](src/test/java/net/originmobi/pdv/service/RecebimentoSistemaIT.java)
- Evidências de JaCoCo, PIT e SonarQube, antes e depois: [`docs/evidencias/entrega2/recebimento/`](docs/evidencias/entrega2/recebimento/)
- Casos de teste manual CT-REC-01..03 — **executados**, com resultados obtidos: [`docs/testes-manuais/CT-REC-01.md`](docs/testes-manuais/CT-REC-01.md)
- Defeitos encontrados (D1 a D9; D1, D2, D3, D6, D7 e D8 corrigidos na Entrega 2): [`docs/bugs/defeitos-recebimento.md`](docs/bugs/defeitos-recebimento.md) — Issues [#4](../../issues/4), [#5](../../issues/5), [#6](../../issues/6), [#10](../../issues/10), [#11](../../issues/11), [#12](../../issues/12), [#24](../../issues/24), [#25](../../issues/25), [#26](../../issues/26)
- Soluções iniciais geradas com IA, preservadas: [`v1`](docs/ai/snapshots/RecebimentoServiceTest.v1-ia.java) (Entrega 1) e [`v2`](docs/ai/snapshots/RecebimentoServiceTest.v2-ia.java) (Entrega 2)
- Revisão das soluções geradas com IA: [`docs/ai/revisao-recebimento-service.md`](docs/ai/revisao-recebimento-service.md)

**João Pedro G. Valadares — `CaixaService`**

- Testes unitários e funcionais (34 testes, dependências isoladas com Mockito e `Clock` fixo; TU-CX-01..11, TF-CX-01..30, TE-CX-01): [`CaixaServiceTest.java`](src/test/java/net/originmobi/pdv/service/CaixaServiceTest.java)
- Testes de integração TI-CX-01..10, com o MySQL do `docker-compose`: [`CaixaServiceIT.java`](src/test/java/net/originmobi/pdv/service/CaixaServiceIT.java)
- Testes de sistema (Selenium) TS-CX-01..08, com o requisito não funcional de segurança: [`CaixaSistemaIT.java`](src/test/java/net/originmobi/pdv/service/CaixaSistemaIT.java)
- Casos manuais CT-CX-01/02, automatizados em TS-CX-01 e TS-CX-04: [`docs/testes-manuais/CT-CAIXA-01.md`](docs/testes-manuais/CT-CAIXA-01.md)
- Correção dos apontamentos do Sonar, sem mudar o comportamento: [`CaixaService.java`](src/main/java/net/originmobi/pdv/service/CaixaService.java) e [`CaixaException.java`](src/main/java/net/originmobi/pdv/service/CaixaException.java)
- Resultados: 100% de arestas (28/28) e de mutação (49/49); Sonar de 21 apontamentos para 1 (informativo, justificado); 7 defeitos registrados (D-CX-01..07), nenhum corrigido por decisão
- Defeitos encontrados (7, agrupados em 5 Issues): senha na URL (D-CX-07) [#29](../../issues/29); filtro por data sem resultado (D-CX-05) [#30](../../issues/30); data dd/mm/aaaa com erro 500 (D-CX-04) [#31](../../issues/31); erro técnico com dado ausente ou caixa inexistente (D-CX-01..03) [#32](../../issues/32); `listaBancos` com erro de SQL (D-CX-06) [#33](../../issues/33)
- Relatório da Entrega 2 (projeto funcional, estrutural, mutação, integração, sistema, Sonar, ISO/IEC 25010, uso de IA e prints): [Google Docs do grupo](https://docs.google.com/document/d/1oVK7AhjF8N6JUp6qeUy8XOYfglWvrWK11OmjJYEYRDw/edit?tab=t.0), seção "RELATÓRIO DA ENTREGA 2", parte "João Pedro G. Valadares — CaixaService"
- Solução inicial gerada com IA, preservada: [`docs/ai/snapshots/CaixaServiceTest.v1-ia.java`](docs/ai/snapshots/CaixaServiceTest.v1-ia.java) e [`docs/ai/snapshots/CaixaServiceIT.v1-ia.java`](docs/ai/snapshots/CaixaServiceIT.v1-ia.java)
- Revisão da solução gerada com IA (versão inicial, final e alterações): [`docs/ai/revisao-caixa-service.md`](docs/ai/revisao-caixa-service.md)

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

