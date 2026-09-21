package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
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
		when(vendas.findByCodigoEquals(COD_VENDA)).thenReturn(vendaAberta(null));

		esperaErro("Venda sem valor, verifique", 0.0, 0.0, 0.0, new String[] { "0.00" }, new String[] { "1" });

		verify(formaPagamentos, never()).busca(anyLong());
	}

	@Test
	public void fechaVenda_deveLancarExcecao_quandoAVistaEmDinheiroSemCaixaAberto() {
		preparaFechamento("00", null);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));
		when(caixas.caixaIsAberto()).thenReturn(false);

		esperaErro("nenhum caixa aberto", 100.0, 0.0, 0.0, new String[] { "100.00" }, new String[] { "1" });

		verify(lancamentos, never()).lancamento(any());
		verify(vendas, never()).fechaVenda(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	public void fechaVenda_deveLancarExcecao_quandoVendaAPrazoSemCliente() {
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
		preparaFechamento("30/60", cliente);
		when(tituloService.busca(1L)).thenReturn(Optional.of(titulo(TituloTipo.DIN)));

		String retorno = service.fechaVenda(COD_VENDA, COD_PAGAMENTO, 100.0, 0.0, 0.0,
				new String[] { "50.00", "50.00" }, new String[] { "1", "1" });

		assertEquals("Venda finalizada com sucesso", retorno);

		verify(parcelas).gerarParcela(eq(50.0), eq(0.0), eq(0.0), eq(0.0), eq(50.0), any(Receber.class), eq(0), eq(1),
				any(Timestamp.class), any(Date.class));
		verify(parcelas).gerarParcela(eq(50.0), eq(0.0), eq(0.0), eq(0.0), eq(50.0), any(Receber.class), eq(0), eq(2),
				any(Timestamp.class), any(Date.class));
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

	private Titulo titulo(TituloTipo sigla) {
		net.originmobi.pdv.model.TituloTipo tipo = new net.originmobi.pdv.model.TituloTipo();
		tipo.setSigla(sigla.toString());

		Titulo titulo = new Titulo();
		titulo.setCodigo(1L);
		titulo.setTipo(tipo);
		return titulo;
	}
}
