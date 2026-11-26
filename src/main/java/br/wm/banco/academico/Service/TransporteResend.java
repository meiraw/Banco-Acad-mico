package br.wm.banco.academico.Service;

import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;

public final class TransporteResend implements TransporteEmail {
    private final RestClient cliente;
    private final String remetente;

    public TransporteResend(RestClient cliente, String remetente) {
        this.cliente = cliente;
        this.remetente = remetente;
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpo, boolean html) {
        try {
            cliente.post()
                    .uri("https://api.resend.com/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", remetente, "to", List.of(destinatario),
                            "subject", assunto, html ? "html" : "text", corpo))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        int status = response.getStatusCode().value();
                        String categoria = switch (status) {
                            case 401 -> "AUTENTICACAO";
                            case 403 -> "PERMISSAO";
                            case 429 -> "LIMITE";
                            default -> status >= 500 ? "INDISPONIVEL" : "RESPOSTA_INESPERADA";
                        };
                        throw new Falha(categoria, status);
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException ex) {
            Throwable causa = ex.getMostSpecificCause();
            boolean timeout = causa instanceof HttpTimeoutException || causa instanceof SocketTimeoutException;
            throw new Falha(timeout ? "TIMEOUT" : "CONEXAO", null);
        } catch (RestClientException ex) {
            throw new Falha("TRANSPORTE", null);
        }
    }

    static final class Falha extends MailException {
        private final String diagnostico;

        private Falha(String categoria, Integer status) {
            super("Falha no envio pelo Resend.");
            diagnostico = status == null ? categoria : categoria + " (HTTP " + status + ")";
        }

        String diagnostico() {
            return diagnostico;
        }
    }
}
