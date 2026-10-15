package br.wm.banco.academico;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.ConfirmacaoEmailService;
import br.wm.banco.academico.Service.RecuperacaoSenhaService;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ConfirmacaoEmailTests {
    JdbcTemplate jdbc;
    UsuarioRepository usuarios;
    JavaMailSender mail;
    ConfirmacaoEmailService service;
    UsuarioModel usuario;
    String token;
    @BeforeEach void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE confirmacoes_email(token_hash CHAR(64) PRIMARY KEY, usuario_id UUID, expira_em TIMESTAMP WITH TIME ZONE)");
        usuarios = mock(UsuarioRepository.class); mail = mock(JavaMailSender.class);
        service = new ConfirmacaoEmailService(usuarios, jdbc, mail, "envio@example.org", "https://example.org/confirmar-email");
        usuario = new UsuarioModel(); usuario.setEmail("teste@example.org");
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(usuarios.bloquearPorId(usuario.getId())).thenReturn(Optional.of(usuario));
        doAnswer(inv -> {
            SimpleMailMessage mensagem = inv.getArgument(0);
            assertEquals(usuario.getEmail(), mensagem.getTo()[0]);
            assertEquals("envio@example.org", mensagem.getFrom());
            token = mensagem.getText().split("#token=")[1];
            return null;
        }).when(mail).send(any(SimpleMailMessage.class));
    }
    @Test void enviaTokenSeguroArmazenaSoHashEConfirmaUmaVez() {
        service.solicitar(usuario.getEmail());
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(RecuperacaoSenhaService.hash(token), jdbc.queryForObject("SELECT token_hash FROM confirmacoes_email", String.class));
        Instant exp = jdbc.queryForObject("SELECT expira_em FROM confirmacoes_email", Timestamp.class).toInstant();
        assertTrue(exp.isAfter(Instant.now().plusSeconds(890)) && exp.isBefore(Instant.now().plusSeconds(901)));
        service.confirmar(token);
        assertTrue(usuario.isEmailConfirmado());
        verify(usuarios).saveAndFlush(usuario);
        assertThrows(ResponseStatusException.class, () -> service.confirmar(token));
    }
    @Test void rejeitaExpiradoDesconhecidoEMalformado() {
        service.solicitar(usuario.getEmail());
        jdbc.update("UPDATE confirmacoes_email SET expira_em = ?", Timestamp.from(Instant.now().minusSeconds(1)));
        for (String valor : new String[]{token, "A".repeat(43), "invalido"})
            assertThrows(ResponseStatusException.class, () -> service.confirmar(valor));
        assertFalse(usuario.isEmailConfirmado());
        verify(usuarios, never()).saveAndFlush(any());
    }
    @Test void reenvioInvalidaLinkAnterior() {
        service.solicitar(usuario.getEmail()); String anterior = token;
        service.solicitar(usuario.getEmail());
        assertNotEquals(anterior, token);
        assertThrows(ResponseStatusException.class, () -> service.confirmar(anterior));
        service.confirmar(token);
        assertTrue(usuario.isEmailConfirmado());
    }
    @Test void ausenteEConfirmadoNaoRecebemEmail() {
        service.solicitar("ausente@example.org");
        usuario.setEmailConfirmado(true);
        service.solicitar(usuario.getEmail());
        verifyNoInteractions(mail);
    }
    @Test void falhaSmtpRemoveTokenEPreservaContaPendente() {
        doThrow(new MailSendException("falha")).when(mail).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.solicitar(usuario.getEmail()));
        assertFalse(usuario.isEmailConfirmado());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM confirmacoes_email", Integer.class));
    }
    @Test void migrationPreservaContasAnterioresENovasSaoPendentes() throws Exception {
        var banco = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", ""));
        try (var conexao = banco.getDataSource().getConnection()) {
            banco.execute("CREATE TABLE usuarios(id UUID PRIMARY KEY)");
            banco.update("INSERT INTO usuarios(id) VALUES (?)", usuario.getId());
            String sql = new String(getClass().getResourceAsStream("/db/migration/V8__confirmacao_email.sql").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String comando : sql.split(";")) if (!comando.isBlank()) banco.execute(comando);
            assertTrue(banco.queryForObject("SELECT email_confirmado FROM usuarios WHERE id = ?", Boolean.class, usuario.getId()));
            UUID novo = UUID.randomUUID(); banco.update("INSERT INTO usuarios(id) VALUES (?)", novo);
            assertFalse(banco.queryForObject("SELECT email_confirmado FROM usuarios WHERE id = ?", Boolean.class, novo));
        }
    }
}
