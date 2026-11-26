package br.wm.banco.academico;

import br.wm.banco.academico.Service.DiagnosticoEmail;
import br.wm.banco.academico.Service.RecuperacaoEmailService;
import br.wm.banco.academico.Service.TransporteResend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TransporteResendTests {
    private static final String URL = "https://api.resend.com/emails";
    private MockRestServiceServer servidor;
    private TransporteResend resend;

    @BeforeEach
    void configurar() {
        var builder = RestClient.builder().defaultHeaders(h -> h.setBearerAuth("chave-ficticia"));
        servidor = MockRestServiceServer.bindTo(builder).build();
        resend = new TransporteResend(builder.build(), "envio@example.test");
    }

    @ParameterizedTest
    @CsvSource({"true,html", "false,text"})
    void enviaMensagemComFormatoOriginal(boolean html, String campo) {
        servidor.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave-ficticia"))
                .andExpect(content().json("""
                        {"from":"envio@example.test","to":["destino@example.test"],"subject":"Assunto","%s":"Conteúdo"}
                        """.formatted(campo), org.springframework.test.json.JsonCompareMode.STRICT))
                .andRespond(withSuccess());

        resend.enviar("destino@example.test", "Assunto", "Conteúdo", html);
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource({"302,RESPOSTA_INESPERADA", "400,RESPOSTA_INESPERADA", "401,AUTENTICACAO", "403,PERMISSAO",
            "429,LIMITE", "503,INDISPONIVEL"})
    void classificaSemIncluirCorpoDaResposta(int status, String categoria) {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status)).body("conteúdo privado"));

        var falha = assertThrows(MailException.class,
                () -> resend.enviar("destino@example.test", "Assunto", "código fictício", false));
        assertEquals(categoria + " (HTTP " + status + ")", DiagnosticoEmail.categoria(falha));
        assertFalse(falha.getMessage().contains("conteúdo privado"));
        assertNull(falha.getCause());
        servidor.verify();
    }

    @Test
    void recuperacaoEnviaCodigoNoTemplateHtml() {
        servidor.expect(requestTo(URL))
                .andExpect(jsonPath("$.html").value(org.hamcrest.Matchers.containsString(">0123</p>")))
                .andExpect(jsonPath("$.text").doesNotExist())
                .andRespond(withSuccess());

        new RecuperacaoEmailService(resend, "https://example.test/redefinir-senha")
                .enviar("destino@example.test", "0123");
        servidor.verify();
    }

    @Test
    void diferenciaTimeoutDeFalhaDeConexao() {
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("detalhe privado")));
        servidor.expect(requestTo(URL)).andRespond(withException(new ConnectException("detalhe privado")));

        for (String categoria : new String[]{"TIMEOUT", "CONEXAO"}) {
            var falha = assertThrows(MailException.class,
                    () -> resend.enviar("destino@example.test", "Assunto", "Corpo", false));
            assertEquals(categoria, DiagnosticoEmail.categoria(falha));
            assertNull(falha.getCause());
        }
        servidor.verify();
    }
}
