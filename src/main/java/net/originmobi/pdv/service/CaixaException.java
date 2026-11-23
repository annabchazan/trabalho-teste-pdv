package net.originmobi.pdv.service;

/**
 * Erro de regra ou de processo na abertura e no fechamento de caixa. A mensagem
 * é a que chega ao usuário na tela.
 */
public class CaixaException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CaixaException(String mensagem) {
		super(mensagem);
	}

	public CaixaException(String mensagem, Throwable causa) {
		super(mensagem, causa);
	}
}
