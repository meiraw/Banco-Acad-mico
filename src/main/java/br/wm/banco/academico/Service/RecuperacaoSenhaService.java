package br.wm.banco.academico.Service;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
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
 private final TransactionTemplate envio;
 public RecuperacaoSenhaService(UsuarioRepository usuarios, JdbcTemplate jdbc,
    PasswordEncoder encoder, RecuperacaoEmailService email, PlatformTransactionManager transactions) {
  this.usuarios = usuarios; this.jdbc = jdbc; this.encoder = encoder; this.email = email;
  this.envio = new TransactionTemplate(transactions);
  this.envio.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 }
 public static String hash(String valor) {
  try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
      .digest(valor.getBytes(StandardCharsets.UTF_8))); }
  catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível"); }
 }
 public void solicitar(String endereco) {
  try {
   envio.executeWithoutResult(status -> solicitarNaTransacao(endereco));
  } catch (MailException ex) {
   org.slf4j.LoggerFactory.getLogger(getClass()).warn("Falha no envio de e-mail na recuperação: categoria={}. Estado anterior preservado.", DiagnosticoEmail.categoria(ex));
  }
 }
 private void solicitarNaTransacao(String endereco) {
  var conta = usuarios.findByEmail(endereco);
  if (conta.isEmpty()) return;
  UsuarioModel usuario = usuarios.bloquearPorId(conta.get().getId()).orElseThrow();
  String token = String.format(Locale.ROOT, "%04d", random.nextInt(10_000));
  jdbc.update("DELETE FROM recuperacoes_senha WHERE usuario_id = ? OR expira_em <= ?",
      usuario.getId(), Timestamp.from(Instant.now()));
  jdbc.update("INSERT INTO recuperacoes_senha(token_hash, usuario_id, expira_em) VALUES (?, ?, ?)",
      hash("codigo:" + usuario.getEmail() + ":" + token), usuario.getId(), Timestamp.from(Instant.now().plusSeconds(900)));
  email.enviar(usuario.getEmail(), token);
 }
 @Transactional
 public String validarCodigo(String endereco, String codigo) {
  if (codigo == null || !codigo.matches("[0-9]{4}")) throw invalido();
  var conta = usuarios.findByEmail(endereco);
  if (conta.isEmpty()) throw invalido();
  var usuario = usuarios.bloquearPorId(conta.get().getId()).orElseThrow(RecuperacaoSenhaService::invalido);
  String codigoHash = hash("codigo:" + endereco + ":" + codigo);
  var expiracoes = jdbc.query("SELECT expira_em FROM recuperacoes_senha WHERE token_hash = ? AND usuario_id = ?",
      (rs, n) -> rs.getTimestamp(1).toInstant(), codigoHash, usuario.getId());
  if (expiracoes.isEmpty() || !expiracoes.getFirst().isAfter(Instant.now())) throw invalido();
  byte[] bytes = new byte[32]; random.nextBytes(bytes);
  String autorizacao = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  jdbc.update("UPDATE recuperacoes_senha SET token_hash = ? WHERE token_hash = ? AND usuario_id = ?",
      hash(autorizacao), codigoHash, usuario.getId());
  return autorizacao;
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
