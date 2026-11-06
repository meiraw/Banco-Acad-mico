package br.wm.banco.academico;
import br.wm.banco.academico.Controller.RecuperacaoSenhaController;
import br.wm.banco.academico.Service.*;
import br.wm.banco.academico.Exception.GlobalExpcetionHandler;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class RecuperacaoEndpointTests {
 RecuperacaoSenhaService service;
 RecuperacaoLimiteService limites;
 MockMvc mvc;
 @BeforeEach void setup() {
  service = mock(RecuperacaoSenhaService.class); limites = mock(RecuperacaoLimiteService.class);
  when(limites.permitir(anyString(), anyInt())).thenReturn(true);
  mvc = MockMvcBuilders.standaloneSetup(new RecuperacaoSenhaController(service, limites))
      .setControllerAdvice(new GlobalExpcetionHandler()).build();
 }
 @Test void mensagemGenericaEContratoJson() throws Exception {
  for (String email : new String[]{"teste@unemat.br", "ausente@unemat.br"})
   mvc.perform(post("/auth/esqueci-senha").contentType("application/json")
    .content("{\"email\":\"" + email + "\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("mensagem").value(
      "Se houver uma conta para este e-mail, enviaremos um código de redefinição."));
  mvc.perform(post("/auth/redefinir-senha").contentType("application/json")
    .content("{\"token\":\"" + "A".repeat(43) + "\",\"novaSenha\":\"NovaSenha123!\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("mensagem").exists());
 }
 @Test void limitesDeEnvioEValidacao() throws Exception {
  when(limites.permitir(startsWith("envio-ip:"), anyInt())).thenReturn(false);
  mvc.perform(post("/auth/esqueci-senha").contentType("application/json").content("{\"email\":\"teste@unemat.br\"}"))
    .andExpect(status().isTooManyRequests());
  when(limites.permitir(startsWith("redefinir-ip:"), anyInt())).thenReturn(false);
  mvc.perform(post("/auth/redefinir-senha").contentType("application/json")
    .content("{\"token\":\"" + "A".repeat(43) + "\",\"novaSenha\":\"NovaSenha123!\"}"))
    .andExpect(status().isTooManyRequests());
  verifyNoInteractions(service);
 }
 @Test void enderecoLimitadoRecebeMesmaMensagemSemEnvio() throws Exception {
  when(limites.permitir(startsWith("envio-email:"), anyInt())).thenReturn(false);
  mvc.perform(post("/auth/esqueci-senha").contentType("application/json").content("{\"email\":\"teste@unemat.br\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("mensagem").exists());
  verifyNoInteractions(service);
 }
 @Test void validaEntradas() throws Exception {
  mvc.perform(post("/auth/esqueci-senha").contentType("application/json").content("{\"email\":\"invalido\"}"))
    .andExpect(status().isBadRequest());
  mvc.perform(post("/auth/redefinir-senha").contentType("application/json").content("{\"token\":\"invalido\",\"novaSenha\":\"curta\"}"))
    .andExpect(status().isBadRequest());
  verifyNoInteractions(service);
 }
 @Test void validaCodigoAntesDeAutorizarENegaExcessoDeTentativas() throws Exception {
  when(service.validarCodigo("teste@unemat.br", "0123")).thenReturn("A".repeat(43));
  mvc.perform(post("/auth/validar-codigo-senha").contentType("application/json")
    .content("{\"email\":\"teste@unemat.br\",\"codigo\":\"0123\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("token").value("A".repeat(43)));
  clearInvocations(service);
  when(limites.permitir("codigo-email:teste@unemat.br", 5)).thenReturn(false);
  mvc.perform(post("/auth/validar-codigo-senha").contentType("application/json")
    .content("{\"email\":\"teste@unemat.br\",\"codigo\":\"0123\"}"))
    .andExpect(status().isTooManyRequests());
  verifyNoInteractions(service);
 }
 @Test void enviaCodigoSemLinkOuToken() throws Exception {
  JavaMailSender sender = mock(JavaMailSender.class);
  when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
  new RecuperacaoEmailService(sender, "nao-responda@example.org", "https://example.org/redefinir-senha")
    .enviar("teste@unemat.br", "0123");
  ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
  verify(sender).send(captor.capture());
  var mensagem = captor.getValue();
  mensagem.saveChanges();
  assertTrue(mensagem.isMimeType("text/html"));
  String html = (String) mensagem.getContent();
  String textoVisivel = html.replaceAll("<[^>]*>", "");
  assertTrue(textoVisivel.contains("0123"));
  assertFalse(html.contains("href="));
  assertTrue(textoVisivel.contains("15 minutos"));
  assertFalse(textoVisivel.contains("A".repeat(43)));
  assertFalse(textoVisivel.contains("#token="));
  assertFalse(textoVisivel.contains("https://example.org"));
  assertEquals("teste@unemat.br", mensagem.getAllRecipients()[0].toString());
 }
 @Test void contadorPersistenteAplicaLimite() {
  JdbcTemplate jdbc = mock(JdbcTemplate.class);
  when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(1, 3, 4);
  RecuperacaoLimiteService contador = new RecuperacaoLimiteService(jdbc);
  assertTrue(contador.permitir("envio-email:teste@unemat.br", 3));
  assertTrue(contador.permitir("envio-email:teste@unemat.br", 3));
  assertFalse(contador.permitir("envio-email:teste@unemat.br", 3));
 }
}
