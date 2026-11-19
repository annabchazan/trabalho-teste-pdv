package net.originmobi.pdv.service.notafiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.notafiscal.NotaFiscalTipo;
import net.originmobi.pdv.enumerado.produto.ProdutoSubstTributaria;
import net.originmobi.pdv.model.CFOP;
import net.originmobi.pdv.model.Cidade;
import net.originmobi.pdv.model.CstCsosn;
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
import net.originmobi.pdv.repository.notafiscal.NotaFiscalItemRepository;
import net.originmobi.pdv.service.ProdutoService;

/**
 * Testes unitários de NotaFiscalItemService. Todas as dependências são
 * substituídas por mocks; os IDs TU-NFI-xx correspondem ao Plano de Teste.
 */
@RunWith(MockitoJUnitRunner.class)
public class NotaFiscalItemServiceTest {

	@Mock
	private NotaFiscalItemRepository itemServer;

	@Mock
	private NotaFiscalItemImpostoService impostos;

	@Mock
	private NotaFiscalTotaisServer totais;

	@Mock
	private ProdutoService produtos;

	@Mock
	private NotaFiscalService notas;

	@InjectMocks
	private NotaFiscalItemService service;

	private static final Long COD_PRODUTO = 1L;
	private static final Long COD_NOTA = 10L;
	private static final Long COD_ITEM = 50L;
	private static final String UF_DESTINATARIO = "SP";
	private static final Double VALOR_VENDA = 10.0;

	private Produto produto;
	private NotaFiscal notaFiscal;
	private NotaFiscalItemImposto impostoCalculado;

	@Before
	public void setUp() {
		produto = produtoValido();
		notaFiscal = notaFiscalValida(NotaFiscalTipo.SAIDA, new ArrayList<>());
		impostoCalculado = new NotaFiscalItemImposto();

		when(produtos.buscaProduto(COD_PRODUTO)).thenReturn(Optional.of(produto));
		when(notas.busca(COD_NOTA)).thenReturn(Optional.of(notaFiscal));
		when(impostos.calcula(any(), anyDouble(), any(TributacaoRegra.class), anyChar(), anyInt()))
				.thenReturn(impostoCalculado);
	}

	// insere(): caminho feliz

	// TU-NFI-01
	@Test
	public void insere_deveCriarNovoItemEAtualizarTotais_quandoDadosValidos() {
		TributacaoRegra regraSaida = produto.getTributacao().getRegra().get(0);

		String retorno = service.insere(COD_PRODUTO, COD_NOTA, 3, NotaFiscalTipo.SAIDA);

		assertEquals("ok", retorno);

		// imposto calculado com: item novo (sem código), total = 3 x 10, a regra da UF,
		// origem = 1º dígito do CST ("0102") e o tipo da Modalidade BC ICMS
		verify(impostos).calcula(null, 30.0, regraSaida, '0', 0);

		NotaFiscalItem salvo = itemSalvo();
		assertNull(salvo.getCodigo());
		assertEquals(COD_PRODUTO, salvo.getCodProd());
		assertEquals(3, salvo.getQtd());
		assertEquals(3, salvo.getQtd_tribu());
		assertEquals(Double.valueOf(30.0), salvo.getVlTotal());
		assertEquals(VALOR_VENDA, salvo.getV_uniTribu());
		assertEquals("UN", salvo.getUnidade_tribu());
		assertEquals("5102", salvo.getCfop());
		assertSame(notaFiscal, salvo.getNotaFiscal());
		assertSame(impostoCalculado, salvo.getImpostos());

		verify(totais).atualiza(COD_NOTA, notaFiscal.getTotais());
	}

	// TU-NFI-02
	@Test
	public void insere_deveSomarQuantidadeEManterCodigo_quandoProdutoJaEstaNaNota() {
		// a nota já tem esse produto lançado (qtd 5, item 99, imposto 77)
		NotaFiscalItemImposto impostoExistente = new NotaFiscalItemImposto();
		impostoExistente.setCodigo(77L);

		NotaFiscalItem itemExistente = new NotaFiscalItem(COD_PRODUTO, 5, 50.0, "UN", 5, VALOR_VENDA, notaFiscal,
				impostoExistente, "5102");
		itemExistente.setCodigo(99L);

		notaFiscal.setItens(new ArrayList<>(Arrays.asList(itemExistente)));

		service.insere(COD_PRODUTO, COD_NOTA, 3, NotaFiscalTipo.SAIDA);

		// recalcula sobre o imposto existente, com o total da quantidade somada (8 x 10)
		verify(impostos).calcula(77L, 80.0, produto.getTributacao().getRegra().get(0), '0', 0);

		NotaFiscalItem salvo = itemSalvo();
		assertEquals(Long.valueOf(99L), salvo.getCodigo());
		assertEquals(8, salvo.getQtd());
		assertEquals(Double.valueOf(80.0), salvo.getVlTotal());

		verify(totais).atualiza(COD_NOTA, notaFiscal.getTotais());
	}

	// insere(): validações de verificaRegraDeTributacao()

	// TU-NFI-03
	@Test
	public void insere_deveLancarExcecao_quandoProdutoNaoEncontrado() {
		when(produtos.buscaProduto(COD_PRODUTO)).thenReturn(Optional.empty());

		insereEsperandoErro(NotaFiscalTipo.SAIDA, "Nenhum produto encontrado, favor verifique");

		verificaQueNadaFoiGravado();
	}

	// TU-NFI-04
	@Test
	public void insere_deveLancarExcecao_quandoProdutoSemTributacao() {
		produto.setTributacao(null);

		insereEsperandoErro(NotaFiscalTipo.SAIDA, "Produto sem tributação, favor verifique");

		verificaQueNadaFoiGravado();
	}

	// TU-NFI-05
	@Test
	public void insere_deveLancarExcecao_quandoProdutoSemNcm() {
		produto.setNcm("");

		insereEsperandoErro(NotaFiscalTipo.SAIDA, "Produto sem código NCM, favor verifique");

		verificaQueNadaFoiGravado();
	}

	// TU-NFI-06
	@Test
	public void insere_deveLancarExcecao_quandoSubstitutoTributarioSemCest() {
		produto.setSubtributaria(ProdutoSubstTributaria.SIM);
		produto.setCest("");

		insereEsperandoErro(NotaFiscalTipo.SAIDA,
				"Produto de substituição tributária sem código CEST, favor verifique");

		verificaQueNadaFoiGravado();
	}

	// TU-NFI-07
	@Test
	public void insere_deveLancarExcecao_quandoProdutoSemUnidade() {
		produto.setUnidade("");

		insereEsperandoErro(NotaFiscalTipo.SAIDA, "Produto sem unidade, favor verifique");

		verificaQueNadaFoiGravado();
	}

	// TU-NFI-08
	@Test
	public void insere_deveLancarExcecao_quandoTributacaoSemRegraDeSaida() {
		// tributação só possui regra de ENTRADA, mas a nota é de SAIDA
		produto.getTributacao().setRegra(new ArrayList<>(Arrays.asList(regra(EntradaSaida.ENTRADA, UF_DESTINATARIO))));

		insereEsperandoErro(NotaFiscalTipo.SAIDA, "Tributação sem regra de saída, verifique");

		verificaQueNadaFoiGravado();
	}

	// TU-NFI-09
	@Test
	public void insere_deveLancarExcecao_quandoTributacaoSemRegraDeEntrada() {
		notaFiscal.setTipo(NotaFiscalTipo.ENTRADA);
		// tributação só possui regra de SAIDA, mas a nota é de ENTRADA
		produto.getTributacao().setRegra(new ArrayList<>(Arrays.asList(regra(EntradaSaida.SAIDA, UF_DESTINATARIO))));

		insereEsperandoErro(NotaFiscalTipo.ENTRADA, "Tributação sem regra de entrada, verifique");

		verificaQueNadaFoiGravado();
	}

	// insere(): regra do tipo certo, mas de outra UF

	// TU-NFI-10
	@Test
	public void insere_deveLancarExcecao_quandoNenhumaRegraCasaComUfDoDestinatario() {
		// regra existe e satisfaz a verificação inicial (SAIDA), mas é de outra UF
		produto.getTributacao().setRegra(new ArrayList<>(Arrays.asList(regra(EntradaSaida.SAIDA, "RJ"))));

		insereEsperandoErro(NotaFiscalTipo.SAIDA, "Nenhuma regra de tributação cadastrada para a UF do destinatário");

		verificaQueNadaFoiGravado();
	}

	// insere(): falha do repositório vira mensagem amigável

	// TU-NFI-11
	@Test
	public void insere_deveEncapsularExcecao_quandoRepositorioFalhaAoSalvar() {
		doThrow(new RuntimeException("erro de conexão")).when(itemServer).save(any(NotaFiscalItem.class));

		insereEsperandoErro(NotaFiscalTipo.SAIDA, "Erro ao salvar item na nota, chame o suporte");

		// tentou salvar uma vez e não atualizou os totais de uma nota inconsistente
		verify(itemServer, times(1)).save(any(NotaFiscalItem.class));
		verifyNoInteractions(totais);
	}

	// TU-NFI-14: reproduz o defeito real encontrado manualmente no CT-NFI-02
	// (Issue #2). Hoje NotaFiscalItemService faz
	// produto.getModBcIcms().getTipo() sem checar antes se o produto tem
	// Modalidade BC ICMS cadastrada, diferente das demais validações de
	// verificaRegraDeTributacao(); o resultado é NoSuchElementException
	// ("No value present") em vez da mensagem de validação abaixo.
	// @Ignore até o defeito ser corrigido — remover a anotação reproduz a falha.
	@Ignore("Issue #2: NoSuchElementException em vez de validação amigável quando produto não tem Modalidade BC ICMS")
	@Test
	public void insere_deveLancarExcecao_quandoProdutoSemModalidadeBcIcms() {
		produto.setModBcIcms(null);

		insereEsperandoErro(NotaFiscalTipo.SAIDA, "Produto sem Modalidade BC ICMS, favor verifique");

		verificaQueNadaFoiGravado();
	}

	// remove()

	// TU-NFI-12
	@Test
	public void remove_deveRemoverItemEAtualizarTotaisDaNota() {
		service.remove(COD_ITEM, COD_NOTA);

		// remove o item e só depois recalcula os totais da mesma nota
		InOrder ordem = inOrder(itemServer, notas, totais);
		ordem.verify(itemServer).deleteById(COD_ITEM);
		ordem.verify(notas).busca(COD_NOTA);
		ordem.verify(totais).atualiza(COD_NOTA, notaFiscal.getTotais());
	}

	// TU-NFI-13
	@Test
	public void remove_deveEncapsularExcecao_quandoRepositorioFalhaAoRemover() {
		doThrow(new RuntimeException("erro de conexão")).when(itemServer).deleteById(COD_ITEM);

		try {
			service.remove(COD_ITEM, COD_NOTA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Erro ao tentar remover o item da nota, chame o suporte", e.getMessage());
		}

		// sem remoção, a nota não é consultada nem tem os totais recalculados
		verify(notas, never()).busca(any());
		verifyNoInteractions(totais);
	}

	// apoio

	private void insereEsperandoErro(NotaFiscalTipo tipo, String mensagemEsperada) {
		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, tipo);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals(mensagemEsperada, e.getMessage());
		}
	}

	private void verificaQueNadaFoiGravado() {
		verifyNoInteractions(itemServer, impostos, totais);
	}

	private NotaFiscalItem itemSalvo() {
		ArgumentCaptor<NotaFiscalItem> captor = ArgumentCaptor.forClass(NotaFiscalItem.class);
		verify(itemServer, times(1)).save(captor.capture());
		return captor.getValue();
	}

	// fixtures

	private Produto produtoValido() {
		Produto p = new Produto();
		p.setCodigo(COD_PRODUTO);
		p.setDescricao("Produto de teste");
		p.setValor_venda(VALOR_VENDA);
		p.setUnidade("UN");
		p.setNcm("12345678");
		p.setCest("");
		p.setSubtributaria(ProdutoSubstTributaria.NAO);

		ModBcIcms modBc = new ModBcIcms();
		modBc.setTipo(0);
		p.setModBcIcms(modBc);

		Tributacao tributacao = new Tributacao();
		tributacao.setCodigo(1L);
		tributacao.setRegra(new ArrayList<>(Arrays.asList(regra(EntradaSaida.SAIDA, UF_DESTINATARIO))));
		p.setTributacao(tributacao);

		return p;
	}

	private TributacaoRegra regra(EntradaSaida tipo, String ufSigla) {
		TributacaoRegra regra = new TributacaoRegra();
		regra.setTipo(tipo);

		Estado uf = new Estado();
		uf.setSigla(ufSigla);
		regra.setUf(uf);

		CFOP cfop = new CFOP();
		cfop.setCfop("5102");
		regra.setCfop(cfop);

		CstCsosn cst = new CstCsosn();
		cst.setCst_csosn("0102");
		regra.setCst_csosn(cst);

		return regra;
	}

	private NotaFiscal notaFiscalValida(NotaFiscalTipo tipo, List<NotaFiscalItem> itens) {
		NotaFiscal nf = new NotaFiscal();
		nf.setCodigo(COD_NOTA);
		nf.setTipo(tipo);
		nf.setItens(itens);
		nf.setTotais(new NotaFiscalTotais(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
		nf.setDestinatario(destinatarioEm(UF_DESTINATARIO));
		return nf;
	}

	private Pessoa destinatarioEm(String ufSigla) {
		Estado estado = new Estado();
		estado.setSigla(ufSigla);

		Cidade cidade = new Cidade();
		cidade.setEstado(estado);

		Endereco endereco = new Endereco();
		endereco.setCidade(cidade);

		Pessoa pessoa = new Pessoa();
		pessoa.setEndereco(endereco);
		return pessoa;
	}
}
