package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Response.MaterialResponseDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class EstatisticasMaterialService {
    private final NamedParameterJdbcTemplate jdbc;
    public EstatisticasMaterialService(JdbcTemplate jdbc) { this.jdbc = new NamedParameterJdbcTemplate(jdbc); }

    // Recebe apenas materiais cuja visibilidade já foi verificada pelo endpoint.
    public void preencher(List<MaterialResponseDTO> materiais) {
        if (materiais.isEmpty()) return;
        var porId = new HashMap<UUID, long[]>();
        for (var material : materiais) porId.put(material.getId(), new long[3]);
        var parametros = Map.of("ids", porId.keySet());
        jdbc.query("""
            SELECT material_id,
                count(CASE WHEN tipo = 'CURTIDA' THEN 1 END) AS curtidas,
                count(CASE WHEN tipo = 'DESLIKE' THEN 1 END) AS deslikes
            FROM curtidas_material WHERE material_id IN (:ids) GROUP BY material_id
            """, parametros, rs -> {
                var totais = porId.get(rs.getObject("material_id", UUID.class));
                totais[0] = rs.getLong("curtidas"); totais[1] = rs.getLong("deslikes");
            });
        jdbc.query("SELECT material_id, count(*) AS total FROM favoritos WHERE material_id IN (:ids) GROUP BY material_id",
            parametros, rs -> { porId.get(rs.getObject("material_id", UUID.class))[2] = rs.getLong("total"); });
        for (var material : materiais) {
            var totais = porId.get(material.getId());
            material.definirTotais(totais[0], totais[1], totais[2]);
        }
    }
}
