package br.wm.banco.academico.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ArmazenamentoService {
    private final Path raiz;
    private final long reserva;
    public ArmazenamentoService(@Value("${app.upload.raiz:Uploads}") String raiz,
            @Value("${app.upload.reserva-disco:104857600}") long reserva) {
        this.raiz = Path.of(raiz).toAbsolutePath().normalize();
        if (reserva < 0) throw new IllegalArgumentException("Reserva de disco inválida.");
        this.reserva = reserva;
    }
    private void verificarRaiz() throws IOException {
        Files.createDirectories(raiz);
        // Rejeita também junctions/links em diretórios ancestrais.
        if (!raiz.toRealPath().equals(raiz)) throw new IOException("Raiz com redirecionamento.");
    }
    public Path novoCaminho() {
        return raiz.resolve("validado-v1-" + UUID.randomUUID());
    }
    public static boolean validado(String caminho) {
        if (caminho == null) return false;
        try { return Path.of(caminho).getFileName().toString().matches("validado-v1-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"); }
        catch (InvalidPathException ex) { return false; }
    }
    public Path permitido(String caminho) {
        Path arquivo = Path.of(caminho).toAbsolutePath().normalize();
        try {
            verificarRaiz();
            if (!raiz.equals(arquivo.getParent()) || Files.isSymbolicLink(arquivo)
                    || (Files.exists(arquivo, LinkOption.NOFOLLOW_LINKS) && !arquivo.toRealPath().getParent().equals(raiz)))
                throw new ResourceNotFoundException("Caminho de arquivo inválido.");
            return arquivo;
        } catch (IOException ex) { throw new ResourceNotFoundException("Armazenamento indisponível."); }
    }
    public void salvar(Path destino, byte[] bytes) {
        Path temporario = null;
        try {
            verificarRaiz();
            permitido(destino.toString());
            if (Files.getFileStore(raiz).getUsableSpace() - reserva < bytes.length)
                throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE, "Espaço insuficiente para receber o arquivo.");
            temporario = Files.createTempFile(raiz, ".upload-", ".tmp");
            Files.write(temporario, bytes);
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível armazenar o arquivo. Tente novamente.");
        } finally {
            if (temporario != null) remover(temporario);
        }
    }
    public void remover(Path caminho) {
        try { Files.deleteIfExists(permitido(caminho.toString())); }
        catch (IOException | RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Não foi possível limpar um arquivo de upload.");
        }
    }
}
