package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.enumerado.VendaSituacao;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.filter.VendaFilter;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.PagamentoTipo;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Receber;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.model.Venda;
import net.originmobi.pdv.repository.VendaRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;
import net.originmobi.pdv.singleton.Aplicacao;

/**
 * Testes unitarios de VendaService - classe de alta complexidade do modulo de
 * Venda (Entrega 1 / Qualidade e Teste).
 *
 * Todas as dependencias sao isoladas com Mockito; nenhum teste toca banco de
 * dados. Os cenarios cobrem os metodos publicos de fluxo (abreVenda, busca,
 * addProduto, removeProduto, lista, qtdAbertos e fechaVenda) e tambem
 * caracterizam defeitos ja presentes no codigo de producao - esses testes estao
 * marcados com "DEFEITO" e asseguram o comportamento REAL de hoje, servindo de
 * rede de protecao para a correcao na Entrega 2.
 *
 * Entrega 2 (Etapa 1): o singleton Aplicacao passa a ser zerado a cada teste,
 * os vencimentos das parcelas passam a ser conferidos em relacao a data de hoje
 * e foram incluidos os cenarios T-1 a T-9 (ver
 * docs/ai/decisoes-entrega2-venda.md, D-10).
 */
@RunWith(MockitoJUnitRunner.class)
public class VendaServiceTest {

	@Mock
	private VendaRepository vendas;

	@Mock
	private UsuarioService usuarios;

	@Mock
	private VendaProdutoService vendaProdutos;

	@Mock
	private PagamentoTipoService formaPagamentos;

	@Mock
	private CaixaService caixas;

	@Mock
	private ReceberService receberServ;

	@Mock
	private ParcelaService parcelas;

	@Mock
	private CaixaLancamentoService lancamentos;

	@Mock
	private TituloService tituloService;

	@Mock
	private CartaoLancamentoService cartaoLancamento;

	@Mock
	private ProdutoService produtos;

	@InjectMocks
	private VendaService service;

	private static final Long COD_VENDA = 1L;
	private static final Long COD_PAGAMENTO = 5L;
	private static final Long COD_PRODUTO = 10L;
	private static final String USUARIO_LOGADO = "gerente";

	private Usuario usuario;
	private Pessoa cliente;

	@Before
	public void setUp() {
		// Isolamento (Entrega 2, I-1): Aplicacao e um singleton que guarda em campo
		// estatico o usuario lido na primeira chamada. Sem zera-lo, o resultado
		// dependeria da ordem dos testes e de outras classes de teste da execucao.
		ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null);

		// VendaService usa o singleton Aplicacao, que le o usuario autenticado
		// direto do SecurityContextHolder - sem isso o teste estoura NPE.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(USUARIO_LOGADO, "123"));
		SecurityContextHolder.setContext(context);

		usuario = new Usuario();
		usuario.setUser(USUARIO_LOGADO);

		cliente = new Pessoa();
		cliente.setNome("Cliente de teste");
	}

	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
		ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null);
	}

	// abreVenda()

	@Test
	public void abreVenda_deveCadastrarVendaAberta_quandoVendaNova() {
		Venda venda = new Venda();
		when(usuarios.buscaUsuario(any())).thenReturn(usuario);
		// o codigo so existe depois do insert (@GeneratedValue); o mock simula isso
		when(vendas.save(any(Venda.class))).thenAnswer(invocacao -> {
			Venda salva = invocacao.getArgument(0);
			salva.setCodigo(COD_VENDA);
			return salva;
		});

		Long retorno = service.abreVenda(venda);

		assertEquals(COD_VENDA, retorno);
		assertEquals(VendaSituacao.ABERTA, venda.getSituacao());
		assertSame(usuario, venda.getUsuario());
		assertEquals(Double.valueOf(0.00), venda.getValor_produtos());
		assertNotNull(venda.getData_cadastro());
		verify(vendas, never()).updateDadosVenda(any(), any(), any());
	}

	@Test
	public void abreVenda_deveApenasAtualizarDados_quandoVendaJaExiste() {
		Venda venda = new Venda();
		venda.setCodigo(COD_VENDA);
		venda.setPessoa(cliente);
		venda.setObservacao("troca de cliente");

		Long retorno = service.abreVenda(venda);

		assertEquals(COD_VENDA, retorno);
		verify(vendas).updateDadosVenda(cliente, "troca de cliente", COD_VENDA);
		verify(vendas, never()).save(any(Venda.class));
		verify(usuarios, never()).buscaUsuario(any());
	}

	@Test
	public void abreVenda_deveEngolirExcecaoERetornarCodigoNulo_quandoRepositorioFalhaAoSalvar() {
		// DEFEITO: a falha do insert e silenciada (e.getStackTrace()) e o chamador
		// recebe null como se nada tivesse acontecido.
		when(usuarios.buscaUsuario(any())).thenReturn(usuario);
		when(vendas.save(any(Venda.class))).thenThrow(new RuntimeException("falha de conexao"));

		assertNull(service.abreVenda(new Venda()));
	}

	@Test
	public void abreVenda_deveEngolirExcecao_quandoRepositorioFalhaAoAtualizarVendaExistente() {
		// T-1 / DEFEITO: a falha do update tambem e silenciada; o chamador recebe o
		// codigo da venda como se os dados (cliente, observacao) tivessem sido salvos.
		Venda venda = new Venda();
		venda.setCodigo(COD_VENDA);
		venda.setPessoa(cliente);
		venda.setObservacao("troca de cliente");
		doThrow(new RuntimeException("falha de conexao")).when(vendas).updateDadosVenda(any(), any(), any());

		assertEquals(COD_VENDA, service.abreVenda(venda));
		verify(vendas, never()).save(any(Venda.class));
	}

	// busca()

	@Test
	public void busca_deveConsultarPorCodigo_quandoFiltroInformaCodigo() {
		VendaFilter filter = new VendaFilter();
		filter.setCodigo(7L);
		Pageable pageable = PageRequest.of(0, 10);
		Page<Venda> esperado = new PageImpl<>(Collections.singletonList(new Venda()));
		when(vendas.findByCodigoIn(7L, pageable)).thenReturn(esperado);

		assertSame(esperado, service.busca(filter, "ABERTA", pageable));
		verify(vendas, never()).findBySituacaoEquals(any(), any());
	}

	@Test
	public void busca_deveConsultarVendasAbertas_quandoSituacaoAbertaESemCodigo() {
		Pageable pageable = PageRequest.of(0, 10);
		Page<Venda> esperado = new PageImpl<>(Collections.singletonList(new Venda()));
		when(vendas.findBySituacaoEquals(VendaSituacao.ABERTA, pageable)).thenReturn(esperado);

		assertSame(esperado, service.busca(new VendaFilter(), "ABERTA", pageable));
	}

	@Test
	public void busca_deveConsultarVendasFechadas_quandoSituacaoDiferenteDeAberta() {
		Pageable pageable = PageRequest.of(0, 10);
		Page<Venda> esperado = new PageImpl<>(Collections.singletonList(new Venda()));
		when(vendas.findBySituacaoEquals(VendaSituacao.FECHADA, pageable)).thenReturn(esperado);

		assertSame(esperado, service.busca(new VendaFilter(), "FECHADA", pageable));
	}

	// addProduto()

	@Test
	public void addProduto_deveSalvarProdutoNaVenda_quandoVendaEstaAberta() {
		when(vendas.verificaSituacao(COD_VENDA)).thenReturn(VendaSituacao.ABERTA.toString());

		assertEquals("ok", service.addProduto(COD_VENDA, COD_PRODUTO, 2.5));

		verify(vendaProdutos).salvar(argThat(vp -> COD_PRODUTO.equals(vp.getProduto())
				&& COD_VENDA.equals(vp.getVenda()) && Double.valueOf(2.5).equals(vp.getValor_balanca())));
	}

	@Test
	public void addProduto_deveRecusar_quandoVendaEstaFechada() {
		when(vendas.verificaSituacao(COD_VENDA)).thenReturn(VendaSituacao.FECHADA.toString());

		assertEquals("Venda fechada", service.addProduto(COD_VENDA, COD_PRODUTO, 0.0));

		verify(vendaProdutos, never()).salvar(any());
	}

	@Test
	public void addProduto_deveResponderOk_mesmoQuandoRepositorioFalha() {
		// DEFEITO: o catch engole a excecao e a tela recebe "ok" sem o produto ter
		// sido gravado.
		when(vendas.verificaSituacao(COD_VENDA)).thenReturn(VendaSituacao.ABERTA.toString());
		doThrow(new RuntimeException("falha de conexao")).when(vendaProdutos).salvar(any());

		assertEquals("ok", service.addProduto(COD_VENDA, COD_PRODUTO, 0.0));
	}

	// removeProduto()

	@Test
	public void removeProduto_deveRemoverItem_quandoVendaEstaAberta() {
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(vendaAberta(null));

		assertEquals("ok", service.removeProduto(3L, COD_VENDA));

		verify(vendaProdutos).removeProduto(3L);
	}

	@Test
	public void removeProduto_deveRecusar_quandoVendaEstaFechada() {
		Venda venda = vendaAberta(null);
		venda.setSituacao(VendaSituacao.FECHADA);
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(venda);

		assertEquals("Venda fechada", service.removeProduto(3L, COD_VENDA));

		verify(vendaProdutos, never()).removeProduto(anyLong());
	}

	@Test
	public void removeProduto_deveResponderOk_quandoVendaNaoExiste() {
		// DEFEITO: venda inexistente gera NullPointerException, que e engolida pelo
		// catch e devolve "ok" sem remover nada.
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(null);

		assertEquals("ok", service.removeProduto(3L, COD_VENDA));

		verify(vendaProdutos, never()).removeProduto(anyLong());
	}

	// lista() e qtdAbertos()

	@Test
	public void lista_deveDelegarParaORepositorio() {
		List<Venda> esperado = Collections.singletonList(new Venda());
		when(vendas.findAll()).thenReturn(esperado);

		assertSame(esperado, service.lista());
	}

	@Test
	public void qtdAbertos_deveRetornarQuantidadeDeVendasEmAberto() {
		when(vendas.qtdVendasEmAberto()).thenReturn(4);

		assertEquals(4, service.qtdAbertos());
	}

	// fechaVenda() - validacoes de entrada

	@Test
	public void fechaVenda_deveLancarExcecao_quandoVendaJaEstaFechada() {
		Venda venda = vendaAberta(null);
		venda.setSituacao(VendaSituacao.FECHADA);
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(venda);

		esperaErro("venda fechada", 100.0, 0.0, 0.0, new String[] { "100.00" }, new String[] { "1" });

		verify(formaPagamentos, never()).busca(anyLong());
	}

	@Test
	public void fechaVenda_deveLancarExcecao_quandoValorDeProdutosNaoEPositivo() {
		// Teste funcional: limite E2 na fronteira (0,00) - docs/teste-funcional-venda.md
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(vendaAberta(null));

		esperaErro("Venda sem valor, verifique", 0.0, 0.0, 0.0, new String[] { "0.00" }, new String[] { "1" });

		verify(formaPagamentos, never()).busca(anyLong());
	}

	@Test
	public void fechaVenda_deveLancarExcecao_quandoAVistaEmDinheiroSemCaixaAberto() {
		// Teste funcional: R2 (a vista, dinheiro, caixa fechado) - docs/teste-funcional-venda.md
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(false);

		esperaErro("nenhum caixa aberto", 100.0, 0.0, 0.0, new String[] { "100.00" }, new String[] { "1" });

		verify(lancamentos, never()).lancamento(any());
		verify(vendas, never()).fechaVenda(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	public void fechaVenda_deveLancarExcecao_quandoVendaAPrazoSemCliente() {
		// Teste funcional: R7 (a prazo, sem cliente) - docs/teste-funcional-venda.md
		preparaFechamento("30", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		esperaErro("Venda sem cliente, verifique", 100.0, 0.0, 0.0, new String[] { "100.00" }, new String[] { "1" });

		verify(parcelas, never()).gerarParcela(any(), any(), any(), any(), any(), any(), eq(0), eq(1), any(), any());
	}

	@Test
	public void fechaVenda_deveLancarExcecao_quandoParcelaAVistaVemSemValor() {
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);

		esperaErro("Parcela sem valor, verifique", 100.0, 0.0, 0.0, new String[] { "" }, new String[] { "1" });
	}

	@Test
	public void fechaVenda_deveLancarExcecao_quandoParcelaAPrazoVemSemValor() {
		preparaFechamento("30", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		esperaErro("valor de recebimento invalido", 100.0, 0.0, 0.0, new String[] { "" }, new String[] { "1" });

		verify(parcelas, never()).gerarParcela(any(), any(), any(), any(), any(), any(), eq(0), eq(1), any(), any());
	}

	// fechaVenda() - caminhos felizes

	@Test
	public void fechaVenda_deveLancarNoCaixaEFecharVenda_quandoAVistaEmDinheiro() {
		// Teste funcional: R1 (a vista, dinheiro, caixa aberto) e limite E6 na fronteira (100,00) - docs/teste-funcional-venda.md
		PagamentoTipo formaPagamento = preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		when(usuarios.buscaUsuario(any())).thenReturn(usuario);

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		assertEquals("Venda finalizada com sucesso", retorno);

		verify(receberServ).cadastrar(argThat(receber -> Double.valueOf(100.0).equals(receber.getValor_total())
				&& ("Recebimento referente a venda " + COD_VENDA).equals(receber.getObservacao())));

		verify(lancamentos).lancamento(argThat(lancamento -> Double.valueOf(100.0).equals(lancamento.getValor())
				&& TipoLancamento.RECEBIMENTO.equals(lancamento.getTipo())
				&& EstiloLancamento.ENTRADA.equals(lancamento.getEstilo())
				&& usuario.equals(lancamento.getUsuario())));

		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(100.0), eq(0.0), eq(0.0),
				any(Timestamp.class), eq(formaPagamento));
		verify(produtos).movimentaEstoque(COD_VENDA, EntradaSaida.SAIDA);
		verify(parcelas, never()).gerarParcela(any(), any(), any(), any(), any(), any(), eq(0), eq(1), any(), any());
	}

	@Test
	public void fechaVenda_deveLancarNoCartao_quandoAVistaComCartaoDeCredito() {
		// Teste funcional: R4 (a vista, cartao de credito) - docs/teste-funcional-venda.md
		preparaFechamento("00", null);
		Optional<Titulo> tituloCartao = Optional.of(titulo(TituloTipo.CARTCRED));
		when(tituloService.busca(1L)).thenReturn(tituloCartao);

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		assertEquals("Venda finalizada com sucesso", retorno);
		verify(cartaoLancamento).lancamento(100.0, tituloCartao);
		// cartao nao movimenta caixa
		verify(caixas, never()).caixaIsAberto();
		verify(lancamentos, never()).lancamento(any());
		verify(produtos).movimentaEstoque(COD_VENDA, EntradaSaida.SAIDA);
	}

	@Test
	public void fechaVenda_deveGerarUmaParcelaPorPrazo_quandoVendaAPrazo() {
		// Teste funcional: R6 (a prazo, com cliente) - docs/teste-funcional-venda.md
		preparaFechamento("30/60", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0,
				new String[] { "50.00", "50.00" }, new String[] { "1", "1" });

		assertEquals("Venda finalizada com sucesso", retorno);

		// T-8 (isolamento I-2): o vencimento de cada parcela e conferido em relacao a
		// data de hoje, em vez de aceitar qualquer data.
		verify(parcelas).gerarParcela(eq(50.0), eq(0.0), eq(0.0), eq(0.0), eq(50.0), any(Receber.class), eq(0), eq(1),
				any(Timestamp.class), eq(hojeMais(30)));
		verify(parcelas).gerarParcela(eq(50.0), eq(0.0), eq(0.0), eq(0.0), eq(50.0), any(Receber.class), eq(0), eq(2),
				any(Timestamp.class), eq(hojeMais(60)));
		verify(lancamentos, never()).lancamento(any());
		verify(produtos).movimentaEstoque(COD_VENDA, EntradaSaida.SAIDA);
	}

	@Test
	public void fechaVenda_deveCombinarCaixaEParcela_quandoPagamentoMistoAVistaEAPrazo() {
		preparaFechamento("00/30", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(tituloService.busca(2L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		when(usuarios.buscaUsuario(any())).thenReturn(usuario);

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0,
				new String[] { "50.00", "50.00" }, new String[] { "1", "2" });

		assertEquals("Venda finalizada com sucesso", retorno);

		verify(lancamentos).lancamento(argThat(lancamento -> Double.valueOf(50.0).equals(lancamento.getValor())));
		verify(parcelas).gerarParcela(eq(50.0), eq(0.0), eq(0.0), eq(0.0), eq(50.0), any(Receber.class), eq(0), eq(1),
				any(Timestamp.class), any(Date.class));
	}

	@Test
	public void fechaVenda_deveLancarNoCartao_quandoAVistaComCartaoDeDebito() {
		// Teste funcional: R3 (a vista, cartao de debito) - docs/teste-funcional-venda.md
		// T-2: o caso de cartao de credito ja existia; o de debito passa pelo
		// primeiro operando do "||" e nao era verificado em caminho de sucesso.
		preparaFechamento("00", null);
		Optional<Titulo> tituloCartao = Optional.of(titulo(TituloTipo.CARTDEB));
		when(tituloService.busca(1L)).thenReturn(tituloCartao);

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		assertEquals("Venda finalizada com sucesso", retorno);
		verify(cartaoLancamento).lancamento(100.0, tituloCartao);
		verify(caixas, never()).caixaIsAberto();
		verify(lancamentos, never()).lancamento(any());
		verify(produtos).movimentaEstoque(COD_VENDA, EntradaSaida.SAIDA);
	}

	@Test
	public void fechaVenda_deveRegistrarOReceberComDescontoEAcrescimo() {
		// T-4: valor do receber = (produtos + acrescimo) - desconto = 100 + 20 - 10.
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.CARTCRED)));

		service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 10.0, 20.0, new String[] { "100.00" },
				new String[] { "1" });

		verify(receberServ).cadastrar(argThat(receber -> Double.valueOf(110.0).equals(receber.getValor_total())));
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(110.0), eq(10.0), eq(20.0),
				any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveGravarAFormaDePagamentoNaVenda() {
		// T-5: a venda carregada recebe a forma de pagamento escolhida.
		Venda venda = vendaAberta(null);
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(venda);
		PagamentoTipo formaPagamento = new PagamentoTipo();
		formaPagamento.setCodigo(COD_PAGAMENTO);
		formaPagamento.setFormaPagamento("00");
		when(formaPagamentos.busca(COD_PAGAMENTO)).thenReturn(formaPagamento);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.CARTCRED)));

		service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		assertSame(formaPagamento, venda.getPagamentotipo());
		verify(receberServ).cadastrar(argThat(receber -> receber.getVenda() == venda));
	}

	// fechaVenda() - defeitos caracterizados

	@Test
	public void fechaVenda_deveInverterDescontoEAcrescimo_noLancamentoDeCaixaAVista() {
		// DEFEITO: avistaDinheiro() e declarado como (..., Double acre, Double desc)
		// mas e chamado com (..., desc, acre). Com R$ 10 de desconto o caixa recebe
		// 110,00 (100 + 10) enquanto a venda e fechada por 90,00.
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		when(usuarios.buscaUsuario(any())).thenReturn(usuario);

		service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 10.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		verify(lancamentos).lancamento(argThat(lancamento -> Double.valueOf(110.0).equals(lancamento.getValor())));
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(90.0), eq(10.0), eq(0.0),
				any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveInverterDescontoEAcrescimo_noValorDaParcelaAPrazo() {
		// DEFEITO: mesma troca de parametros em aprazo(). Com R$ 20 de acrescimo a
		// parcela sai por 80,00 (100 - 20) enquanto a venda e fechada por 120,00.
		preparaFechamento("30", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 20.0, new String[] { "100.00" },
				new String[] { "1" });

		verify(parcelas).gerarParcela(eq(80.0), eq(0.0), eq(0.0), eq(0.0), eq(80.0), any(Receber.class), eq(0), eq(1),
				any(Timestamp.class), any(Date.class));
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(120.0), eq(0.0), eq(20.0),
				any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveRecusarVendaMistaValida_porSomarSempreAPrimeiraParcela() {
		// DEFEITO: o somatorio de conferencia em avistaDinheiro() usa vlParcelas[i]
		// dentro do laco de indice aux, somando sempre a MESMA parcela. Venda de
		// 60,00 (50,00 a vista + 10,00 a prazo) e recusada porque o service calcula
		// 2 x 50,00 = 100,00.
		preparaFechamento("00/30", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);

		esperaErro("Valor das parcelas diferente do valor total de produtos, verifique", 60.0, 0.0, 0.0,
				new String[] { "50.00", "10.00" }, new String[] { "1", "1" });

		verify(lancamentos, never()).lancamento(any());
		verify(parcelas, never()).gerarParcela(any(), any(), any(), any(), any(), any(), eq(0), eq(1), any(), any());
	}

	@Test
	public void fechaVenda_deveFecharAVendaUmaVezPorFormaDePagamento() {
		// DEFEITO: vendas.fechaVenda() esta dentro do laco de formas de pagamento,
		// entao uma venda em 3x dispara 3 updates (os dois ultimos sem efeito por
		// causa do "and data_finalizado is null" da query).
		preparaFechamento("30/60/90", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 90.0, 0.0, 0.0, new String[] { "30.00", "30.00", "30.00" },
				new String[] { "1", "1", "1" });

		verify(vendas, times(3)).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(90.0), eq(0.0), eq(0.0),
				any(Timestamp.class), any(PagamentoTipo.class));
		verify(produtos, times(1)).movimentaEstoque(COD_VENDA, EntradaSaida.SAIDA);
	}

	@Test
	public void fechaVenda_deveFecharSemRegistrarRecebimento_quandoTituloAVistaNaoEDinheiroNemCartao() {
		// Teste funcional: R5 (a vista, outro tipo de titulo) - docs/teste-funcional-venda.md
		// T-3 / DEFEITO: o if/else-if de fechaVenda() nao tem "else". Um titulo a
		// vista de outro tipo (ex.: PIX cadastrado na tabela titulo_tipo) fecha a
		// venda e baixa o estoque sem lancar nada no caixa nem no cartao.
		preparaFechamento("00", null);
		Titulo pix = titulo(TituloTipo.DIN);
		pix.getTipo().setSigla("PIX");
		when(tituloService.busca(1L)).thenReturn(Optional.of(pix));

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		assertEquals("Venda finalizada com sucesso", retorno);
		verify(lancamentos, never()).lancamento(any());
		verify(cartaoLancamento, never()).lancamento(any(), any());
		verify(parcelas, never()).gerarParcela(any(), any(), any(), any(), any(), any(), eq(0), eq(1), any(), any());
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(100.0), eq(0.0), eq(0.0),
				any(Timestamp.class), any(PagamentoTipo.class));
		verify(produtos).movimentaEstoque(COD_VENDA, EntradaSaida.SAIDA);
	}

	@Test
	public void fechaVenda_deveRatearDescontoEAcrescimoComSinaisTrocados_nasParcelasAPrazo() {
		// T-6 / DEFEITO: desconto (10) e acrescimo (4) sao divididos pelas 2 parcelas
		// (5 e 2), mas como aprazo() recebe os argumentos trocados cada parcela sai
		// por 50 + 5 - 2 = 53,00 (o correto seria 50 + 2 - 5 = 47,00). A venda e
		// fechada por 94,00, mas as parcelas somam 106,00.
		preparaFechamento("30/60", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 10.0, 4.0, new String[] { "50.00", "50.00" },
				new String[] { "1", "1" });

		verify(parcelas).gerarParcela(eq(53.0), eq(0.0), eq(0.0), eq(0.0), eq(53.0), any(Receber.class), eq(0), eq(1),
				any(Timestamp.class), eq(hojeMais(30)));
		verify(parcelas).gerarParcela(eq(53.0), eq(0.0), eq(0.0), eq(0.0), eq(53.0), any(Receber.class), eq(0), eq(2),
				any(Timestamp.class), eq(hojeMais(60)));
		verify(vendas, times(2)).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(94.0), eq(10.0), eq(4.0),
				any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveSubtrairOAcrescimo_noLancamentoDeCaixaAVista() {
		// T-7 / DEFEITO: mesma troca de argumentos em avistaDinheiro(), agora com
		// acrescimo. Com R$ 10 de acrescimo o caixa recebe 90,00 (100 - 10) enquanto
		// a venda e fechada por 110,00.
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		when(usuarios.buscaUsuario(any())).thenReturn(usuario);

		service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 10.0, new String[] { "100.00" },
				new String[] { "1" });

		verify(lancamentos).lancamento(argThat(lancamento -> Double.valueOf(90.0).equals(lancamento.getValor())));
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(110.0), eq(0.0), eq(10.0),
				any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveLancarErroTecnico_quandoTituloNaoExiste() {
		// T-9a / DEFEITO: titulo inexistente gera NoSuchElementException (Optional.get
		// sem verificacao), sem mensagem para o usuario, depois de o receber ja ter
		// sido cadastrado.
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.empty());

		try {
			service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "100.00" },
					new String[] { "1" });
			fail("Deveria ter lancado NoSuchElementException");
		} catch (NoSuchElementException e) {
			// comportamento atual documentado
		}

		verify(receberServ).cadastrar(any(Receber.class));
		verify(vendas, never()).fechaVenda(any(), any(), any(), any(), any(), any(), any());
		verify(produtos, never()).movimentaEstoque(anyLong(), any());
	}

	@Test
	public void fechaVenda_deveLancarErroTecnico_quandoVendaNaoExiste() {
		// T-9b / DEFEITO: venda inexistente gera NullPointerException em
		// vendaIsAberta(), em vez de uma mensagem como "venda nao encontrada".
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(null);

		try {
			service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "100.00" },
					new String[] { "1" });
			fail("Deveria ter lancado NullPointerException");
		} catch (NullPointerException e) {
			// comportamento atual documentado
		}

		verify(formaPagamentos, never()).busca(anyLong());
		verify(receberServ, never()).cadastrar(any(Receber.class));
	}

	// fechaVenda() - falhas de infraestrutura

	@Test
	public void fechaVenda_deveEncapsularExcecao_quandoFalhaAoCadastrarOReceber() {
		preparaFechamento("00", null);
		doThrow(new RuntimeException("falha de conexao")).when(receberServ).cadastrar(any(Receber.class));

		esperaErro("Erro ao fechar a venda, chame o suporte", 100.0, 0.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		verify(tituloService, never()).busca(anyLong());
	}

	@Test
	public void fechaVenda_deveEncapsularExcecao_quandoFalhaAoLancarNoCaixa() {
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		when(usuarios.buscaUsuario(any())).thenReturn(usuario);
		doThrow(new RuntimeException("falha de conexao")).when(lancamentos).lancamento(any());

		esperaErro("Erro ao fechar a venda, chame o suporte", 100.0, 0.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		verify(produtos, never()).movimentaEstoque(anyLong(), any());
	}

	@Test
	public void fechaVenda_devePerderAMensagemDeErro_quandoFalhaAoGerarParcela() {
		// DEFEITO: o catch de aprazo() faz "throw new RuntimeException()" sem causa
		// nem mensagem - a tela mostra um erro vazio e o log nao guarda nada.
		preparaFechamento("30", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		doThrow(new RuntimeException("falha de conexao")).when(parcelas).gerarParcela(any(), any(), any(), any(),
				any(), any(), eq(0), eq(1), any(), any());

		try {
			service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "100.00" },
					new String[] { "1" });
			fail("Deveria ter lancado RuntimeException");
		} catch (RuntimeException e) {
			assertNull(e.getMessage());
			assertNull(e.getCause());
		}

		verify(produtos, never()).movimentaEstoque(anyLong(), any());
	}

	@Test
	public void fechaVenda_deveEncapsularExcecao_quandoFalhaAoAtualizarAVenda() {
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.CARTDEB)));
		doThrow(new RuntimeException("falha de conexao")).when(vendas).fechaVenda(any(), any(), any(), any(), any(),
				any(), any());

		esperaErro("Erro ao fechar a venda, chame o suporte", 100.0, 0.0, 0.0, new String[] { "100.00" },
				new String[] { "1" });

		verify(produtos, never()).movimentaEstoque(anyLong(), any());
	}

	// Teste funcional (Entrega 2, Etapa 2) - classes de equivalencia e
	// valores-limite derivados dos requisitos RF-02 a RF-04.
	// Projeto completo em docs/teste-funcional-venda.md.

	@Test
	public void fechaVenda_deveRecusar_quandoValorDeProdutosLogoAbaixoDeZero() {
		// TF-VEN-01 - limite E2: -0,01
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(vendaAberta(null));

		esperaErro("Venda sem valor, verifique", -0.01, 0.0, 0.0, new String[] { "-0.01" }, new String[] { "1" });

		verify(formaPagamentos, never()).busca(anyLong());
	}

	@Test
	public void fechaVenda_deveAceitar_quandoValorDeProdutosLogoAcimaDeZero() {
		// TF-VEN-02 - limite E2: 0,01
		preparaFechamento("00", null);
		Optional<Titulo> tituloCartao = Optional.of(titulo(TituloTipo.CARTCRED));
		when(tituloService.busca(1L)).thenReturn(tituloCartao);

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 0.01, 0.0, 0.0, new String[] { "0.01" },
				new String[] { "1" });

		assertEquals("Venda finalizada com sucesso", retorno);
		verify(cartaoLancamento).lancamento(0.01, tituloCartao);
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(0.01), eq(0.0), eq(0.0),
				any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveRecusar_quandoParcelaAVistaUmCentavoAbaixoDoTotal() {
		// TF-VEN-03 - limite E6: 99,99 para produtos de 100,00
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);

		esperaErro("Valor das parcelas diferente do valor total de produtos, verifique", 100.0, 0.0, 0.0,
				new String[] { "99.99" }, new String[] { "1" });

		verify(lancamentos, never()).lancamento(any());
	}

	@Test
	public void fechaVenda_deveRecusar_quandoParcelaAVistaUmCentavoAcimaDoTotal() {
		// TF-VEN-04 - limite E6: 100,01 para produtos de 100,00
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(true);

		esperaErro("Valor das parcelas diferente do valor total de produtos, verifique", 100.0, 0.0, 0.0,
				new String[] { "100.01" }, new String[] { "1" });

		verify(lancamentos, never()).lancamento(any());
	}

	@Test
	public void fechaVenda_deveFecharVendaDeValorZero_quandoDescontoIgualAoTotal() {
		// TF-VEN-05 / DEFEITO - limite E3 na fronteira: desconto de 100,00 sobre
		// 100,00. O requisito nao preve venda de valor zero, mas o service nao valida
		// o desconto e fecha a venda com total 0,00.
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.CARTCRED)));

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 100.0, 0.0, new String[] { "0.00" },
				new String[] { "1" });

		assertEquals("Venda finalizada com sucesso", retorno);
		verify(receberServ).cadastrar(argThat(receber -> Double.valueOf(0.0).equals(receber.getValor_total())));
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(0.0), eq(100.0), eq(0.0),
				any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveFecharVendaDeValorNegativo_quandoDescontoMaiorQueOTotal() {
		// TF-VEN-06 / DEFEITO - limite E3 acima da fronteira: desconto de 100,01
		// sobre 100,00 fecha a venda com total negativo. Comparacao com tolerancia
		// porque 100 - 100,01 nao e exatamente -0,01 em double.
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.CARTCRED)));

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 100.01, 0.0, new String[] { "0.00" },
				new String[] { "1" });

		assertEquals("Venda finalizada com sucesso", retorno);
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), doubleThat(v -> Math.abs(v + 0.01) < 1e-6),
				eq(100.01), eq(0.0), any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveAceitarAcrescimoNegativoComoDesconto() {
		// TF-VEN-07 / DEFEITO - classe invalida E4 (I6): acrescimo de -10,00 nao e
		// recusado e reduz o total para 90,00.
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.CARTCRED)));

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, -10.0, new String[] { "90.00" },
				new String[] { "1" });

		assertEquals("Venda finalizada com sucesso", retorno);
		verify(receberServ).cadastrar(argThat(receber -> Double.valueOf(90.0).equals(receber.getValor_total())));
		verify(vendas).fechaVenda(eq(COD_VENDA), eq(VendaSituacao.FECHADA), eq(90.0), eq(0.0), eq(-10.0),
				any(Timestamp.class), any(PagamentoTipo.class));
	}

	@Test
	public void fechaVenda_deveTratarPrazoZeroComoAPrazo_exigindoCliente() {
		// TF-VEN-08 - classe E5 (I7): so "00" e reconhecido como a vista. O prazo "0"
		// cai no ramo a prazo e, sem cliente, a venda e recusada.
		preparaFechamento("0", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		esperaErro("Venda sem cliente, verifique", 100.0, 0.0, 0.0, new String[] { "100.00" }, new String[] { "1" });

		verify(caixas, never()).caixaIsAberto();
		verify(lancamentos, never()).lancamento(any());
	}

	@Test
	public void fechaVenda_deveLancarErroTecnico_quandoFaltamTitulosParaAsFormasDePagamento() {
		// TF-VEN-09 / DEFEITO - classe invalida E10 (I14): 2 formas de pagamento e 1
		// titulo. A 1a parcela e gerada e a venda e marcada como fechada antes de
		// estourar ArrayIndexOutOfBoundsException na 2a.
		preparaFechamento("30/60", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		try {
			service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0, new String[] { "50.00", "50.00" },
					new String[] { "1" });
			fail("Deveria ter lancado ArrayIndexOutOfBoundsException");
		} catch (ArrayIndexOutOfBoundsException e) {
			// comportamento atual documentado
		}

		verify(parcelas).gerarParcela(eq(50.0), eq(0.0), eq(0.0), eq(0.0), eq(50.0), any(Receber.class), eq(0), eq(1),
				any(Timestamp.class), eq(hojeMais(30)));
		verify(vendas, times(1)).fechaVenda(any(), any(), any(), any(), any(), any(), any());
		verify(produtos, never()).movimentaEstoque(anyLong(), any());
	}

	@Test
	public void addProduto_deveLancarErroTecnico_quandoVendaNaoExiste() {
		// TF-VEN-10 / DEFEITO - classe invalida E11 (I16): verificaSituacao devolve
		// null para venda inexistente e o equals() fora do try estoura NPE.
		try {
			service.addProduto(COD_VENDA, COD_PRODUTO, 0.0);
			fail("Deveria ter lancado NullPointerException");
		} catch (NullPointerException e) {
			// comportamento atual documentado
		}

		verify(vendaProdutos, never()).salvar(any());
	}

	@Test
	public void busca_deveConsultarVendasFechadas_quandoSituacaoEmMinusculas() {
		// TF-VEN-11 - classe E12 (I17): a comparacao diferencia maiusculas; "aberta"
		// nao e "ABERTA" e a busca devolve as vendas FECHADAS.
		Pageable pageable = PageRequest.of(0, 10);
		Page<Venda> fechadas = new PageImpl<>(Collections.singletonList(new Venda()));
		when(vendas.findBySituacaoEquals(VendaSituacao.FECHADA, pageable)).thenReturn(fechadas);

		assertSame(fechadas, service.busca(new VendaFilter(), "aberta", pageable));
		verify(vendas, never()).findBySituacaoEquals(eq(VendaSituacao.ABERTA), any());
	}

	@Test
	public void busca_deveLancarErroTecnico_quandoSituacaoNula() {
		// TF-VEN-12 / DEFEITO - classe invalida E12 (I18): situacao nula estoura NPE.
		try {
			service.busca(new VendaFilter(), null, PageRequest.of(0, 10));
			fail("Deveria ter lancado NullPointerException");
		} catch (NullPointerException e) {
			// comportamento atual documentado
		}

		verify(vendas, never()).findBySituacaoEquals(any(), any());
		verify(vendas, never()).findByCodigoIn(any(), any());
	}

	// fixtures e utilitarios

	/**
	 * Deixa a venda pronta para o fechamento: aberta, com a forma de pagamento
	 * informada (ex. "00", "30/60") e o cliente indicado.
	 */
	private PagamentoTipo preparaFechamento(String formaPagamento, Pessoa pessoa) {
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(vendaAberta(pessoa));

		PagamentoTipo pagamento = new PagamentoTipo();
		pagamento.setCodigo(COD_PAGAMENTO);
		pagamento.setFormaPagamento(formaPagamento);
		when(formaPagamentos.busca(COD_PAGAMENTO)).thenReturn(pagamento);

		return pagamento;
	}

	private void esperaErro(String mensagem, Double vlProdutos, Double desconto, Double acrescimo, String[] vlParcelas,
			String[] titulos) {
		try {
			service.fechaVenda(COD_VENDA, COD_PAGAMENTO, vlProdutos, desconto, acrescimo, vlParcelas, titulos);
			fail("Deveria ter lancado RuntimeException: " + mensagem);
		} catch (RuntimeException e) {
			assertEquals(mensagem, e.getMessage());
		}
	}

	private Venda vendaAberta(Pessoa pessoa) {
		Venda venda = new Venda();
		venda.setCodigo(COD_VENDA);
		venda.setSituacao(VendaSituacao.ABERTA);
		venda.setValor_produtos(0.00);
		venda.setPessoa(pessoa);
		return venda;
	}

	/**
	 * Data de vencimento esperada: hoje + dias, no mesmo formato que DataAtual
	 * entrega ao service (java.sql.Date, fuso padrao da JVM).
	 */
	private Date hojeMais(int dias) {
		return Date.valueOf(LocalDate.now().plusDays(dias));
	}

	private Titulo titulo(TituloTipo sigla) {
		net.originmobi.pdv.model.TituloTipo tipo = new net.originmobi.pdv.model.TituloTipo();
		tipo.setSigla(sigla.toString());

		Titulo titulo = new Titulo();
		titulo.setCodigo(1L);
		titulo.setTipo(tipo);
		return titulo;
	}
}
