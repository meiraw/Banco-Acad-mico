package br.wm.banco.academico;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CadastroAcademicoMigrationTests {
    @Test void v9PreservaUsuarioAntigoEValidaSemestre() throws Exception {
        var banco = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", ""));
        try (var connection = banco.getDataSource().getConnection()) {
            executar(banco, "V6__criar_tabela_usuarios.sql");
            UUID id = UUID.randomUUID();
            String hash = new BCryptPasswordEncoder().encode("SenhaAntiga123!");
            banco.update("INSERT INTO usuarios(id, nome, email, senha, perfil) VALUES (?, ?, ?, ?, ?)",
                    id, "Conta antiga", "antiga@example.org", hash, "ADMIN");
            executar(banco, "V7__recuperacao_senha.sql");
            executar(banco, "V8__confirmacao_email.sql");
            executar(banco, "V9__cadastro_academico.sql");
            assertEquals(1, banco.queryForObject("SELECT count(*) FROM usuarios", Integer.class));
            assertEquals("Ciência da Computação", banco.queryForObject("SELECT curso FROM usuarios", String.class));
            assertNull(banco.queryForObject("SELECT semestre_atual FROM usuarios", Integer.class));
            assertEquals("ADMIN", banco.queryForObject("SELECT perfil FROM usuarios", String.class));
            assertEquals(hash, banco.queryForObject("SELECT senha FROM usuarios", String.class));
            assertTrue(new BCryptPasswordEncoder().matches("SenhaAntiga123!", hash));
            assertEquals(id, banco.queryForObject("SELECT id FROM usuarios", UUID.class));
            for (int semestre : new int[]{0, 9}) {
                assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                        () -> banco.update("UPDATE usuarios SET semestre_atual = ?", semestre));
            }
            for (int semestre : new int[]{1, 8}) {
                banco.update("UPDATE usuarios SET semestre_atual = ?", semestre);
                assertEquals(semestre, banco.queryForObject("SELECT semestre_atual FROM usuarios", Integer.class));
            }
        }
    }

    private void executar(JdbcTemplate banco, String migration) throws Exception {
        try (var stream = getClass().getResourceAsStream("/db/migration/" + migration)) {
            assertNotNull(stream);
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String comando : sql.split(";")) if (!comando.isBlank()) banco.execute(comando);
        }
    }
}
