# Plano de Teste — PDV (Qualidade e Teste)

> Rascunho local. Migrar para Google Docs (todos os membros logados em suas
> contas) e colar o link final na seção correspondente do `README.md`.

## 1. Escopo

- **Sistema sob teste:** [pdv](https://github.com/repo-software-testing-courses/pdv)
  — ERP web (Java 8 / Spring Boot 2.0.2 / Thymeleaf / MySQL / Flyway), fork em
  `annabchazan/trabalho-teste-pdv`.
- **Grupo:** 4 integrantes
- **Módulos/componentes incluídos nesta entrega:**
  - Venda (`VendaService`)
  - Nota Fiscal — item (`NotaFiscalItemService`)
  - Recebimento (`RecebimentoService`)
  - Caixa (`CaixaService`)
- **Fora de escopo:** módulos puramente CRUD sem lógica de negócio relevante
  (ex. cadastro simples de categoria, fornecedor, banco).

## 2. Distribuição por membro

| Membro | Classe (unitário) | Funcionalidade (teste manual) |
|--------|--------------------|--------------------------------|
| [preencher] | `VendaService` | [preencher] |
| Anna Beatriz Chaboudet Chazan | `NotaFiscalItemService` | [preencher] |
| [preencher] | `RecebimentoService` | [preencher] |
| [preencher] | `CaixaService` | [preencher] |

## 3. Artefatos gerados

- Casos de teste unitário: `src/test/java/...` (JUnit)
- Casos de teste manual: TestLink (link: [preencher]) + planilha/documento
  complementar para os demais cenários
- Relatório de bugs: GitHub Issues do repositório do grupo
- Este Plano de Teste (versão final no Google Docs: [preencher link])

## 4. Ferramentas

- **Build/execução:** Maven (`mvnw`), Docker Compose (MySQL)
- **Teste unitário:** JUnit (+ Mockito na Entrega 2, para isolar dependências)
- **Teste manual:** TestLink (ao menos 1 cenário obrigatório)
- **Bug tracking:** GitHub Issues
- **Controle de versão / colaboração:** Git + GitHub

## 5. Critérios e observações

- Nenhuma classe escolhida é CRUD trivial; todas contêm lógica de negócio
  (cálculo, regras de status, condicionais de fluxo).
- Complexidade ciclomática formal (mínimo 10 por classe, exigido para a classe
  de alta complexidade) será medida com ferramenta dedicada na Entrega 2;
  ranking desta entrega usou contagem manual de pontos de decisão como proxy
  (ver `docs/ai/AI-LOG.md`).
