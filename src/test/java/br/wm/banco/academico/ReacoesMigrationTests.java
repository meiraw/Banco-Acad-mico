package br.wm.banco.academico;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ReacoesMigrationTests {
    @Test void preservaCurtidasAntigasEImpedeDuasReacoesDaMesmaConta() throws Exception {
        var source = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "", true);
        var jdbc = new JdbcTemplate(source);
        try {
            jdbc.execute("CREATE TABLE curtidas_material(material_id UUID, usuario_id UUID, PRIMARY KEY(material_id, usuario_id))");
            UUID material = UUID.randomUUID(), usuario = UUID.randomUUID();
            jdbc.update("INSERT INTO curtidas_material VALUES (?,?)", material, usuario);
            try (var stream = getClass().getResourceAsStream("/db/migration/V18__reacoes_negativas.sql")) {
                assertNotNull(stream);
                for (String sql : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";"))
                    if (!sql.isBlank()) jdbc.execute(sql);
            }
            assertEquals("CURTIDA", jdbc.queryForObject("SELECT tipo FROM curtidas_material", String.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO curtidas_material VALUES (?,?,'DESLIKE')", material, usuario));
            jdbc.update("UPDATE curtidas_material SET tipo = 'DESLIKE'");
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM curtidas_material", Integer.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE curtidas_material SET tipo = 'INVALIDO'"));
        } finally { source.destroy(); }
    }
}
