package br.wm.banco.academico.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class FavoritoRepository {
    private final JdbcTemplate jdbc;
    public FavoritoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void adicionar(UUID usuarioId, UUID materialId) {
        jdbc.update("INSERT INTO favoritos(usuario_id, material_id) VALUES (?, ?) ON CONFLICT DO NOTHING", usuarioId, materialId);
    }

    public void remover(UUID usuarioId, UUID materialId) {
        jdbc.update("DELETE FROM favoritos WHERE usuario_id = ? AND material_id = ?", usuarioId, materialId);
    }

    public Page<UUID> listarIds(UUID usuarioId, Pageable pagina) {
        Long total = jdbc.queryForObject("""
            SELECT count(*) FROM favoritos f JOIN materiais m ON m.id = f.material_id
            WHERE f.usuario_id = ? AND m.status_moderacao = 'APROVADO'
            """, Long.class, usuarioId);
        var ids = jdbc.query("""
            SELECT f.material_id FROM favoritos f JOIN materiais m ON m.id = f.material_id
            WHERE f.usuario_id = ? AND m.status_moderacao = 'APROVADO'
            ORDER BY f.criado_em DESC, f.material_id LIMIT ? OFFSET ?
            """, (rs, linha) -> rs.getObject(1, UUID.class), usuarioId, pagina.getPageSize(), pagina.getOffset());
        return new PageImpl<>(ids, pagina, total == null ? 0 : total);
    }
}
