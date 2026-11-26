package br.wm.banco.academico.Service;

public interface TransporteEmail {
    void enviar(String destinatario, String assunto, String corpo, boolean html);
}
