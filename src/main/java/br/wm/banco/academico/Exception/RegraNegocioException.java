package br.wm.banco.academico.Exception;

public class RegraNegocioException extends RuntimeException {
        public RegraNegocioException(String mensagem) {
            super(mensagem); //Ela é uma exception criada por nós para situações em que o código funciona, mas uma regra do sistema impede a operação.
        }
}
