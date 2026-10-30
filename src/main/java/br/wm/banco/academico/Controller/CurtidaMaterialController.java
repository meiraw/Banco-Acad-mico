package br.wm.banco.academico.Controller;

import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/material/{materialId}/curtida")
public class CurtidaMaterialController {
    private final JdbcTemplate jdbc;
    private final MaterialService materiais;
    private final UsuarioRepository usuarios;

    public CurtidaMaterialController(JdbcTemplate jdbc, MaterialService materiais, UsuarioRepository usuarios) {
        this.jdbc = jdbc; this.materiais = materiais; this.usuarios = usuarios;
    }

    public record Curtida(long total, boolean curtido) {}

    private UUID usuario(Principal principal) {
        return usuarios.findByEmail(principal.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
    }

    private Curtida estado(UUID materialId, UUID usuarioId) {
        long total = jdbc.queryForObject("SELECT count(*) FROM curtidas_material WHERE material_id = ?", Long.class, materialId);
        boolean curtido = jdbc.queryForObject("SELECT count(*) FROM curtidas_material WHERE material_id = ? AND usuario_id = ?",
                Long.class, materialId, usuarioId) > 0;
        return new Curtida(total, curtido);
    }

    @GetMapping
    public Curtida consultar(@PathVariable UUID materialId, Principal principal) {
        materiais.buscarPorId(materialId);
        return estado(materialId, usuario(principal));
    }

    @PutMapping
    @Transactional
    public Curtida curtir(@PathVariable UUID materialId, Principal principal) {
        materiais.buscarPorId(materialId);
        UUID usuarioId = usuario(principal);
        // Serializa alterações para impedir curtidas duplicadas em cliques simultâneos.
        jdbc.queryForObject("SELECT id FROM materiais WHERE id = ? FOR UPDATE", UUID.class, materialId);
        if (!estado(materialId, usuarioId).curtido()) {
            jdbc.update("INSERT INTO curtidas_material(material_id, usuario_id) VALUES (?, ?)", materialId, usuarioId);
        }
        return estado(materialId, usuarioId);
    }

    @DeleteMapping
    @Transactional
    public Curtida descurtir(@PathVariable UUID materialId, Principal principal) {
        materiais.buscarPorId(materialId);
        UUID usuarioId = usuario(principal);
        jdbc.queryForObject("SELECT id FROM materiais WHERE id = ? FOR UPDATE", UUID.class, materialId);
        jdbc.update("DELETE FROM curtidas_material WHERE material_id = ? AND usuario_id = ?", materialId, usuarioId);
        return estado(materialId, usuarioId);
    }
}
