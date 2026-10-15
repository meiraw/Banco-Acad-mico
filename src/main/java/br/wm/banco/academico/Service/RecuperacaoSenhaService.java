package br.wm.banco.academico.Service;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
@Service
public class RecuperacaoSenhaService {
 private final UsuarioRepository usuarios;
 private final JdbcTemplate jdbc;
 private final PasswordEncoder encoder;
 private final RecuperacaoEmailService email;
 private final SecureRandom random = new SecureRandom();
 public RecuperacaoSenhaService(UsuarioRepository usuarios, JdbcTemplate jdbc,
    PasswordEncoder encoder, RecuperacaoEmailService email) {
  this.usuarios = usuarios; this.jdbc = jdbc; this.encoder = encoder; this.email = email;
 }
 public static String hash(String valor) {
  try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
      .digest(valor.getBytes(StandardCharsets.UTF_8))); }
  catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível"); }
 }
 @Transactional
 public void solicitar(String endereco) {
  var conta = usuarios.findByEmail(endereco);
  if (conta.isEmpty()) return;
  UsuarioModel usuario = usuarios.bloquearPorId(conta.get().getId()).orElseThrow();
  byte[] bytes = new byte[32]; random.nextBytes(bytes);
  String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  jdbc.update("DELETE FROM recuperacoes_senha WHERE usuario_id = ? OR expira_em <= ?",
      usuario.getId(), Timestamp.from(Instant.now()));
  jdbc.update("INSERT INTO recuperacoes_senha(token_hash, usuario_id, expira_em) VALUES (?, ?, ?)",
      hash(token), usuario.getId(), Timestamp.from(Instant.now().plusSeconds(900)));
  try { email.enviar(usuario.getEmail(), token); }
  catch (MailException ex) {
   jdbc.update("DELETE FROM recuperacoes_senha WHERE usuario_id = ?", usuario.getId());
   org.slf4j.LoggerFactory.getLogger(getClass()).warn("Falha no envio de recuperação de senha pelo SMTP.");
  }
 }
 @Transactional
 public void redefinir(String token, String senha) {
  if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalido();
  if (senha == null || senha.isBlank() || senha.length() < 8
      || senha.getBytes(StandardCharsets.UTF_8).length > 72)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha deve ter ao menos 8 caracteres e no máximo 72 bytes.");
  String hash = hash(token);
  var ids = jdbc.query("SELECT usuario_id FROM recuperacoes_senha WHERE token_hash = ?",
      (rs, n) -> rs.getObject(1, UUID.class), hash);
  if (ids.isEmpty()) throw invalido();
  // O bloqueio por usuário serializa solicitações e uso de tokens concorrentes.
  UsuarioModel usuario = usuarios.bloquearPorId(ids.getFirst()).orElseThrow(RecuperacaoSenhaService::invalido);
  var expiracoes = jdbc.query("SELECT expira_em FROM recuperacoes_senha WHERE token_hash = ? AND usuario_id = ?",
      (rs, n) -> rs.getTimestamp(1).toInstant(), hash, usuario.getId());
  if (expiracoes.isEmpty() || !expiracoes.getFirst().isAfter(Instant.now())) throw invalido();
  usuario.setSenha(encoder.encode(senha));
  usuario.setVersaoCredenciais(usuario.getVersaoCredenciais() + 1);
  usuarios.saveAndFlush(usuario);
  jdbc.update("DELETE FROM recuperacoes_senha WHERE usuario_id = ?", usuario.getId());
 }
 private static ResponseStatusException invalido() {
  return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado.");
 }
}
