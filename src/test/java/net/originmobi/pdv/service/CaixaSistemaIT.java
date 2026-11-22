package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Testes de sistema da abertura e do fechamento de caixa (TS-CX-01 a 08), pela
 * interface web com Selenium.
 *
 * Pré-requisitos: aplicação no ar (docker compose up -d) em
 * http://localhost:8080 e Chrome instalado. A massa de dados fica no MySQL do
 * docker-compose (localhost:3307): no fim de cada caso são apagados os caixas e
 * lançamentos criados depois do maior código existente no início. Um caixa do
 * tipo CAIXA que já estiver aberto no banco é marcado como fechado durante o
 * caso e restaurado depois, como em VendaSistemaIT. Por padrão o Chrome roda
 * sem janela; -Dselenium.headless=false mostra o navegador.
 *
 * TS-CX-07 e 08 verificam o requisito não funcional de segurança.
 */
public class CaixaSistemaIT {

	private static final String URL = System.getProperty("pdv.url", "http://localhost:8080");
	private static final String DB_URL = System.getProperty("pdv.db.url",
			"jdbc:mysql://localhost:3307/pdv?allowPublicKeyRetrieval=true&useSSL=false");

	private static final String SENHA_GERENTE = "123";
	private static final double DELTA = 0.001;

	private WebDriver driver;
	private WebDriverWait espera;
	private Connection banco;
	private long maiorCodigoAntes;
	private final List<Object[]> caixasOriginaisAbertos = new ArrayList<>();

	@Before
	public void setUp() throws SQLException {
		banco = DriverManager.getConnection(DB_URL, "root", "123456");
		maiorCodigoAntes = consultaNumero("select coalesce(max(codigo), 0) from caixa");
		fechaCaixasOriginais();

		ChromeOptions opcoes = new ChromeOptions();
		if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true")))
			opcoes.addArguments("--headless=new");
		opcoes.addArguments("--window-size=1366,900");
		// log de rede do Chrome, usado em TS-CX-08 para ver a URL das requisições
		LoggingPreferences logs = new LoggingPreferences();
		logs.enable(LogType.PERFORMANCE, Level.ALL);
		opcoes.setCapability("goog:loggingPrefs", logs);
		driver = new ChromeDriver(opcoes);
		espera = new WebDriverWait(driver, Duration.ofSeconds(15));
	}

	@After
	public void tearDown() throws SQLException {
		if (driver != null)
			driver.quit();
		if (banco != null) {
			try {
				executa("delete from caixa_lancamento where caixa_codigo > ?", maiorCodigoAntes);
				executa("delete from caixa where codigo > ?", maiorCodigoAntes);
			} finally {
				restauraCaixasOriginais();
				banco.close();
			}
		}
	}

	// TS-CX-01 (CT-CX-01): abrir caixa com R$ 50,00

	@Test
	public void abrirCaixa_deveIrParaGerenciarComLancamentoDeAbertura() throws SQLException {
		entraNoSistema();

		long caixa = abreCaixaPelaTela("Caixa Selenium", "5000");

		WebElement titulo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h2.titulo-h1")));
		assertEquals("Gerenciar Caixa", titulo.getText());
		assertTrue(driver.findElement(By.id("tabLancamentos")).getText().contains("Abertura de caixa"));
		salvaPrint("TS-CX-01-caixa-aberto");

		assertEquals(50.0, consultaDecimal("select valor_total from caixa where codigo = ?", caixa), DELTA);
		assertEquals(1, consultaNumero("select count(*) from caixa_lancamento where caixa_codigo = ?", caixa));
	}

	// TS-CX-02: segundo caixa com o primeiro aberto

	@Test
	public void abrirSegundoCaixa_deveAvisarQueExisteCaixaAberto() throws SQLException {
		entraNoSistema();
		abreCaixaPelaTela("Caixa Selenium", "1000");

		driver.get(URL + "/caixa/form");
		preencheAbertura("Caixa Selenium 2", "1000");
		driver.findElement(By.cssSelector(".btn-abrir-caixa")).click();

		Alert alerta = espera.until(ExpectedConditions.alertIsPresent());
		assertEquals("Existe caixa de dias anteriores em aberto, favor verifique", alerta.getText());
		alerta.accept();
		assertEquals(1, consultaNumero("select count(*) from caixa where codigo > ?", maiorCodigoAntes));
	}

	// TS-CX-03: valor de abertura negativo (a máscara aceita o sinal de menos)

	@Test
	public void abrirCaixaComValorNegativo_deveAvisarValorInvalido() throws SQLException {
		entraNoSistema();

		driver.get(URL + "/caixa/form");
		preencheAbertura("Caixa Selenium", "-1000");
		driver.findElement(By.cssSelector(".btn-abrir-caixa")).click();

		Alert alerta = espera.until(ExpectedConditions.alertIsPresent());
		assertEquals("Valor informado é inválido", alerta.getText());
		alerta.accept();
		assertEquals(0, consultaNumero("select count(*) from caixa where codigo > ?", maiorCodigoAntes));
	}

	// TS-CX-04 (CT-CX-02): fechar caixa com a senha correta

	@Test
	public void fecharCaixaComSenhaCorreta_deveMostrarFechadoEBloquearBotoes() throws SQLException {
		entraNoSistema();
		long caixa = abreCaixaPelaTela("Caixa Selenium", "7500");

		String mensagem = fechaPelaTela(SENHA_GERENTE);

		assertEquals("Caixa fechado com sucesso", mensagem);
		espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusFechado")));
		espera.until(d -> !d.findElement(By.id("btnfechacaixa")).isEnabled());
		assertFalse(driver.findElement(By.id("btnSuprimento")).isEnabled());
		salvaPrint("TS-CX-04-caixa-fechado");

		assertEquals(75.0, consultaDecimal("select valor_fechamento from caixa where codigo = ?", caixa), DELTA);
		assertEquals(1, consultaNumero("select count(*) from caixa where codigo = ? and data_fechamento is not null",
				caixa));
	}

	// TS-CX-05: fechar caixa com senha errada

	@Test
	public void fecharCaixaComSenhaErrada_deveAvisarEManterAberto() throws SQLException {
		entraNoSistema();
		long caixa = abreCaixaPelaTela("Caixa Selenium", "1000");

		String mensagem = fechaPelaTela("senha-errada");

		assertEquals("Senha incorreta, favor verifique", mensagem);
		assertTrue(driver.findElements(By.id("statusFechado")).isEmpty());
		assertNull(consultaTexto("select data_fechamento from caixa where codigo = ?", caixa));
	}

	// TS-CX-06 / DEFEITO D-CX-05: filtro da lista de caixas pela data de hoje

	@Test
	public void filtrarCaixasPelaDataDeHoje_naoMostraOCaixaAbertoHoje() throws SQLException {
		entraNoSistema();
		long caixa = abreCaixaPelaTela("Caixa Selenium", "1000");

		driver.get(URL + "/caixa");
		assertTrue("sem filtro o caixa aparece", linhasDaLista().contains(String.valueOf(caixa)));

		WebElement data = driver.findElement(By.name("data_cadastro"));
		data.sendKeys(LocalDate.now().toString().replace("-", "/"));
		data.submit();
		espera.until(ExpectedConditions.urlContains("data_cadastro="));
		salvaPrint("TS-CX-06-filtro-por-data-vazio");

		// DEFEITO: o caixa foi aberto hoje, mas o filtro pela data de hoje não o
		// encontra (consulta com LIKE sobre data e hora, ver D-CX-05)
		assertFalse(linhasDaLista().contains(String.valueOf(caixa)));
	}

	// TS-CX-07: requisito não funcional de segurança - telas de caixa exigem login

	@Test
	public void semLogin_telasDeCaixaLevamAoLoginEFechamentoNaoAcontece() throws SQLException {
		long caixa = insereCaixaAberto();

		for (String tela : new String[] { "/caixa", "/caixa/form", "/caixa/gerenciar/" + caixa }) {
			driver.get(URL + tela);
			espera.until(ExpectedConditions.urlContains("/login"));
			assertTrue(tela + " deveria levar ao login", driver.getCurrentUrl().endsWith("/login"));
		}

		// chamada direta ao endereço de fechamento, sem sessão, com a senha certa
		((JavascriptExecutor) driver).executeAsyncScript("var pronto = arguments[arguments.length - 1];"
				+ "fetch('/caixa/fechar?caixa=" + caixa + "&senha=" + SENHA_GERENTE + "', {method: 'POST'})"
				+ ".then(function () { pronto(); }, function () { pronto(); });");

		assertNull(consultaTexto("select data_fechamento from caixa where codigo = ?", caixa));
	}

	// TS-CX-08: requisito não funcional de segurança - a senha não deve aparecer na URL
	// DEFEITO D-CX-07: o fechamento envia a senha na query string

	@Test
	public void fecharCaixa_enviaASenhaNaUrlDaRequisicao() throws SQLException {
		entraNoSistema();
		abreCaixaPelaTela("Caixa Selenium", "1000");
		driver.manage().logs().get(LogType.PERFORMANCE); // descarta o que veio antes

		assertEquals("Caixa fechado com sucesso", fechaPelaTela(SENHA_GERENTE));

		List<String> urls = driver.manage().logs().get(LogType.PERFORMANCE).getAll().stream()
				.map(LogEntry::getMessage)
				.filter(m -> m.contains("\"Network.requestWillBeSent\"") && m.contains("/caixa/fechar"))
				.collect(Collectors.toList());
		assertFalse("a requisição de fechamento deveria aparecer no log de rede", urls.isEmpty());
		// DEFEITO: a senha vai em texto puro na URL, que fica em histórico,
		// logs de servidor e proxies
		assertTrue(urls.get(0).contains("senha=" + SENHA_GERENTE));
	}

	// passos na interface

	private void entraNoSistema() {
		driver.get(URL + "/login");
		driver.findElement(By.name("username")).sendKeys("gerente");
		driver.findElement(By.name("password")).sendKeys(SENHA_GERENTE);
		driver.findElement(By.id("btn-login")).click();
		espera.until(d -> !d.getCurrentUrl().contains("/login"));
	}

	private void preencheAbertura(String descricao, String valorDigitado) {
		new Select(espera.until(ExpectedConditions.elementToBeClickable(By.id("caixatipo")))).selectByValue("CAIXA");
		driver.findElement(By.id("descricao")).sendKeys(descricao);
		WebElement valor = driver.findElement(By.id("valorAbertura"));
		valor.click();
		valor.sendKeys(valorDigitado);
	}

	private long abreCaixaPelaTela(String descricao, String valorDigitado) {
		driver.get(URL + "/caixa/form");
		preencheAbertura(descricao, valorDigitado);
		driver.findElement(By.cssSelector(".btn-abrir-caixa")).click();
		espera.until(ExpectedConditions.urlMatches("/caixa/gerenciar/\\d+$"));
		return Long.parseLong(driver.getCurrentUrl().replaceAll(".*/caixa/gerenciar/", ""));
	}

	private String fechaPelaTela(String senha) {
		espera.until(ExpectedConditions.elementToBeClickable(By.id("btnfechacaixa"))).click();
		WebElement campo = espera.until(ExpectedConditions.visibilityOfElementLocated(By.id("admsenha")));
		campo.sendKeys(senha);
		driver.findElement(By.cssSelector(".btn-fechar-caixa")).click();
		Alert alerta = espera.until(ExpectedConditions.alertIsPresent());
		String mensagem = alerta.getText();
		alerta.accept();
		return mensagem;
	}

	private String linhasDaLista() {
		return driver.findElements(By.cssSelector("table tbody tr td:first-child")).stream().map(WebElement::getText)
				.collect(Collectors.joining(" ", " ", " "));
	}

	private void salvaPrint(String nome) {
		try {
			File destino = new File("target/selenium-prints/" + nome + ".png");
			destino.getParentFile().mkdirs();
			Files.copy(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath(), destino.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("Não foi possível salvar o print " + nome + ": " + e.getMessage());
		}
	}

	// massa de dados

	private long insereCaixaAberto() throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(
				"insert into caixa (descricao, valor_abertura, valor_total, tipo, usuario_codigo, data_cadastro) "
						+ "values ('Caixa Selenium', 0, 0, 'CAIXA', 1, current_date)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				rs.next();
				return rs.getLong(1);
			}
		}
	}

	private void fechaCaixasOriginais() throws SQLException {
		caixasOriginaisAbertos.clear();
		try (Statement st = banco.createStatement();
				ResultSet rs = st.executeQuery(
						"select codigo, data_alteracao from caixa where tipo = 'CAIXA' and data_fechamento is null")) {
			while (rs.next())
				caixasOriginaisAbertos.add(new Object[] { rs.getLong(1), rs.getTimestamp(2) });
		}
		for (Object[] caixa : caixasOriginaisAbertos)
			executa("update caixa set data_fechamento = now(), data_alteracao = data_alteracao where codigo = ?",
					caixa[0]);
	}

	private void restauraCaixasOriginais() throws SQLException {
		for (Object[] caixa : caixasOriginaisAbertos) {
			try (PreparedStatement ps = banco
					.prepareStatement("update caixa set data_fechamento = null, data_alteracao = ? where codigo = ?")) {
				ps.setTimestamp(1, (Timestamp) caixa[1]);
				ps.setLong(2, (Long) caixa[0]);
				ps.executeUpdate();
			}
		}
		caixasOriginaisAbertos.clear();
	}

	private void executa(String sql, Object parametro) throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(sql)) {
			ps.setObject(1, parametro);
			ps.executeUpdate();
		}
	}

	private long consultaNumero(String sql, Object... parametros) throws SQLException {
		String texto = consultaTexto(sql, parametros);
		return texto == null ? 0 : Long.parseLong(texto);
	}

	private double consultaDecimal(String sql, Object... parametros) throws SQLException {
		return Double.parseDouble(consultaTexto(sql, parametros));
	}

	private String consultaTexto(String sql, Object... parametros) throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(sql)) {
			for (int i = 0; i < parametros.length; i++)
				ps.setObject(i + 1, parametros[i]);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getString(1) : null;
			}
		}
	}
}
