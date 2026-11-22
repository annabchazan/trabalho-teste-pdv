package net.originmobi.pdv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

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
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Testes de sistema da venda (TS-VEN-01 a 07), pela interface web com
 * Selenium. Projeto e resultados em docs/teste-sistema-venda.md.
 *
 * Pré-requisitos: aplicação no ar (docker compose up -d) em
 * http://localhost:8080 e Chrome instalado. A massa de dados (produto, título,
 * formas de pagamento, caixa e vendas) é criada direto no MySQL do
 * docker-compose (localhost:3307), com códigos 9201/9202 e vendas com a
 * observação "Venda Selenium", e apagada antes e depois de cada caso. O caixa
 * do tipo CAIXA que já estiver aberto no banco é marcado como fechado durante
 * o caso e restaurado no fim (decisão D-20). Por padrão o Chrome roda sem
 * janela; -Dselenium.headless=false mostra o navegador.
 */
public class VendaSistemaIT {

	private static final String URL = System.getProperty("pdv.url", "http://localhost:8080");
	private static final String DB_URL = System.getProperty("pdv.db.url",
			"jdbc:mysql://localhost:3307/pdv?allowPublicKeyRetrieval=true&useSSL=false");

	private static final String OBS = "Venda Selenium";
	private static final long COD_PRODUTO = 9201L;
	private static final long COD_TITULO_DIN = 9201L;
	private static final long COD_AVISTA = 9201L;
	private static final long COD_APRAZO = 9202L;
	private static final long COD_CAIXA = 9201L;
	private static final long COD_USUARIO_GERENTE = 1L;
	private static final long COD_CLIENTE = 1L;
	private static final double VALOR_PRODUTO = 10.0;
	private static final double DELTA = 0.001;

	// requisito não funcional TS-VEN-07
	private static final long LIMITE_FECHAMENTO_MS = 2000;
	private static final int MEDICOES = 5;

	private WebDriver driver;
	private WebDriverWait espera;
	private Connection banco;
	private final List<Object[]> caixasOriginaisAbertos = new ArrayList<>();

	@Before
	public void setUp() throws SQLException {
		banco = DriverManager.getConnection(DB_URL, "root", "123456");
		limpa();
		fechaCaixasOriginais();
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
			try {
				limpa();
			} finally {
				restauraCaixasOriginais();
				banco.close();
			}
		}
	}

	// TS-VEN-01 (CT-VENDA-01): abrir venda, incluir e remover produto

	@Test
	public void abrirVendaIncluirERemoverProduto_deveAtualizarItensETotal() throws SQLException {
		entraNoSistema();

		driver.get(URL + "/venda/form");
		new Select(espera.until(ExpectedConditions.elementToBeClickable(By.id("cliente"))))
				.selectByValue(String.valueOf(COD_CLIENTE));
		driver.findElement(By.id("observacao")).sendKeys(OBS);
		driver.findElement(By.id("btn-salva")).click();

		espera.until(ExpectedConditions.urlMatches("/venda/\\d+$"));
		long venda = Long.parseLong(driver.getCurrentUrl().replaceAll(".*/venda/", ""));
		assertEquals("ABERTA", consultaTexto("select situacao from venda where codigo = ?", venda));

		insereProduto();
		List<WebElement> linhas = esperaQuantidadeDeItens(1);
		assertEquals("Produto Selenium", linhas.get(0).findElements(By.tagName("td")).get(1).getText());
		assertEquals(VALOR_PRODUTO, esperaTotalNaTela(VALOR_PRODUTO), DELTA);

		driver.findElement(By.cssSelector("#tabProdutoVenda a.js-removeproduto-venda")).click();
		Alert confirma = espera.until(ExpectedConditions.alertIsPresent());
		assertEquals("Remover produto?", confirma.getText());
		confirma.accept();

		esperaQuantidadeDeItens(0);
		assertEquals(0.0, esperaTotalNaTela(0.0), DELTA);
	}

	// TS-VEN-02 (CT-VENDA-02): fechar à vista em dinheiro

	@Test
	public void fecharAVistaEmDinheiro_deveFinalizarEBloquearNovosItens() throws SQLException {
		abreCaixaDeTeste();
		long venda = vendaComUmItem(null);
		entraNoSistema();
		abreVenda(venda);

		preparaFechamento(COD_AVISTA, null, null);
		salvaPrint("TS-VEN-02-modal-pagamento");
		driver.findElement(By.cssSelector(".btn-pagamento")).click();
		Alert alerta = espera.until(ExpectedConditions.alertIsPresent());
		String mensagem = alerta.getText();
		alerta.accept();

		assertEquals("Venda finalizada com sucesso", mensagem);
		assertEquals("FECHADA", consultaTexto("select situacao from venda where codigo = ?", venda));
		assertEquals(VALOR_PRODUTO, consultaDouble("select valor from caixa_lancamento where caixa_codigo = ?",
				COD_CAIXA), DELTA);

		// depois do reload a tela da venda fechada bloqueia o botão de pagamento e o combo de produtos
		espera.until(d -> !d.findElement(By.id("btn-venda")).isEnabled());
		assertTrue(!driver.findElement(By.id("codigoProduto")).isEnabled());
		salvaPrint("TS-VEN-02-venda-fechada");
	}

	// TS-VEN-03 (CT-VENDA-03): fechar a prazo com cliente

	@Test
	public void fecharAPrazoComCliente_deveFinalizarEGerarParcela() throws SQLException {
		// Num banco recém-criado, parcela.data_alteracao é obrigatória e sem
		// valor padrão (defeito D4, Issue #10) e o fechamento a prazo falha.
		// Nesse caso o teste é pulado. Ver a pré-condição em
		// docs/teste-sistema-venda.md.
		assumeTrue("Pulado: parcela.data_alteracao sem valor padrão no banco (defeito D4, Issue #10). "
				+ "Aplique: alter table parcela modify data_alteracao timestamp not null default current_timestamp",
				parcelaAceitaInsertSemDataAlteracao());

		long venda = vendaComUmItem(COD_CLIENTE);
		entraNoSistema();
		abreVenda(venda);

		String mensagem = fechaPelaTela(COD_APRAZO, null, null);

		assertEquals("Venda finalizada com sucesso", mensagem);
		assertEquals("FECHADA", consultaTexto("select situacao from venda where codigo = ?", venda));
		assertEquals(LocalDate.now().plusDays(30).toString(), consultaTexto(
				"select date(p.data_vencimento) from parcela p join receber r on r.codigo = p.receber_codigo "
						+ "where r.venda_codigo = ?", venda));
	}

	// TS-VEN-04 (CT-VENDA-06): fechar à vista sem caixa aberto

	@Test
	public void fecharAVistaSemCaixaAberto_deveMostrarMensagemEManterVendaAberta() throws SQLException {
		long venda = vendaComUmItem(null);
		entraNoSistema();
		abreVenda(venda);

		String mensagem = fechaPelaTela(COD_AVISTA, null, null);

		assertEquals("nenhum caixa aberto", mensagem);
		assertEquals("ABERTA", consultaTexto("select situacao from venda where codigo = ?", venda));
		assertEquals(0L, consultaLong("select count(*) from receber where venda_codigo = ?", venda));
	}

	// TS-VEN-05 (CT-VENDA-07): fechar a prazo sem cliente

	@Test
	public void fecharAPrazoSemCliente_deveMostrarMensagemEManterVendaAberta() throws SQLException {
		long venda = vendaComUmItem(null);
		entraNoSistema();
		abreVenda(venda);

		String mensagem = fechaPelaTela(COD_APRAZO, null, null);

		assertEquals("Venda sem cliente, verifique", mensagem);
		assertEquals("ABERTA", consultaTexto("select situacao from venda where codigo = ?", venda));
		assertEquals(0L, consultaLong("select count(*) from receber where venda_codigo = ?", venda));
	}

	// TS-VEN-06 (CT-VENDA-08): desconto no fechamento à vista em dinheiro.
	// Falha hoje pelo defeito D-VEN-01: avistaDinheiro recebe desconto e acréscimo
	// trocados e lança no caixa 10 + 2 = 12,00 em vez de 10 - 2 = 8,00.
	// @Ignore até o defeito ser corrigido; remover a anotação reproduz a falha.
	@Ignore("D-VEN-01: desconto e acréscimo trocados no lançamento de caixa à vista")
	@Test
	public void fecharAVistaComDesconto_deveLancarNoCaixaOValorComDesconto() throws SQLException {
		abreCaixaDeTeste();
		long venda = vendaComUmItem(null);
		entraNoSistema();
		abreVenda(venda);

		String mensagem = fechaPelaTela(COD_AVISTA, "200", null);

		assertEquals("Venda finalizada com sucesso", mensagem);
		assertEquals(8.0, consultaDouble("select valor_total from venda where codigo = ?", venda), DELTA);
		assertEquals(8.0, consultaDouble("select valor from caixa_lancamento where caixa_codigo = ?", COD_CAIXA),
				DELTA);
	}

	// TS-VEN-07: não funcional (eficiência de desempenho - comportamento temporal).
	// Requisito: fechar uma venda pela tela leva no máximo 2 s, do clique em "Pagar"
	// até a mensagem de confirmação. 1 fechamento de aquecimento + 5 medidos.

	@Test
	public void fecharVenda_deveResponderEmAte2Segundos() throws SQLException {
		abreCaixaDeTeste();
		entraNoSistema();

		List<Long> tempos = new ArrayList<>();
		for (int i = 0; i <= MEDICOES; i++) {
			long venda = vendaComUmItem(null);
			abreVenda(venda);
			long inicio = preparaFechamento(COD_AVISTA, null, null);
			driver.findElement(By.cssSelector(".btn-pagamento")).click();
			Alert alerta = espera.until(ExpectedConditions.alertIsPresent());
			long tempo = (System.nanoTime() - inicio) / 1_000_000;
			String mensagem = alerta.getText();
			alerta.accept();

			assertEquals("Venda finalizada com sucesso", mensagem);
			if (i > 0)
				tempos.add(tempo);
		}

		long maior = Collections.max(tempos);
		double media = tempos.stream().mapToLong(Long::longValue).average().orElse(0);
		System.out.println("TS-VEN-07 tempos de fechamento (ms): " + tempos + " | média " + Math.round(media)
				+ " | maior " + maior + " | limite " + LIMITE_FECHAMENTO_MS);

		assertTrue("maior tempo " + maior + " ms acima do limite de " + LIMITE_FECHAMENTO_MS + " ms",
				maior <= LIMITE_FECHAMENTO_MS);
	}

	// passos na interface

	private void entraNoSistema() {
		driver.get(URL + "/login");
		driver.findElement(By.name("username")).sendKeys("gerente");
		driver.findElement(By.name("password")).sendKeys("123");
		driver.findElement(By.id("btn-login")).click();
		espera.until(d -> !d.getCurrentUrl().contains("/login"));
	}

	private void abreVenda(long venda) {
		driver.get(URL + "/venda/" + venda);
		espera.until(ExpectedConditions.elementToBeClickable(By.id("btn-venda")));
	}

	private void insereProduto() {
		// o combo usa bootstrap-select: abre a lista e clica na opção pelo texto
		espera.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-id='codigoProduto']"))).click();
		By opcao = By.xpath("//div[contains(@class,'bootstrap-select')]//li//a[contains(normalize-space(.),'COD: "
				+ COD_PRODUTO + " -')]");
		espera.until(ExpectedConditions.elementToBeClickable(opcao)).click();
		driver.findElement(By.cssSelector(".js-addvenda-produto")).click();
	}

	/**
	 * Abre o modal de pagamento, escolhe a forma de pagamento e o título e
	 * informa desconto e acréscimo (digitados como no campo com máscara, ex.:
	 * "200" = 2,00). Devolve o instante logo antes do clique em "Pagar".
	 */
	private long preparaFechamento(long formaPagamento, String desconto, String acrescimo) {
		driver.findElement(By.id("btn-venda")).click();
		WebElement combo = espera.until(ExpectedConditions.elementToBeClickable(By.id("pagamento")));
		new Select(combo).selectByValue(String.valueOf(formaPagamento));

		By tituloDaParcela = By.cssSelector("#tipopagamento .dadoslinha select option[value='" + COD_TITULO_DIN + "']");
		espera.until(ExpectedConditions.presenceOfElementLocated(tituloDaParcela));
		new Select(driver.findElement(By.cssSelector("#tipopagamento .dadoslinha select")))
				.selectByValue(String.valueOf(COD_TITULO_DIN));

		if (desconto != null)
			driver.findElement(By.id("desconto")).sendKeys(desconto);
		if (acrescimo != null)
			driver.findElement(By.id("acrescimo")).sendKeys(acrescimo);

		return System.nanoTime();
	}

	private String fechaPelaTela(long formaPagamento, String desconto, String acrescimo) {
		preparaFechamento(formaPagamento, desconto, acrescimo);
		driver.findElement(By.cssSelector(".btn-pagamento")).click();
		Alert alerta = espera.until(ExpectedConditions.alertIsPresent());
		String mensagem = alerta.getText();
		alerta.accept();
		return mensagem;
	}

	// evidência: grava o print da tela em target/selenium-prints
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

	private List<WebElement> itensNaTela() {
		return driver.findElements(By.cssSelector("#tabdados tbody tr"));
	}

	private List<WebElement> esperaQuantidadeDeItens(int quantidade) {
		espera.until(d -> itensNaTela().size() == quantidade);
		return itensNaTela();
	}

	private double esperaTotalNaTela(double esperado) {
		try {
			espera.until(d -> Math.abs(totalNaTela() - esperado) < DELTA);
		} catch (org.openqa.selenium.TimeoutException e) {
			// deixa o assert mostrar o valor encontrado
		}
		return totalNaTela();
	}

	private double totalNaTela() {
		return valor(driver.findElement(By.cssSelector("#total_produtos h4")).getText());
	}

	// aceita "R$ 10.0", "10,00" ou "1.010,00"
	private static double valor(String texto) {
		String limpo = texto.replace("R$", "").trim();
		if (limpo.contains(","))
			limpo = limpo.replace(".", "").replace(",", ".");
		return limpo.isEmpty() ? 0.0 : Double.parseDouble(limpo);
	}

	// massa de dados no banco

	// true se o insert da parcela funciona sem informar data_alteracao (D4, Issue #10)
	private boolean parcelaAceitaInsertSemDataAlteracao() throws SQLException {
		try (Statement st = banco.createStatement();
				ResultSet rs = st.executeQuery("select column_default, is_nullable from information_schema.columns "
						+ "where table_schema = database() and table_name = 'parcela' "
						+ "and column_name = 'data_alteracao'")) {
			return rs.next() && (rs.getString(1) != null || "YES".equals(rs.getString(2)));
		}
	}

	private long vendaComUmItem(Long cliente) throws SQLException {
		long venda;
		try (PreparedStatement ps = banco.prepareStatement("insert into venda (observacao, valor_produtos, situacao, "
				+ "pessoa_codigo, usuario_codigo, data_cadastro) values (?, 0, 'ABERTA', ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, OBS);
			ps.setObject(2, cliente);
			ps.setLong(3, COD_USUARIO_GERENTE);
			ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				rs.next();
				venda = rs.getLong(1);
			}
		}
		// o trigger tr_atualizaTotalProdutosVenda soma o valor do produto na venda
		executa("insert into venda_produtos (produto_codigo, venda_codigo, valor_balanca) values (?, ?, 0)",
				COD_PRODUTO, venda);
		return venda;
	}

	private void abreCaixaDeTeste() throws SQLException {
		executa("insert into caixa (codigo, descricao, valor_abertura, valor_total, valor_entrada, valor_saida, "
				+ "tipo, usuario_codigo, data_cadastro) values (?, 'Caixa Selenium', 0, 0, 0, 0, 'CAIXA', ?, curdate())",
				COD_CAIXA, COD_USUARIO_GERENTE);
	}

	private void criaMassaDeDados() throws SQLException {
		long fornecedor = consultaLong("select min(codigo) from fornecedor");

		// o trigger insere_estoque_inicial cria o saldo com 0; o caso precisa de estoque
		executa("insert into produto (codigo, descricao, valor_venda, balanca, ativo, fornecedor_codigo, grupo_codigo, "
				+ "unidade, subtributaria, categoria_codigo, vendavel, controla_estoque, data_cadastro) "
				+ "values (?, 'Produto Selenium', ?, 0, 'ATIVO', ?, 1, 'UN', 0, 1, 'SIM', 'SIM', curdate())",
				COD_PRODUTO, VALOR_PRODUTO, fornecedor);
		executa("update produto_estoque set qtd = 100 where produto_codigo = ?", COD_PRODUTO);

		// tipo 1 = Dinheiro (DIN), carregado pelo Flyway
		executa("insert into titulo (codigo, descricao, tipo_codigo) values (?, 'Dinheiro Selenium', 1)",
				COD_TITULO_DIN);

		String pagamento = "insert into pagamento_tipo (codigo, descricao, forma_pagamento, qtd_parcelas, "
				+ "data_cadastro) values (?, ?, ?, 1, curdate())";
		executa(pagamento, COD_AVISTA, "A vista Selenium", "00");
		executa(pagamento, COD_APRAZO, "A prazo Selenium", "30");
	}

	private void limpa() throws SQLException {
		List<Long> vendas = new ArrayList<>();
		try (PreparedStatement ps = banco.prepareStatement("select codigo from venda where observacao = ?")) {
			ps.setString(1, OBS);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					vendas.add(rs.getLong(1));
			}
		}
		for (Long venda : vendas) {
			executa("delete p from parcela p join receber r on r.codigo = p.receber_codigo where r.venda_codigo = ?",
					venda);
			executa("delete from receber where venda_codigo = ?", venda);
			executa("delete from pagamento_tipo_venda where ven_codigo = ?", venda);
			executa("delete from venda_produtos where venda_codigo = ?", venda);
			executa("delete from venda where codigo = ?", venda);
		}
		executa("delete from caixa_lancamento where caixa_codigo = ?", COD_CAIXA);
		executa("delete from caixa where codigo = ?", COD_CAIXA);
		executa("delete from estoque_movimentacao where produto_codigo = ?", COD_PRODUTO);
		executa("delete from produto_estoque where produto_codigo = ?", COD_PRODUTO);
		executa("delete from produto where codigo = ?", COD_PRODUTO);
		executa("delete from titulo where codigo = ?", COD_TITULO_DIN);
		executa("delete from pagamento_tipo where codigo in (?, ?)", COD_AVISTA, COD_APRAZO);
	}

	// caixa já aberto no banco (D-20): fechado durante o caso e restaurado depois

	private void fechaCaixasOriginais() throws SQLException {
		caixasOriginaisAbertos.clear();
		try (PreparedStatement ps = banco.prepareStatement("select codigo, data_alteracao from caixa "
				+ "where tipo = 'CAIXA' and data_fechamento is null and codigo <> ?")) {
			ps.setLong(1, COD_CAIXA);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					caixasOriginaisAbertos.add(new Object[] { rs.getLong(1), rs.getTimestamp(2) });
			}
		}
		for (Object[] caixa : caixasOriginaisAbertos)
			executa("update caixa set data_fechamento = now(), data_alteracao = data_alteracao where codigo = ?",
					caixa[0]);
	}

	private void restauraCaixasOriginais() throws SQLException {
		for (Object[] caixa : caixasOriginaisAbertos)
			executa("update caixa set data_fechamento = null, data_alteracao = ? where codigo = ?", caixa[1], caixa[0]);
		caixasOriginaisAbertos.clear();
	}

	// acesso ao banco

	private void executa(String sql, Object... parametros) throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(sql)) {
			for (int i = 0; i < parametros.length; i++)
				ps.setObject(i + 1, parametros[i]);
			ps.executeUpdate();
		}
	}

	private long consultaLong(String sql, Object... parametros) throws SQLException {
		return Long.parseLong(consultaTexto(sql, parametros));
	}

	private double consultaDouble(String sql, Object... parametros) throws SQLException {
		return Double.parseDouble(consultaTexto(sql, parametros));
	}

	private String consultaTexto(String sql, Object... parametros) throws SQLException {
		try (PreparedStatement ps = banco.prepareStatement(sql)) {
			for (int i = 0; i < parametros.length; i++)
				ps.setObject(i + 1, parametros[i]);
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getString(1);
			}
		}
	}
}
