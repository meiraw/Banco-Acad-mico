package br.wm.banco.academico.Service;

import jakarta.mail.MessagingException;
import org.springframework.mail.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.*;

/** Classifica por tipo, sem incluir mensagens, destinatários ou segredos nos logs. */
public final class DiagnosticoEmail {
    private DiagnosticoEmail() {}

    public static String categoria(Throwable falha) {
        var pendentes = new ArrayDeque<Throwable>();
        var visitados = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        pendentes.add(falha);
        String categoria = falha instanceof MailException ? "TRANSPORTE" : "INTERNA";
        while (!pendentes.isEmpty() && visitados.size() < 64) {
            Throwable atual = pendentes.removeFirst();
            if (!visitados.add(atual)) continue;
            if (atual instanceof MailAuthenticationException) return "AUTENTICACAO";
            if (atual instanceof SocketTimeoutException) return "TIMEOUT";
            if (atual instanceof ConnectException) return "CONEXAO";
            if (atual instanceof MailPreparationException) categoria = "PREPARACAO";
            if (atual instanceof MailParseException) categoria = "FORMATO";
            if (atual.getCause() != null) pendentes.add(atual.getCause());
            if (atual instanceof MessagingException mail && mail.getNextException() != null)
                pendentes.add(mail.getNextException());
            if (atual instanceof MailSendException mail)
                for (Exception erro : mail.getMessageExceptions()) pendentes.add(erro);
        }
        return categoria;
    }
}
