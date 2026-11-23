package net.originmobi.pdv.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class CaixaService {

	private static final Map<CaixaTipo, String> DESCRICAO_PADRAO = new EnumMap<>(CaixaTipo.class);
	private static final Map<CaixaTipo, String> OBSERVACAO_ABERTURA = new EnumMap<>(CaixaTipo.class);

	static {
		DESCRICAO_PADRAO.put(CaixaTipo.CAIXA, "Caixa diário");
		DESCRICAO_PADRAO.put(CaixaTipo.COFRE, "Cofre");
		DESCRICAO_PADRAO.put(CaixaTipo.BANCO, "Banco");

		OBSERVACAO_ABERTURA.put(CaixaTipo.CAIXA, "Abertura de caixa");
		OBSERVACAO_ABERTURA.put(CaixaTipo.COFRE, "Abertura de cofre");
		OBSERVACAO_ABERTURA.put(CaixaTipo.BANCO, "Abertura de banco");
	}

	private final CaixaRepository caixas;
	private final UsuarioService usuarios;
	private final CaixaLancamentoService lancamentos;
	private final Clock relogio;

	@Autowired
	public CaixaService(CaixaRepository caixas, UsuarioService usuarios, CaixaLancamentoService lancamentos) {
		this(caixas, usuarios, lancamentos, Clock.systemDefaultZone());
	}

	CaixaService(CaixaRepository caixas, UsuarioService usuarios, CaixaLancamentoService lancamentos, Clock relogio) {
		this.caixas = caixas;
		this.usuarios = usuarios;
		this.lancamentos = lancamentos;
		this.relogio = relogio;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long cadastro(Caixa caixa) {
		validaAbertura(caixa);

		Usuario usuario = usuarios.buscaUsuario(Aplicacao.getInstancia().getUsuarioAtual());

		caixa.setDescricao(descricaoDoCaixa(caixa));
		caixa.setUsuario(usuario);
		caixa.setData_cadastro(Date.valueOf(LocalDate.now(relogio)));

		// se for BANCO, limpa os valores especiais de agencia e conta
		if (caixa.getTipo().equals(CaixaTipo.BANCO)) {
			caixa.setAgencia(somenteDigitos(caixa.getAgencia()));
			caixa.setConta(somenteDigitos(caixa.getConta()));
		}

		try {
			caixas.save(caixa);
		} catch (Exception e) {
			throw new CaixaException("Erro no processo de abertura, chame o suporte técnico", e);
		}

		if (caixa.getValor_abertura() > 0)
			registraLancamentoDeAbertura(caixa, usuario);
		else
			// sem lançamento de abertura o valor total do caixa fica 0.0
			caixa.setValor_total(0.0);

		return caixa.getCodigo();
	}

	private void validaAbertura(Caixa caixa) {
		if (caixa.getTipo().equals(CaixaTipo.CAIXA) && caixaIsAberto())
			throw new CaixaException("Existe caixa de dias anteriores em aberto, favor verifique");

		// valor de abertura null vira 0.0, valor que também vai para o valor_total
		if (caixa.getValor_abertura() == null)
			caixa.setValor_abertura(0.0);

		if (caixa.getValor_abertura() < 0)
			throw new CaixaException("Valor informado é inválido");
	}

	private static String descricaoDoCaixa(Caixa caixa) {
		return caixa.getDescricao().isEmpty() ? DESCRICAO_PADRAO.get(caixa.getTipo()) : caixa.getDescricao();
	}

	private static String somenteDigitos(String valor) {
		return valor.replaceAll("\\D", "");
	}

	private void registraLancamentoDeAbertura(Caixa caixa, Usuario usuario) {
		CaixaLancamento lancamento = new CaixaLancamento(OBSERVACAO_ABERTURA.get(caixa.getTipo()),
				caixa.getValor_abertura(), TipoLancamento.SALDOINICIAL, EstiloLancamento.ENTRADA, caixa, usuario);

		try {
			lancamentos.lancamento(lancamento);
		} catch (Exception e) {
			throw new CaixaException("Erro no processo, chame o suporte", e);
		}
	}

	public String fechaCaixa(Long caixa, String senha) {
		Usuario usuario = usuarios.buscaUsuario(Aplicacao.getInstancia().getUsuarioAtual());

		if (senha.isEmpty())
			return "Favor, informe a senha";

		if (!new BCryptPasswordEncoder().matches(senha, usuario.getSenha()))
			return "Senha incorreta, favor verifique";

		Optional<Caixa> caixaAtual = caixas.findById(caixa);

		if (caixaAtual.map(Caixa::getData_fechamento).isPresent())
			throw new CaixaException("Caixa já esta fechado");

		Double valorTotal = caixaAtual.map(Caixa::getValor_total).orElse(0.0);

		Caixa fechado = caixaAtual.get();
		fechado.setData_fechamento(Timestamp.valueOf(LocalDateTime.now(relogio)));
		fechado.setValor_fechamento(valorTotal);

		try {
			caixas.save(fechado);
		} catch (Exception e) {
			throw new CaixaException("Ocorreu um erro ao fechar o caixa, chame o suporte", e);
		}

		return "Caixa fechado com sucesso";
	}

	public boolean caixaIsAberto() {
		return caixas.caixaAberto().isPresent();
	}

	public List<Caixa> listaTodos() {
		return caixas.findByCodigoOrdenado();
	}

	public List<Caixa> listarCaixas(CaixaFilter filter) {
		if (temData(filter.getData_cadastro())) {
			filter.setData_cadastro(filter.getData_cadastro().replace("/", "-"));
			return caixas.buscaCaixasPorDataAbertura(Date.valueOf(filter.getData_cadastro()));
		}

		return caixas.listaCaixasAbertos();
	}

	public Optional<Caixa> caixaAberto() {
		return caixas.caixaAberto();
	}

	public List<Caixa> caixasAbertos() {
		return caixas.caixasAbertos();
	}

	public Optional<Caixa> busca(Long codigo) {
		return caixas.findById(codigo);
	}

	// pega o caixa aberto do usuário informado
	public Optional<Caixa> buscaCaixaUsuario(String usuario) {
		Usuario usu = usuarios.buscaUsuario(usuario);
		return Optional.ofNullable(caixas.findByCaixaAbertoUsuario(usu.getCodigo()));
	}

	public List<Caixa> listaBancos() {
		return caixas.buscaBancos(CaixaTipo.BANCO);
	}

	public List<Caixa> listaCaixasAbertosTipo(CaixaTipo tipo) {
		return caixas.buscaCaixaTipo(tipo);
	}

	public List<Caixa> listaBancosAbertosTipoFilterBanco(CaixaTipo tipo, BancoFilter filter) {
		if (temData(filter.getData_cadastro())) {
			filter.setData_cadastro(filter.getData_cadastro().replace("/", "-"));
			return caixas.buscaCaixaTipoData(tipo, Date.valueOf(filter.getData_cadastro()));
		}

		return caixas.buscaCaixaTipo(CaixaTipo.BANCO);
	}

	private static boolean temData(String data) {
		return data != null && !data.isEmpty();
	}
}
