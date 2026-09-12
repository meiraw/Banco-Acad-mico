package br.wm.banco.academico.Exception;

public class ErrorResponse {

    private String mensagem;
    private int status;

    public ErrorResponse (String mensagem,int status){
        this.mensagem = mensagem;
        this.status = status;
    }

    public String getMensagem(){
        return mensagem;
    }

    public int getStatus(){
        return status;
    }
}
