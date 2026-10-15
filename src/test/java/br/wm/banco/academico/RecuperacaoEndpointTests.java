package br.wm.banco.academico;
import br.wm.banco.academico.Controller.RecuperacaoSenhaController;
import br.wm.banco.academico.Service.*;
import br.wm.banco.academico.Exception.GlobalExpcetionHandler;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
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
      "Se houver uma conta para este e-mail, enviaremos um link de redefinição."));
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
 @Test void enviaLinkSemSenhaEComFragmento() {
  JavaMailSender sender = mock(JavaMailSender.class);
  new RecuperacaoEmailService(sender, "nao-responda@example.org", "https://example.org/redefinir-senha")
    .enviar("teste@unemat.br", "A".repeat(43));
  ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
  verify(sender).send(captor.capture());
  assertTrue(captor.getValue().getText().contains("https://example.org/redefinir-senha#token=" + "A".repeat(43)));
  assertEquals("teste@unemat.br", captor.getValue().getTo()[0]);
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
