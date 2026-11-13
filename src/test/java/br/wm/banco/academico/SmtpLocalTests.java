package br.wm.banco.academico;

import br.wm.banco.academico.Service.DiagnosticoEmail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** SMTP descartável: somente loopback, portas efêmeras, dados e credenciais fictícios. */
class SmtpLocalTests {
    @ParameterizedTest
    @ValueSource(strings = {"sucesso", "autenticacao", "rejeicao", "interrupcao", "timeout"})
    void transporteRealContraServidorLocal(String modo) throws Exception {
        try (var servidor = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             var executor = Executors.newSingleThreadExecutor()) {
            servidor.setSoTimeout(3000);
            Future<?> atendimento = executor.submit(() -> {
                try (Socket socket = servidor.accept()) {
                    socket.setSoTimeout(3000);
                    var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    var saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
                    if (modo.equals("interrupcao")) return;
                    if (modo.equals("timeout")) {
                        // O cliente fecha a conexão ao atingir seu timeout de leitura.
                        while (entrada.readLine() != null) { }
                        return;
                    }
                    responder(saida, "220 localhost teste");
                    boolean dados = false;
                    String linha;
                    while ((linha = entrada.readLine()) != null) {
                        if (dados) {
                            if (linha.equals(".")) { dados = false; responder(saida, "250 aceito localmente"); }
                        } else if (linha.startsWith("EHLO")) {
                            responder(saida, modo.equals("autenticacao") ? "250-localhost\r\n250 AUTH PLAIN" : "250 localhost");
                        } else if (linha.startsWith("AUTH")) {
                            responder(saida, "535 5.7.8 credenciais ficticias rejeitadas");
                        } else if (linha.startsWith("RCPT")) {
                            responder(saida, modo.equals("rejeicao") ? "550 5.1.1 destinatario ficticio rejeitado" : "250 ok");
                        } else if (linha.equals("DATA")) {
                            dados = true; responder(saida, "354 fim com ponto");
                        } else if (linha.equals("QUIT")) {
                            responder(saida, "221 fim"); return;
                        } else responder(saida, "250 ok");
                    }
                } catch (IOException ex) { throw new UncheckedIOException(ex); }
            });
            JavaMailSenderImpl mail = remetente(servidor.getLocalPort());
            if (modo.equals("autenticacao")) {
                mail.setUsername("usuario-ficticio"); mail.setPassword("senha-ficticia");
                mail.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
            }
            var mensagem = new SimpleMailMessage();
            mensagem.setFrom("origem@example.test"); mensagem.setTo("destino@example.test");
            mensagem.setSubject("Teste local"); mensagem.setText("Conteudo ficticio");
            if (modo.equals("sucesso")) assertDoesNotThrow(() -> mail.send(mensagem));
            else {
                MailException falha = assertThrows(MailException.class, () -> mail.send(mensagem));
                if (modo.equals("autenticacao")) assertEquals("AUTENTICACAO", DiagnosticoEmail.categoria(falha));
                if (modo.equals("timeout")) assertEquals("TIMEOUT", DiagnosticoEmail.categoria(falha));
            }
            atendimento.get(5, TimeUnit.SECONDS);
        }
    }

    @Test void servidorIndisponivelRecusaConexao() throws Exception {
        int porta;
        try (var reserva = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) { porta = reserva.getLocalPort(); }
        var mensagem = new SimpleMailMessage();
        mensagem.setFrom("origem@example.test"); mensagem.setTo("destino@example.test"); mensagem.setText("Teste");
        var falha = assertThrows(MailException.class, () -> remetente(porta).send(mensagem));
        assertEquals("CONEXAO", DiagnosticoEmail.categoria(falha));
    }

    private static JavaMailSenderImpl remetente(int porta) {
        var mail = new JavaMailSenderImpl(); mail.setHost("127.0.0.1"); mail.setPort(porta);
        var props = mail.getJavaMailProperties();
        props.setProperty("mail.smtp.auth", "false");
        props.setProperty("mail.smtp.starttls.enable", "false");
        props.setProperty("mail.smtp.connectiontimeout", "1000");
        props.setProperty("mail.smtp.timeout", "500");
        props.setProperty("mail.smtp.writetimeout", "1000");
        return mail;
    }

    private static void responder(BufferedWriter saida, String resposta) throws IOException {
        saida.write(resposta + "\r\n"); saida.flush();
    }
}
