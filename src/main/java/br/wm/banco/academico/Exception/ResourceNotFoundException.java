package br.wm.banco.academico.Exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException (String mensagem){
        super(mensagem);
    }
}
