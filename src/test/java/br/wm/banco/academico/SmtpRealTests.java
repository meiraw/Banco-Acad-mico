package br.wm.banco.academico;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: envia uma mensagem de diagnóstico apenas à própria conta SMTP. */
@EnabledIfSystemProperty(named = "smtp.real", matches = "true")
class SmtpRealTests {
    @Test
    void autenticaEEnviaDiagnosticoParaPropriaConta() throws Exception {
        Properties env = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of(".env"), StandardCharsets.UTF_8)) {
            env.load(reader);
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(required(env, "SMTP_HOST"));
        sender.setPort(Integer.parseInt(required(env, "SMTP_PORT")));
        sender.setUsername(required(env, "SMTP_USERNAME"));
        sender.setPassword(required(env, "SMTP_PASSWORD"));
        sender.setDefaultEncoding("UTF-8");
        Properties settings = sender.getJavaMailProperties();
        settings.setProperty("mail.smtp.auth", env.getProperty("SMTP_AUTH", "true"));
        settings.setProperty("mail.smtp.starttls.enable", env.getProperty("SMTP_STARTTLS", "true"));
        settings.setProperty("mail.smtp.starttls.required", env.getProperty("SMTP_STARTTLS", "true"));
        settings.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        settings.setProperty("mail.smtp.connectiontimeout", "10000");
        settings.setProperty("mail.smtp.timeout", "10000");
        settings.setProperty("mail.smtp.writetimeout", "10000");
        try {
            sender.testConnection();
        } catch (Exception ex) {
            fail("SMTP não conectou/autenticou. Tipo: " + ex.getClass().getSimpleName());
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(required(env, "SMTP_FROM"));
        message.setTo(sender.getUsername());
        message.setSubject("Banco Acadêmico — teste de configuração SMTP");
        message.setText("Teste solicitado para verificar o envio de e-mails do Banco Acadêmico. "
                + "Esta mensagem é de diagnóstico, não altera sua senha e não contém link de recuperação. "
                + "O recebimento confirma a entrega desta mensagem na sua caixa de e-mail.");
        try {
            sender.send(message);
        } catch (Exception ex) {
            fail("SMTP não aceitou a mensagem. Tipo: " + ex.getClass().getSimpleName());
        }
    }

    private static String required(Properties env, String key) {
        String value = env.getProperty(key);
        assertNotNull(value, key + " ausente");
        assertFalse(value.isBlank(), key + " vazio");
        return value;
    }
}
