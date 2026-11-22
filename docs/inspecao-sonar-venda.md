# Inspeção com o SonarQube e correções — `VendaService`

**Responsável:** Felipe Martins Bittencourt
**Entrega 2, Etapa 7.** Decisões registradas em
[`docs/ai/decisoes-entrega2-venda.md`](ai/decisoes-entrega2-venda.md)
(D-27 a D-30).

> Este é o rascunho. A versão oficial vai para o Google Docs do grupo, na
> seção de inspeção de código.

## Resumo

| Métrica (só `VendaService.java`) | Antes | Depois |
|---|---|---|
| Problemas abertos | **11** (todos `java:S6813`) | **0** (os 11 aparecem como "Fixed") |
| Bugs / vulnerabilidades | 0 / 0 | 0 / 0 |
| Dívida técnica | 55 min | 0 min |
| Notas de manutenibilidade, confiabilidade e segurança | A, A, A | A, A, A |
| Linhas / linhas de código | 332 / 225 | 324 / 224 |
| Complexidade ciclomática | 29 | 29 |
| Complexidade cognitiva | 44 | 43 |
| Cobertura (linhas / ramos) | 98,2% (98,4% / 97,4%) | 100% (100% / 100%) |
| Duplicação | 0,0% | 0,0% |

**Ferramenta:** SonarQube Community Build v26.11 (modo MQR), local, em
`http://localhost:9000`. O projeto é `trabalho-teste-pdv`. A análise "antes"
é de 20/11/2026 às 15:31 (UTC) e a "depois", de 20/11/2026 às 22:00 (UTC).

**Evidências** em [`docs/evidencias/entrega2/`](evidencias/entrega2/):

| Evidência | Antes (`baseline/`) | Depois (`depois-sonar/`) |
|---|---|---|
| Lista de problemas do arquivo | `sonar-VendaService-issues-baseline.png` | `sonar-VendaService-issues.png` (filtro "Fixed") |
| Cabeçalho do arquivo (linhas, cobertura, problemas) | `sonar-VendaService-metricas-baseline.png` | `sonar-VendaService-metricas.png` |
| Números lidos da API do Sonar | `sonar-VendaService-baseline.json` | `sonar-VendaService.json` |
| JaCoCo e PIT depois das correções | — | `jacoco-VendaService-metodos.png`, `pit-VendaService.png` |

A tela de complexidade do Sonar não coube inteira na captura. Por isso, os
números exatos foram lidos da API do próprio Sonar e guardados nos arquivos
JSON.

## 1. O que o Sonar apontou

Os 11 problemas são da regra **`java:S6813`**, *"Remove this field injection
and use constructor injection instead"*. Cada um vale 5 minutos de esforço e
tem severidade média. A regra aponta as 11 dependências de `VendaService`
injetadas direto no campo, com `@Autowired`, nas linhas 40 a 70.

Por que isso é um problema:
- com a injeção no campo, a classe pode ser criada sem as dependências, e o
  erro só aparece quando alguém chama o método;
- os campos não podem ser `final`, então podem ser trocados depois de criados;
- a classe fica presa ao Spring: para testar, é preciso usar reflexão ou o
  `@InjectMocks`;
- 11 dependências ficam escondidas. Num construtor, esse número fica visível
  e mostra que a classe acumula responsabilidades demais.

Nesta versão do Sonar, cada problema conta tanto em manutenibilidade quanto
em confiabilidade. Por isso o painel mostra 11 nas duas qualidades. São os
mesmos 11 problemas.

O Sonar não apontou bugs nem vulnerabilidades. Os defeitos funcionais achados
pelos testes, como o D-VEN-01 (desconto e acréscimo trocados), não são
detectados por análise estática. Isso mostra que a inspeção e os testes se
complementam.

## 2. Correções feitas

### 2.1 Injeção pelo construtor (resolve os 11 problemas)

```java
private final VendaRepository vendas;
private final UsuarioService usuarios;
// ... as outras 9 dependências, todas final

@Autowired
public VendaService(VendaRepository vendas, UsuarioService usuarios, /* ... */ ProdutoService produtos) {
    this.vendas = vendas;
    this.usuarios = usuarios;
    // ...
}
```

É o mesmo formato que a Anna usou em `NotaFiscalItemService` (commit
`5aa1420`). Quem usa `VendaService` (`VendaController` e `HomeController`)
não mudou. Os testes unitários também não mudaram: o `@InjectMocks` do
Mockito passou a usar o construtor sozinho.

### 2.2 Remoção de código morto (não apontado pelo Sonar)

As Etapas 3 e 4 tinham mostrado código que roda mas não tem efeito nenhum. Ele
gerava os 6 mutantes equivalentes do PIT:

| Trecho | Por que era morto |
|---|---|
| Mapa `modeloPagar` em `fechaVenda` | Preenchido com as formas de pagamento e nunca lido |
| `qtdVezes` | Calculado em `fechaVenda`, decrementado e devolvido por `avistaDinheiro`, mas nunca usado para nada. `avistaDinheiro` passou a ser `void` |
| Parâmetro `vlprodutos` de `aprazo` | Recebido e nunca usado |
| 3 `System.out.println(e)` nos `catch` | Escreviam a exceção na saída padrão. A mensagem de erro para o usuário continua a mesma |

### 2.3 O que não foi corrigido

Os defeitos funcionais ficaram como estavam, porque corrigi-los muda o
comportamento da classe. Eles estão caracterizados pelos testes:
- **D-VEN-01:** desconto e acréscimo entram trocados em `avistaDinheiro` e
  `aprazo`. A chamada continua passando `desc, acre` para parâmetros
  declarados como `acre, desc`;
- venda de valor zero ou negativo, acréscimo negativo e título à vista de
  outro tipo (ver [`teste-funcional-venda.md`](teste-funcional-venda.md)).

## 3. Como as métricas mudaram

- **Problemas e dívida técnica:** de 11 para 0, e de 55 min para 0.
- **Complexidade ciclomática:** ficou em 29. O laço do `modeloPagar` saiu
  (−1) e o construtor entrou (+1). Em `fechaVenda`, ela caiu de 11 para 10.
- **Complexidade cognitiva:** de 44 para 43, por causa do laço removido.
- **Cobertura:** de 98,2% para 100%. A maior parte dessa subida vem dos testes
  das Etapas 1 e 2, e não das correções. A análise "antes" foi feita na
  Etapa 0, ainda com os testes da Entrega 1.

## 4. Verificação depois das correções

| Verificação | Resultado |
|---|---|
| Suíte unitária completa (`mvn test`, com o banco) | 114 testes, 0 falhas, 7 pulados (os `@Ignore` de defeitos já registrados) |
| `VendaServiceTest` + JaCoCo | 53 de 53; 100% das linhas (130/130) e dos ramos (36/36) |
| PIT | 60 mutantes, 60 mortos (100%). Antes: 66 mutantes, 60 mortos (91%), com 6 equivalentes |
| `VendaServiceIT` | 9 de 9; o Spring cria o serviço pelo construtor, sem dependência circular |
| `VendaSistemaIT` (aplicação reconstruída) | 6 passaram e 1 pulado (TS-VEN-06, D-VEN-01); TS-VEN-07 com no máximo 576 ms |

**PIT, primeira execução:** os 60 mutantes deram `TIMED_OUT`, porque a
máquina estava com 94% de memória ocupada (SonarQube e aplicação no ar). Com o
limite de tempo maior (`-DtimeoutConstant=30000`), todos foram mortos. O
limite não muda o critério: um mutante só conta como morto quando um teste
falha.

## 5. Como reproduzir

```sh
# 1. Testes unitários e cobertura (Java 8, com o banco para o PdvApplicationTests)
docker compose up -d pdv-db
docker run --rm --network trabalho-teste-pdv_default -v "$PWD":/app -w /app -v pdv-m2:/root/.m2 trabalho-teste-pdv-pdv-app mvn -B test

# 2. Análise (Java 21), com um token de análise do projeto
docker run --rm -e SONAR_TOKEN=SEU_TOKEN -v "$PWD":/app -w /app -v pdv-m2:/root/.m2 maven:3.9-eclipse-temurin-21 \
  mvn -B sonar:sonar -Dsonar.host.url=http://host.docker.internal:9000
```

Para ver o resultado, abra `http://localhost:9000`, vá em *Issues* e filtre
pelo arquivo `VendaService.java`.
