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

## Artefatos da Entrega 1

- **Plano de Teste:** [docs/plano-teste.md](docs/plano-teste.md) (versão final colaborativa: [Google Docs](https://docs.google.com/document/d/1oVK7AhjF8N6JUp6qeUy8XOYfglWvrWK11OmjJYEYRDw/edit?tab=t.0))
- **Registro de uso de IA:** [docs/ai/AI-LOG.md](docs/ai/AI-LOG.md)
- **Código-fonte original:** `src/main/java/net/originmobi/pdv/`
- **Testes unitários:** `src/test/java/net/originmobi/pdv/`
- **Casos de teste manual (TestLink):** [link — preencher]
- **Relatório de bugs:** [Issues deste repositório](../../issues)
- **Slides/apresentação:** [preencher diretório indicado pela professora]

### Artefatos por integrante

**Anna Beatriz Chaboudet Chazan — `NotaFiscalItemService`**

- Testes unitários TU-NFI-01..13: [`NotaFiscalItemServiceTest.java`](src/test/java/net/originmobi/pdv/service/notafiscal/NotaFiscalItemServiceTest.java)
- Casos manuais CT-NFI-01/02: documentados no Plano de Teste

**João Portela — `RecebimentoService`**

- Testes unitários TU-REC-01..24: [`RecebimentoServiceTest.java`](src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java)
- Casos de teste manual CT-REC-01..03: [`docs/testes-manuais/CT-REC-01.md`](docs/testes-manuais/CT-REC-01.md)
- Defeitos encontrados (D1, D2, D3): [`docs/bugs/defeitos-recebimento.md`](docs/bugs/defeitos-recebimento.md) — Issues [#4](../../issues/4), [#5](../../issues/5), [#6](../../issues/6)
- Solução inicial gerada com IA, preservada: [`docs/ai/snapshots/RecebimentoServiceTest.v1-ia.java`](docs/ai/snapshots/RecebimentoServiceTest.v1-ia.java)
- Revisão da solução gerada com IA: [`docs/ai/revisao-recebimento-service.md`](docs/ai/revisao-recebimento-service.md)

## Como executar os testes

O projeto tem `source/target 1.8`, incompatível com JDKs recentes. A forma
garantida de rodar a suíte é pelo container Java 8 do `docker-compose.yml`:

```sh
docker compose run --rm --no-deps -v pdv-m2:/root/.m2 pdv-app mvn -B test
```

Relatório de cobertura JaCoCo gerado em `target/site/jacoco/index.html`.

Com um JDK 8 instalado localmente, `./mvnw test` também funciona.

## Responsabilidades por membro

| Membro | Classe sob teste unitário | Funcionalidade testada manualmente |
|--------|---------------------------|-------------------------------------|
| Felipe Martins Bittencourt | `VendaService` | [preencher] |
| Anna Beatriz Chaboudet Chazan | `NotaFiscalItemService` | [preencher] |
| João Portela | `RecebimentoService` | Receber parcelas em aberto de um cliente |
| João Pedro G. Valadares | `CaixaService` | Abertura e fechamento de caixa |

