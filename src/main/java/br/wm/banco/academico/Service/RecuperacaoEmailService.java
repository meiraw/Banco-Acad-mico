package br.wm.banco.academico.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.MessagingException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import java.nio.charset.StandardCharsets;
import java.net.URI;
@Service
public class RecuperacaoEmailService {
 private final JavaMailSender mail;
 private final String remetente, url;
 public RecuperacaoEmailService(JavaMailSender mail,
   @Value("${app.recuperacao.remetente}") String remetente,
   @Value("${app.recuperacao.url}") String url) {
  this.mail = mail; this.remetente = remetente;
  URI uri = URI.create(url);
  if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
      || uri.getHost() == null || uri.getRawFragment() != null || uri.getRawQuery() != null)
   throw new IllegalArgumentException("URL de recuperação inválida");
  this.url = url;
 }
 public void enviar(String email, String codigo) {
  var mensagem = mail.createMimeMessage();
  try {
   var helper = new MimeMessageHelper(mensagem, StandardCharsets.UTF_8.name());
   helper.setFrom(remetente);
   helper.setTo(email);
   helper.setSubject("Banco Acadêmico — redefinição de senha");
   helper.setText("""
       <!doctype html>
       <html lang="pt-BR"><body style="font-family:Arial,sans-serif;color:#183b45">
       <h1>Redefinição de senha</h1>
       <p>Informe o código abaixo na tela de recuperação do Banco Acadêmico.</p>
       <p style="font-size:32px;font-weight:bold;letter-spacing:6px">%s</p>
       <p>Este código é válido por 15 minutos e só pode ser usado uma vez.</p>
       <p>Se você não solicitou a redefinição, ignore este e-mail.</p>
       </body></html>
       """.formatted(HtmlUtils.htmlEscape(codigo)), true);
  } catch (MessagingException ex) {
   throw new MailPreparationException("Não foi possível preparar o e-mail de recuperação.", ex);
  }
  mail.send(mensagem);
 }
}
