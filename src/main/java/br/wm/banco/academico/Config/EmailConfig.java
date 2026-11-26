package br.wm.banco.academico.Config;

import br.wm.banco.academico.Service.TransporteEmail;
import br.wm.banco.academico.Service.TransporteResend;
import br.wm.banco.academico.Service.TransporteSmtp;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
class EmailConfig {
    @Bean
    TransporteEmail transporteEmail(ObjectProvider<JavaMailSender> smtp,
            @Value("${app.email.provider}") String provider,
            @Value("${app.recuperacao.remetente}") String smtpFrom,
            @Value("${app.email.resend.api-key}") String apiKey,
            @Value("${app.email.resend.from}") String resendFrom) {
        return switch (provider.strip().toLowerCase(Locale.ROOT)) {
            case "smtp" -> new TransporteSmtp(smtp.getObject(), smtpFrom);
            case "resend" -> resend(apiKey, resendFrom);
            default -> throw new IllegalArgumentException("EMAIL_PROVIDER deve ser smtp ou resend.");
        };
    }

    private TransporteResend resend(String apiKey, String remetente) {
        if (apiKey.isBlank() || remetente.isBlank()) {
            throw new IllegalArgumentException("Configure RESEND_API_KEY e RESEND_FROM para usar Resend.");
        }
        Duration timeout = Duration.ofSeconds(5);
        var http = HttpClient.newBuilder().connectTimeout(timeout).build();
        var requisicoes = new JdkClientHttpRequestFactory(http);
        requisicoes.setReadTimeout(timeout);
        var cliente = RestClient.builder()
                .requestFactory(requisicoes)
                .defaultHeaders(headers -> headers.setBearerAuth(apiKey.strip()))
                .build();
        return new TransporteResend(cliente, remetente.strip());
    }
}
