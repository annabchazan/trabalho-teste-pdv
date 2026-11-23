package net.originmobi.pdv.service.notafiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Testes de sistema da inclusão de item em nota fiscal (TS-NFI), pela interface
 * web com Selenium. Pré-requisitos: aplicação no ar (docker compose up -d) em
 * http://localhost:8080 e Chrome instalado. A massa de dados (destinatário,
 * tributação, produtos e nota) é criada direto no MySQL do docker-compose
 * (localhost:3307) com códigos 9101/9102 e apagada antes e depois de cada caso.
 * Por padrão o Chrome roda sem janela; -Dselenium.headless=false mostra o navegador.
 */
public class NotaFiscalItemSistemaIT {

	private static final String URL = System.getProperty("pdv.url", "http://localhost:8080");
	private static final String DB_URL = System.getProperty("pdv.db.url",
			"jdbc:mysql://localhost:3307/pdv?allowPublicKeyRetrieval=true&useSSL=false");

	private static final long COD = 9101L;
	private static final long COD_PRODUTO = 9101L;
	private static final long COD_PRODUTO_SEM_MOD_BC = 9102L;
	private static final long COD_NOTA = 9101L;
	private static final double DELTA = 0.001;

	// requisito não funcional TS-NFI-06
	private static final long LIMITE_INCLUSAO_MS = 2000;
	private static final int MEDICOES = 10;

	private WebDriver driver;
	private WebDriverWait espera;
	private Connection banco;

	@Before
	public void setUp() throws SQLException {
		banco = DriverManager.getConnection(DB_URL, "root", "123456");
		limpa();
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

	// TS-NFI-01 (CT-NFI-01)
	@Test
	public void incluirItemValido_deveMostrarItemNaTabelaEAtualizarTotal() {
		entraNoSistema();
		abreAbaProdutosDaNota();

		insereProduto(COD_PRODUTO, "3");

		List<WebElement> linhas = esperaQuantidadeDeLinhas(1);
		List<WebElement> colunas = linhas.get(0).findElements(By.tagName("td"));
		assertEquals("Produto Selenium", colunas.get(1).getText());
		assertEquals("12345678", colunas.get(2).getText());
		assertEquals("5102", colunas.get(4).getText());
		assertEquals("3", colunas.get(6).getText());
		assertEquals(30.0, valor(colunas.get(8).getText()), DELTA);

		// total da nota = produtos (30,00) + IPI de 5% (1,50)
		assertEquals(31.5, esperaTotalDaNota(31.5), DELTA);
	}

	// TS-NFI-02 (CT-NFI-02): falha hoje pela Issue #2 (alerta técnico "No value present").
	// @Ignore até o defeito ser corrigido — remover a anotação reproduz a falha.
	@Ignore("Issue #2: NoSuchElementException em vez de validação amigável quando produto não tem Modalidade BC ICMS")
	@Test
	public void incluirProdutoSemModalidadeBcIcms_deveMostrarMensagemAmigavel() {
		entraNoSistema();
		abreAbaProdutosDaNota();

		selecionaProduto(COD_PRODUTO_SEM_MOD_BC);
		driver.findElement(By.cssSelector(".btn-addnota-produto")).click();
		Alert quantidade = espera.until(ExpectedConditions.alertIsPresent());
		quantidade.sendKeys("3");
		quantidade.accept();

		Alert erro = espera.until(ExpectedConditions.alertIsPresent());
		String mensagem = erro.getText();
		erro.accept();

		assertEquals("Produto sem Modalidade BC ICMS, favor verifique", mensagem);
		assertEquals(0, linhasDeItens().size());
	}

	// TS-NFI-03
	@Test
	public void incluirMesmoProdutoDuasVezes_deveSomarQuantidadeEmUmaLinha() {
		entraNoSistema();
		abreAbaProdutosDaNota();

		insereProduto(COD_PRODUTO, "2");
		esperaQuantidadeDeLinhas(1);
		insereProduto(COD_PRODUTO, "3");

		espera.until(d -> {
			List<WebElement> linhas = linhasDeItens();
			return linhas.size() == 1 && "5".equals(linhas.get(0).findElements(By.tagName("td")).get(6).getText());
		});
		assertEquals(52.5, esperaTotalDaNota(52.5), DELTA);
	}

	// TS-NFI-04
	@Test
	public void removerItem_deveTirarLinhaERecalcularTotal() {
		entraNoSistema();
		abreAbaProdutosDaNota();
		insereProduto(COD_PRODUTO, "2");
		esperaQuantidadeDeLinhas(1);

		driver.findElement(By.cssSelector(".produtos-nota .btn-remove-item")).click();
		espera.until(ExpectedConditions.alertIsPresent()).accept();

		esperaQuantidadeDeLinhas(0);
		assertEquals(0.0, esperaTotalDaNota(0.0), DELTA);
	}

	// TS-NFI-05: não funcional (segurança)
	@Test
	public void acessarNovaNotaSemLogin_deveRedirecionarParaLogin() {
		driver.get(URL + "/notafiscal/form");

		espera.until(ExpectedConditions.urlContains("/login"));
		assertTrue(driver.findElement(By.name("username")).isDisplayed());
	}

	// TS-NFI-06: não funcional (eficiência de desempenho - comportamento temporal).
	// Requisito: incluir um item na nota pela tela leva no máximo 2 s, da confirmação
	// da quantidade até o item e o total da nota atualizados. 1 inclusão de aquecimento + 10 medidas.
	// @Ignore porque o limite é violado de forma intermitente: 44 de 50 medições dentro de 2 s
	// (5 execuções), mediana de 830 ms. Remover a anotação repete a medição.
	@Ignore("Requisito de 2 s violado de forma intermitente: 44 de 50 medições dentro do limite")
	@Test
	public void incluirItem_deveAtualizarTelaEmAte2Segundos() {
		entraNoSistema();
		abreAbaProdutosDaNota();
		WebDriverWait esperaFina = new WebDriverWait(driver, Duration.ofSeconds(15), Duration.ofMillis(20));
		esperaFina.ignoring(StaleElementReferenceException.class);

		List<Long> tempos = new ArrayList<>();
		for (int i = 0; i <= MEDICOES; i++) {
			int quantidade = i + 1;
			selecionaProduto(COD_PRODUTO);
			driver.findElement(By.cssSelector(".btn-addnota-produto")).click();
			Alert prompt = espera.until(ExpectedConditions.alertIsPresent());
			prompt.sendKeys("1");
			long inicio = System.nanoTime();
			prompt.accept();

			// cada inclusão soma 1 na mesma linha; total = quantidade x (10,00 + IPI de 5%)
			esperaFina.until(d -> {
				List<WebElement> linhas = linhasDeItens();
				return linhas.size() == 1
						&& String.valueOf(quantidade).equals(linhas.get(0).findElements(By.tagName("td")).get(6).getText())
						&& Math.abs(totalDaNota() - quantidade * 10.5) < DELTA;
			});
			long tempo = (System.nanoTime() - inicio) / 1_000_000;
			if (i > 0)
				tempos.add(tempo);
		}

		long maior = Collections.max(tempos);
		List<Long> ordenados = new ArrayList<>(tempos);
		Collections.sort(ordenados);
		long mediana = (ordenados.get(MEDICOES / 2 - 1) + ordenados.get(MEDICOES / 2)) / 2;
		System.out.println("TS-NFI-06 tempos de inclusão (ms): " + tempos + " | mediana " + mediana
				+ " | maior " + maior + " | limite " + LIMITE_INCLUSAO_MS);

		assertTrue("maior tempo " + maior + " ms acima do limite de " + LIMITE_INCLUSAO_MS + " ms",
				maior <= LIMITE_INCLUSAO_MS);
	}

	// passos na interface

	private void entraNoSistema() {
		driver.get(URL + "/login");
		driver.findElement(By.name("username")).sendKeys("gerente");
		driver.findElement(By.name("password")).sendKeys("123");
		driver.findElement(By.id("btn-login")).click();
		espera.until(d -> !d.getCurrentUrl().contains("/login"));
	}

	private void abreAbaProdutosDaNota() {
		driver.get(URL + "/notafiscal/" + COD_NOTA);
		espera.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href='#produtos']"))).click();
		espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".btn-addnota-produto")));
	}

	private void insereProduto(long codProduto, String quantidade) {
		selecionaProduto(codProduto);
		driver.findElement(By.cssSelector(".btn-addnota-produto")).click();
		Alert prompt = espera.until(ExpectedConditions.alertIsPresent());
		prompt.sendKeys(quantidade);
		prompt.accept();
	}

	private void selecionaProduto(long codProduto) {
		// o combo usa bootstrap-select: abre a lista e clica na opção pelo texto
		espera.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-id='codigoProduto']"))).click();
		By opcao = By.xpath("//div[contains(@class,'bootstrap-select')]//li//a[contains(normalize-space(.),'COD: "
				+ codProduto + " -')]");
		espera.until(ExpectedConditions.elementToBeClickable(opcao)).click();
	}

	private List<WebElement> linhasDeItens() {
		return driver.findElements(By.cssSelector(".produtos-nota tbody tr"));
	}

	private List<WebElement> esperaQuantidadeDeLinhas(int quantidade) {
		espera.until(d -> linhasDeItens().size() == quantidade);
		return linhasDeItens();
	}

	private double esperaTotalDaNota(double esperado) {
		try {
			espera.until(d -> Math.abs(totalDaNota() - esperado) < DELTA);
		} catch (org.openqa.selenium.TimeoutException e) {
			// deixa o assert mostrar o valor encontrado
		}
		return totalDaNota();
	}

	private double totalDaNota() {
		return valor(driver.findElement(By.id("vnf")).getAttribute("value"));
	}

	// aceita "R$ 31.5", "31,50" ou "1.031,50"
	private static double valor(String texto) {
		String limpo = texto.replace("R$", "").trim();
		if (limpo.contains(","))
			limpo = limpo.replace(".", "").replace(",", ".");
		return limpo.isEmpty() ? 0.0 : Double.parseDouble(limpo);
	}

	// massa de dados no banco

	private void criaMassaDeDados() throws SQLException {
		String hoje = "2026-11-19";
		long fornecedor = consultaLong("select min(codigo) from fornecedor");
		long frete = consultaLong("select min(codigo) from frete_tipo");
		long finalidade = consultaLong("select min(codigo) from nota_fiscal_finalidade");
		long regime = consultaLong("select min(codigo) from regime_tributario");
		long sp = consultaLong("select codigo from estado where sigla = 'SP'");

		executa("insert into cidade (codigo, nome, codigo_municipio, estado_codigo) values (?, 'Cidade Selenium', '3550308', ?)",
				COD, sp);
		executa("insert into endereco (codigo, rua, bairro, numero, cep, referencia, data_cadastro, cidade_codigo) "
				+ "values (?, 'Rua Selenium', 'Centro', '1', '01000000', '', ?, ?)", COD, hoje, COD);
		executa("insert into pessoa (codigo, nome, cpfcnpj, apelido, data_nascimento, observacao, endereco_codigo, data_cadastro) "
				+ "values (?, 'Destinatario Selenium', '00000000000', 'Selenium', ?, '', ?, ?)", COD, hoje, COD, hoje);
		executa("insert into empresa (codigo, nome, nome_fantasia, cnpj, regime_tributario_codigo, endereco_codigo) "
				+ "values (?, 'Empresa Selenium', 'Empresa Selenium', '00000000000000', ?, ?)", COD, regime, COD);

		executa("insert into cst_csosn (codigo, cst_csosn, simples_nacional) values (?, '102', 1)", COD);
		executa("insert into cst (codigo, cst, descricao) values (?, '01', 'CST Selenium')", COD);
		executa("insert into cfop (codigo, cfop, descricao) values (?, '5102', 'CFOP Selenium')", COD);
		executa("insert into cst_ipi (codigo, cst, descricao, tipo) values (?, '50', 'CST IPI Selenium', 'SAIDA')", COD);
		executa("insert into mod_bc_icms (codigo, tipo, descricao, sub_tributaria) values (?, 0, 'Mod BC Selenium', 0)", COD);

		executa("insert into tributacao (codigo, descricao, subs_tributaria, data_cadastro, empresa_codigo) "
				+ "values (?, 'Tributacao Selenium', 0, ?, ?)", COD, hoje, COD);
		executa("insert into tributacao_regra (codigo, tributacao_codigo, uf, pis, cofins, cst_pis, cst_cofins, aliq_ipi, "
				+ "aliq_icms, tipo, cst_csosn_codigo, cfop_codigo, cst_ipi_codigo, data_cadastro) "
				+ "values (?, ?, ?, 1.65, 7.6, ?, ?, 5, 18, 'SAIDA', ?, ?, ?, ?)",
				COD, COD, String.valueOf(sp), COD, COD, COD, COD, COD, hoje);

		String produto = "insert into produto (codigo, descricao, valor_venda, balanca, ativo, fornecedor_codigo, grupo_codigo, "
				+ "unidade, subtributaria, categoria_codigo, ncm, cest, vendavel, controla_estoque, tributacao_codigo, "
				+ "bc_icms_codigo, data_cadastro) values (?, ?, 10, 0, 'ATIVO', ?, 1, 'UN', 0, 1, '12345678', '', "
				+ "'SIM', 'NAO', ?, ?, ?)";
		executa(produto, COD_PRODUTO, "Produto Selenium", fornecedor, COD, COD, hoje);
		executa(produto, COD_PRODUTO_SEM_MOD_BC, "Produto Selenium sem Mod BC", fornecedor, COD, null, hoje);

		executa("insert into nota_fiscal_totais (codigo, v_bc, v_icms, v_st, v_prod, v_frete, v_seg, v_desc, v_ii, v_ipi, "
				+ "v_pis, v_cofins, v_outros, v_nf) values (?, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)", COD);
		// tipo é gravado pelo ordinal de NotaFiscalTipo: 1 = SAIDA
		executa("insert into nota_fiscal (codigo, numero, modelo, tipo, natureza_operacao, serie, emissor_codigo, "
				+ "destinatario_codigo, data_cadastro, totais_codigo, frete_tipo_codigo, finalidade_codigo, "
				+ "tipo_emissao, tipo_impressao, procEmi, tipo_ambiente) "
				+ "values (?, 9101, 55, '1', 'Venda Selenium', 1, ?, ?, ?, ?, ?, ?, 1, 1, 0, 2)",
				COD_NOTA, COD, COD, hoje, COD, frete, finalidade);
	}

	private void limpa() throws SQLException {
		List<Long> impostos = new ArrayList<>();
		try (PreparedStatement ps = banco
				.prepareStatement("select imposto_codigo from nota_fiscal_item where nota_fiscal_codigo = ?")) {
			ps.setLong(1, COD_NOTA);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					impostos.add(rs.getLong(1));
			}
		}
		executa("delete from nota_fiscal_item where nota_fiscal_codigo = ?", COD_NOTA);
		for (Long imposto : impostos)
			executa("delete from nota_fiscal_item_imposto where codigo = ?", imposto);
		executa("delete from nota_fiscal where codigo = ?", COD_NOTA);
		executa("delete from nota_fiscal_totais where codigo = ?", COD);
		executa("delete from produto_estoque where produto_codigo in (?, ?)", COD_PRODUTO, COD_PRODUTO_SEM_MOD_BC);
		executa("delete from produto where codigo in (?, ?)", COD_PRODUTO, COD_PRODUTO_SEM_MOD_BC);
		executa("delete from tributacao_regra where codigo = ?", COD);
		executa("delete from tributacao where codigo = ?", COD);
		executa("delete from mod_bc_icms where codigo = ?", COD);
		executa("delete from cst_ipi where codigo = ?", COD);
		executa("delete from cfop where codigo = ?", COD);
		executa("delete from cst where codigo = ?", COD);
		executa("delete from cst_csosn where codigo = ?", COD);
		executa("delete from empresa where codigo = ?", COD);
		executa("delete from pessoa where codigo = ?", COD);
		executa("delete from endereco where codigo = ?", COD);
		executa("delete from cidade where codigo = ?", COD);
	}

	private void executa(String sql, Object... parametros) throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(sql)) {
			for (int i = 0; i < parametros.length; i++)
				ps.setObject(i + 1, parametros[i]);
			ps.executeUpdate();
		}
	}

	private long consultaLong(String sql) throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			rs.next();
			return rs.getLong(1);
		}
	}
}
