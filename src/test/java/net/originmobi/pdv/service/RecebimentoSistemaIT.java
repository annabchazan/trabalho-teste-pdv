package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Testes de sistema do recebimento de parcelas (TS-REC), pela interface web com
 * Selenium. Pré-requisitos: aplicação no ar (docker compose up -d) em
 * http://localhost:8080 e Chrome instalado. A massa de dados (cliente, venda a
 * prazo com duas parcelas, caixa aberto e título em dinheiro) é criada direto no
 * MySQL do docker-compose (localhost:3307) com códigos 93xx e apagada antes e
 * depois de cada caso. Por padrão o Chrome roda sem janela;
 * -Dselenium.headless=false mostra o navegador.
 *
 * Pré-condição: nenhum caixa aberto no banco antes da execução, porque o
 * serviço usa "o" caixa aberto. O setUp falha com mensagem clara se houver.
 */
public class RecebimentoSistemaIT {

	private static final String URL = System.getProperty("pdv.url", "http://localhost:8080");
	private static final String DB_URL = System.getProperty("pdv.db.url",
			"jdbc:mysql://localhost:3307/pdv?allowPublicKeyRetrieval=true&useSSL=false");

	private static final long CLIENTE = 9301L;
	private static final long VENDA = 9301L;
	private static final long RECEBER = 9301L;
	private static final long PARCELA_100 = 9301L;
	private static final long PARCELA_50 = 9302L;
	private static final long CAIXA = 9301L;
	private static final long TITULO_DINHEIRO = 9301L;
	private static final long GRUPO_SEM_PERMISSAO = 9301L;
	private static final long USUARIO_SEM_PERMISSAO = 9301L;
	private static final String LOGIN_SEM_PERMISSAO = "operador-sel-9301";
	private static final double DELTA = 0.001;

	// requisito não funcional de desempenho: o recebimento confirmado em até 2 s
	private static final long LIMITE_MS = 2000;
	private static final int MEDICOES = 5;

	private WebDriver driver;
	private WebDriverWait espera;
	private Connection banco;

	@Before
	public void setUp() throws SQLException {
		banco = DriverManager.getConnection(DB_URL, "root", "123456");
		limpa();
		// o serviço usa "o" caixa aberto: outro caixa aberto desviaria o lançamento
		long abertos = consultaLong("select count(*) from caixa where tipo = 'CAIXA' and data_fechamento is null");
		if (abertos > 0)
			org.junit.Assert.fail("Pré-condição: o banco não pode ter caixa aberto antes destes testes (encontrados: "
					+ abertos + "). Feche-o, por exemplo: update caixa set data_fechamento = now() where data_fechamento is null");
		criaMassaDeDados();

		ChromeOptions opcoes = new ChromeOptions();
		if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true")))
			opcoes.addArguments("--headless=new");
		opcoes.addArguments("--window-size=1366,900");
		driver = new ChromeDriver(opcoes);
		espera = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	@After
	public void tearDown() throws SQLException {
		if (driver != null)
			driver.quit();
		if (banco != null) {
			limpa();
			banco.close();
		}
	}

	// TS-REC-01 (CT-REC-01): recebimento total em dinheiro
	@Test
	public void receberParcelasEmDinheiro_deveQuitarTudoELancarNoCaixa() throws SQLException {
		entraNoSistema();
		abreRecebimentoDasParcelas(PARCELA_100, PARCELA_50);

		assertEquals(150.0, valor(driver.findElement(By.id("total")).getAttribute("value")), DELTA);

		assertEquals("Recebimento realizado com sucesso", recebe(TITULO_DINHEIRO, "15000"));

		// a tela de contas a receber não mostra mais parcelas em aberto do cliente
		abreContasAReceberDoCliente();
		assertEquals(0, driver.findElements(By.cssSelector(".tabela-receber input.marcado")).size());

		// conferência no banco, e não só na tela
		assertEquals(1, consultaLong("select quitado from parcela where codigo = ?", PARCELA_100));
		assertEquals(1, consultaLong("select quitado from parcela where codigo = ?", PARCELA_50));
		assertEquals(150.0, consultaDouble("select valor_total from caixa where codigo = ?", CAIXA), DELTA);
		assertEquals(1, consultaLong("select count(*) from caixa_lancamento where caixa_codigo = ?", CAIXA));
	}

	// TS-REC-02 (CT-REC-02): confirmar sem escolher o título
	@Test
	public void confirmarSemTitulo_deveMostrarMensagemENaoQuitarNada() throws SQLException {
		entraNoSistema();
		abreRecebimentoDasParcelas(PARCELA_100, PARCELA_50);

		assertEquals("Selecione um título para realizar o recebimento", recebe(null, "15000"));

		assertEquals(0, consultaLong("select quitado from parcela where codigo = ?", PARCELA_100));
		assertEquals(0, consultaLong("select count(*) from caixa_lancamento where caixa_codigo = ?", CAIXA));
	}

	// TS-REC-03 (CT-REC-03): reenviar um recebimento já processado. Não era
	// executável como teste manual porque a tela desabilita os campos; aqui eles
	// são reabilitados por JavaScript, como faria alguém contornando a tela.
	@Test
	public void reenviarRecebimentoJaProcessado_deveSerRecusadoPeloServidor() throws SQLException {
		entraNoSistema();
		String endereco = abreRecebimentoDasParcelas(PARCELA_100, PARCELA_50);
		assertEquals("Recebimento realizado com sucesso", recebe(TITULO_DINHEIRO, "15000"));

		driver.get(endereco);
		WebElement valorRecebido = espera
				.until(ExpectedConditions.presenceOfElementLocated(By.id("vlrecebido")));
		assertTrue("a tela deveria desabilitar os campos de um recebimento processado",
				!valorRecebido.isEnabled());
		((JavascriptExecutor) driver).executeScript(
				"['#vlrecebido', '#titulo', '#desc', '#acres'].forEach(function (c) { $(c).prop('disabled', false); });");

		assertEquals("Recebimento já esta fechado", recebe(TITULO_DINHEIRO, "15000"));

		// o caixa recebeu uma vez só
		assertEquals(1, consultaLong("select count(*) from caixa_lancamento where caixa_codigo = ?", CAIXA));
		assertEquals(150.0, consultaDouble("select valor_total from caixa where codigo = ?", CAIXA), DELTA);
	}

	// TS-REC-04 (D6): acionar o recebimento sem marcar parcela
	@Test
	public void receberSemMarcarParcela_deveMostrarMensagemAmigavel() throws SQLException {
		entraNoSistema();
		abreContasAReceberDoCliente();

		driver.findElement(By.cssSelector(".btn-receber-varios")).click();

		assertEquals("Selecione ao menos uma parcela para receber", textoDoAlerta());
		assertEquals(0, consultaLong("select count(*) from recebimento where pessoa_codigo = ?", CLIENTE));
	}

	// TS-REC-05: não funcional (segurança) - as telas de recebimento exigem login
	@Test
	public void acessarTelasDeRecebimentoSemLogin_deveRedirecionarParaLogin() {
		driver.get(URL + "/receber");
		espera.until(ExpectedConditions.urlContains("/login"));
		assertTrue(driver.findElement(By.name("username")).isDisplayed());

		driver.get(URL + "/recebimento/1");
		espera.until(ExpectedConditions.urlContains("/login"));
		assertTrue(driver.findElement(By.name("username")).isDisplayed());
	}

	// TS-REC-06: não funcional (desempenho) - do clique em Receber até a
	// confirmação na tela, em até 2 s, em 5 recebimentos seguidos
	@Test
	public void confirmarRecebimento_deveResponderEmAteDoisSegundos() throws SQLException {
		List<Long> tempos = new ArrayList<>();
		entraNoSistema();

		for (int i = 0; i < MEDICOES; i++) {
			long parcela = 9311L + i;
			executa("insert into parcela (codigo, valor_total, valor_desconto, valor_acrescimo, valor_recebido, valor_restante, "
					+ "data_cadastro, data_alteracao, data_vencimento, receber_codigo, quitado, sequencia) "
					+ "values (?, 10, 0, 0, 0, 10, now(), now(), now(), ?, 0, ?)", parcela, RECEBER, 3 + i);

			abreRecebimentoDasParcelas(parcela);
			preencheRecebimento(TITULO_DINHEIRO, "1000");

			long inicio = System.nanoTime();
			driver.findElement(By.cssSelector(".btn-receber-titulo")).click();
			String mensagem = textoDoAlerta();
			tempos.add((System.nanoTime() - inicio) / 1_000_000);

			assertEquals("Recebimento realizado com sucesso", mensagem);
		}

		System.out.println("TS-REC-06 tempos (ms): " + tempos);
		for (long tempo : tempos)
			assertTrue("recebimento levou " + tempo + " ms; tempos: " + tempos, tempo <= LIMITE_MS);
	}

	// TS-REC-07: não funcional (segurança) - usuário sem a permissão
	// REALIZAR_RECEBIMENTO não pode receber, mesmo chamando o endereço direto.
	// Falha hoje pelo defeito D9: no SecurityConfig a regra é "/recebimento/", que
	// não corresponde a nenhuma requisição real; o sistema só esconde o botão.
	// @Ignore até o defeito ser corrigido - remover a anotação reproduz a falha.
	@org.junit.Ignore("Defeito D9 (Issue #26): SecurityConfig protege \"/recebimento/\" em vez de \"/recebimento/**\"; ver docs/bugs/defeitos-recebimento.md")
	@Test
	public void usuarioSemPermissao_naoPodeReceberPeloEnderecoDireto() throws SQLException {
		long recebimento = 9301L;
		executa("insert into recebimento (codigo, valor_total, pessoa_codigo, data_cadastro) values (?, 150, ?, now())",
				recebimento, CLIENTE);
		executa("insert into recebimento_parcelas (recebimento_cod, parcela_cod) values (?, ?), (?, ?)", recebimento,
				PARCELA_100, recebimento, PARCELA_50);
		executa("insert into grupousuario (codigo, nome, descricao) values (?, 'SEM RECEBIMENTO SEL', 'Teste Selenium')",
				GRUPO_SEM_PERMISSAO);
		executa("insert into permissoes_grupo_usuario (grupo_usuario_codigo, permissoes_codigo) values (?, 1)",
				GRUPO_SEM_PERMISSAO);
		// mesma senha do gerente (123)
		executa("insert into usuario (codigo, user, senha, data_cadastro, pessoa_codigo) "
				+ "select ?, ?, senha, now(), ? from usuario where user = 'gerente'", USUARIO_SEM_PERMISSAO,
				LOGIN_SEM_PERMISSAO, CLIENTE);
		executa("insert into usuario_grupousuario (grupo_usuario_codigo, usuario_codigo) values (?, ?)",
				GRUPO_SEM_PERMISSAO, USUARIO_SEM_PERMISSAO);

		entraNoSistema(LOGIN_SEM_PERMISSAO);

		// controle: a lista de contas a receber já é bloqueada para esse usuário
		assertEquals(403L, status("GET", "/receber"));

		long resposta = status("POST", "/recebimento?receber=" + recebimento + "&titulo=" + TITULO_DINHEIRO
				+ "&vlrecebido=150.00&desconto=&acrescimo=");

		assertEquals("o servidor deveria negar o recebimento", 403L, resposta);
		assertEquals(0, consultaLong("select quitado from parcela where codigo = ?", PARCELA_100));
		assertEquals(0, consultaLong("select count(*) from caixa_lancamento where caixa_codigo = ?", CAIXA));
	}

	// passos na interface

	// faz a requisição de dentro da página, com a sessão do usuário logado, e
	// devolve o status HTTP
	private long status(String metodo, String caminho) {
		Object status = ((JavascriptExecutor) driver).executeAsyncScript(
				"var pronto = arguments[arguments.length - 1];"
						+ "fetch(arguments[1], {method: arguments[0], credentials: 'same-origin', redirect: 'manual'})"
						+ ".then(function (r) { pronto(r.status); }, function () { pronto(-1); });",
				metodo, URL + caminho);
		return ((Number) status).longValue();
	}

	private void entraNoSistema() {
		entraNoSistema("gerente");
	}

	private void entraNoSistema(String usuario) {
		driver.get(URL + "/login");
		driver.findElement(By.name("username")).sendKeys(usuario);
		driver.findElement(By.name("password")).sendKeys("123");
		driver.findElement(By.id("btn-login")).click();
		espera.until(d -> !d.getCurrentUrl().contains("/login"));
	}

	private void abreContasAReceberDoCliente() {
		driver.get(URL + "/receber?codigo=" + CLIENTE + "&pago=false");
		espera.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".btn-receber-varios")));
	}

	// marca as parcelas na tela de contas a receber e aciona o recebimento;
	// devolve o endereço da tela do recebimento aberto
	private String abreRecebimentoDasParcelas(long... codigos) {
		abreContasAReceberDoCliente();
		for (long codigo : codigos)
			driver.findElement(By.cssSelector(".tabela-receber input.marcado[value='" + codigo + "']")).click();
		driver.findElement(By.cssSelector(".btn-receber-varios")).click();

		espera.until(ExpectedConditions.urlMatches(".*/recebimento/\\d+$"));
		espera.until(ExpectedConditions.presenceOfElementLocated(By.id("vlrecebido")));
		return driver.getCurrentUrl();
	}

	private void preencheRecebimento(Long titulo, String valorDigitado) {
		Select combo = new Select(driver.findElement(By.id("titulo")));
		if (titulo == null)
			combo.selectByValue("");
		else
			combo.selectByValue(String.valueOf(titulo));

		// o campo tem máscara de dinheiro: os dígitos são lidos como centavos
		WebElement valor = driver.findElement(By.id("vlrecebido"));
		valor.clear();
		valor.sendKeys(valorDigitado);
	}

	private String recebe(Long titulo, String valorDigitado) {
		preencheRecebimento(titulo, valorDigitado);
		driver.findElement(By.cssSelector(".btn-receber-titulo")).click();
		return textoDoAlerta();
	}

	private String textoDoAlerta() {
		Alert alerta = espera.until(ExpectedConditions.alertIsPresent());
		String texto = alerta.getText();
		alerta.accept();
		return texto;
	}

	// aceita "R$ 150,00", "150,00", "150.0" ou "1.150,00"
	private static double valor(String texto) {
		String limpo = texto.replace("R$", "").trim();
		if (limpo.contains(","))
			limpo = limpo.replace(".", "").replace(",", ".");
		return limpo.isEmpty() ? 0.0 : Double.parseDouble(limpo);
	}

	// massa de dados no banco

	private void criaMassaDeDados() throws SQLException {
		String hoje = "2026-11-21";
		long sp = consultaLong("select codigo from estado where sigla = 'SP'");

		executa("insert into cidade (codigo, nome, codigo_municipio, estado_codigo) values (?, 'Cidade Selenium REC', '3550308', ?)",
				CLIENTE, sp);
		executa("insert into endereco (codigo, rua, bairro, numero, cep, referencia, data_cadastro, cidade_codigo) "
				+ "values (?, 'Rua Selenium', 'Centro', '1', '01000000', '', ?, ?)", CLIENTE, hoje, CLIENTE);
		executa("insert into pessoa (codigo, nome, cpfcnpj, apelido, data_nascimento, observacao, endereco_codigo, data_cadastro) "
				+ "values (?, 'Cliente Selenium Recebimento', '00000000191', 'Selenium', ?, '', ?, ?)", CLIENTE, hoje,
				CLIENTE, hoje);
		executa("insert into venda (codigo, observacao, valor_produtos, valor_desconto, valor_acrescimo, valor_total, situacao, "
				+ "pessoa_codigo, usuario_codigo, data_cadastro) values (?, 'Venda Selenium', 150, 0, 0, 150, 'FECHADA', ?, 1, ?)",
				VENDA, CLIENTE, hoje);
		executa("insert into receber (codigo, observacao, valor_total, pessoa_codigo, data_cadastro, venda_codigo) "
				+ "values (?, 'Receber Selenium', 150, ?, ?, ?)", RECEBER, CLIENTE, hoje, VENDA);
		String parcela = "insert into parcela (codigo, valor_total, valor_desconto, valor_acrescimo, valor_recebido, valor_restante, "
				+ "data_cadastro, data_alteracao, data_vencimento, receber_codigo, quitado, sequencia) "
				+ "values (?, ?, 0, 0, 0, ?, ?, now(), ?, ?, 0, ?)";
		executa(parcela, PARCELA_100, 100.0, 100.0, hoje, "2026-12-21", RECEBER, 1);
		executa(parcela, PARCELA_50, 50.0, 50.0, hoje, "2027-01-21", RECEBER, 2);
		executa("insert into caixa (codigo, descricao, valor_abertura, valor_total, tipo, usuario_codigo, data_cadastro) "
				+ "values (?, 'Caixa Selenium', 0, 0, 'CAIXA', 1, ?)", CAIXA, hoje);
		executa("insert into titulo (codigo, descricao, tipo_codigo, maquina_cartao_codigo) values (?, 'Dinheiro Selenium', 1, null)",
				TITULO_DINHEIRO);
	}

	private void limpa() throws SQLException {
		executa("delete from caixa_lancamento where caixa_codigo = ? or recebimento_codigo in "
				+ "(select codigo from recebimento where pessoa_codigo = ?)", CAIXA, CLIENTE);
		executa("delete from recebimento_parcelas where recebimento_cod in (select codigo from recebimento where pessoa_codigo = ?)",
				CLIENTE);
		executa("delete from recebimento where pessoa_codigo = ?", CLIENTE);
		executa("delete from parcela where receber_codigo = ?", RECEBER);
		executa("delete from receber where codigo = ?", RECEBER);
		executa("delete from venda where codigo = ?", VENDA);
		executa("delete from usuario_grupousuario where usuario_codigo = ?", USUARIO_SEM_PERMISSAO);
		executa("delete from usuario where codigo = ?", USUARIO_SEM_PERMISSAO);
		executa("delete from permissoes_grupo_usuario where grupo_usuario_codigo = ?", GRUPO_SEM_PERMISSAO);
		executa("delete from grupousuario where codigo = ?", GRUPO_SEM_PERMISSAO);
		executa("delete from titulo where codigo = ?", TITULO_DINHEIRO);
		executa("delete from caixa where codigo = ?", CAIXA);
		executa("delete from pessoa where codigo = ?", CLIENTE);
		executa("delete from endereco where codigo = ?", CLIENTE);
		executa("delete from cidade where codigo = ?", CLIENTE);
	}

	private void executa(String sql, Object... parametros) throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(sql)) {
			for (int i = 0; i < parametros.length; i++)
				ps.setObject(i + 1, parametros[i]);
			ps.executeUpdate();
		}
	}

	private long consultaLong(String sql, Object... parametros) throws SQLException {
		Number numero = (Number) consulta(sql, parametros);
		assertNotNull(sql, numero);
		return numero.longValue();
	}

	private double consultaDouble(String sql, Object... parametros) throws SQLException {
		Number numero = (Number) consulta(sql, parametros);
		assertNotNull(sql, numero);
		return numero.doubleValue();
	}

	private Object consulta(String sql, Object... parametros) throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(sql)) {
			for (int i = 0; i < parametros.length; i++)
				ps.setObject(i + 1, parametros[i]);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getObject(1) : null;
			}
		}
	}
}
