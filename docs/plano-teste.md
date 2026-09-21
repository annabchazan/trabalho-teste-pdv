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
| João Portela | `RecebimentoService` | Receber parcelas em aberto de um cliente |
| João Pedro G. Valadares | `CaixaService` | Abertura e fechamento de caixa |

## 3. Artefatos gerados

- Casos de teste unitário: `src/test/java/...` (JUnit e Mockito)
- Casos de teste manual: TestLink (link: [preencher]) + planilha/documento
  complementar para os demais cenários
- Relatório de bugs: GitHub Issues do repositório do grupo
- Este Plano de Teste (versão final no Google Docs: [preencher link])

### 3.1 Artefatos de `RecebimentoService` (João Portela)

- Testes unitários TU-REC-01 a TU-REC-24:
  `src/test/java/net/originmobi/pdv/service/RecebimentoServiceTest.java`
- Casos de teste manual CT-REC-01 a CT-REC-03 (projetados):
  `docs/testes-manuais/CT-REC-01.md`
- Defeitos D1, D2 e D3, com causa raiz e correção sugerida:
  `docs/bugs/defeitos-recebimento.md`
- Versão inicial gerada com IA, preservada:
  `docs/ai/snapshots/RecebimentoServiceTest.v1-ia.java`
- Descrição das alterações feitas na revisão da solução gerada com IA:
  `docs/ai/revisao-recebimento-service.md`

## 4. Ferramentas

- **Build/execução:** Maven (`mvnw`), Docker Compose (MySQL)
- **Teste unitário:** JUnit e Mockito (isolamento das dependências)
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

## 6. Cobertura medida (baseline para a Entrega 2)

Medição feita com JaCoCo 0.8.11 (`mvn test` → `target/site/jacoco/`), executando
a suíte dentro do container `eclipse-temurin:8-jdk` do `docker-compose.yml`.

| Classe | Arestas (branch) | Linhas | Complexidade ciclomática |
|---|---|---|---|
| `RecebimentoService` | 32/34 — **94,1%** | 88/91 — 96,7% | 21 |
| `NotaFiscalItemService` | 35/40 — 87,5% | 77/79 — 97,5% | 25 |

Ambas já superam o critério de 80% em todas-arestas exigido na Entrega 2 e têm
complexidade ciclomática acima do mínimo de 10 por classe sob teste.

As 2 arestas não cobertas em `RecebimentoService` foram analisadas e são
inalcançáveis no estado atual do código — uma delas em consequência do próprio
defeito D2. A análise está em `docs/ai/revisao-recebimento-service.md`, seção 4.
