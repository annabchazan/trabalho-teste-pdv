package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Parcela;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Receber;
import net.originmobi.pdv.model.Recebimento;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.RecebimentoRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;

@RunWith(MockitoJUnitRunner.class)
public class RecebimentoServiceTest {

	@Mock
	private RecebimentoRepository recebimentos;

	@Mock
	private PessoaService pessoas;

	@Mock
	private RecebimentoParcelaService receParcelas;

	@Mock
	private ParcelaService parcelas;

	@Mock
	private CaixaService caixas;

	@Mock
	private UsuarioService usuarios;

	@Mock
	private CaixaLancamentoService lancamentos;

	@Mock
	private TituloService titulos;

	@Mock
	private CartaoLancamentoService cartaoLancamentos;

	@InjectMocks
	private RecebimentoService service;

	private static final Long COD_PESSOA = 1L;
	private static final Long COD_RECEBIMENTO = 20L;
	private static final Long COD_TITULO = 5L;
	private static final String USUARIO_LOGADO = "gerente";

	@Before
	public void setUp() {
		// receber() instancia Aplicacao, que le o usuario direto do SecurityContextHolder
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(USUARIO_LOGADO, "123"));
	}

	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}

	// ---------------------------------------------------------------
	// abrirRecebimento()
	// ---------------------------------------------------------------

	// TU-REC-01
	@Test
	public void abrirRecebimento_deveSomarParcelasERetornarCodigo_quandoParcelasValidas() {
		when(parcelas.busca(10L)).thenReturn(parcela(10L, COD_PESSOA, 100.0, 0));
		when(parcelas.busca(11L)).thenReturn(parcela(11L, COD_PESSOA, 50.0, 0));
		when(pessoas.buscaPessoa(COD_PESSOA)).thenReturn(Optional.of(pessoa(COD_PESSOA)));
		simulaGeracaoDeCodigoNoSave(77L);

		String retorno = service.abrirRecebimento(COD_PESSOA, new String[] { "10", "11" });

		assertEquals("77", retorno);
		verify(recebimentos).save(argThat(rec -> rec.getValor_total().equals(150.0) && rec.getParcela().size() == 2));
	}

	// TU-REC-02
	@Test
	public void abrirRecebimento_deveLancarExcecao_quandoParcelaJaQuitada() {
		when(parcelas.busca(10L)).thenReturn(parcela(10L, COD_PESSOA, 100.0, 1));

		try {
			service.abrirRecebimento(COD_PESSOA, new String[] { "10" });
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Parcela 10 já esta quitada, verifique.", e.getMessage());
		}

		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	// TU-REC-03
	@Test
	public void abrirRecebimento_deveLancarExcecao_quandoParcelaEDeOutroCliente() {
		when(parcelas.busca(10L)).thenReturn(parcela(10L, 2L, 100.0, 0));

		try {
			service.abrirRecebimento(COD_PESSOA, new String[] { "10" });
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("A parcela 10 não pertence ao cliente selecionado", e.getMessage());
		}
	}

	// TU-REC-04
	@Test
	public void abrirRecebimento_deveLancarExcecao_quandoClienteNaoEncontrado() {
		when(parcelas.busca(10L)).thenReturn(parcela(10L, COD_PESSOA, 100.0, 0));
		when(pessoas.buscaPessoa(COD_PESSOA)).thenReturn(Optional.empty());

		try {
			service.abrirRecebimento(COD_PESSOA, new String[] { "10" });
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Cliente não encontrado", e.getMessage());
		}

		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	// TU-REC-05
	@Test
	public void abrirRecebimento_deveEncapsularExcecao_quandoRepositorioFalhaAoSalvar() {
		when(parcelas.busca(10L)).thenReturn(parcela(10L, COD_PESSOA, 100.0, 0));
		when(pessoas.buscaPessoa(COD_PESSOA)).thenReturn(Optional.of(pessoa(COD_PESSOA)));
		doThrow(new RuntimeException("erro de conexão")).when(recebimentos).save(any(Recebimento.class));

		try {
			service.abrirRecebimento(COD_PESSOA, new String[] { "10" });
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Erro ao receber, chame o suporte", e.getMessage());
		}
	}

	// TU-REC-06 - defeito D1
	@Test
	@Ignore("Defeito D1 - RecebimentoService:72 compara Long com != (docs/bugs/defeitos-recebimento.md)")
	public void abrirRecebimento_deveAceitarParcela_quandoClienteTemCodigoAcimaDoCacheDeLong() {
		// Long.valueOf() so reaproveita instancias de -128 a 127; acima disso cada
		// chamada devolve um objeto novo e a comparacao por referencia (!=) falha
		Long codPessoaArgumento = Long.valueOf(1000L);
		Long codPessoaEntidade = Long.valueOf(1000L);

		when(parcelas.busca(10L)).thenReturn(parcela(10L, codPessoaEntidade, 100.0, 0));
		when(pessoas.buscaPessoa(codPessoaArgumento)).thenReturn(Optional.of(pessoa(codPessoaEntidade)));
		simulaGeracaoDeCodigoNoSave(77L);

		assertEquals("77", service.abrirRecebimento(codPessoaArgumento, new String[] { "10" }));
	}

	// ---------------------------------------------------------------
	// receber()
	// ---------------------------------------------------------------

	// TU-REC-07
	@Test
	public void receber_deveQuitarParcelaELancarNoCaixa_quandoTituloEmDinheiro() {
		Recebimento recebimento = recebimentoEmAberto(100.0);
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimento));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO))
				.thenReturn(Arrays.asList(parcela(10L, COD_PESSOA, 100.0, 0)));
		when(usuarios.buscaUsuario(USUARIO_LOGADO)).thenReturn(usuario());
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

		String retorno = service.receber(COD_RECEBIMENTO, 100.0, 2.0, 1.0, COD_TITULO);

		assertEquals("Recebimento realizado com sucesso", retorno);
		verify(parcelas).receber(10L, 100.0, 0.00, 0.00);
		verify(lancamentos).lancamento(argThat(lancamento -> lancamento.getValor().equals(100.0)
				&& lancamento.getTipo().equals(TipoLancamento.RECEBIMENTO)
				&& lancamento.getEstilo().equals(EstiloLancamento.ENTRADA)));
		verify(recebimentos).save(argThat(rec -> rec.getValor_recebido().equals(100.0)
				&& rec.getValor_acrescimo().equals(2.0) && rec.getValor_desconto().equals(1.0)
				&& rec.getData_processamento() != null));
	}

	// TU-REC-08
	@Test
	public void receber_deveLancarExcecao_quandoTituloNaoInformado() {
		try {
			service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, 0L);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Selecione um título para realizar o recebimento", e.getMessage());
		}
	}

	// TU-REC-09
	@Test
	public void receber_deveLancarExcecao_quandoRecebimentoJaProcessado() {
		Recebimento recebimento = recebimentoEmAberto(100.0);
		recebimento.setData_processamento(new Timestamp(System.currentTimeMillis()));
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimento));

		try {
			service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, COD_TITULO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Recebimento já esta fechado", e.getMessage());
		}
	}

	// TU-REC-10
	@Test
	public void receber_deveLancarExcecao_quandoValorRecebidoSuperiorAoTotal() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		try {
			service.receber(COD_RECEBIMENTO, 200.0, 0.0, 0.0, COD_TITULO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Valor de recebimento é superior aos títulos", e.getMessage());
		}
	}

	// TU-REC-11
	@Test
	public void receber_deveLancarExcecao_quandoRecebimentoSemParcelas() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO)).thenReturn(new ArrayList<Parcela>());

		try {
			service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, COD_TITULO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Recebimento não possue parcelas", e.getMessage());
		}
	}

	// TU-REC-12
	@Test
	public void receber_deveLancarExcecao_quandoValorRecebidoNaoEPositivo() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO))
				.thenReturn(Arrays.asList(parcela(10L, COD_PESSOA, 100.0, 0)));

		try {
			service.receber(COD_RECEBIMENTO, 0.0, 0.0, 0.0, COD_TITULO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Valor de recebimento inválido", e.getMessage());
		}
	}

	// TU-REC-13
	@Test
	public void receber_deveRatearValorEntreAsParcelasNaOrdem_quandoValorNaoCobreTodas() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(200.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO))
				.thenReturn(Arrays.asList(parcela(10L, COD_PESSOA, 100.0, 0), parcela(11L, COD_PESSOA, 100.0, 0)));
		when(usuarios.buscaUsuario(USUARIO_LOGADO)).thenReturn(usuario());
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

		service.receber(COD_RECEBIMENTO, 150.0, 0.0, 0.0, COD_TITULO);

		ArgumentCaptor<Long> codParcela = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Double> valorQuitado = ArgumentCaptor.forClass(Double.class);
		verify(parcelas, times(2)).receber(codParcela.capture(), valorQuitado.capture(), eq(0.00), eq(0.00));

		assertEquals(Arrays.asList(10L, 11L), codParcela.getAllValues());
		assertEquals(Arrays.asList(100.0, 50.0), valorQuitado.getAllValues());
	}

	// TU-REC-14
	@Test
	public void receber_deveLancarNoCartaoENaoNoCaixa_quandoTituloECartaoDeCredito() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.CARTCRED)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO))
				.thenReturn(Arrays.asList(parcela(10L, COD_PESSOA, 100.0, 0)));
		when(usuarios.buscaUsuario(USUARIO_LOGADO)).thenReturn(usuario());

		service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, COD_TITULO);

		verify(cartaoLancamentos).lancamento(eq(100.0), any());
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
	}

	// TU-REC-15
	@Test
	public void receber_deveLancarNoCartaoENaoNoCaixa_quandoTituloECartaoDeDebito() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.CARTDEB)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO))
				.thenReturn(Arrays.asList(parcela(10L, COD_PESSOA, 100.0, 0)));
		when(usuarios.buscaUsuario(USUARIO_LOGADO)).thenReturn(usuario());

		service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, COD_TITULO);

		verify(cartaoLancamentos).lancamento(eq(100.0), any());
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
	}

	// TU-REC-16
	@Test
	public void receber_deveEncapsularExcecao_quandoFalhaAoQuitarParcela() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO))
				.thenReturn(Arrays.asList(parcela(10L, COD_PESSOA, 100.0, 0)));
		doThrow(new RuntimeException("erro de conexão")).when(parcelas).receber(anyLong(), anyDouble(), anyDouble(),
				anyDouble());

		try {
			service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, COD_TITULO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Ocorreu um erro ao realizar o recebimento, chame o suporte", e.getMessage());
		}
	}

	// TU-REC-17
	@Test
	public void receber_deveEncapsularExcecao_quandoFalhaNoLancamentoDeCaixa() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO))
				.thenReturn(Arrays.asList(parcela(10L, COD_PESSOA, 100.0, 0)));
		when(usuarios.buscaUsuario(USUARIO_LOGADO)).thenReturn(usuario());
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		doThrow(new RuntimeException("erro de conexão")).when(lancamentos).lancamento(any(CaixaLancamento.class));

		try {
			service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, COD_TITULO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Ocorreu um erro ao realizar o recebimento, chame o suporte", e.getMessage());
		}

		verify(recebimentos, never()).save(any(Recebimento.class));
	}

	// TU-REC-18
	@Test
	public void receber_deveEncapsularExcecao_quandoFalhaAoSalvarORecebimento() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(receParcelas.parcelasDoReceber(COD_RECEBIMENTO))
				.thenReturn(Arrays.asList(parcela(10L, COD_PESSOA, 100.0, 0)));
		when(usuarios.buscaUsuario(USUARIO_LOGADO)).thenReturn(usuario());
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		doThrow(new RuntimeException("erro de conexão")).when(recebimentos).save(any(Recebimento.class));

		try {
			service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, COD_TITULO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Ocorreu um erro ao realizar o recebimento, chame o suporte", e.getMessage());
		}
	}

	// TU-REC-19 - defeito D2
	@Test
	@Ignore("Defeito D2 - RecebimentoService:108 faz unboxing antes de testar null (docs/bugs/defeitos-recebimento.md)")
	public void receber_deveLancarMensagemAmigavel_quandoCodigoDoTituloENulo() {
		try {
			service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, null);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Selecione um título para realizar o recebimento", e.getMessage());
		}
	}

	// TU-REC-20 - defeito D3
	@Test
	@Ignore("Defeito D3 - RecebimentoService:115 consome Optional vazio com get() (docs/bugs/defeitos-recebimento.md)")
	public void receber_deveLancarMensagemAmigavel_quandoTituloNaoExiste() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		when(titulos.busca(COD_TITULO)).thenReturn(Optional.empty());

		try {
			service.receber(COD_RECEBIMENTO, 100.0, 0.0, 0.0, COD_TITULO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Título não encontrado, favor verifique", e.getMessage());
		}
	}

	// ---------------------------------------------------------------
	// remover()
	// ---------------------------------------------------------------

	// TU-REC-21
	@Test
	public void remover_deveRemoverRecebimento_quandoAindaNaoProcessado() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));

		assertEquals("removido com sucesso", service.remover(COD_RECEBIMENTO));

		verify(recebimentos).deleteById(COD_RECEBIMENTO);
	}

	// TU-REC-22
	@Test
	public void remover_deveLancarExcecao_quandoRecebimentoJaProcessado() {
		Recebimento recebimento = recebimentoEmAberto(100.0);
		recebimento.setData_processamento(new Timestamp(System.currentTimeMillis()));
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimento));

		try {
			service.remover(COD_RECEBIMENTO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Esse recebimento não pode ser removido, pois ele já esta processado", e.getMessage());
		}

		verify(recebimentos, never()).deleteById(anyLong());
	}

	// TU-REC-23
	@Test
	public void remover_deveEncapsularExcecao_quandoRepositorioFalhaAoRemover() {
		when(recebimentos.findById(COD_RECEBIMENTO)).thenReturn(Optional.of(recebimentoEmAberto(100.0)));
		doThrow(new RuntimeException("erro de conexão")).when(recebimentos).deleteById(COD_RECEBIMENTO);

		try {
			service.remover(COD_RECEBIMENTO);
			fail("Deveria ter lançado RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Erro ao remover orçamento, chame o suporte", e.getMessage());
		}
	}

	// ---------------------------------------------------------------
	// fixtures
	// ---------------------------------------------------------------

	private void simulaGeracaoDeCodigoNoSave(Long codigoGerado) {
		// o servico devolve recebimento.getCodigo().toString(); com o repositorio
		// mockado o @GeneratedValue nao acontece, entao simulamos aqui
		doAnswer(invocation -> {
			Recebimento salvo = invocation.getArgument(0);
			salvo.setCodigo(codigoGerado);
			return salvo;
		}).when(recebimentos).save(any(Recebimento.class));
	}

	private Parcela parcela(Long codigo, Long codPessoa, Double valorRestante, int quitado) {
		Receber receber = new Receber();
		receber.setPessoa(pessoa(codPessoa));

		Parcela parcela = new Parcela();
		parcela.setCodigo(codigo);
		parcela.setValor_restante(valorRestante);
		parcela.setQuitado(quitado);
		parcela.setReceber(receber);
		return parcela;
	}

	private Pessoa pessoa(Long codigo) {
		Pessoa pessoa = new Pessoa();
		pessoa.setCodigo(codigo);
		return pessoa;
	}

	private Recebimento recebimentoEmAberto(Double valorTotal) {
		Recebimento recebimento = new Recebimento(valorTotal, new Timestamp(System.currentTimeMillis()),
				pessoa(COD_PESSOA), new ArrayList<Parcela>());
		recebimento.setCodigo(COD_RECEBIMENTO);
		return recebimento;
	}

	private Titulo titulo(TituloTipo sigla) {
		net.originmobi.pdv.model.TituloTipo tipo = new net.originmobi.pdv.model.TituloTipo();
		tipo.setSigla(sigla.toString());

		Titulo titulo = new Titulo();
		titulo.setCodigo(COD_TITULO);
		titulo.setTipo(tipo);
		return titulo;
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setCodigo(1L);
		usuario.setUser(USUARIO_LOGADO);
		return usuario;
	}
}
