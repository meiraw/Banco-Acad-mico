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
@RequestMapping("/material/{materialId}")
public class CurtidaMaterialController {
    private final JdbcTemplate jdbc;
    private final MaterialService materiais;
    private final UsuarioRepository usuarios;

    public CurtidaMaterialController(JdbcTemplate jdbc, MaterialService materiais, UsuarioRepository usuarios) {
        this.jdbc = jdbc; this.materiais = materiais; this.usuarios = usuarios;
    }

    public record Curtida(long total, boolean curtido, long totalDeslikes, boolean deslike) {}

    private UUID usuario(Principal principal) {
        return usuarios.findByEmail(principal.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
    }

    private Curtida estado(UUID materialId, UUID usuarioId) {
        return jdbc.queryForObject("""
            SELECT count(CASE WHEN tipo = 'CURTIDA' THEN 1 END) AS positivas,
                   count(CASE WHEN tipo = 'CURTIDA' AND usuario_id = ? THEN 1 END) AS minha_positiva,
                   count(CASE WHEN tipo = 'DESLIKE' THEN 1 END) AS negativas,
                   count(CASE WHEN tipo = 'DESLIKE' AND usuario_id = ? THEN 1 END) AS minha_negativa
            FROM curtidas_material WHERE material_id = ?
            """, (rs, n) -> new Curtida(rs.getLong("positivas"), rs.getLong("minha_positiva") > 0,
                rs.getLong("negativas"), rs.getLong("minha_negativa") > 0), usuarioId, usuarioId, materialId);
    }

    @GetMapping({"/curtida", "/deslike"})
    public Curtida consultar(@PathVariable UUID materialId, Principal principal) {
        materiais.buscarPorId(materialId);
        return estado(materialId, usuario(principal));
    }

    @PutMapping("/curtida")
    @Transactional
    public Curtida curtir(@PathVariable UUID materialId, Principal principal) {
        materiais.buscarPorId(materialId);
        UUID usuarioId = usuario(principal);
        // Serializa alterações para impedir curtidas duplicadas em cliques simultâneos.
        jdbc.queryForObject("SELECT id FROM materiais WHERE id = ? FOR UPDATE", UUID.class, materialId);
        registrar(materialId, usuarioId, "CURTIDA");
        return estado(materialId, usuarioId);
    }

    @DeleteMapping("/curtida")
    @Transactional
    public Curtida descurtir(@PathVariable UUID materialId, Principal principal) {
        materiais.buscarPorId(materialId);
        UUID usuarioId = usuario(principal);
        jdbc.queryForObject("SELECT id FROM materiais WHERE id = ? FOR UPDATE", UUID.class, materialId);
        jdbc.update("DELETE FROM curtidas_material WHERE material_id = ? AND usuario_id = ? AND tipo = 'CURTIDA'", materialId, usuarioId);
        return estado(materialId, usuarioId);
    }

    private void registrar(UUID materialId, UUID usuarioId, String tipo) {
        int alteradas = jdbc.update("UPDATE curtidas_material SET tipo = ? WHERE material_id = ? AND usuario_id = ?", tipo, materialId, usuarioId);
        if (alteradas == 0) jdbc.update("INSERT INTO curtidas_material(material_id, usuario_id, tipo) VALUES (?, ?, ?)", materialId, usuarioId, tipo);
    }

    @PutMapping("/deslike")
    @Transactional
    public Curtida reagirNegativamente(@PathVariable UUID materialId, Principal principal) {
        materiais.buscarPorId(materialId);
        UUID usuarioId = usuario(principal);
        jdbc.queryForObject("SELECT id FROM materiais WHERE id = ? FOR UPDATE", UUID.class, materialId);
        registrar(materialId, usuarioId, "DESLIKE");
        return estado(materialId, usuarioId);
    }

    @DeleteMapping("/deslike")
    @Transactional
    public Curtida removerNegativa(@PathVariable UUID materialId, Principal principal) {
        materiais.buscarPorId(materialId);
        UUID usuarioId = usuario(principal);
        jdbc.queryForObject("SELECT id FROM materiais WHERE id = ? FOR UPDATE", UUID.class, materialId);
        jdbc.update("DELETE FROM curtidas_material WHERE material_id = ? AND usuario_id = ? AND tipo = 'DESLIKE'", materialId, usuarioId);
        return estado(materialId, usuarioId);
    }
}
