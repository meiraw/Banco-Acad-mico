package br.wm.banco.academico;
import br.wm.banco.academico.Service.*;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.DTOs.Request.LoginRequestDTO;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mail.MailSendException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class RecuperacaoSenhaTests {
 JdbcTemplate jdbc;
 UsuarioRepository usuarios;
 RecuperacaoEmailService mail;
 RecuperacaoSenhaService service;
 UsuarioModel usuario;
 BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
 String token;
 @BeforeEach void setup() {
  jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
  jdbc.execute("CREATE TABLE recuperacoes_senha(token_hash CHAR(64) PRIMARY KEY, usuario_id UUID, expira_em TIMESTAMP WITH TIME ZONE)");
  usuarios = mock(UsuarioRepository.class); mail = mock(RecuperacaoEmailService.class);
  service = new RecuperacaoSenhaService(usuarios, jdbc, encoder, mail);
  usuario = new UsuarioModel(); ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
  usuario.setEmail("teste@unemat.br"); usuario.setNome("Teste"); usuario.setPerfil(PerfilUsuario.USER);
  usuario.setSenha(encoder.encode("Antiga123!"));
  when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
  when(usuarios.bloquearPorId(usuario.getId())).thenReturn(Optional.of(usuario));
  doAnswer(i -> { token = i.getArgument(1); return null; }).when(mail).enviar(anyString(), anyString());
 }
 @Test void cadastradoRecebeTokenSeguroSomenteHashComQuinzeMinutos() {
  service.solicitar(usuario.getEmail());
  assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
  assertEquals(RecuperacaoSenhaService.hash(token), jdbc.queryForObject("SELECT token_hash FROM recuperacoes_senha", String.class));
  Instant exp = jdbc.queryForObject("SELECT expira_em FROM recuperacoes_senha", Timestamp.class).toInstant();
  assertTrue(exp.isAfter(Instant.now().plusSeconds(890)) && exp.isBefore(Instant.now().plusSeconds(901)));
 }
 @Test void naoCadastradoNaoEnviaEmail() {
  service.solicitar("ausente@unemat.br"); verifyNoInteractions(mail);
  assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM recuperacoes_senha", Integer.class));
 }
 @Test void validoTrocaSenhaInvalidaPendentesEJwtEImpedeReuso() {
  TokenService jwt = new TokenService(AuthenticationTests.SECRET);
  String antigo = jwt.gerarToken(usuario);
  service.solicitar(usuario.getEmail()); String usado = token;
  jdbc.update("INSERT INTO recuperacoes_senha VALUES (?, ?, ?)", RecuperacaoSenhaService.hash("B".repeat(43)), usuario.getId(), Timestamp.from(Instant.now().plusSeconds(900)));
  service.redefinir(usado, "NovaSenha123!");
  assertTrue(encoder.matches("NovaSenha123!", usuario.getSenha()));
  assertFalse(encoder.matches("Antiga123!", usuario.getSenha()));
  assertEquals(1, usuario.getVersaoCredenciais());
  assertThrows(com.auth0.jwt.exceptions.JWTVerificationException.class, () -> jwt.validarVersao(antigo, usuario));
  assertDoesNotThrow(() -> jwt.validarVersao(jwt.gerarToken(usuario), usuario));
  assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM recuperacoes_senha", Integer.class));
  assertThrows(ResponseStatusException.class, () -> service.redefinir(usado, "Outra123!"));
  assertThrows(ResponseStatusException.class, () -> service.redefinir("B".repeat(43), "Outra123!"));
  usuario.setEmailConfirmado(true);
  UsuarioService login = new UsuarioService(usuarios, encoder, mock(ConfirmacaoEmailService.class));
  LoginRequestDTO dto = new LoginRequestDTO();
  ReflectionTestUtils.setField(dto, "email", usuario.getEmail());
  ReflectionTestUtils.setField(dto, "senha", "NovaSenha123!");
  assertEquals(usuario, login.login(dto));
  ReflectionTestUtils.setField(dto, "senha", "Antiga123!");
  assertThrows(br.wm.banco.academico.Exception.RegraNegocioException.class, () -> login.login(dto));
 }
 @Test void expiradoInvalidoEMalformadoNaoAlteramSenha() {
  service.solicitar(usuario.getEmail());
  jdbc.update("UPDATE recuperacoes_senha SET expira_em = ?", Timestamp.from(Instant.now().minusSeconds(1)));
  for (String t : new String[]{token, "A".repeat(43), "invalido"})
   assertThrows(ResponseStatusException.class, () -> service.redefinir(t, "NovaSenha123!"));
  verify(usuarios, never()).saveAndFlush(any());
  assertTrue(encoder.matches("Antiga123!", usuario.getSenha()));
 }
 @Test void novaSolicitacaoInvalidaAnteriorEFalhaSmtpNaoVazaConta() {
  service.solicitar(usuario.getEmail()); String anterior = token;
  service.solicitar(usuario.getEmail()); assertNotEquals(anterior, token);
  assertThrows(ResponseStatusException.class, () -> service.redefinir(anterior, "NovaSenha123!"));
  doThrow(new MailSendException("falha")).when(mail).enviar(anyString(), anyString());
  assertDoesNotThrow(() -> service.solicitar(usuario.getEmail()));
  assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM recuperacoes_senha", Integer.class));
 }
 @Test void migrationPreservaUsuarioExistenteEExcluiTokensEmCascata() throws Exception {
  var banco = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", ""));
  // Manter uma conexão aberta enquanto se executam os comandos no banco em memória.
  try (var conexao = banco.getDataSource().getConnection()) {
   String v6 = new String(getClass().getResourceAsStream("/db/migration/V6__criar_tabela_usuarios.sql").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
   for (String sql : v6.split(";")) if (!sql.isBlank()) banco.execute(sql);
   banco.update("INSERT INTO usuarios VALUES (?, ?, ?, ?, ?)", usuario.getId(), "Teste", usuario.getEmail(), usuario.getSenha(), "USER");
   String v7 = new String(getClass().getResourceAsStream("/db/migration/V7__recuperacao_senha.sql").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
   for (String sql : v7.split(";")) if (!sql.isBlank()) banco.execute(sql);
   assertEquals(0L, banco.queryForObject("SELECT versao_credenciais FROM usuarios", Long.class));
   banco.update("INSERT INTO recuperacoes_senha VALUES (?, ?, ?)", RecuperacaoSenhaService.hash("A".repeat(43)), usuario.getId(), Timestamp.from(Instant.now().plusSeconds(900)));
   banco.update("DELETE FROM usuarios WHERE id = ?", usuario.getId());
   assertEquals(0, banco.queryForObject("SELECT count(*) FROM recuperacoes_senha", Integer.class));
  }
 }
 @Test void rejeitaSenhaAcimaDoLimiteBcryptEmBytes() {
  service.solicitar(usuario.getEmail());
  assertThrows(ResponseStatusException.class, () -> service.redefinir(token, "á".repeat(37)));
 }
}
