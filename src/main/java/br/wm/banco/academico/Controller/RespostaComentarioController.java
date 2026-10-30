package br.wm.banco.academico.Controller;

import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.MaterialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/material/{materialId}/comentarios/{comentarioId}/respostas")
public class RespostaComentarioController {
    private final JdbcTemplate jdbc;
    private final MaterialService materiais;
    private final UsuarioRepository usuarios;

    public RespostaComentarioController(JdbcTemplate jdbc, MaterialService materiais, UsuarioRepository usuarios) {
        this.jdbc = jdbc; this.materiais = materiais; this.usuarios = usuarios;
    }

    public record NovaResposta(@NotBlank @Size(max = 2000) String texto, UUID respostaPaiId) {}
    public record Resposta(UUID id, UUID autorId, String autorNome, String texto, Instant criadoEm,
                           UUID respostaPaiId, String respondendoA) {}

    private UUID validarComentario(UUID materialId, UUID comentarioId) {
        materiais.buscarPorId(materialId);
        var autores = jdbc.queryForList("SELECT usuario_id FROM comentarios_material WHERE id = ? AND material_id = ?",
                UUID.class, comentarioId, materialId);
        if (autores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentário não encontrado nesta publicação.");
        }
        return autores.getFirst();
    }

    @GetMapping
    public List<Resposta> listar(@PathVariable UUID materialId, @PathVariable UUID comentarioId,
            @RequestParam(defaultValue = "0") int pagina) {
        validarComentario(materialId, comentarioId);
        if (pagina < 0 || pagina > 1000000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return jdbc.query("""
                SELECT r.id, r.usuario_id, u.nome, r.texto, r.criado_em, r.resposta_pai_id,
                pai_autor.nome AS respondendo_a FROM respostas_comentario r
                JOIN usuarios u ON u.id = r.usuario_id
                LEFT JOIN respostas_comentario pai ON pai.id = r.resposta_pai_id
                LEFT JOIN usuarios pai_autor ON pai_autor.id = pai.usuario_id
                WHERE r.comentario_id = ?
                ORDER BY r.criado_em ASC, r.id ASC LIMIT 50 OFFSET ?
                """, (rs, n) -> new Resposta(rs.getObject("id", UUID.class), rs.getObject("usuario_id", UUID.class), rs.getString("nome"),
                rs.getString("texto"), rs.getTimestamp("criado_em").toInstant(), rs.getObject("resposta_pai_id", UUID.class),
                rs.getString("respondendo_a")), comentarioId, pagina * 50);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Resposta publicar(@PathVariable UUID materialId, @PathVariable UUID comentarioId,
            @Valid @RequestBody NovaResposta dados, Principal principal) {
        UUID autorComentario = validarComentario(materialId, comentarioId);
        String respondendoA = null;
        if (dados.respostaPaiId() != null) {
            var alvo = jdbc.queryForList("SELECT usuario_id FROM respostas_comentario WHERE id = ? AND comentario_id = ?",
                    UUID.class, dados.respostaPaiId(), comentarioId);
            if (alvo.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resposta não encontrada nesta conversa.");
            autorComentario = alvo.getFirst();
            respondendoA = jdbc.queryForObject("SELECT nome FROM usuarios WHERE id = ?", String.class, autorComentario);
        }
        var usuario = usuarios.findByEmail(principal.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getId().equals(autorComentario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode responder ao próprio comentário.");
        }
        String texto = dados.texto().strip();
        if (texto.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva uma resposta.");
        var resposta = new Resposta(UUID.randomUUID(), usuario.getId(), usuario.getNome(), texto, Instant.now(), dados.respostaPaiId(), respondendoA);
        jdbc.update("INSERT INTO respostas_comentario(id, comentario_id, usuario_id, texto, criado_em, resposta_pai_id) VALUES (?, ?, ?, ?, ?, ?)",
                resposta.id(), comentarioId, usuario.getId(), texto, java.sql.Timestamp.from(resposta.criadoEm()), dados.respostaPaiId());
        return resposta;
    }
}
