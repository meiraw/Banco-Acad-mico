package br.wm.banco.academico.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
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
 public void enviar(String email, String token) {
  SimpleMailMessage mensagem = new SimpleMailMessage();
  mensagem.setFrom(remetente); mensagem.setTo(email);
  mensagem.setSubject("Banco Acadêmico — redefinição de senha");
  // O fragmento não é enviado ao servidor HTTP nem aos seus logs de acesso.
  mensagem.setText("Defina sua nova senha em até 15 minutos. O link só pode ser usado uma vez. "
    + "Se não solicitou, ignore este e-mail.\n\n" + url + "#token=" + token);
  mail.send(mensagem);
 }
}
