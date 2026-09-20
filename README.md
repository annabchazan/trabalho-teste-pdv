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

- **Plano de Teste:** [docs/plano-teste.md](docs/plano-teste.md) (versão final colaborativa: [link Google Docs — preencher])
- **Registro de uso de IA:** [docs/ai/AI-LOG.md](docs/ai/AI-LOG.md)
- **Código-fonte original:** `src/main/java/net/originmobi/pdv/`
- **Testes unitários:** `src/test/java/net/originmobi/pdv/`
- **Casos de teste manual (TestLink):** [link — preencher]
- **Relatório de bugs:** [Issues deste repositório](../../issues)
- **Slides/apresentação:** [preencher diretório indicado pela professora]

## Responsabilidades por membro

| Membro | Classe sob teste unitário | Funcionalidade testada manualmente |
|--------|---------------------------|-------------------------------------|
| [preencher] | `VendaService` | [preencher] |
| Anna Beatriz Chaboudet Chazan | `NotaFiscalItemService` | [preencher] |
| [preencher] | `RecebimentoService` | [preencher] |
| João Pedro G. Valadares | `CaixaService` | Abertura e fechamento de caixa |

