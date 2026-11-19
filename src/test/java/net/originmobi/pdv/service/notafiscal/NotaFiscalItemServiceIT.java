package net.originmobi.pdv.service.notafiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.notafiscal.NotaFiscalTipo;
import net.originmobi.pdv.enumerado.produto.ProdutoSubstTributaria;
import net.originmobi.pdv.model.CFOP;
import net.originmobi.pdv.model.Cidade;
import net.originmobi.pdv.model.Cst;
import net.originmobi.pdv.model.CstCsosn;
import net.originmobi.pdv.model.CstIPI;
import net.originmobi.pdv.model.Endereco;
import net.originmobi.pdv.model.Estado;
import net.originmobi.pdv.model.ModBcIcms;
import net.originmobi.pdv.model.NotaFiscal;
import net.originmobi.pdv.model.NotaFiscalItem;
import net.originmobi.pdv.model.NotaFiscalItemImposto;
import net.originmobi.pdv.model.NotaFiscalTotais;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.model.Tributacao;
import net.originmobi.pdv.model.TributacaoRegra;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalItemImpostoRepository;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalItemRepository;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalTotaisRepository;
import net.originmobi.pdv.service.ProdutoService;

/**
 * Testes de integração de NotaFiscalItemService (TI-NFI). TI-NFI-01 integra os
 * serviços entre si com os repositórios mockados; os demais usam o contexto
 * Spring e o MySQL do docker-compose (pdv-db), com o esquema criado pelo
 * Flyway. Os dados de teste usam códigos 9001/9002 e são apagados antes e
 * depois de cada caso.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class NotaFiscalItemServiceIT {

	private static final long COD = 9001L;
	private static final long COD_PRODUTO = 9001L;
	private static final long COD_PRODUTO_SEM_MOD_BC = 9002L;
	private static final long COD_NOTA = 9001L;
	private static final double DELTA = 0.001;

	@Autowired
	private NotaFiscalItemService service;

	@Autowired
	private JdbcTemplate jdbc;

	@Before
	public void setUp() {
		limpa();
		criaMassaDeDados();
	}

	@After
	public void tearDown() {
		limpa();
	}

	// TI-NFI-01: entre serviços (sem banco)

	@Test
	public void insere_deveCalcularImpostoEAtualizarTotais_comServicosReais() {
		NotaFiscalItemRepository itemRepository = mock(NotaFiscalItemRepository.class);
		NotaFiscalItemImpostoRepository impostoRepository = mock(NotaFiscalItemImpostoRepository.class);
		NotaFiscalTotaisRepository totaisRepository = mock(NotaFiscalTotaisRepository.class);
		ProdutoService produtos = mock(ProdutoService.class);
		NotaFiscalService notas = mock(NotaFiscalService.class);

		NotaFiscalItemImpostoService impostos = new NotaFiscalItemImpostoService();
		ReflectionTestUtils.setField(impostos, "impostos", impostoRepository);

		NotaFiscalTotaisServer totais = new NotaFiscalTotaisServer();
		ReflectionTestUtils.setField(totais, "totais", totaisRepository);

		NotaFiscalItemService itemService = new NotaFiscalItemService(itemRepository, impostos, totais, produtos, notas);

		NotaFiscal nota = notaEmMemoria();
		when(produtos.buscaProduto(COD_PRODUTO)).thenReturn(Optional.of(produtoEmMemoria()));
		when(notas.busca(COD_NOTA)).thenReturn(Optional.of(nota));
		when(totaisRepository.calcula(COD_NOTA)).thenReturn("20.0,3.6,0.33,1.52,1.0");

		assertEquals("ok", itemService.insere(COD_PRODUTO, COD_NOTA, 2, NotaFiscalTipo.SAIDA));

		// imposto calculado de verdade a partir do item: total 2 x 10 = 20
		ArgumentCaptor<NotaFiscalItemImposto> imposto = ArgumentCaptor.forClass(NotaFiscalItemImposto.class);
		verify(impostoRepository).save(imposto.capture());
		assertEquals(1, imposto.getValue().getOrig());
		assertEquals(102, imposto.getValue().getCst());
		assertEquals(20.0, imposto.getValue().getV_bc(), DELTA);
		assertEquals(3.6, imposto.getValue().getV_icms(), DELTA);
		assertEquals(0.33, imposto.getValue().getV_pis(), DELTA);
		assertEquals(1.52, imposto.getValue().getV_cofins(), DELTA);
		assertEquals(1.0, imposto.getValue().getV_ipi(), DELTA);

		// o item gravado aponta para esse mesmo imposto
		ArgumentCaptor<NotaFiscalItem> item = ArgumentCaptor.forClass(NotaFiscalItem.class);
		verify(itemRepository).save(item.capture());
		assertSame(imposto.getValue(), item.getValue().getImpostos());

		// totais da nota recalculados a partir da consulta: v_nf = produtos + IPI
		verify(totaisRepository).save(any(NotaFiscalTotais.class));
		assertEquals(20.0, nota.getTotais().getV_prod(), DELTA);
		assertEquals(3.6, nota.getTotais().getV_icms(), DELTA);
		assertEquals(21.0, nota.getTotais().getV_nf(), DELTA);
	}

	// TI-NFI-02 a 05: com banco real

	// TI-NFI-02
	@Test
	public void insere_deveGravarItemImpostoETotaisNoBanco() {
		assertEquals("ok", service.insere(COD_PRODUTO, COD_NOTA, 2, NotaFiscalTipo.SAIDA));

		List<Map<String, Object>> itens = itensDaNota();
		assertEquals(1, itens.size());
		Map<String, Object> item = itens.get(0);
		assertEquals(COD_PRODUTO, ((Number) item.get("cod_prod")).longValue());
		assertEquals(2, ((Number) item.get("qtd")).intValue());
		assertEquals(20.0, ((Number) item.get("vl_total")).doubleValue(), DELTA);
		assertEquals("5102", item.get("cfop"));

		Map<String, Object> imposto = jdbc.queryForMap(
				"select * from nota_fiscal_item_imposto where codigo = ?", item.get("imposto_codigo"));
		assertEquals(3.6, ((Number) imposto.get("v_icms")).doubleValue(), DELTA);
		assertEquals(1.0, ((Number) imposto.get("v_ipi")).doubleValue(), DELTA);

		Map<String, Object> totais = totaisDaNota();
		assertEquals(20.0, ((Number) totais.get("v_prod")).doubleValue(), DELTA);
		assertEquals(3.6, ((Number) totais.get("v_icms")).doubleValue(), DELTA);
		assertEquals(21.0, ((Number) totais.get("v_nf")).doubleValue(), DELTA);
	}

	// TI-NFI-03
	@Test
	public void insere_deveSomarQuantidadeNoMesmoItem_quandoProdutoInseridoDuasVezes() {
		service.insere(COD_PRODUTO, COD_NOTA, 2, NotaFiscalTipo.SAIDA);
		service.insere(COD_PRODUTO, COD_NOTA, 3, NotaFiscalTipo.SAIDA);

		List<Map<String, Object>> itens = itensDaNota();
		assertEquals(1, itens.size());
		assertEquals(5, ((Number) itens.get(0).get("qtd")).intValue());
		assertEquals(50.0, ((Number) itens.get(0).get("vl_total")).doubleValue(), DELTA);

		// o imposto foi atualizado, não duplicado
		assertEquals(Integer.valueOf(1), jdbc.queryForObject(
				"select count(*) from nota_fiscal_item_imposto where codigo = ?", Integer.class,
				itens.get(0).get("imposto_codigo")));
		assertEquals(52.5, ((Number) totaisDaNota().get("v_nf")).doubleValue(), DELTA);
	}

	// TI-NFI-04
	@Test
	public void remove_deveApagarItemERecalcularTotaisNoBanco() {
		service.insere(COD_PRODUTO, COD_NOTA, 2, NotaFiscalTipo.SAIDA);
		Long codItem = ((Number) itensDaNota().get(0).get("codigo")).longValue();

		service.remove(codItem, COD_NOTA);

		assertEquals(0, itensDaNota().size());
		Map<String, Object> totais = totaisDaNota();
		assertEquals(0.0, ((Number) totais.get("v_prod")).doubleValue(), DELTA);
		assertEquals(0.0, ((Number) totais.get("v_nf")).doubleValue(), DELTA);
	}

	// TI-NFI-05: mesmo defeito do TU-NFI-14 (Issue #2), agora com banco real.
	// @Ignore até o defeito ser corrigido — remover a anotação reproduz a falha.
	@Ignore("Issue #2: NoSuchElementException em vez de validação amigável quando produto não tem Modalidade BC ICMS")
	@Test
	public void insere_deveRejeitarSemGravar_quandoProdutoSemModalidadeBcIcms() {
		try {
			service.insere(COD_PRODUTO_SEM_MOD_BC, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Produto sem Modalidade BC ICMS, favor verifique", e.getMessage());
		}

		assertEquals(0, itensDaNota().size());
	}

	// apoio

	private List<Map<String, Object>> itensDaNota() {
		return jdbc.queryForList("select * from nota_fiscal_item where nota_fiscal_codigo = ?", COD_NOTA);
	}

	private Map<String, Object> totaisDaNota() {
		return jdbc.queryForMap("select * from nota_fiscal_totais where codigo = ?", COD);
	}

	// massa de dados no banco

	private void criaMassaDeDados() {
		String hoje = "2026-11-19";
		Long fornecedor = jdbc.queryForObject("select min(codigo) from fornecedor", Long.class);
		Long frete = jdbc.queryForObject("select min(codigo) from frete_tipo", Long.class);
		Long finalidade = jdbc.queryForObject("select min(codigo) from nota_fiscal_finalidade", Long.class);
		Long regime = jdbc.queryForObject("select min(codigo) from regime_tributario", Long.class);
		Long sp = jdbc.queryForObject("select codigo from estado where sigla = 'SP'", Long.class);

		jdbc.update("insert into cidade (codigo, nome, codigo_municipio, estado_codigo) values (?, 'Cidade IT', '3550308', ?)",
				COD, sp);
		jdbc.update("insert into endereco (codigo, rua, bairro, numero, cep, referencia, data_cadastro, cidade_codigo) "
				+ "values (?, 'Rua IT', 'Centro', '1', '01000000', '', ?, ?)", COD, hoje, COD);
		jdbc.update("insert into pessoa (codigo, nome, cpfcnpj, apelido, data_nascimento, observacao, endereco_codigo, data_cadastro) "
				+ "values (?, 'Destinatario IT', '00000000000', 'IT', ?, '', ?, ?)", COD, hoje, COD, hoje);
		jdbc.update("insert into empresa (codigo, nome, nome_fantasia, cnpj, regime_tributario_codigo, endereco_codigo) "
				+ "values (?, 'Empresa IT', 'Empresa IT', '00000000000000', ?, ?)", COD, regime, COD);

		jdbc.update("insert into cst_csosn (codigo, cst_csosn, simples_nacional) values (?, '102', 1)", COD);
		jdbc.update("insert into cst (codigo, cst, descricao) values (?, '01', 'CST IT')", COD);
		jdbc.update("insert into cfop (codigo, cfop, descricao) values (?, '5102', 'CFOP IT')", COD);
		jdbc.update("insert into cst_ipi (codigo, cst, descricao, tipo) values (?, '50', 'CST IPI IT', 'SAIDA')", COD);
		jdbc.update("insert into mod_bc_icms (codigo, tipo, descricao, sub_tributaria) values (?, 0, 'Mod BC IT', 0)", COD);

		jdbc.update("insert into tributacao (codigo, descricao, subs_tributaria, data_cadastro, empresa_codigo) "
				+ "values (?, 'Tributacao IT', 0, ?, ?)", COD, hoje, COD);
		jdbc.update("insert into tributacao_regra (codigo, tributacao_codigo, uf, pis, cofins, cst_pis, cst_cofins, aliq_ipi, "
				+ "aliq_icms, tipo, cst_csosn_codigo, cfop_codigo, cst_ipi_codigo, data_cadastro) "
				+ "values (?, ?, ?, 1.65, 7.6, ?, ?, 5, 18, 'SAIDA', ?, ?, ?, ?)",
				COD, COD, String.valueOf(sp), COD, COD, COD, COD, COD, hoje);

		String produto = "insert into produto (codigo, descricao, valor_venda, balanca, ativo, fornecedor_codigo, grupo_codigo, "
				+ "unidade, subtributaria, categoria_codigo, ncm, cest, vendavel, controla_estoque, tributacao_codigo, "
				+ "bc_icms_codigo, data_cadastro) values (?, 'Produto IT', 10, 0, 'ATIVO', ?, 1, 'UN', 0, 1, '12345678', '', "
				+ "'SIM', 'NAO', ?, ?, ?)";
		jdbc.update(produto, COD_PRODUTO, fornecedor, COD, COD, hoje);
		jdbc.update(produto, COD_PRODUTO_SEM_MOD_BC, fornecedor, COD, null, hoje);

		jdbc.update("insert into nota_fiscal_totais (codigo, v_bc, v_icms, v_st, v_prod, v_frete, v_seg, v_desc, v_ii, v_ipi, "
				+ "v_pis, v_cofins, v_outros, v_nf) values (?, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)", COD);
		// tipo é gravado pelo ordinal de NotaFiscalTipo: 1 = SAIDA; tipo_emissao,
		// tipo_impressao, procEmi e tipo_ambiente são int em NotaFiscal e não aceitam NULL
		jdbc.update("insert into nota_fiscal (codigo, numero, modelo, tipo, natureza_operacao, serie, emissor_codigo, "
				+ "destinatario_codigo, data_cadastro, totais_codigo, frete_tipo_codigo, finalidade_codigo, "
				+ "tipo_emissao, tipo_impressao, procEmi, tipo_ambiente) "
				+ "values (?, 9001, 55, '1', 'Venda IT', 1, ?, ?, ?, ?, ?, ?, 1, 1, 0, 2)",
				COD_NOTA, COD, COD, hoje, COD, frete, finalidade);
	}

	private void limpa() {
		List<Long> impostos = jdbc.queryForList(
				"select imposto_codigo from nota_fiscal_item where nota_fiscal_codigo = ?", Long.class, COD_NOTA);
		jdbc.update("delete from nota_fiscal_item where nota_fiscal_codigo = ?", COD_NOTA);
		for (Long imposto : impostos)
			jdbc.update("delete from nota_fiscal_item_imposto where codigo = ?", imposto);
		jdbc.update("delete from nota_fiscal where codigo = ?", COD_NOTA);
		jdbc.update("delete from nota_fiscal_totais where codigo = ?", COD);
		jdbc.update("delete from produto_estoque where produto_codigo in (?, ?)", COD_PRODUTO, COD_PRODUTO_SEM_MOD_BC);
		jdbc.update("delete from produto where codigo in (?, ?)", COD_PRODUTO, COD_PRODUTO_SEM_MOD_BC);
		jdbc.update("delete from tributacao_regra where codigo = ?", COD);
		jdbc.update("delete from tributacao where codigo = ?", COD);
		jdbc.update("delete from mod_bc_icms where codigo = ?", COD);
		jdbc.update("delete from cst_ipi where codigo = ?", COD);
		jdbc.update("delete from cfop where codigo = ?", COD);
		jdbc.update("delete from cst where codigo = ?", COD);
		jdbc.update("delete from cst_csosn where codigo = ?", COD);
		jdbc.update("delete from empresa where codigo = ?", COD);
		jdbc.update("delete from pessoa where codigo = ?", COD);
		jdbc.update("delete from endereco where codigo = ?", COD);
		jdbc.update("delete from cidade where codigo = ?", COD);
	}

	// objetos em memória para o TI-NFI-01

	private Produto produtoEmMemoria() {
		Estado sp = new Estado();
		sp.setSigla("SP");

		Cst cst = new Cst();
		cst.setCst("01");
		CstIPI cstIpi = new CstIPI();
		cstIpi.setCst("50");
		CstCsosn csosn = new CstCsosn();
		csosn.setCst_csosn("102");
		CFOP cfop = new CFOP();
		cfop.setCfop("5102");

		TributacaoRegra regra = new TributacaoRegra();
		regra.setTipo(EntradaSaida.SAIDA);
		regra.setUf(sp);
		regra.setCfop(cfop);
		regra.setCst_csosn(csosn);
		regra.setCst_pis(cst);
		regra.setCst_cofins(cst);
		regra.setCst_ipi(cstIpi);
		regra.setAliq_icms(18.0);
		regra.setPis(1.65);
		regra.setCofins(7.6);
		regra.setAliq_ipi(5.0);

		Tributacao tributacao = new Tributacao();
		tributacao.setRegra(new ArrayList<>(Arrays.asList(regra)));

		ModBcIcms modBc = new ModBcIcms();
		modBc.setTipo(0);

		Produto p = new Produto();
		p.setCodigo(COD_PRODUTO);
		p.setValor_venda(10.0);
		p.setUnidade("UN");
		p.setNcm("12345678");
		p.setCest("");
		p.setSubtributaria(ProdutoSubstTributaria.NAO);
		p.setModBcIcms(modBc);
		p.setTributacao(tributacao);
		return p;
	}

	private NotaFiscal notaEmMemoria() {
		Estado sp = new Estado();
		sp.setSigla("SP");
		Cidade cidade = new Cidade();
		cidade.setEstado(sp);
		Endereco endereco = new Endereco();
		endereco.setCidade(cidade);
		Pessoa destinatario = new Pessoa();
		destinatario.setEndereco(endereco);

		NotaFiscal nf = new NotaFiscal();
		nf.setCodigo(COD_NOTA);
		nf.setTipo(NotaFiscalTipo.SAIDA);
		nf.setItens(new ArrayList<>());
		nf.setDestinatario(destinatario);
		nf.setTotais(new NotaFiscalTotais(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
		return nf;
	}
}
