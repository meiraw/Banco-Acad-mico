package br.wm.banco.academico;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TipoPublicacaoMigrationTests {
    @Test void preservaLegadosSemInventarTipoEValidaReferenciaNoBanco() throws Exception {
        var source = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "", true);
        var jdbc = new JdbcTemplate(source);
        try {
            jdbc.execute("CREATE TABLE materiais(id UUID PRIMARY KEY, titulo VARCHAR(255), tipo_arquivo VARCHAR(255), status_moderacao VARCHAR(20))");
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO materiais VALUES (?, ?, ?, ?)", id, "Material antigo", "application/pdf", "APROVADO");
            try (var stream = getClass().getResourceAsStream("/db/migration/V15__tipo_publicacao_e_referencia.sql")) {
                assertNotNull(stream);
                for (String sql : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";"))
                    if (!sql.isBlank()) jdbc.execute(sql);
            }
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM materiais", Integer.class));
            assertNull(jdbc.queryForObject("SELECT tipo_publicacao FROM materiais", String.class));
            assertNull(jdbc.queryForObject("SELECT referencia_bibliografica FROM materiais", String.class));
            assertEquals("APROVADO", jdbc.queryForObject("SELECT status_moderacao FROM materiais", String.class));
            assertEquals("application/pdf", jdbc.queryForObject("SELECT tipo_arquivo FROM materiais", String.class));
            assertEquals("Material antigo", jdbc.queryForObject("SELECT titulo FROM materiais", String.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE materiais SET referencia_bibliografica = 'Referência'"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE materiais SET tipo_publicacao = 'INVALIDO'"));
            jdbc.update("UPDATE materiais SET tipo_publicacao = 'LIVRO', referencia_bibliografica = 'Referência'");
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE materiais SET tipo_publicacao = 'ARTIGO'"));
            jdbc.update("UPDATE materiais SET tipo_publicacao = 'ARTIGO', referencia_bibliografica = NULL");
            try (var stream = getClass().getResourceAsStream("/db/migration/V16__referencia_para_artigos.sql")) {
                assertNotNull(stream);
                for (String sql : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";"))
                    if (!sql.isBlank()) jdbc.execute(sql);
            }
            jdbc.update("UPDATE materiais SET referencia_bibliografica = 'Autor. Artigo. 2026.'");
            assertEquals("Autor. Artigo. 2026.", jdbc.queryForObject("SELECT referencia_bibliografica FROM materiais", String.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE materiais SET tipo_publicacao = 'SLIDE'"));
            jdbc.update("UPDATE materiais SET tipo_publicacao = 'LIVRO'");
            assertEquals("Autor. Artigo. 2026.", jdbc.queryForObject("SELECT referencia_bibliografica FROM materiais", String.class));
        } finally { source.destroy(); }
    }
}
