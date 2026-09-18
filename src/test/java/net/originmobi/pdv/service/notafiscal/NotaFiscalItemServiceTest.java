package net.originmobi.pdv.service.notafiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
	private static final String UF_DESTINATARIO = "SP";

	private Produto produto;
	private NotaFiscal notaFiscal;

	@Before
	public void setUp() {
		produto = produtoValido();
		notaFiscal = notaFiscalValida(NotaFiscalTipo.SAIDA, new ArrayList<>());

		when(produtos.buscaProduto(COD_PRODUTO)).thenReturn(Optional.of(produto));
		when(notas.busca(COD_NOTA)).thenReturn(Optional.of(notaFiscal));
		when(impostos.calcula(any(), anyDouble(), any(TributacaoRegra.class), anyChar(), anyInt()))
				.thenReturn(new NotaFiscalItemImposto());
	}

	// caminho feliz

	@Test
	public void insere_deveCriarNovoItemEAtualizarTotais_quandoDadosValidos() {
		String retorno = service.insere(COD_PRODUTO, COD_NOTA, 3, NotaFiscalTipo.SAIDA);

		assertEquals("ok", retorno);

		// item novo -> qtd = informada, valor total = qtd * valor de venda, sem codigo ainda
		verify(itemServer, times(1)).save(argThat(item -> item.getQtd() == 3
				&& item.getVlTotal().equals(3 * produto.getValor_venda()) && item.getCodigo() == null));

		verify(totais, times(1)).atualiza(eq(COD_NOTA), any(NotaFiscalTotais.class));
	}

	@Test
	public void insere_deveSomarQuantidadeEManterCodigo_quandoProdutoJaEstaNaNota() {
		// nota ja tem esse produto lancado (qtd 5, codigo 99) -> inserir de novo deveria só atualizar
		NotaFiscalItemImposto impostoExistente = new NotaFiscalItemImposto();
		impostoExistente.setCodigo(77L);

		NotaFiscalItem itemExistente = new NotaFiscalItem(COD_PRODUTO, 5, 50.0, "UN", 5, 10.0, notaFiscal,
				impostoExistente, "5102");
		itemExistente.setCodigo(99L);

		notaFiscal.setItens(new ArrayList<>(Arrays.asList(itemExistente)));

		service.insere(COD_PRODUTO, COD_NOTA, 3, NotaFiscalTipo.SAIDA);

		verify(itemServer).save(argThat(item -> item.getQtd() == 8 // 5 + 3
				&& Long.valueOf(99L).equals(item.getCodigo()))); // mantem o codigo do item existente
	}

	// validacoes de verificaRegraDeTributacao() - cada throw do metodo vira um teste aqui

	@Test
	public void insere_deveLancarExcecao_quandoProdutoNaoEncontrado() {
		when(produtos.buscaProduto(COD_PRODUTO)).thenReturn(Optional.empty());

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Nenhum produto encontrado, favor verifique", e.getMessage());
		}

		verify(itemServer, never()).save(any());
	}

	@Test
	public void insere_deveLancarExcecao_quandoProdutoSemTributacao() {
		produto.setTributacao(null);

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Produto sem tributação, favor verifique", e.getMessage());
		}
	}

	@Test
	public void insere_deveLancarExcecao_quandoProdutoSemNcm() {
		produto.setNcm("");

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Produto sem código NCM, favor verifique", e.getMessage());
		}
	}

	@Test
	public void insere_deveLancarExcecao_quandoSubstitutoTributarioSemCest() {
		produto.setSubtributaria(ProdutoSubstTributaria.SIM);
		produto.setCest("");

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Produto de substituição tributária sem código CEST, favor verifique", e.getMessage());
		}
	}

	@Test
	public void insere_deveLancarExcecao_quandoProdutoSemUnidade() {
		produto.setUnidade("");

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Produto sem unidade, favor verifique", e.getMessage());
		}
	}

	@Test
	public void insere_deveLancarExcecao_quandoTributacaoSemRegraDeSaida() {
		// tributação só possui regra de ENTRADA, mas a nota é de SAIDA
		produto.getTributacao().setRegra(new ArrayList<>(Arrays.asList(regra(EntradaSaida.ENTRADA, UF_DESTINATARIO))));

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Tributação sem regra de saída, verifique", e.getMessage());
		}
	}

	@Test
	public void insere_deveLancarExcecao_quandoTributacaoSemRegraDeEntrada() {
		notaFiscal.setTipo(NotaFiscalTipo.ENTRADA);
		// tributação só possui regra de SAIDA, mas a nota é de ENTRADA
		produto.getTributacao().setRegra(new ArrayList<>(Arrays.asList(regra(EntradaSaida.SAIDA, UF_DESTINATARIO))));

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.ENTRADA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Tributação sem regra de entrada, verifique", e.getMessage());
		}
	}

	// a regra bate com o tipo (saida/entrada) mas nao com a UF do destinatario

	@Test
	public void insere_deveLancarExcecao_quandoNenhumaRegraCasaComUfDoDestinatario() {
		// regra existe e satisfaz a verificação inicial (SAIDA), mas é de outra UF
		produto.getTributacao().setRegra(new ArrayList<>(Arrays.asList(regra(EntradaSaida.SAIDA, "RJ"))));

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Nenhuma regra de tributação cadastrada para a UF do destinatário", e.getMessage());
		}

		verify(itemServer, never()).save(any());
	}

	// se o repositorio explodir, tem que virar uma mensagem amigavel, nao vazar a exception

	@Test
	public void insere_deveEncapsularExcecao_quandoRepositorioFalhaAoSalvar() {
		doThrow(new RuntimeException("erro de conexão")).when(itemServer).save(any(NotaFiscalItem.class));

		try {
			service.insere(COD_PRODUTO, COD_NOTA, 1, NotaFiscalTipo.SAIDA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Erro ao salvar item na nota, chame o suporte", e.getMessage());
		}

		verify(totais, never()).atualiza(any(), any());
	}

	// remove()

	@Test
	public void remove_deveRemoverItemEAtualizarTotaisDaNota() {
		when(notas.busca(COD_NOTA)).thenReturn(Optional.of(notaFiscal));

		service.remove(50L, COD_NOTA);

		verify(itemServer, times(1)).deleteById(50L);
		verify(totais, times(1)).atualiza(eq(COD_NOTA), any(NotaFiscalTotais.class));
	}

	@Test
	public void remove_deveEncapsularExcecao_quandoRepositorioFalhaAoRemover() {
		doThrow(new RuntimeException("erro de conexão")).when(itemServer).deleteById(50L);

		try {
			service.remove(50L, COD_NOTA);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Erro ao tentar remover o item da nota, chame o suporte", e.getMessage());
		}

		verify(totais, never()).atualiza(any(), any());
	}

	// fixtures usadas pelos testes acima

	private Produto produtoValido() {
		Produto p = new Produto();
		p.setCodigo(COD_PRODUTO);
		p.setDescricao("Produto de teste");
		p.setValor_venda(10.0);
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