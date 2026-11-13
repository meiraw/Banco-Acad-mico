package br.wm.banco.academico;

import br.wm.banco.academico.Service.*;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import java.net.*;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EmailFalhasTests {
    static void verificarLogSeguro(Class<?> servico, Runnable operacao, String... segredos) {
        Logger logger = (Logger) LoggerFactory.getLogger(servico);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            assertDoesNotThrow(operacao::run);
            assertEquals(1, appender.list.size());
            var evento = appender.list.getFirst();
            assertTrue(evento.getFormattedMessage().contains("categoria="));
            assertFalse(evento.getFormattedMessage().contains("SENHA_TOKEN_DESTINATARIO_NAO_LOGAR"));
            for (String segredo : segredos) assertFalse(evento.getFormattedMessage().contains(segredo));
            assertNull(evento.getThrowableProxy());
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    static MailException falha(String tipo) {
        String segredo = "SENHA_TOKEN_DESTINATARIO_NAO_LOGAR";
        return switch (tipo) {
            case "autenticacao" -> new MailAuthenticationException(segredo);
            case "timeout" -> new MailSendException(segredo, new SocketTimeoutException(segredo));
            case "conexao" -> new MailSendException(segredo, new ConnectException(segredo));
            case "preparacao" -> new MailPreparationException(segredo);
            default -> new MailSendException(segredo);
        };
    }

    @Test void classificaSemUsarTextoDaExcecao() {
        assertEquals("AUTENTICACAO", DiagnosticoEmail.categoria(falha("autenticacao")));
        assertEquals("TIMEOUT", DiagnosticoEmail.categoria(falha("timeout")));
        assertEquals("CONEXAO", DiagnosticoEmail.categoria(falha("conexao")));
        assertEquals("PREPARACAO", DiagnosticoEmail.categoria(falha("preparacao")));
        assertEquals("TRANSPORTE", DiagnosticoEmail.categoria(falha("rejeicao")));
        var aninhada = new jakarta.mail.MessagingException("segredo", new SocketTimeoutException());
        var agregada = new MailSendException(java.util.Map.of(new Object(), aninhada));
        assertEquals("TIMEOUT", DiagnosticoEmail.categoria(agregada));
    }

    @Test void moderacaoRegistraCategoriaSemExporSegredosOuRepetirEnvio() {
        JavaMailSender mail = mock(JavaMailSender.class);
        Logger logger = (Logger) LoggerFactory.getLogger(ModeracaoEmailService.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            doThrow(falha("autenticacao")).when(mail).send(any(SimpleMailMessage.class));
            var service = new ModeracaoEmailService(mail, "origem@example.test", "destino@example.test");
            assertDoesNotThrow(() -> service.notificar(new ModeracaoEmailService.MaterialPendente(
                    UUID.randomUUID(), "TITULO_PRIVADO", "DISCIPLINA_PRIVADA", "AUTOR_PRIVADO", Instant.now())));
            verify(mail, times(1)).send(any(SimpleMailMessage.class));
            assertEquals(1, appender.list.size());
            var evento = appender.list.getFirst();
            assertTrue(evento.getFormattedMessage().contains("AUTENTICACAO"));
            for (String segredo : new String[]{"SENHA_TOKEN", "example.test", "TITULO_PRIVADO", "AUTOR_PRIVADO"})
                assertFalse(evento.getFormattedMessage().contains(segredo));
            assertNull(evento.getThrowableProxy());
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}
