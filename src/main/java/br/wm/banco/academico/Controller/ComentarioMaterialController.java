package br.wm.banco.academico.Controller;

import br.wm.banco.academico.Service.MaterialService;
import br.wm.banco.academico.Repository.UsuarioRepository;
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
@RequestMapping("/material/{materialId}/comentarios")
public class ComentarioMaterialController {
    private final JdbcTemplate jdbc;
    private final MaterialService materiais;
    private final UsuarioRepository usuarios;

    public ComentarioMaterialController(JdbcTemplate jdbc, MaterialService materiais, UsuarioRepository usuarios) {
        this.jdbc = jdbc;
        this.materiais = materiais;
        this.usuarios = usuarios;
    }

    public record NovoComentario(@NotBlank @Size(max = 2000) String texto) {}
    public record Comentario(UUID id, UUID autorId, String autorNome, String texto, Instant criadoEm, long totalRespostas) {}

    @GetMapping
    public List<Comentario> listar(@PathVariable UUID materialId,
            @RequestParam(defaultValue = "0") int pagina) {
        materiais.buscarPorId(materialId);
        if (pagina < 0 || pagina > 1000000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return jdbc.query("""
                SELECT c.id, c.usuario_id, u.nome, c.texto, c.criado_em,
                (SELECT count(*) FROM respostas_comentario r WHERE r.comentario_id = c.id) AS total_respostas
                FROM comentarios_material c
                JOIN usuarios u ON u.id = c.usuario_id WHERE c.material_id = ?
                ORDER BY c.criado_em DESC, c.id DESC LIMIT 50 OFFSET ?
                """, (rs, n) -> new Comentario(rs.getObject("id", UUID.class), rs.getObject("usuario_id", UUID.class), rs.getString("nome"),
                rs.getString("texto"), rs.getTimestamp("criado_em").toInstant(), rs.getLong("total_respostas")), materialId, pagina * 50);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Comentario publicar(@PathVariable UUID materialId, @Valid @RequestBody NovoComentario dados, Principal principal) {
        materiais.buscarPorId(materialId);
        var usuario = usuarios.findByEmail(principal.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var comentario = new Comentario(UUID.randomUUID(), usuario.getId(), usuario.getNome(), dados.texto().strip(), Instant.now(), 0);
        if (comentario.texto().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva um comentário.");
        jdbc.update("INSERT INTO comentarios_material(id, material_id, usuario_id, texto, criado_em) VALUES (?, ?, ?, ?, ?)",
                comentario.id(), materialId, usuario.getId(), comentario.texto(), java.sql.Timestamp.from(comentario.criadoEm()));
        return comentario;
    }
}
