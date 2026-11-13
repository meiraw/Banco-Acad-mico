package br.wm.banco.academico.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.Semaphore;

/** Limites de admissão por instância; a cota persistente é verificada sob bloqueio no banco. */
@Service
public class LimiteUploadService {
    private record Janela(long inicio, int tentativas, long bytes) {}
    private final Map<String, Janela> janelas = new HashMap<>();
    private final Set<String> ativos = new HashSet<>();
    private final Semaphore vagas;
    private final int tentativas;
    private final long bytes;
    public LimiteUploadService(@Value("${app.upload.concorrencia:2}") int concorrencia,
            @Value("${app.upload.tentativas-15min:20}") int tentativas,
            @Value("${app.upload.bytes-15min:104857600}") long bytes) {
        if (concorrencia < 1 || tentativas < 1 || bytes < 1) throw new IllegalArgumentException("Limites de upload inválidos.");
        this.vagas = new Semaphore(concorrencia); this.tentativas = tentativas; this.bytes = bytes;
    }
    public synchronized Permissao adquirir(String email, long tamanho) {
        long agora = System.currentTimeMillis();
        Janela janela = janelas.get(email);
        if (janela == null || agora - janela.inicio >= 900_000) janela = new Janela(agora, 0, 0);
        if (ativos.contains(email) || janela.tentativas >= tentativas || tamanho < 0 || tamanho > bytes - janela.bytes)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de uploads atingido. Aguarde para tentar novamente.");
        if (janelas.size() >= 10_000 && !janelas.containsKey(email)) {
            limpar();
            if (janelas.size() >= 10_000) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Uploads temporariamente ocupados.");
        }
        if (!vagas.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Uploads ocupados. Tente novamente em instantes.");
        janelas.put(email, new Janela(janela.inicio, janela.tentativas + 1, janela.bytes + tamanho));
        ativos.add(email);
        return new Permissao(email);
    }
    @Scheduled(fixedDelay = 900000)
    public synchronized void limpar() {
        long agora = System.currentTimeMillis();
        janelas.entrySet().removeIf(e -> agora - e.getValue().inicio >= 900_000 && !ativos.contains(e.getKey()));
    }
    public final class Permissao implements AutoCloseable {
        private final String email;
        private boolean fechada;
        private Permissao(String email) { this.email = email; }
        @Override public void close() {
            synchronized (LimiteUploadService.this) {
                if (!fechada) { fechada = true; ativos.remove(email); vagas.release(); }
            }
        }
    }
}
