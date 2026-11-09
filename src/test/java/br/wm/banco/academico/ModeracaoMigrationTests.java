package br.wm.banco.academico;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ModeracaoMigrationTests {
    @Test void preservaLegadosESomenteNovosRegistrosComecamPendentes() throws Exception {
        // H2 vincula a comparação do CHECK IN à conexão que criou a constraint.
        var source = new SingleConnectionDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "", true);
        var banco = new JdbcTemplate(source);
        try {
        banco.execute("CREATE TABLE usuarios(id UUID PRIMARY KEY)");
        banco.execute("CREATE TABLE materiais(id UUID PRIMARY KEY, titulo VARCHAR(255), publicado_em TIMESTAMP WITH TIME ZONE)");
        UUID antigo = UUID.randomUUID(), novo = UUID.randomUUID();
        banco.update("INSERT INTO materiais(id,titulo) VALUES (?,?)", antigo, "Material legado");
        try (var stream = getClass().getResourceAsStream("/db/migration/V14__moderacao_materiais.sql")) {
            assertNotNull(stream);
            for (String sql : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";"))
                if (!sql.isBlank()) banco.execute(sql);
        }
        assertEquals("APROVADO", banco.queryForObject("SELECT status_moderacao FROM materiais WHERE id = ?", String.class, antigo));
        assertEquals("Material legado", banco.queryForObject("SELECT titulo FROM materiais WHERE id = ?", String.class, antigo));
        assertNull(banco.queryForObject("SELECT decisao_em FROM materiais WHERE id = ?", java.sql.Timestamp.class, antigo));
        assertNull(banco.queryForObject("SELECT moderador_id FROM materiais WHERE id = ?", UUID.class, antigo));
        banco.update("INSERT INTO materiais(id,titulo) VALUES (?,?)", novo, "Novo material");
        assertEquals("PENDENTE", banco.queryForObject("SELECT status_moderacao FROM materiais WHERE id = ?", String.class, novo));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> banco.update("UPDATE materiais SET status_moderacao = 'INVALIDO' WHERE id = ?", novo));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> banco.update("UPDATE materiais SET moderador_id = ? WHERE id = ?", UUID.randomUUID(), novo));
        } finally { source.destroy(); }
    }
}
