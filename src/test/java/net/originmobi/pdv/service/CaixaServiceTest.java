package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
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

import net.originmobi.pdv.filter.CaixaFilter;
import net.originmobi.pdv.model.Caixa;
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
}
