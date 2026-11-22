package net.originmobi.pdv.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ibm.icu.text.DecimalFormat;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Parcela;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Recebimento;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.RecebimentoRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;
import net.originmobi.pdv.singleton.Aplicacao;
import net.originmobi.pdv.utilitarios.DataAtual;

@Service
public class RecebimentoService {

	private static final String ERRO_AO_RECEBER = "Ocorreu um erro ao realizar o recebimento, chame o suporte";
	private static final String VALOR_SUPERIOR = "Valor de recebimento é superior aos títulos";

	private final RecebimentoRepository recebimentos;
	private final PessoaService pessoas;
	private final RecebimentoParcelaService receParcelas;
	private final ParcelaService parcelas;
	private final CaixaService caixas;
	private final UsuarioService usuarios;
	private final CaixaLancamentoService lancamentos;
	private final TituloService titulos;
	private final CartaoLancamentoService cartaoLancamentos;

	public RecebimentoService(RecebimentoRepository recebimentos, PessoaService pessoas,
			RecebimentoParcelaService receParcelas, ParcelaService parcelas, CaixaService caixas,
			UsuarioService usuarios, CaixaLancamentoService lancamentos, TituloService titulos,
			CartaoLancamentoService cartaoLancamentos) {
		this.recebimentos = recebimentos;
		this.pessoas = pessoas;
		this.receParcelas = receParcelas;
		this.parcelas = parcelas;
		this.caixas = caixas;
		this.usuarios = usuarios;
		this.lancamentos = lancamentos;
		this.titulos = titulos;
		this.cartaoLancamentos = cartaoLancamentos;
	}

	public String abrirRecebimento(Long codpes, String[] arrayParcelas) {
		List<String> codigosParcelas = codigosInformados(arrayParcelas);

		// D6: sem parcela marcada a tela envia um texto vazio
		if (codigosParcelas.isEmpty())
			throw new RuntimeException("Selecione ao menos uma parcela para receber");

		List<Parcela> lista = new ArrayList<>();
		DataAtual dataAtual = new DataAtual();
		Double vlTotal = 0.0;

		for (String codigo : codigosParcelas) {
			Parcela parcela = parcelas.busca(Long.decode(codigo));

			if (parcela.getQuitado() == 1)
				throw new RuntimeException("Parcela " + parcela.getCodigo() + " já esta quitada, verifique.");

			// D1: Long comparado por valor, e não por referência
			if (!parcela.getReceber().getPessoa().getCodigo().equals(codpes))
				throw new RuntimeException("A parcela " + parcela.getCodigo() + " não pertence ao cliente selecionado");

			lista.add(parcela);
			vlTotal = vlTotal + parcela.getValor_restante();
		}

		Optional<Pessoa> pessoa = pessoas.buscaPessoa(codpes);

		if (!pessoa.isPresent())
			throw new RuntimeException("Cliente não encontrado");

		Recebimento recebimento = new Recebimento(vlTotal, dataAtual.dataAtualTimeStamp(), pessoa.get(), lista);

		try {
			recebimentos.save(recebimento);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao receber, chame o suporte", e);
		}

		return recebimento.getCodigo().toString();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String receber(Long codreceber, Double vlrecebido, Double vlacrescimo, Double vldesconto, Long codtitulo) {
		// D2: testa o nulo antes de comparar com zero
		if (codtitulo == null || codtitulo == 0)
			throw new RuntimeException("Selecione um título para realizar o recebimento");

		// D3: Optional vazio vira mensagem de validação, e não "No value present"
		Recebimento recebimento = recebimentos.findById(codreceber)
				.orElseThrow(() -> new RuntimeException("Recebimento não encontrado, favor verifique"));

		if (recebimento.getData_processamento() != null)
			throw new RuntimeException("Recebimento já esta fechado");

		Titulo titulo = titulos.busca(codtitulo)
				.orElseThrow(() -> new RuntimeException("Título não encontrado, favor verifique"));

		// vincula o titulo ao recebimento
		recebimento.setTitulo(titulo);

		if (vlrecebido > duasCasas(recebimento.getValor_total()))
			throw new RuntimeException(VALOR_SUPERIOR);

		List<Parcela> listParcelas = receParcelas.parcelasDoReceber(codreceber);

		if (listParcelas.isEmpty())
			throw new RuntimeException("Recebimento não possue parcelas");

		if (vlrecebido <= 0.0)
			throw new RuntimeException("Valor de recebimento inválido");

		// D8: as parcelas podem ter sido pagas pela tela de contas a receber depois
		// que o recebimento foi aberto; o valor não pode passar do que ainda falta
		if (vlrecebido > duasCasas(restanteDas(listParcelas)))
			throw new RuntimeException(VALOR_SUPERIOR);

		boolean cartao = isCartao(titulo);

		// D7: valida o caixa antes de quitar qualquer parcela
		Optional<Caixa> caixa = Optional.empty();
		if (!cartao) {
			caixa = caixas.caixaAberto();
			if (!caixa.isPresent())
				throw new RuntimeException("Nenhum caixa aberto, abra o caixa para receber em dinheiro");
		}

		// distribui o valor entre as parcelas, na ordem: cada uma recebe até o que
		// falta pagar nela e a sobra passa para a seguinte
		Double vlrestante = vlrecebido;
		for (Parcela parcela : listParcelas) {
			if (vlrestante <= 0)
				break;

			Double vlquitado = Math.min(vlrestante, parcela.getValor_restante());
			vlrestante = vlrestante - vlquitado;

			try {
				parcelas.receber(parcela.getCodigo(), vlquitado, 0.00, 0.00);
			} catch (Exception e) {
				throw new RuntimeException(ERRO_AO_RECEBER, e);
			}
		}

		Usuario usuario = usuarios.buscaUsuario(new Aplicacao().getUsuarioAtual());

		if (cartao) {
			cartaoLancamentos.lancamento(vlrecebido, Optional.of(titulo));
		} else {
			CaixaLancamento lancamento = new CaixaLancamento("Referente ao recebimento " + codreceber, vlrecebido,
					TipoLancamento.RECEBIMENTO, EstiloLancamento.ENTRADA, caixa.get(), usuario);

			// vincula o recebimento ao caixa_lancamento
			lancamento.setRecebimento(recebimento);

			try {
				lancamentos.lancamento(lancamento);
			} catch (Exception e) {
				throw new RuntimeException(ERRO_AO_RECEBER, e);
			}
		}

		try {
			recebimento.setValor_recebido(vlrecebido);
			recebimento.setValor_acrescimo(vlacrescimo);
			recebimento.setValor_desconto(vldesconto);
			recebimento.setData_processamento(new DataAtual().dataAtualTimeStamp());

			recebimentos.save(recebimento);
		} catch (Exception e) {
			throw new RuntimeException(ERRO_AO_RECEBER, e);
		}

		return "Recebimento realizado com sucesso";
	}

	public String remover(Long codigo) {
		Optional<Recebimento> recebimento = recebimentos.findById(codigo);

		if (recebimento.map(Recebimento::getData_processamento).isPresent())
			throw new RuntimeException("Esse recebimento não pode ser removido, pois ele já esta processado");

		try {
			recebimentos.deleteById(codigo);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao remover orçamento, chame o suporte", e);
		}

		return "removido com sucesso";
	}

	private static List<String> codigosInformados(String[] arrayParcelas) {
		List<String> codigos = new ArrayList<>();
		if (arrayParcelas != null)
			for (String codigo : arrayParcelas)
				if (!codigo.trim().isEmpty())
					codigos.add(codigo.trim());
		return codigos;
	}

	private static boolean isCartao(Titulo titulo) {
		String sigla = titulo.getTipo().getSigla();
		return sigla.equals(TituloTipo.CARTDEB.toString()) || sigla.equals(TituloTipo.CARTCRED.toString());
	}

	private static Double restanteDas(List<Parcela> listParcelas) {
		Double restante = 0.0;
		for (Parcela parcela : listParcelas)
			restante = restante + parcela.getValor_restante();
		return restante;
	}

	private static Double duasCasas(Double valor) {
		DecimalFormat formata = new DecimalFormat("0.00");
		return Double.valueOf(formata.format(valor).replaceAll(",", "."));
	}

}
