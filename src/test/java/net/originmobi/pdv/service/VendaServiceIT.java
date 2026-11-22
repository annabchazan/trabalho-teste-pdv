package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import net.originmobi.pdv.enumerado.VendaSituacao;
import net.originmobi.pdv.filter.VendaFilter;
import net.originmobi.pdv.model.Venda;
import net.originmobi.pdv.repository.VendaRepository;
import net.originmobi.pdv.singleton.Aplicacao;

/**
 * Testes de integração de VendaService (TI-VEN-01 a 08). Usam o contexto
 * Spring, os serviços e repositórios reais e o MySQL do docker-compose
 * (pdv-db), com o esquema e os triggers criados pelo Flyway. Projeto e
 * resultados em docs/teste-integracao-venda.md.
 *
 * Massa de dados: produto, título, formas de pagamento e caixa com código
 * 9001/9002; vendas com a observação "Venda IT". Tudo é apagado antes e
 * depois de cada caso. O caixa do tipo CAIXA que já estiver aberto no banco
 * (ex.: "Caixa01" dos testes manuais) é marcado como fechado durante o caso
 * e restaurado no fim, sem receber lançamentos (decisão D-20).
 *
 * Não há @Transactional na classe de teste: a transação de fechaVenda é a
 * real, para que os casos TI-VEN-06 e 07 verifiquem se ela é desfeita.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class VendaServiceIT {

	private static final String OBS = "Venda IT";
	private static final long COD = 9001L;
	private static final long COD_PRODUTO = 9001L;
	private static final long COD_TITULO_DIN = 9001L;
	private static final long COD_AVISTA = 9001L;
	private static final long COD_APRAZO = 9002L;
	private static final long COD_CAIXA = 9001L;
	private static final long COD_USUARIO_GERENTE = 1L;
	private static final long COD_CLIENTE = 1L;
	private static final double VALOR_PRODUTO = 10.0;
	private static final double DELTA = 0.001;

	@Autowired
	private VendaService service;

	@Autowired
	private VendaRepository vendaRepository;

	@Autowired
	private JdbcTemplate jdbc;

	private final List<Map<String, Object>> caixasOriginaisAbertos = new ArrayList<>();

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null);
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("gerente", null));

		limpa();
		fechaCaixasOriginais();
		criaMassaDeDados();
	}

	@After
	public void tearDown() {
		try {
			limpa();
		} finally {
			restauraCaixasOriginais();
			SecurityContextHolder.clearContext();
			ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null);
		}
	}

	// TI-VEN-01: abrir venda (RF-01)

	@Test
	public void abreVenda_devePersistirVendaAbertaDoUsuarioLogado() {
		Venda venda = new Venda();
		venda.setObservacao(OBS);

		Long codigo = service.abreVenda(venda);

		assertNotNull(codigo);
		Map<String, Object> gravada = venda(codigo);
		assertEquals("ABERTA", gravada.get("situacao"));
		assertEquals(COD_USUARIO_GERENTE, ((Number) gravada.get("usuario_codigo")).longValue());
		assertEquals(0.0, ((Number) gravada.get("valor_produtos")).doubleValue(), DELTA);
		assertNull(gravada.get("data_finalizado"));
	}

	// TI-VEN-02: incluir produto em venda aberta e fechada (RF-02)

	@Test
	public void addProduto_deveGravarItemEAtualizarValorPeloTrigger_quandoVendaAberta() {
		long venda = novaVenda("ABERTA", null);

		assertEquals("ok", service.addProduto(venda, COD_PRODUTO, 0.0));

		assertEquals(1, itensDaVenda(venda));
		// o trigger tr_atualizaTotalProdutosVenda soma o valor de venda do produto
		assertEquals(VALOR_PRODUTO, valorProdutos(venda), DELTA);
	}

	@Test
	public void addProduto_deveRecusarSemGravar_quandoVendaFechada() {
		long venda = novaVendaFechada(50.0);

		assertEquals("Venda fechada", service.addProduto(venda, COD_PRODUTO, 0.0));

		assertEquals(0, itensDaVenda(venda));
	}

	// TI-VEN-03: busca e quantidade em aberto (RF-03)

	@Test
	public void busca_eQtdAbertos_devemRefletirAsVendasDoBanco() {
		int abertasAntes = service.qtdAbertos();
		long aberta1 = novaVenda("ABERTA", null);
		long aberta2 = novaVenda("ABERTA", null);
		long fechada = novaVendaFechada(50.0);

		assertEquals(abertasAntes + 2, service.qtdAbertos());

		List<Long> abertas = codigos(service.busca(new VendaFilter(), "ABERTA", PageRequest.of(0, 1000)));
		assertTrue(abertas.contains(aberta1));
		assertTrue(abertas.contains(aberta2));
		assertFalse(abertas.contains(fechada));

		List<Long> fechadas = codigos(service.busca(new VendaFilter(), "FECHADA", PageRequest.of(0, 1000)));
		assertTrue(fechadas.contains(fechada));
		assertFalse(fechadas.contains(aberta1));

		VendaFilter porCodigo = new VendaFilter();
		porCodigo.setCodigo(aberta2);
		List<Long> encontrada = codigos(service.busca(porCodigo, "FECHADA", PageRequest.of(0, 1000)));
		assertEquals(1, encontrada.size());
		assertEquals(Long.valueOf(aberta2), encontrada.get(0));
	}

	// TI-VEN-04: fechar à vista em dinheiro (RF-04, regra R1)

	@Test
	public void fechaVenda_deveFecharLancarNoCaixaEBaixarEstoque_quandoAVistaEmDinheiro() {
		abreCaixaDeTeste();
		defineEstoque(5);
		long venda = novaVenda("ABERTA", null);
		incluiItem(venda);

		String resposta = service.fechaVenda(venda, COD_AVISTA, VALOR_PRODUTO, 0.0, 0.0,
				new String[] { "10.0" }, new String[] { String.valueOf(COD_TITULO_DIN) });

		assertEquals("Venda finalizada com sucesso", resposta);

		Map<String, Object> fechada = venda(venda);
		assertEquals("FECHADA", fechada.get("situacao"));
		assertEquals(VALOR_PRODUTO, ((Number) fechada.get("valor_total")).doubleValue(), DELTA);
		assertNotNull(fechada.get("data_finalizado"));
		assertEquals(Long.valueOf(COD_AVISTA), jdbc.queryForObject(
				"select pag_tipo_codigo from pagamento_tipo_venda where ven_codigo = ?", Long.class, venda));

		List<Map<String, Object>> receber = receberDaVenda(venda);
		assertEquals(1, receber.size());
		assertEquals(VALOR_PRODUTO, ((Number) receber.get(0).get("valor_total")).doubleValue(), DELTA);

		Map<String, Object> lancamento = jdbc.queryForMap(
				"select valor, tipo, estilo from caixa_lancamento where caixa_codigo = ?", COD_CAIXA);
		assertEquals(VALOR_PRODUTO, ((Number) lancamento.get("valor")).doubleValue(), DELTA);
		assertEquals("RECEBIMENTO", lancamento.get("tipo"));
		assertEquals("ENTRADA", lancamento.get("estilo"));
		// o trigger tr_atualizaValoresCaixa atualiza o total do caixa
		assertEquals(VALOR_PRODUTO, jdbc.queryForObject(
				"select valor_total from caixa where codigo = ?", Double.class, COD_CAIXA), DELTA);

		assertEquals(4, estoque());
		Map<String, Object> movimento = jdbc.queryForMap(
				"select tipo, qtd, origem_operacao from estoque_movimentacao where produto_codigo = ?", COD_PRODUTO);
		assertEquals("SAIDA", movimento.get("tipo"));
		assertEquals(1, ((Number) movimento.get("qtd")).intValue());
		assertEquals("Venda " + venda, movimento.get("origem_operacao"));
	}

	// TI-VEN-05: fechar a prazo com cliente (RF-04, regra R6)

	@Test
	public void fechaVenda_deveGerarParcelaComVencimentoEm30Dias_quandoAPrazo() {
		// Num banco recém-criado, parcela.data_alteracao é obrigatória e sem
		// valor padrão (defeito D4, Issue #10) e o insert da parcela falha.
		// Nesse caso o teste é pulado, em vez de falhar por um defeito de outra
		// classe. Ver a pré-condição em docs/teste-integracao-venda.md.
		Map<String, Object> coluna = jdbc.queryForMap("select column_default, is_nullable "
				+ "from information_schema.columns where table_schema = database() "
				+ "and table_name = 'parcela' and column_name = 'data_alteracao'");
		assumeTrue("Pulado: parcela.data_alteracao sem valor padrão no banco (defeito D4, Issue #10). "
				+ "Aplique: alter table parcela modify data_alteracao timestamp not null default current_timestamp",
				coluna.get("column_default") != null || "YES".equals(coluna.get("is_nullable")));

		defineEstoque(5);
		long venda = novaVenda("ABERTA", COD_CLIENTE);
		incluiItem(venda);

		service.fechaVenda(venda, COD_APRAZO, VALOR_PRODUTO, 0.0, 0.0,
				new String[] { "10.0" }, new String[] { String.valueOf(COD_TITULO_DIN) });

		assertEquals("FECHADA", venda(venda).get("situacao"));

		List<Map<String, Object>> receber = receberDaVenda(venda);
		assertEquals(1, receber.size());
		Map<String, Object> parcela = jdbc.queryForMap(
				"select valor_total, valor_restante, quitado, sequencia, date(data_vencimento) as vencimento "
						+ "from parcela where receber_codigo = ?", receber.get(0).get("codigo"));
		assertEquals(VALOR_PRODUTO, ((Number) parcela.get("valor_total")).doubleValue(), DELTA);
		assertEquals(VALOR_PRODUTO, ((Number) parcela.get("valor_restante")).doubleValue(), DELTA);
		assertEquals(0, ((Number) parcela.get("quitado")).intValue());
		assertEquals(1, ((Number) parcela.get("sequencia")).intValue());
		assertEquals(java.sql.Date.valueOf(LocalDate.now().plusDays(30)), parcela.get("vencimento"));

		assertEquals(0, jdbc.queryForObject("select count(*) from caixa_lancamento where caixa_codigo = ?",
				Integer.class, COD_CAIXA).intValue());
	}

	// TI-VEN-06: fechar sem caixa aberto (regra R2) - transação desfeita?

	@Test
	public void fechaVenda_deveDesfazerTudo_quandoNaoHaCaixaAberto() {
		defineEstoque(5);
		long venda = novaVenda("ABERTA", null);
		incluiItem(venda);

		try {
			service.fechaVenda(venda, COD_AVISTA, VALOR_PRODUTO, 0.0, 0.0,
					new String[] { "10.0" }, new String[] { String.valueOf(COD_TITULO_DIN) });
			fail("deveria recusar o fechamento sem caixa aberto");
		} catch (RuntimeException e) {
			assertEquals("nenhum caixa aberto", e.getMessage());
		}

		// o receber foi gravado antes do erro; com a transação desfeita, não fica no banco
		assertEquals(0, receberDaVenda(venda).size());
		Map<String, Object> gravada = venda(venda);
		assertEquals("ABERTA", gravada.get("situacao"));
		assertNull(gravada.get("data_finalizado"));
		assertEquals(0, jdbc.queryForObject("select count(*) from pagamento_tipo_venda where ven_codigo = ?",
				Integer.class, venda).intValue());
		assertEquals(5, estoque());
	}

	// TI-VEN-07: estoque insuficiente depois do update da venda - transação desfeita?

	@Test
	public void fechaVenda_deveManterVendaAberta_quandoEstoqueInsuficiente() {
		abreCaixaDeTeste();
		defineEstoque(0);
		long venda = novaVenda("ABERTA", null);
		incluiItem(venda);

		try {
			service.fechaVenda(venda, COD_AVISTA, VALOR_PRODUTO, 0.0, 0.0,
					new String[] { "10.0" }, new String[] { String.valueOf(COD_TITULO_DIN) });
			fail("deveria recusar o fechamento sem estoque");
		} catch (RuntimeException e) {
			assertEquals("O produto de código " + COD_PRODUTO + " não tem estoque suficiente, verifique",
					e.getMessage());
		}

		Map<String, Object> gravada = venda(venda);
		assertEquals("ABERTA", gravada.get("situacao"));
		assertNull(gravada.get("data_finalizado"));
		assertNull(gravada.get("valor_total"));
		assertEquals(0, receberDaVenda(venda).size());
		assertEquals(0, jdbc.queryForObject("select count(*) from caixa_lancamento where caixa_codigo = ?",
				Integer.class, COD_CAIXA).intValue());
		assertEquals(0.0, jdbc.queryForObject("select valor_total from caixa where codigo = ?", Double.class,
				COD_CAIXA), DELTA);
		assertEquals(0, estoque());
	}

	// TI-VEN-08: venda já fechada não é fechada de novo (regra E1 I1)

	@Test
	public void fechaVenda_naoDeveAlterarVendaJaFechada() {
		abreCaixaDeTeste();
		long venda = novaVendaFechada(50.0);
		Map<String, Object> antes = venda(venda);

		try {
			service.fechaVenda(venda, COD_AVISTA, VALOR_PRODUTO, 0.0, 0.0,
					new String[] { "10.0" }, new String[] { String.valueOf(COD_TITULO_DIN) });
			fail("deveria recusar venda já fechada");
		} catch (RuntimeException e) {
			assertEquals("venda fechada", e.getMessage());
		}

		// a consulta de fechamento do repositório também só altera vendas sem data_finalizado
		vendaRepository.fechaVenda(venda, VendaSituacao.FECHADA, 999.0, 0.0, 0.0,
				new Timestamp(System.currentTimeMillis()), null);

		Map<String, Object> depois = venda(venda);
		assertEquals(antes.get("valor_total"), depois.get("valor_total"));
		assertEquals(antes.get("data_finalizado"), depois.get("data_finalizado"));
		assertEquals(0, receberDaVenda(venda).size());
	}

	// apoio

	private long novaVenda(String situacao, Long cliente) {
		Map<String, Object> valores = new HashMap<>();
		valores.put("observacao", OBS);
		valores.put("valor_produtos", 0.0);
		valores.put("situacao", situacao);
		valores.put("pessoa_codigo", cliente);
		valores.put("usuario_codigo", COD_USUARIO_GERENTE);
		valores.put("data_cadastro", new Timestamp(System.currentTimeMillis()));
		return new SimpleJdbcInsert(jdbc).withTableName("venda")
				.usingColumns("observacao", "valor_produtos", "situacao", "pessoa_codigo", "usuario_codigo",
						"data_cadastro")
				.usingGeneratedKeyColumns("codigo").executeAndReturnKey(valores).longValue();
	}

	private long novaVendaFechada(double valorTotal) {
		long venda = novaVenda("FECHADA", null);
		jdbc.update("update venda set valor_produtos = ?, valor_total = ?, data_finalizado = '2026-11-01 10:00:00' "
				+ "where codigo = ?", valorTotal, valorTotal, venda);
		return venda;
	}

	private void incluiItem(long venda) {
		jdbc.update("insert into venda_produtos (produto_codigo, venda_codigo, valor_balanca) values (?, ?, 0)",
				COD_PRODUTO, venda);
	}

	private void abreCaixaDeTeste() {
		jdbc.update("insert into caixa (codigo, descricao, valor_abertura, valor_total, valor_entrada, valor_saida, "
				+ "tipo, usuario_codigo, data_cadastro) values (?, 'Caixa IT', 0, 0, 0, 0, 'CAIXA', ?, curdate())",
				COD_CAIXA, COD_USUARIO_GERENTE);
	}

	private void defineEstoque(int qtd) {
		jdbc.update("update produto_estoque set qtd = ? where produto_codigo = ?", qtd, COD_PRODUTO);
	}

	private int estoque() {
		return jdbc.queryForObject("select qtd from produto_estoque where produto_codigo = ?", Integer.class,
				COD_PRODUTO);
	}

	private Map<String, Object> venda(long codigo) {
		return jdbc.queryForMap("select * from venda where codigo = ?", codigo);
	}

	private double valorProdutos(long venda) {
		return jdbc.queryForObject("select valor_produtos from venda where codigo = ?", Double.class, venda);
	}

	private int itensDaVenda(long venda) {
		return jdbc.queryForObject("select count(*) from venda_produtos where venda_codigo = ?", Integer.class,
				venda);
	}

	private List<Map<String, Object>> receberDaVenda(long venda) {
		return jdbc.queryForList("select * from receber where venda_codigo = ?", venda);
	}

	private List<Long> codigos(Page<Venda> pagina) {
		return pagina.getContent().stream().map(Venda::getCodigo).collect(Collectors.toList());
	}

	// massa de dados no banco

	private void criaMassaDeDados() {
		Long fornecedor = jdbc.queryForObject("select min(codigo) from fornecedor", Long.class);

		// o trigger insere_estoque_inicial cria o saldo do produto com 0
		jdbc.update("insert into produto (codigo, descricao, valor_venda, balanca, ativo, fornecedor_codigo, "
				+ "grupo_codigo, unidade, subtributaria, categoria_codigo, vendavel, controla_estoque, data_cadastro) "
				+ "values (?, 'Produto IT', ?, 0, 'ATIVO', ?, 1, 'UN', 0, 1, 'SIM', 'SIM', curdate())",
				COD_PRODUTO, VALOR_PRODUTO, fornecedor);

		// tipo 1 = Dinheiro (DIN), carregado pelo Flyway
		jdbc.update("insert into titulo (codigo, descricao, tipo_codigo) values (?, 'Dinheiro IT', 1)",
				COD_TITULO_DIN);

		String pagamento = "insert into pagamento_tipo (codigo, descricao, forma_pagamento, qtd_parcelas, "
				+ "data_cadastro) values (?, ?, ?, 1, curdate())";
		jdbc.update(pagamento, COD_AVISTA, "A vista IT", "00");
		jdbc.update(pagamento, COD_APRAZO, "A prazo IT", "30");
	}

	private void limpa() {
		List<Long> vendas = jdbc.queryForList("select codigo from venda where observacao = ?", Long.class, OBS);
		for (Long venda : vendas) {
			jdbc.update("delete p from parcela p join receber r on r.codigo = p.receber_codigo "
					+ "where r.venda_codigo = ?", venda);
			jdbc.update("delete from receber where venda_codigo = ?", venda);
			jdbc.update("delete from pagamento_tipo_venda where ven_codigo = ?", venda);
			jdbc.update("delete from venda_produtos where venda_codigo = ?", venda);
			jdbc.update("delete from venda where codigo = ?", venda);
		}
		jdbc.update("delete from caixa_lancamento where caixa_codigo = ?", COD_CAIXA);
		jdbc.update("delete from caixa where codigo = ?", COD_CAIXA);
		jdbc.update("delete from estoque_movimentacao where produto_codigo = ?", COD_PRODUTO);
		jdbc.update("delete from produto_estoque where produto_codigo = ?", COD_PRODUTO);
		jdbc.update("delete from produto where codigo = ?", COD_PRODUTO);
		jdbc.update("delete from titulo where codigo = ?", COD_TITULO_DIN);
		jdbc.update("delete from pagamento_tipo where codigo in (?, ?)", COD_AVISTA, COD_APRAZO);
	}

	// caixa já aberto no banco (D-20): fechado durante o caso e restaurado depois

	private void fechaCaixasOriginais() {
		caixasOriginaisAbertos.clear();
		caixasOriginaisAbertos.addAll(jdbc.queryForList("select codigo, data_alteracao from caixa "
				+ "where tipo = 'CAIXA' and data_fechamento is null and codigo <> ?", COD_CAIXA));
		for (Map<String, Object> caixa : caixasOriginaisAbertos)
			jdbc.update("update caixa set data_fechamento = now(), data_alteracao = data_alteracao where codigo = ?",
					caixa.get("codigo"));
	}

	private void restauraCaixasOriginais() {
		for (Map<String, Object> caixa : caixasOriginaisAbertos)
			jdbc.update("update caixa set data_fechamento = null, data_alteracao = ? where codigo = ?",
					caixa.get("data_alteracao"), caixa.get("codigo"));
		caixasOriginaisAbertos.clear();
	}
}
