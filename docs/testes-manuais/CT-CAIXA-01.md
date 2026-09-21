# Casos de teste manual - Abertura e fechamento de caixa

Responsável: **João Pedro G. Valadares**  
Funcionalidade: **Abertura e fechamento de caixa**  
Módulo: `CaixaService` / `CaixaController`

> **Status geral: planejado, aguardando execução manual.** Os campos de
> resultado obtido e status não representam uma execução realizada e devem ser
> preenchidos somente após o teste no sistema.

## Ambiente e pré-condições

| Item | Preparação necessária |
|---|---|
| Sistema | Subir a aplicação com `docker compose up -d` na raiz do projeto. |
| URL | Acessar `http://localhost:8080`. |
| Usuário | Entrar com `gerente` / `123`. |
| Estado inicial | Não existir caixa diário aberto de dia anterior. |
| Registro no TestLink | Cadastrar ao menos um dos cenários abaixo quando a execução manual for realizada. |

## CT-CX-01 - Abrir caixa com valor inicial válido

**Objetivo:** verificar que o sistema abre um caixa diário, registra o valor de
abertura e cria o lançamento inicial quando o valor informado é positivo.

**Status:** ( ) Passou  ( ) Falhou - _preencher durante a execução_

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Acessar o menu **Caixa** e iniciar a abertura de caixa. | - | Formulário de abertura exibido. | |
| 2 | Informar o valor inicial e confirmar a abertura. | R$ 50,00 | Caixa diário criado para o usuário logado. | |
| 3 | Consultar os lançamentos do caixa aberto. | - | Lançamento de entrada identificado como abertura, no valor de R$ 50,00. | |
| 4 | Conferir os dados do caixa. | - | Descrição, data e usuário vinculados ao caixa estão preenchidos. | |

## CT-CX-02 - Fechar caixa com senha válida

**Objetivo:** verificar que o sistema encerra um caixa aberto e registra o valor
total como valor de fechamento após validação da senha do usuário.

**Status:** ( ) Passou  ( ) Falhou - _preencher durante a execução_

| # | Passo | Dado de entrada | Resultado esperado | Resultado obtido |
|---|---|---|---|---|
| 1 | Com o caixa aberto, acessar a opção de fechamento. | Caixa aberto do cenário CT-CX-01. | Tela ou diálogo de confirmação exibido. | |
| 2 | Informar a senha e confirmar. | `123` | Sistema aceita a senha do usuário gerente. | |
| 3 | Conferir a mensagem apresentada. | - | Mensagem `Caixa fechado com sucesso`. | |
| 4 | Consultar o caixa encerrado. | - | Data de fechamento preenchida e valor de fechamento igual ao total do caixa. | |

## Registro após a execução

Anotar aqui a data, o navegador utilizado, os resultados obtidos e qualquer
defeito observado. Caso exista defeito, a abertura de Issue será uma decisão
posterior do grupo.
