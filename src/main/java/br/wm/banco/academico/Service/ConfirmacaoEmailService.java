package br.wm.banco.academico.Service;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class ConfirmacaoEmailService {
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    private final JavaMailSender mail;
    private final String remetente, url;
    private final SecureRandom random = new SecureRandom();

    public ConfirmacaoEmailService(UsuarioRepository usuarios, JdbcTemplate jdbc, JavaMailSender mail,
            @Value("${app.recuperacao.remetente}") String remetente,
            @Value("${app.confirmacao.url}") String url) {
        this.usuarios = usuarios; this.jdbc = jdbc; this.mail = mail;
        this.remetente = remetente;
        URI uri = URI.create(url);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getRawFragment() != null || uri.getRawQuery() != null)
            throw new IllegalArgumentException("URL de confirmação inválida");
        this.url = url;
    }

    @Transactional
    public void revogarRecuperacoes(UUID usuarioId) {
        jdbc.update("DELETE FROM recuperacoes_senha WHERE usuario_id = ?", usuarioId);
    }

    @Transactional
    public void solicitar(String endereco) {
        usuarios.findByEmail(endereco).ifPresent(conta -> {
            UsuarioModel usuario = usuarios.bloquearPorId(conta.getId()).orElseThrow();
            if (usuario.isEmailConfirmado()) return;
            byte[] bytes = new byte[32]; random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            jdbc.update("DELETE FROM confirmacoes_email WHERE usuario_id = ? OR expira_em <= ?",
                    usuario.getId(), Timestamp.from(Instant.now()));
            jdbc.update("INSERT INTO confirmacoes_email VALUES (?, ?, ?)",
                    RecuperacaoSenhaService.hash(token), usuario.getId(),
                    Timestamp.from(Instant.now().plusSeconds(900)));
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(remetente); mensagem.setTo(usuario.getEmail());
            mensagem.setSubject("Banco Acadêmico — confirme seu e-mail");
            mensagem.setText("Confirme seu e-mail em até 15 minutos. O link só pode ser usado uma vez. "
                    + "Se não criou uma conta, ignore esta mensagem.\n\n" + url + "#token=" + token);
            try { mail.send(mensagem); }
            catch (MailException ex) {
                jdbc.update("DELETE FROM confirmacoes_email WHERE usuario_id = ?", usuario.getId());
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Falha no envio de confirmação pelo SMTP.");
            }
        });
    }

    @Transactional
    public void confirmar(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalido();
        String hash = RecuperacaoSenhaService.hash(token);
        var ids = jdbc.query("SELECT usuario_id FROM confirmacoes_email WHERE token_hash = ?",
                (rs, n) -> rs.getObject(1, UUID.class), hash);
        if (ids.isEmpty()) throw invalido();
        UsuarioModel usuario = usuarios.bloquearPorId(ids.getFirst()).orElseThrow(ConfirmacaoEmailService::invalido);
        var expiracoes = jdbc.query("SELECT expira_em FROM confirmacoes_email WHERE token_hash = ? AND usuario_id = ?",
                (rs, n) -> rs.getTimestamp(1).toInstant(), hash, usuario.getId());
        if (expiracoes.isEmpty() || !expiracoes.getFirst().isAfter(Instant.now())) throw invalido();
        usuario.setEmailConfirmado(true);
        usuarios.saveAndFlush(usuario);
        jdbc.update("DELETE FROM confirmacoes_email WHERE usuario_id = ?", usuario.getId());
    }

    private static ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado. Solicite outro link.");
    }
}
