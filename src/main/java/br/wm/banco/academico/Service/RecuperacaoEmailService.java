package br.wm.banco.academico.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import java.net.URI;
@Service
public class RecuperacaoEmailService {
 private final TransporteEmail mail;
 public RecuperacaoEmailService(TransporteEmail mail,
   @Value("${app.recuperacao.url}") String url) {
  this.mail = mail;
  URI uri = URI.create(url);
  if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
      || uri.getHost() == null || uri.getRawFragment() != null || uri.getRawQuery() != null)
   throw new IllegalArgumentException("URL de recuperação inválida");
 }
 public void enviar(String email, String codigo) {
  mail.enviar(email, "Banco Acadêmico — redefinição de senha", """
       <!doctype html>
       <html lang="pt-BR"><body style="font-family:Arial,sans-serif;color:#183b45">
       <h1>Redefinição de senha</h1>
       <p>Informe o código abaixo na tela de recuperação do Banco Acadêmico.</p>
       <p style="font-size:32px;font-weight:bold;letter-spacing:6px">%s</p>
       <p>Este código é válido por 15 minutos e só pode ser usado uma vez.</p>
       <p>Se você não solicitou a redefinição, ignore este e-mail.</p>
       </body></html>
       """.formatted(HtmlUtils.htmlEscape(codigo)), true);
 }
}
