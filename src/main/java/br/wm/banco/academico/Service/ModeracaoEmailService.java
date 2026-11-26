package br.wm.banco.academico.Service;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ModeracaoEmailService {
    public record MaterialPendente(UUID id, String titulo, String disciplina, String autor, Instant enviadoEm) {}
    private final TransporteEmail mail;
    private final String destinatario;

    public ModeracaoEmailService(TransporteEmail mail,
            @Value("${app.moderacao.email-admin:}") String destinatario) {
        this.mail = mail; this.destinatario = destinatario;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificar(MaterialPendente evento) {
        if (destinatario.isBlank()) {
            LoggerFactory.getLogger(getClass()).warn("Notificação de moderação desabilitada: configure MODERATION_ADMIN_EMAIL.");
            return;
        }
        try {
            mail.enviar(destinatario, "Banco Acadêmico — Novo material aguardando aprovação", """
                    Olá, administrador!

                    Um novo material acadêmico foi enviado para a plataforma e está aguardando moderação.

                    Título: %s
                    Disciplina: %s
                    Estudante responsável: %s
                    Data de envio: %s (UTC)

                    Acesse seu perfil no Banco Acadêmico e a seção Moderação de materiais para analisar a publicação.

                    Esta é uma mensagem automática do Banco Acadêmico.
                    """.formatted(evento.titulo(), evento.disciplina(), evento.autor(),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC).format(evento.enviadoEm())), false);
        } catch (RuntimeException ex) {
            // Sem exceção do provedor, destinatário, credenciais ou conteúdo do material no log.
            LoggerFactory.getLogger(getClass()).warn("Falha no envio da notificação de moderação do material {}. Material preservado. categoria={}", evento.id(), DiagnosticoEmail.categoria(ex));
        }
    }
}
