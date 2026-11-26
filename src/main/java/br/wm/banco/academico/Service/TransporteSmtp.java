package br.wm.banco.academico.Service;

import jakarta.mail.MessagingException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

public final class TransporteSmtp implements TransporteEmail {
    private final JavaMailSender mail;
    private final String remetente;

    public TransporteSmtp(JavaMailSender mail, String remetente) {
        this.mail = mail;
        this.remetente = remetente;
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpo, boolean html) {
        if (!html) {
            var mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente);
            mensagem.setTo(destinatario);
            mensagem.setSubject(assunto);
            mensagem.setText(corpo);
            mail.send(mensagem);
            return;
        }

        var mensagem = mail.createMimeMessage();
        try {
            var helper = new MimeMessageHelper(mensagem, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo, true);
        } catch (MessagingException ex) {
            throw new MailPreparationException("Não foi possível preparar o e-mail.", ex);
        }
        mail.send(mensagem);
    }
}
