package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.filter.BancoFilter;
import net.originmobi.pdv.filter.CaixaFilter;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.singleton.Aplicacao;

/**
 * Testes de integração de CaixaService (TI-CX-01 a 10). Usam o contexto
 * Spring, os serviços e repositórios reais (CaixaRepository, UsuarioService,
 * CaixaLancamentoService) e o MySQL do docker-compose (pdv-db), com o esquema
 * e o trigger tr_atualizaValoresCaixa_AFTER_INSERT criados pelo Flyway.
 *
 * Massa de dados: o usuário "gerente" (código 1, senha 123) que vem na carga
 * inicial. Cada caso apaga, no fim, os caixas e lançamentos criados depois do
 * maior código existente no início do caso. Um caixa do tipo CAIXA que já
 * estiver aberto no banco é marcado como fechado durante o caso e restaurado
 * depois, como em VendaServiceIT.
 *
 * Não há @Transactional na classe de teste: a transação de cadastro() é a
 * real, para que o trigger rode e o rollback (TI-CX-06) seja verificado.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CaixaServiceIT {

	private static final long COD_USUARIO_GERENTE = 1L;
	private static final double DELTA = 0.001;

	@Autowired
	private CaixaService service;

	@Autowired
	private JdbcTemplate jdbc;

	private long maiorCodigoAntes;
	private final List<Map<String, Object>> caixasOriginaisAbertos = new ArrayList<>();

	@Before
	public void setUp() {
		ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null);
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("gerente", null));

		maiorCodigoAntes = jdbc.queryForObject("select coalesce(max(codigo), 0) from caixa", Long.class);
		fechaCaixasOriginais();
	}

	@After
	public void tearDown() {
		try {
			jdbc.update("delete from caixa_lancamento where caixa_codigo > ?", maiorCodigoAntes);
			jdbc.update("delete from caixa where codigo > ?", maiorCodigoAntes);
		} finally {
			restauraCaixasOriginais();
			SecurityContextHolder.clearContext();
			ReflectionTestUtils.setField(Aplicacao.class, "aplicacao", null);
		}
	}

	// TI-CX-01: abrir caixa com valor grava o caixa, o lançamento e o total (trigger)

	@Test
	public void cadastro_deveGravarCaixaELancamentoEAtualizarTotalPeloTrigger() {
		Long codigo = service.cadastro(novoCaixa(CaixaTipo.CAIXA, 50.0, "Caixa IT"));

		assertNotNull(codigo);
		Map<String, Object> caixa = caixa(codigo);
		assertEquals("Caixa IT", caixa.get("descricao"));
		assertEquals("CAIXA", caixa.get("tipo"));
		assertEquals(COD_USUARIO_GERENTE, ((Number) caixa.get("usuario_codigo")).longValue());
		assertEquals(Date.valueOf(LocalDate.now()), caixa.get("data_cadastro"));
		assertNull(caixa.get("data_fechamento"));
		// o service não grava o total; quem soma é o trigger do lançamento
		assertEquals(50.0, ((Number) caixa.get("valor_total")).doubleValue(), DELTA);
		assertEquals(50.0, ((Number) caixa.get("valor_entrada")).doubleValue(), DELTA);

		List<Map<String, Object>> lancamentos = lancamentos(codigo);
		assertEquals(1, lancamentos.size());
		assertEquals("Abertura de caixa", lancamentos.get(0).get("observacao"));
		assertEquals("SALDOINICIAL", lancamentos.get(0).get("tipo"));
		assertEquals("ENTRADA", lancamentos.get(0).get("estilo"));
		assertEquals(50.0, ((Number) lancamentos.get(0).get("valor")).doubleValue(), DELTA);
		assertTrue(service.caixaIsAberto());
	}

	// TI-CX-02: a consulta real de caixa aberto impede abrir um segundo caixa

	@Test
	public void cadastro_deveImpedirSegundoCaixa_quandoJaExisteCaixaAbertoNoBanco() {
		service.cadastro(novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa IT"));

		try {
			service.cadastro(novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa IT 2"));
			fail("Deveria ter impedido a abertura");
		} catch (RuntimeException e) {
			assertEquals("Existe caixa de dias anteriores em aberto, favor verifique", e.getMessage());
		}
		assertEquals(1, caixasCriados());
	}

	// TI-CX-03: banco sem valor grava agência e conta só com dígitos, sem lançamento

	@Test
	public void cadastro_deveGravarBancoComAgenciaEContaLimpasESemLancamento() {
		Caixa banco = novoCaixa(CaixaTipo.BANCO, 0.0, "");
		banco.setAgencia("12.34-5");
		banco.setConta("000.123-4");

		Long codigo = service.cadastro(banco);

		Map<String, Object> gravado = caixa(codigo);
		assertEquals("Banco", gravado.get("descricao"));
		assertEquals("12345", gravado.get("agencia"));
		assertEquals("0001234", gravado.get("conta"));
		assertEquals(0.0, ((Number) gravado.get("valor_total")).doubleValue(), DELTA);
		assertTrue(lancamentos(codigo).isEmpty());
	}

	// TI-CX-04: fechar com a senha real do gerente (BCrypt do banco) usa o total do trigger

	@Test
	public void fechaCaixa_deveFecharComValorTotalCalculadoPeloTrigger() {
		Long codigo = service.cadastro(novoCaixa(CaixaTipo.CAIXA, 80.0, "Caixa IT"));

		assertEquals("Caixa fechado com sucesso", service.fechaCaixa(codigo, "123"));

		Map<String, Object> caixa = caixa(codigo);
		assertNotNull(caixa.get("data_fechamento"));
		assertEquals(80.0, ((Number) caixa.get("valor_fechamento")).doubleValue(), DELTA);
		assertFalse(service.caixaIsAberto());
	}

	// TI-CX-05: senha errada não fecha o caixa no banco

	@Test
	public void fechaCaixa_naoDeveFecharNoBanco_quandoSenhaIncorreta() {
		Long codigo = service.cadastro(novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa IT"));

		assertEquals("Senha incorreta, favor verifique", service.fechaCaixa(codigo, "errada"));

		assertNull(caixa(codigo).get("data_fechamento"));
		assertTrue(service.caixaIsAberto());
	}

	// TI-CX-06: agência maior que a coluna (varchar 6) faz o insert falhar e nada fica gravado

	@Test
	public void cadastro_naoDeveGravarNada_quandoBancoRecusaOInsert() {
		Caixa banco = novoCaixa(CaixaTipo.BANCO, 100.0, "Banco IT");
		banco.setAgencia("1234567");
		banco.setConta("1");

		try {
			service.cadastro(banco);
			fail("Deveria ter falhado ao gravar");
		} catch (RuntimeException e) {
			assertEquals("Erro no processo de abertura, chame o suporte técnico", e.getMessage());
		}
		assertEquals(0, caixasCriados());
	}

	// TI-CX-07: caixa aberto do usuário logado, pela consulta real

	@Test
	public void buscaCaixaUsuario_deveEncontrarCaixaAbertoDoGerente() {
		Long codigo = service.cadastro(novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa IT"));

		Optional<Caixa> caixa = service.buscaCaixaUsuario("gerente");

		assertTrue(caixa.isPresent());
		assertEquals(codigo, caixa.get().getCodigo());
	}

	// TI-CX-08: filtro de bancos por data (consulta com "=")

	@Test
	public void listaBancosAbertosTipoFilterBanco_deveEncontrarBancoAbertoHoje() {
		Caixa banco = novoCaixa(CaixaTipo.BANCO, 0.0, "Banco IT");
		banco.setAgencia("1");
		banco.setConta("1");
		Long codigo = service.cadastro(banco);
		BancoFilter filter = new BancoFilter();
		filter.setData_cadastro(LocalDate.now().toString().replace("-", "/"));

		List<Caixa> bancos = service.listaBancosAbertosTipoFilterBanco(CaixaTipo.BANCO, filter);

		assertTrue(bancos.stream().anyMatch(c -> codigo.equals(c.getCodigo())));
	}

	// TI-CX-09 / DEFEITO D-CX-05: o filtro de caixas por data nunca encontra nada

	@Test
	public void listarCaixas_naoEncontraCaixaAbertoHoje_quandoFiltraPorData() {
		service.cadastro(novoCaixa(CaixaTipo.CAIXA, 0.0, "Caixa IT"));
		CaixaFilter filter = new CaixaFilter();
		filter.setData_cadastro(LocalDate.now().toString().replace("-", "/"));

		// DEFEITO: a consulta usa "data_cadastro like ?1" com um java.util.Date,
		// que vai ao banco como data e hora ("2026-11-22 00:00:00"). A coluna é
		// date ("2026-11-22"), o LIKE nunca casa e a lista volta vazia.
		assertTrue(service.listarCaixas(filter).isEmpty());
	}

	// TI-CX-10 / DEFEITO D-CX-06: listaBancos sempre falha com erro de SQL

	@Test
	public void listaBancos_deveFalhar_porqueConsultaNativaDevolveSoOCodigo() {
		Caixa banco = novoCaixa(CaixaTipo.BANCO, 0.0, "Banco IT");
		banco.setAgencia("1");
		banco.setConta("1");
		service.cadastro(banco);

		// DEFEITO: a consulta nativa "select b.codigo from caixa b ..." devolve
		// só a coluna codigo, mas o método declara List<Caixa>. O Hibernate tenta
		// montar a entidade Caixa, não acha a coluna descricao e a consulta
		// falha, mesmo com um banco aberto no banco de dados.
		try {
			service.listaBancos();
			fail("Deveria ter falhado ao montar a lista de bancos");
		} catch (InvalidDataAccessResourceUsageException e) {
			assertTrue(e.getMostSpecificCause().getMessage().contains("descricao"));
		}
	}

	private Caixa novoCaixa(CaixaTipo tipo, Double valorAbertura, String descricao) {
		Caixa caixa = new Caixa();
		caixa.setTipo(tipo);
		caixa.setValor_abertura(valorAbertura);
		caixa.setDescricao(descricao);
		return caixa;
	}

	private Map<String, Object> caixa(long codigo) {
		return jdbc.queryForMap("select * from caixa where codigo = ?", codigo);
	}

	private List<Map<String, Object>> lancamentos(long caixa) {
		return jdbc.queryForList("select * from caixa_lancamento where caixa_codigo = ?", caixa);
	}

	private int caixasCriados() {
		return jdbc.queryForObject("select count(*) from caixa where codigo > ?", Integer.class, maiorCodigoAntes);
	}

	private void fechaCaixasOriginais() {
		caixasOriginaisAbertos.clear();
		caixasOriginaisAbertos.addAll(jdbc.queryForList(
				"select codigo, data_alteracao from caixa where tipo = 'CAIXA' and data_fechamento is null"));
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
