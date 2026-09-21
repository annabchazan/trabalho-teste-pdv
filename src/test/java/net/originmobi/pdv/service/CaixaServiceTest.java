package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import net.originmobi.pdv.filter.CaixaFilter;
import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;

@RunWith(MockitoJUnitRunner.class)
public class CaixaServiceTest {

	@Mock
	private CaixaRepository caixas;

	@Mock
	private UsuarioService usuarios;

	@Mock
	private CaixaLancamentoService lancamentos;

	@InjectMocks
	private CaixaService service;

	@Test
	public void caixaIsAberto_deveRetornarVerdadeiro_quandoRepositorioEncontrarCaixa() {
		Caixa caixa = new Caixa();
		when(caixas.caixaAberto()).thenReturn(Optional.of(caixa));

		assertEquals(true, service.caixaIsAberto());
		verify(caixas).caixaAberto();
	}

	@Test
	public void caixaIsAberto_deveRetornarFalso_quandoRepositorioNaoEncontrarCaixa() {
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		assertEquals(false, service.caixaIsAberto());
		verify(caixas).caixaAberto();
	}

	@Test
	public void listarCaixas_deveNormalizarDataEConsultarPorData_quandoFiltroPossuiData() {
		CaixaFilter filter = new CaixaFilter();
		filter.setData_cadastro("2026/09/20");
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixasPorDataAbertura(Date.valueOf("2026-09-20"))).thenReturn(resultado);

		List<Caixa> retorno = service.listarCaixas(filter);

		assertSame(resultado, retorno);
		assertEquals("2026-09-20", filter.getData_cadastro());
		verify(caixas).buscaCaixasPorDataAbertura(Date.valueOf("2026-09-20"));
	}

	@Test
	public void listarCaixas_deveListarCaixasAbertos_quandoFiltroNaoPossuiData() {
		CaixaFilter filter = new CaixaFilter();
		filter.setData_cadastro("");
		List<Caixa> resultado = Collections.singletonList(new Caixa());
		when(caixas.listaCaixasAbertos()).thenReturn(resultado);

		List<Caixa> retorno = service.listarCaixas(filter);

		assertSame(resultado, retorno);
		verify(caixas).listaCaixasAbertos();
	}

	@Test
	public void cadastro_deveImpedirAbertura_quandoExisteCaixaAnteriorAberto() {
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, "");
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

		esperaErro("Existe caixa de dias anteriores em aberto, favor verifique", () -> service.cadastro(caixa));

		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void cadastro_deveUsarZeroEDescricaoPadrao_quandoAberturaNaoInformaValorNemDescricao() {
		configuraUsuarioAtual();
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, null, "");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		service.cadastro(caixa);

		assertEquals(Double.valueOf(0.0), caixa.getValor_abertura());
		assertEquals(Double.valueOf(0.0), caixa.getValor_total());
		assertEquals("Caixa diário", caixa.getDescricao());
		verify(caixas).save(caixa);
		verify(lancamentos, never()).lancamento(any());
	}

	@Test
	public void cadastro_deveCriarLancamentoInicial_quandoValorDeAberturaForPositivo() {
		configuraUsuarioAtual();
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 25.0, "Caixa da manhã");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		service.cadastro(caixa);

		verify(caixas).save(caixa);
		verify(lancamentos).lancamento(any());
	}

	@Test
	public void cadastro_deveRecusarValorDeAberturaNegativo() {
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, -1.0, "");
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		esperaErro("Valor informado é inválido", () -> service.cadastro(caixa));

		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixa_deveSolicitarSenha_quandoSenhaNaoForInformada() {
		configuraUsuarioAtual();

		assertEquals("Favor, informe a senha", service.fechaCaixa(1L, ""));
		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixa_deveInformarSenhaIncorreta_quandoSenhaNaoConfere() {
		configuraUsuarioAtual();

		assertEquals("Senha incorreta, favor verifique", service.fechaCaixa(1L, "senha-errada"));
		verify(caixas, never()).findById(any());
	}

	@Test
	public void fechaCaixa_deveSalvarDataEValorDeFechamento_quandoSenhaForValida() {
		configuraUsuarioAtual();
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa diário");
		caixa.setValor_total(75.0);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		assertEquals("Caixa fechado com sucesso", service.fechaCaixa(1L, "123"));
		assertEquals(Double.valueOf(75.0), caixa.getValor_fechamento());
		verify(caixas).save(caixa);
	}

	private void configuraUsuarioAtual() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("gerente", "123"));
		Usuario usuario = new Usuario();
		usuario.setSenha(new BCryptPasswordEncoder().encode("123"));
		when(usuarios.buscaUsuario("gerente")).thenReturn(usuario);
	}

	private Caixa novoCaixa(CaixaTipo tipo, Double valorAbertura, String descricao) {
		Caixa caixa = new Caixa();
		caixa.setTipo(tipo);
		caixa.setValor_abertura(valorAbertura);
		caixa.setDescricao(descricao);
		return caixa;
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
