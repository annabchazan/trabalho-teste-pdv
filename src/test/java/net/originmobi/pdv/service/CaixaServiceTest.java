package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.filter.BancoFilter;
import net.originmobi.pdv.filter.CaixaFilter;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import net.originmobi.pdv.singleton.Aplicacao;

/**
 * Testes unitários de CaixaService. As três dependências (CaixaRepository,
 * UsuarioService e CaixaLancamentoService) são mocks do Mockito.
 *
 * TU-CX-01 a 11 vêm da Entrega 1. Os demais vêm do projeto funcional da
 * Entrega 2 (casos TF-CX). Testes marcados com DEFEITO registram o
 * comportamento atual do código, que está errado, e passam enquanto o
 * defeito existir.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaixaServiceTest {

	private static final String USUARIO = "gerente";
	private static final String SENHA = "123";

	@Mock
	private CaixaRepository caixas;

	@Mock
	private UsuarioService usuarios;

	@Mock
	private CaixaLancamentoService lancamentos;

	@InjectMocks
	private CaixaService service;

	private Usuario usuario;

	@Before
	public void setUp() {
		// Aplicacao é um singleton que guarda o usuário do primeiro teste que o
		// cria. Zerar o campo a cada teste evita que um teste dependa de outro.
		ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null);
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(USUARIO, SENHA));

		usuario = new Usuario();
		usuario.setCodigo(1L);
		usuario.setSenha(new BCryptPasswordEncoder().encode(SENHA));
	}

	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
		ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null);
	}

	// caixaIsAberto()

	@Test
	public void caixaIsAberto_deveRetornarVerdadeiro_quandoRepositorioEncontrarCaixa() {
		// TU-CX-01
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

		assertTrue(service.caixaIsAberto());
		verify(caixas).caixaAberto();
	}

	@Test
	public void caixaIsAberto_deveRetornarFalso_quandoRepositorioNaoEncontrarCaixa() {
		// TU-CX-02
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		assertFalse(service.caixaIsAberto());
		verify(caixas).caixaAberto();
	}

	// listarCaixas()

	@Test
	public void listarCaixas_deveNormalizarDataEConsultarPorData_quandoFiltroPossuiData() {
		// TU-CX-03 / TF-CX-22
		CaixaFilter filter = new CaixaFilter();
		filter.setData_cadastro("2026/09/20");
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixasPorDataAbertura(Date.valueOf("2026-09-20"))).thenReturn(resultado);

		List<Caixa> retorno = service.listarCaixas(filter);

		assertSame(resultado, retorno);
		assertEquals("2026-09-20", filter.getData_cadastro());
		verify(caixas, never()).listaCaixasAbertos();
	}

	@Test
	public void listarCaixas_deveListarCaixasAbertos_quandoFiltroNaoPossuiData() {
		// TU-CX-04 / TF-CX-24
		CaixaFilter filter = new CaixaFilter();
		filter.setData_cadastro("");
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.listaCaixasAbertos()).thenReturn(resultado);

		assertSame(resultado, service.listarCaixas(filter));
		verify(caixas, never()).buscaCaixasPorDataAbertura(any());
	}

	@Test
	public void listarCaixas_deveConsultarPorData_quandoDataJaVemComHifen() {
		// TF-CX-23
		CaixaFilter filter = new CaixaFilter();
		filter.setData_cadastro("2026-09-20");
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixasPorDataAbertura(Date.valueOf("2026-09-20"))).thenReturn(resultado);

		assertSame(resultado, service.listarCaixas(filter));
	}

	@Test
	public void listarCaixas_deveListarCaixasAbertos_quandoFiltroTemDataNula() {
		// TF-CX-25
		CaixaFilter filter = new CaixaFilter();
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.listaCaixasAbertos()).thenReturn(resultado);

		assertSame(resultado, service.listarCaixas(filter));
	}

	@Test
	public void listarCaixas_deveLancarExcecaoTecnica_quandoDataEstaNoFormatoBrasileiro() {
		// TF-CX-26 / DEFEITO D-CX-04: "20/09/2026" vira "20-09-2026" e
		// Date.valueOf estoura IllegalArgumentException sem mensagem amigável.
		CaixaFilter filter = new CaixaFilter();
		filter.setData_cadastro("20/09/2026");

		try {
			service.listarCaixas(filter);
			fail("Deveria ter lançado IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			verify(caixas, never()).buscaCaixasPorDataAbertura(any());
		}
	}

	// cadastro() - abertura de caixa, cofre e banco

	@Test
	public void cadastro_deveImpedirAbertura_quandoExisteCaixaAnteriorAberto() {
		// TU-CX-05 / TF-CX-02
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, "");
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

		esperaErro("Existe caixa de dias anteriores em aberto, favor verifique", () -> service.cadastro(caixa));

		verify(caixas, never()).save(any(Caixa.class));
		verify(lancamentos, never()).lancamento(any());
	}

	@Test
	public void cadastro_deveUsarZeroEDescricaoPadrao_quandoAberturaNaoInformaValorNemDescricao() {
		// TU-CX-06 / TF-CX-04 e TF-CX-08 (CAIXA)
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, null, "");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		service.cadastro(caixa);

		assertEquals(Double.valueOf(0.0), caixa.getValor_abertura());
		assertEquals(Double.valueOf(0.0), caixa.getValor_total());
		assertEquals("Caixa diário", caixa.getDescricao());
		verify(caixas).save(caixa);
		verify(lancamentos, never()).lancamento(any());
	}

	@Test
	public void cadastro_deveCriarLancamentoInicial_quandoValorDeAberturaForPositivo() {
		// TU-CX-07 / TF-CX-01 e TF-CX-09 (CAIXA)
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 50.0, "Caixa da manhã");
		caixa.setCodigo(7L);
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		LocalDate antes = LocalDate.now();
		Long codigo = service.cadastro(caixa);
		LocalDate depois = LocalDate.now();

		assertEquals(Long.valueOf(7L), codigo);
		assertEquals("Caixa da manhã", caixa.getDescricao());
		assertSame(usuario, caixa.getUsuario());
		assertNotNull(caixa.getData_cadastro());
		LocalDate dataCadastro = new Date(caixa.getData_cadastro().getTime()).toLocalDate();
		assertTrue(!dataCadastro.isBefore(antes) && !dataCadastro.isAfter(depois));
		verify(caixas).save(caixa);

		CaixaLancamento lancamento = lancamentoGerado();
		assertEquals("Abertura de caixa", lancamento.getObservacao());
		assertEquals(Double.valueOf(50.0), lancamento.getValor());
		assertEquals(TipoLancamento.SALDOINICIAL, lancamento.getTipo());
		assertEquals(EstiloLancamento.ENTRADA, lancamento.getEstilo());
		assertSame(caixa, lancamento.getCaixa().get());
		assertSame(usuario, lancamento.getUsuario());
	}

	@Test
	public void cadastro_deveRecusarValorDeAberturaNegativo() {
		// TU-CX-08 / TF-CX-05 (limite inferior inválido: -0,01)
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, -0.01, "");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		esperaErro("Valor informado é inválido", () -> service.cadastro(caixa));

		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void cadastro_naoDeveGerarLancamento_quandoValorDeAberturaForZero() {
		// TF-CX-06 (limite: 0,00)
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, "");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		service.cadastro(caixa);

		assertEquals(Double.valueOf(0.0), caixa.getValor_total());
		verify(caixas).save(caixa);
		verify(lancamentos, never()).lancamento(any());
	}

	@Test
	public void cadastro_deveGerarLancamento_quandoValorDeAberturaForUmCentavo() {
		// TF-CX-07 (limite: 0,01)
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.01, "");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		service.cadastro(caixa);

		assertEquals(Double.valueOf(0.01), lancamentoGerado().getValor());
	}

	@Test
	public void cadastro_devePermitirAbrirCofre_mesmoComCaixaAberto() {
		// TF-CX-03 e TF-CX-09: a regra de caixa anterior aberto vale só para CAIXA
		Caixa cofre = novoCaixa(CaixaTipo.COFRE, 100.0, "");
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		service.cadastro(cofre);

		assertEquals("Cofre", cofre.getDescricao());
		assertEquals("Abertura de cofre", lancamentoGerado().getObservacao());
		verify(caixas, never()).caixaAberto();
		verify(caixas).save(cofre);
	}

	@Test
	public void cadastro_deveManterDescricaoInformada_quandoCofreTemDescricao() {
		// TF-CX-08 (COFRE com descrição)
		Caixa cofre = novoCaixa(CaixaTipo.COFRE, 0.0, "Cofre da loja");
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		service.cadastro(cofre);

		assertEquals("Cofre da loja", cofre.getDescricao());
	}

	@Test
	public void cadastro_deveLimparAgenciaEConta_quandoAbreBanco() {
		// TF-CX-10 e TF-CX-09 (BANCO)
		Caixa banco = novoCaixa(CaixaTipo.BANCO, 200.0, "");
		banco.setAgencia("12.34-5");
		banco.setConta("000.123-4");
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		service.cadastro(banco);

		assertEquals("12345", banco.getAgencia());
		assertEquals("0001234", banco.getConta());
		assertEquals("Banco", banco.getDescricao());
		assertEquals("Abertura de banco", lancamentoGerado().getObservacao());
	}

	@Test
	public void cadastro_deveManterDescricaoInformada_quandoBancoTemDescricao() {
		// TF-CX-08 (BANCO com descrição)
		Caixa banco = novoCaixa(CaixaTipo.BANCO, 0.0, "Banco do Brasil");
		banco.setAgencia("1234");
		banco.setConta("5678");
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		service.cadastro(banco);

		assertEquals("Banco do Brasil", banco.getDescricao());
	}

	@Test
	public void cadastro_deveInformarErroDeAbertura_quandoRepositorioFalhaAoSalvar() {
		// TF-CX-11
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 10.0, "");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(caixas.save(caixa)).thenThrow(new RuntimeException("falha no banco"));

		esperaErro("Erro no processo de abertura, chame o suporte técnico", () -> service.cadastro(caixa));

		verify(lancamentos, never()).lancamento(any());
	}

	@Test
	public void cadastro_deveInformarErro_quandoLancamentoInicialFalha() {
		// TF-CX-12
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 10.0, "");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(lancamentos.lancamento(any())).thenThrow(new RuntimeException("falha no lançamento"));

		esperaErro("Erro no processo, chame o suporte", () -> service.cadastro(caixa));
	}

	@Test
	public void cadastro_deveLancarNullPointer_quandoDescricaoNaoInformada() {
		// TF-CX-13 / DEFEITO D-CX-01: descrição nula não recebe o valor padrão;
		// getDescricao().isEmpty() estoura NullPointerException.
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, null);
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		try {
			service.cadastro(caixa);
			fail("Deveria ter lançado NullPointerException");
		} catch (NullPointerException e) {
			verify(caixas, never()).save(any(Caixa.class));
		}
	}

	// fechaCaixa()

	@Test
	public void fechaCaixa_deveSolicitarSenha_quandoSenhaNaoForInformada() {
		// TU-CX-09 / TF-CX-14
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		assertEquals("Favor, informe a senha", service.fechaCaixa(1L, ""));
		verify(caixas, never()).findById(any());
		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixa_deveInformarSenhaIncorreta_quandoSenhaNaoConfere() {
		// TU-CX-10 / TF-CX-15
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		assertEquals("Senha incorreta, favor verifique", service.fechaCaixa(1L, "senha-errada"));
		verify(caixas, never()).findById(any());
		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixa_deveSalvarDataEValorDeFechamento_quandoSenhaForValida() {
		// TU-CX-11 / TF-CX-16
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa diário");
		caixa.setValor_total(75.0);
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		long antes = System.currentTimeMillis();
		assertEquals("Caixa fechado com sucesso", service.fechaCaixa(1L, SENHA));
		long depois = System.currentTimeMillis();

		assertEquals(Double.valueOf(75.0), caixa.getValor_fechamento());
		assertNotNull(caixa.getData_fechamento());
		assertTrue(caixa.getData_fechamento().getTime() >= antes && caixa.getData_fechamento().getTime() <= depois);
		verify(caixas).save(caixa);
	}

	@Test
	public void fechaCaixa_deveImpedirFechamento_quandoCaixaJaEstaFechado() {
		// TF-CX-17
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa diário");
		caixa.setData_fechamento(new java.sql.Timestamp(System.currentTimeMillis()));
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		esperaErro("Caixa já esta fechado", () -> service.fechaCaixa(1L, SENHA));

		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixa_deveFecharComZero_quandoCaixaNaoTemValorTotal() {
		// TF-CX-18
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa diário");
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		assertEquals("Caixa fechado com sucesso", service.fechaCaixa(1L, SENHA));

		assertEquals(Double.valueOf(0.0), caixa.getValor_fechamento());
	}

	@Test
	public void fechaCaixa_deveInformarErro_quandoRepositorioFalhaAoSalvar() {
		// TF-CX-19
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa diário");
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));
		when(caixas.save(caixa)).thenThrow(new RuntimeException("falha no banco"));

		esperaErro("Ocorreu um erro ao fechar o caixa, chame o suporte", () -> service.fechaCaixa(1L, SENHA));
	}

	@Test
	public void fechaCaixa_deveLancarNoSuchElement_quandoCaixaNaoExiste() {
		// TF-CX-20 / DEFEITO D-CX-02: caixa inexistente gera
		// NoSuchElementException ("No value present") em vez de uma mensagem.
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(caixas.findById(99L)).thenReturn(Optional.empty());

		try {
			service.fechaCaixa(99L, SENHA);
			fail("Deveria ter lançado NoSuchElementException");
		} catch (NoSuchElementException e) {
			verify(caixas, never()).save(any(Caixa.class));
		}
	}

	@Test
	public void fechaCaixa_deveLancarNullPointer_quandoSenhaNula() {
		// TF-CX-21 / DEFEITO D-CX-03: senha nula estoura NullPointerException
		// em senha.equals("") em vez de pedir a senha.
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);

		try {
			service.fechaCaixa(1L, null);
			fail("Deveria ter lançado NullPointerException");
		} catch (NullPointerException e) {
			verify(caixas, never()).save(any(Caixa.class));
		}
	}

	// listaBancosAbertosTipoFilterBanco()

	@Test
	public void listaBancosAbertosTipoFilterBanco_deveConsultarPorTipoEData_quandoFiltroPossuiData() {
		// TF-CX-27
		BancoFilter filter = new BancoFilter();
		filter.setData_cadastro("2026/09/20");
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixaTipoData(CaixaTipo.BANCO, Date.valueOf("2026-09-20"))).thenReturn(resultado);

		assertSame(resultado, service.listaBancosAbertosTipoFilterBanco(CaixaTipo.BANCO, filter));
		assertEquals("2026-09-20", filter.getData_cadastro());
	}

	@Test
	public void listaBancosAbertosTipoFilterBanco_deveListarBancosAbertos_quandoFiltroNaoPossuiData() {
		// TF-CX-28
		BancoFilter filter = new BancoFilter();
		filter.setData_cadastro("");
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixaTipo(CaixaTipo.BANCO)).thenReturn(resultado);

		assertSame(resultado, service.listaBancosAbertosTipoFilterBanco(CaixaTipo.BANCO, filter));
		verify(caixas, never()).buscaCaixaTipoData(any(), any());
	}

	@Test
	public void listaBancosAbertosTipoFilterBanco_deveListarBancosAbertos_quandoFiltroTemDataNula() {
		// TE-CX-01: aresta falsa de "data != null" (linha 195), que o projeto
		// funcional não exercitou
		BancoFilter filter = new BancoFilter();
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixaTipo(CaixaTipo.BANCO)).thenReturn(resultado);

		assertSame(resultado, service.listaBancosAbertosTipoFilterBanco(CaixaTipo.BANCO, filter));
		verify(caixas, never()).buscaCaixaTipoData(any(), any());
	}

	// consultas que repassam ao repositório

	@Test
	public void buscaCaixaUsuario_deveRetornarCaixaAbertoDoUsuario() {
		// TF-CX-29
		Caixa caixa = new Caixa();
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(caixas.findByCaixaAbertoUsuario(1L)).thenReturn(caixa);

		Optional<Caixa> retorno = service.buscaCaixaUsuario(USUARIO);

		assertTrue(retorno.isPresent());
		assertSame(caixa, retorno.get());
	}

	@Test
	public void buscaCaixaUsuario_deveRetornarVazio_quandoUsuarioNaoTemCaixaAberto() {
		// TF-CX-29
		when(usuarios.buscaUsuario(USUARIO)).thenReturn(usuario);
		when(caixas.findByCaixaAbertoUsuario(1L)).thenReturn(null);

		assertFalse(service.buscaCaixaUsuario(USUARIO).isPresent());
	}

	@Test
	public void consultas_deveRepassarResultadoDoRepositorio() {
		// TF-CX-30
		List<Caixa> todos = Collections.singletonList(new Caixa());
		List<Caixa> abertos = Collections.singletonList(new Caixa());
		List<Caixa> bancos = Collections.singletonList(new Caixa());
		List<Caixa> cofres = Collections.singletonList(new Caixa());
		Optional<Caixa> aberto = Optional.of(new Caixa());
		Optional<Caixa> porCodigo = Optional.of(new Caixa());
		when(caixas.findByCodigoOrdenado()).thenReturn(todos);
		when(caixas.caixasAbertos()).thenReturn(abertos);
		when(caixas.buscaBancos(CaixaTipo.BANCO)).thenReturn(bancos);
		when(caixas.buscaCaixaTipo(CaixaTipo.COFRE)).thenReturn(cofres);
		when(caixas.caixaAberto()).thenReturn(aberto);
		when(caixas.findById(5L)).thenReturn(porCodigo);

		assertSame(todos, service.listaTodos());
		assertSame(abertos, service.caixasAbertos());
		assertSame(bancos, service.listaBancos());
		assertSame(cofres, service.listaCaixasAbertosTipo(CaixaTipo.COFRE));
		assertSame(aberto, service.caixaAberto());
		assertSame(porCodigo, service.busca(5L));
	}

	private Caixa novoCaixa(CaixaTipo tipo, Double valorAbertura, String descricao) {
		Caixa caixa = new Caixa();
		caixa.setTipo(tipo);
		caixa.setValor_abertura(valorAbertura);
		caixa.setDescricao(descricao);
		return caixa;
	}

	private CaixaLancamento lancamentoGerado() {
		ArgumentCaptor<CaixaLancamento> captor = ArgumentCaptor.forClass(CaixaLancamento.class);
		verify(lancamentos).lancamento(captor.capture());
		return captor.getValue();
	}

	private void esperaErro(String mensagem, Runnable operacao) {
		try {
			operacao.run();
			fail("Deveria ter lançado RuntimeException: " + mensagem);
		} catch (RuntimeException e) {
			assertEquals(mensagem, e.getMessage());
		}
	}
}
