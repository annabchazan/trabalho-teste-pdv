package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Testes de integração de RecebimentoService (TI-REC). Usam o contexto Spring e
 * o MySQL do docker-compose (pdv-db), com o esquema criado pelo Flyway: o
 * serviço, os serviços que ele chama, os repositórios JPA, o @Transactional e o
 * gatilho do banco que atualiza o saldo do caixa rodam de verdade.
 *
 * A massa de dados usa códigos 92xx e é apagada antes e depois de cada caso. O
 * cliente tem código 9201, acima de 127, então todos os casos passam pela
 * comparação de códigos do defeito D1. As parcelas são inseridas com
 * data_alteracao preenchida para não esbarrar no defeito D4 (Issue #10), que é
 * do esquema do banco e não desta classe.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RecebimentoServiceIT {

	private static final long CLIENTE = 9201L;
	private static final long VENDA = 9201L;
	private static final long RECEBER = 9201L;
	private static final long PARCELA_100 = 9201L;
	private static final long PARCELA_50 = 9202L;
	private static final long CAIXA = 9201L;
	private static final long BANCO = 9202L;
	private static final long MAQUINA = 9201L;
	private static final long TITULO_DINHEIRO = 9201L;
	private static final long TITULO_CREDITO = 9202L;
	private static final String[] AS_DUAS_PARCELAS = { String.valueOf(PARCELA_100), String.valueOf(PARCELA_50) };
	private static final double DELTA = 0.001;

	@Autowired
	private RecebimentoService service;

	@Autowired
	private ParcelaService parcelaService;

	@Autowired
	private JdbcTemplate jdbc;

	@Before
	public void setUp() {
		entraComo("gerente");
		limpa();
		criaMassaDeDados();
	}

	@After
	public void tearDown() {
		limpa();
		SecurityContextHolder.clearContext();
	}

	// TI-REC-01
	@Test
	public void receberEmDinheiro_deveQuitarParcelasLancarNoCaixaEFecharORecebimento() {
		long recebimento = abre(AS_DUAS_PARCELAS);

		assertEquals(150.0, valor("select valor_total from recebimento where codigo = ?", recebimento), DELTA);
		assertEquals(2, conta("select count(*) from recebimento_parcelas where recebimento_cod = ?", recebimento));

		assertEquals("Recebimento realizado com sucesso",
				service.receber(recebimento, 150.0, 0.0, 0.0, TITULO_DINHEIRO));

		Map<String, Object> parcela100 = parcela(PARCELA_100);
		assertEquals(1, ((Number) parcela100.get("quitado")).intValue());
		assertEquals(0.0, ((Number) parcela100.get("valor_restante")).doubleValue(), DELTA);
		assertEquals(100.0, ((Number) parcela100.get("valor_recebido")).doubleValue(), DELTA);
		assertNotNull(parcela100.get("data_pagamento"));
		assertEquals(1, ((Number) parcela(PARCELA_50).get("quitado")).intValue());

		Map<String, Object> lancamento = jdbc.queryForMap(
				"select valor, tipo, estilo, recebimento_codigo, usuario_codigo from caixa_lancamento where caixa_codigo = ?",
				CAIXA);
		assertEquals(150.0, ((Number) lancamento.get("valor")).doubleValue(), DELTA);
		assertEquals("RECEBIMENTO", lancamento.get("tipo"));
		assertEquals("ENTRADA", lancamento.get("estilo"));
		assertEquals(recebimento, ((Number) lancamento.get("recebimento_codigo")).longValue());
		assertEquals(1L, ((Number) lancamento.get("usuario_codigo")).longValue());

		// o gatilho do banco soma o lançamento ao saldo do caixa
		assertEquals(150.0, valor("select valor_total from caixa where codigo = ?", CAIXA), DELTA);

		Map<String, Object> fechado = jdbc.queryForMap(
				"select valor_recebido, titulo_codigo, data_processamento from recebimento where codigo = ?", recebimento);
		assertEquals(150.0, ((Number) fechado.get("valor_recebido")).doubleValue(), DELTA);
		assertEquals(TITULO_DINHEIRO, ((Number) fechado.get("titulo_codigo")).longValue());
		assertNotNull(fechado.get("data_processamento"));
	}

	// TI-REC-02
	@Test
	public void receberParcial_deveRatearNaOrdemEDeixarOSaldoNaSegundaParcela() {
		long recebimento = abre(AS_DUAS_PARCELAS);

		service.receber(recebimento, 120.0, 0.0, 0.0, TITULO_DINHEIRO);

		assertEquals(1, ((Number) parcela(PARCELA_100).get("quitado")).intValue());
		Map<String, Object> parcela50 = parcela(PARCELA_50);
		assertEquals(0, ((Number) parcela50.get("quitado")).intValue());
		assertEquals(20.0, ((Number) parcela50.get("valor_recebido")).doubleValue(), DELTA);
		assertEquals(30.0, ((Number) parcela50.get("valor_restante")).doubleValue(), DELTA);
		assertEquals(120.0, valor("select valor_total from caixa where codigo = ?", CAIXA), DELTA);
	}

	// TI-REC-03
	@Test
	public void receberNoCartaoDeCredito_deveGravarLancamentoDeCartaoENaoMovimentarOCaixa() {
		long recebimento = abre(AS_DUAS_PARCELAS);

		service.receber(recebimento, 150.0, 0.0, 0.0, TITULO_CREDITO);

		Map<String, Object> cartao = jdbc.queryForMap(
				"select vl_parcela, taxa, vl_liq_parcela, tipo, situacao from cartao_lancamento where maquina_cartao_codigo = ?",
				MAQUINA);
		assertEquals(150.0, ((Number) cartao.get("vl_parcela")).doubleValue(), DELTA);
		assertEquals(3.0, ((Number) cartao.get("taxa")).doubleValue(), DELTA);
		assertEquals(145.5, ((Number) cartao.get("vl_liq_parcela")).doubleValue(), DELTA);
		assertEquals("CREDITO", cartao.get("tipo"));
		assertEquals("APROCESSAR", cartao.get("situacao"));

		assertEquals(0, conta("select count(*) from caixa_lancamento where caixa_codigo = ?", CAIXA));
		assertEquals(1, ((Number) parcela(PARCELA_100).get("quitado")).intValue());
	}

	// TI-REC-04
	@Test
	public void remover_deveApagarRecebimentoEmAberto_eRecusarRecebimentoJaProcessado() {
		long emAberto = abre(new String[] { String.valueOf(PARCELA_100) });

		assertEquals("removido com sucesso", service.remover(emAberto));
		assertEquals(0, conta("select count(*) from recebimento where codigo = ?", emAberto));
		assertEquals(0, conta("select count(*) from recebimento_parcelas where recebimento_cod = ?", emAberto));

		long processado = abre(new String[] { String.valueOf(PARCELA_50) });
		service.receber(processado, 50.0, 0.0, 0.0, TITULO_DINHEIRO);

		try {
			service.remover(processado);
			fail("Deveria ter recusado a remoção");
		} catch (RuntimeException e) {
			assertEquals("Esse recebimento não pode ser removido, pois ele já esta processado", e.getMessage());
		}
		assertEquals(1, conta("select count(*) from recebimento where codigo = ?", processado));
	}

	// TI-REC-05
	@Test
	public void falhaNoLancamentoDeCaixa_deveDesfazerAQuitacaoDasParcelas() {
		long recebimento = abre(AS_DUAS_PARCELAS);

		// o usuário logado não existe no banco: o lançamento de caixa falha pelo
		// usuario_codigo obrigatório, depois que as duas parcelas já foram quitadas
		entraComo("usuario-inexistente-it");

		try {
			service.receber(recebimento, 150.0, 0.0, 0.0, TITULO_DINHEIRO);
			fail("Deveria ter falhado no lançamento de caixa");
		} catch (RuntimeException e) {
			assertEquals("Ocorreu um erro ao realizar o recebimento, chame o suporte", e.getMessage());
		}

		// o @Transactional de receber() desfaz tudo
		assertEquals(0, ((Number) parcela(PARCELA_100).get("quitado")).intValue());
		assertEquals(100.0, ((Number) parcela(PARCELA_100).get("valor_restante")).doubleValue(), DELTA);
		assertEquals(0, ((Number) parcela(PARCELA_50).get("quitado")).intValue());
		assertEquals(0, conta("select count(*) from caixa_lancamento where caixa_codigo = ?", CAIXA));
		assertNull(jdbc.queryForObject("select data_processamento from recebimento where codigo = ?", Object.class,
				recebimento));
	}

	// TI-REC-06 - defeito D8 no banco real
	@Test
	public void parcelaPagaPelaOutraTela_naoPodeGerarLancamentoMaiorQueOQueFaltava() {
		long recebimento = abre(AS_DUAS_PARCELAS);

		// depois de aberto o recebimento, a parcela de R$ 50,00 é paga pela tela de
		// contas a receber (ReceberController -> ParcelaService.receber)
		parcelaService.receber(PARCELA_50, 50.0, 0.0, 0.0);

		try {
			service.receber(recebimento, 150.0, 0.0, 0.0, TITULO_DINHEIRO);
			fail("Deveria ter recusado o valor");
		} catch (RuntimeException e) {
			assertEquals("Valor de recebimento é superior aos títulos", e.getMessage());
		}

		assertEquals(0, ((Number) parcela(PARCELA_100).get("quitado")).intValue());
		assertEquals(0.0, valor("select coalesce(valor_total, 0) from caixa where codigo = ?", CAIXA), DELTA);

		// o que ainda falta (R$ 100,00) continua podendo ser recebido
		assertEquals("Recebimento realizado com sucesso",
				service.receber(recebimento, 100.0, 0.0, 0.0, TITULO_DINHEIRO));
		assertEquals(100.0, valor("select valor_total from caixa where codigo = ?", CAIXA), DELTA);
	}

	// TI-REC-07 - defeito D7 no banco real
	@Test
	public void receberEmDinheiroComCaixaFechado_deveAvisarSemQuitarParcelas() {
		long recebimento = abre(AS_DUAS_PARCELAS);
		jdbc.update("update caixa set data_fechamento = now() where codigo = ?", CAIXA);

		try {
			service.receber(recebimento, 150.0, 0.0, 0.0, TITULO_DINHEIRO);
			fail("Deveria ter avisado que não há caixa aberto");
		} catch (RuntimeException e) {
			assertEquals("Nenhum caixa aberto, abra o caixa para receber em dinheiro", e.getMessage());
		}

		assertEquals(0, ((Number) parcela(PARCELA_100).get("quitado")).intValue());
		assertEquals(0, ((Number) parcela(PARCELA_50).get("quitado")).intValue());
	}

	// ---------------------------------------------------------------
	// apoio
	// ---------------------------------------------------------------

	private long abre(String[] codigosParcelas) {
		return Long.parseLong(service.abrirRecebimento(Long.valueOf(CLIENTE), codigosParcelas));
	}

	private static void entraComo(String usuario) {
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(usuario, "123"));
	}

	private Map<String, Object> parcela(long codigo) {
		return jdbc.queryForMap(
				"select quitado, valor_restante, valor_recebido, data_pagamento from parcela where codigo = ?", codigo);
	}

	private double valor(String sql, long codigo) {
		return jdbc.queryForObject(sql, Double.class, codigo);
	}

	private int conta(String sql, long codigo) {
		return jdbc.queryForObject(sql, Integer.class, codigo);
	}

	private void criaMassaDeDados() {
		String hoje = "2026-11-21";
		Long sp = jdbc.queryForObject("select codigo from estado where sigla = 'SP'", Long.class);

		jdbc.update("insert into cidade (codigo, nome, codigo_municipio, estado_codigo) values (?, 'Cidade IT REC', '3550308', ?)",
				CLIENTE, sp);
		jdbc.update("insert into endereco (codigo, rua, bairro, numero, cep, referencia, data_cadastro, cidade_codigo) "
				+ "values (?, 'Rua IT', 'Centro', '1', '01000000', '', ?, ?)", CLIENTE, hoje, CLIENTE);
		jdbc.update("insert into pessoa (codigo, nome, cpfcnpj, apelido, data_nascimento, observacao, endereco_codigo, data_cadastro) "
				+ "values (?, 'Cliente IT Recebimento', '00000000191', 'IT', ?, '', ?, ?)", CLIENTE, hoje, CLIENTE, hoje);

		jdbc.update("insert into venda (codigo, observacao, valor_produtos, valor_desconto, valor_acrescimo, valor_total, situacao, "
				+ "pessoa_codigo, usuario_codigo, data_cadastro) values (?, 'Venda IT', 150, 0, 0, 150, 'FECHADA', ?, 1, ?)",
				VENDA, CLIENTE, hoje);
		jdbc.update("insert into receber (codigo, observacao, valor_total, pessoa_codigo, data_cadastro, venda_codigo) "
				+ "values (?, 'Receber IT', 150, ?, ?, ?)", RECEBER, CLIENTE, hoje, VENDA);
		String parcela = "insert into parcela (codigo, valor_total, valor_desconto, valor_acrescimo, valor_recebido, valor_restante, "
				+ "data_cadastro, data_alteracao, data_vencimento, receber_codigo, quitado, sequencia) "
				+ "values (?, ?, 0, 0, 0, ?, ?, now(), ?, ?, 0, ?)";
		jdbc.update(parcela, PARCELA_100, 100.0, 100.0, hoje, "2026-12-21", RECEBER, 1);
		jdbc.update(parcela, PARCELA_50, 50.0, 50.0, hoje, "2027-01-21", RECEBER, 2);

		String caixa = "insert into caixa (codigo, descricao, valor_abertura, valor_total, tipo, usuario_codigo, data_cadastro) "
				+ "values (?, ?, 0, 0, ?, 1, ?)";
		jdbc.update(caixa, CAIXA, "Caixa IT", "CAIXA", hoje);
		jdbc.update(caixa, BANCO, "Banco IT", "BANCO", hoje);
		jdbc.update("insert into maquina_cartao (codigo, descricao, taxa_debito, taxa_credito, dias_debito, dias_credito, "
				+ "taxa_antecipacao, banco_codigo) values (?, 'Maquina IT', 2, 3, 1, 30, 4, ?)", MAQUINA, BANCO);
		jdbc.update("insert into titulo (codigo, descricao, tipo_codigo, maquina_cartao_codigo) values (?, 'Dinheiro IT', 1, null)",
				TITULO_DINHEIRO);
		jdbc.update("insert into titulo (codigo, descricao, tipo_codigo, maquina_cartao_codigo) values (?, 'Credito IT', 3, ?)",
				TITULO_CREDITO, MAQUINA);
	}

	private void limpa() {
		jdbc.update("delete from caixa_lancamento where caixa_codigo in (?, ?) or recebimento_codigo in "
				+ "(select codigo from recebimento where pessoa_codigo = ?)", CAIXA, BANCO, CLIENTE);
		jdbc.update("delete from cartao_lancamento where maquina_cartao_codigo = ?", MAQUINA);
		jdbc.update("delete from recebimento_parcelas where recebimento_cod in (select codigo from recebimento where pessoa_codigo = ?)",
				CLIENTE);
		jdbc.update("delete from recebimento where pessoa_codigo = ?", CLIENTE);
		jdbc.update("delete from parcela where receber_codigo = ?", RECEBER);
		jdbc.update("delete from receber where codigo = ?", RECEBER);
		jdbc.update("delete from venda where codigo = ?", VENDA);
		jdbc.update("delete from titulo where codigo in (?, ?)", TITULO_DINHEIRO, TITULO_CREDITO);
		jdbc.update("delete from maquina_cartao where codigo = ?", MAQUINA);
		jdbc.update("delete from caixa where codigo in (?, ?)", CAIXA, BANCO);
		jdbc.update("delete from pessoa where codigo = ?", CLIENTE);
		jdbc.update("delete from endereco where codigo = ?", CLIENTE);
		jdbc.update("delete from cidade where codigo = ?", CLIENTE);
	}
}
