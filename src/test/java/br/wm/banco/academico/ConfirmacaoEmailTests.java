package br.wm.banco.academico;

import br.wm.banco.academico.Service.TransporteSmtp;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.ConfirmacaoEmailService;
import br.wm.banco.academico.Service.RecuperacaoSenhaService;
import br.wm.banco.academico.Service.TransporteResend;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

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
        service = new ConfirmacaoEmailService(usuarios, jdbc, new TransporteSmtp(mail, "envio@example.org"), "https://example.org/confirmar-email",
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
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
    @Test void confirmacaoViaResendPreservaLinkAnteriorQuandoReenvioFalha() {
        service.solicitar(usuario.getEmail());
        String anterior = token;
        var builder = RestClient.builder();
        var api = MockRestServiceServer.bindTo(builder).build();
        api.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString(
                        "https://example.org/confirmar-email#token=")))
                .andRespond(withServiceUnavailable());
        var confirmacao = new ConfirmacaoEmailService(usuarios, jdbc,
                new TransporteResend(builder.build(), "envio@example.org"), "https://example.org/confirmar-email",
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));

        confirmacao.solicitar(usuario.getEmail());
        assertEquals(RecuperacaoSenhaService.hash(anterior),
                jdbc.queryForObject("SELECT token_hash FROM confirmacoes_email", String.class));
        confirmacao.confirmar(anterior);
        assertTrue(usuario.isEmailConfirmado());
        api.verify();
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
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"autenticacao", "timeout", "conexao", "preparacao", "rejeicao"})
    void reenvioComFalhaPreservaLinkAnterior(String falha) {
        service.solicitar(usuario.getEmail());
        String anterior = token;
        Timestamp validade = jdbc.queryForObject("SELECT expira_em FROM confirmacoes_email", Timestamp.class);
        doThrow(EmailFalhasTests.falha(falha)).when(mail).send(any(SimpleMailMessage.class));
        EmailFalhasTests.verificarLogSeguro(ConfirmacaoEmailService.class,
                () -> service.solicitar(usuario.getEmail()), anterior, usuario.getEmail());
        assertEquals(RecuperacaoSenhaService.hash(anterior), jdbc.queryForObject("SELECT token_hash FROM confirmacoes_email", String.class));
        assertEquals(validade, jdbc.queryForObject("SELECT expira_em FROM confirmacoes_email", Timestamp.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM confirmacoes_email", Integer.class));
        assertFalse(usuario.isEmailConfirmado());
        assertDoesNotThrow(() -> service.confirmar(anterior));
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
