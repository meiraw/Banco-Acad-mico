package br.wm.banco.academico;

import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.TokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:favoritos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "security.jwt.secret=test-only-secret-with-at-least-32-bytes", "management.health.mail.enabled=false"
})
class FavoritosTests {
    @DynamicPropertySource static void postgresOpcional(DynamicPropertyRegistry properties) {
        String url = System.getenv("FAVORITOS_TEST_JDBC_URL");
        if (url == null) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/favoritos_test"))
            throw new IllegalArgumentException("Os testes PostgreSQL exigem banco favoritos_test isolado em localhost.");
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> System.getenv("FAVORITOS_TEST_DB_PASSWORD"));
    }
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenService tokens;
    @MockitoBean JavaMailSender mail;
    MockMvc mvc;
    UsuarioModel a, b, admin;
    UUID disciplina;

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        a = usuario(PerfilUsuario.USER); b = usuario(PerfilUsuario.USER); admin = usuario(PerfilUsuario.ADMIN);
        UUID curso = UUID.randomUUID(), semestre = UUID.randomUUID(); disciplina = UUID.randomUUID();
        jdbc.update("INSERT INTO cursos(id,nome) VALUES (?, ?)", curso, "Curso " + curso);
        jdbc.update("INSERT INTO semestres(id,nome,numero,curso_id) VALUES (?, ?, ?, ?)", semestre, "Semestre", 1, curso);
        jdbc.update("INSERT INTO disciplinas(id,nome,descricao,semestre_id) VALUES (?, ?, ?, ?)", disciplina, "Disciplina " + disciplina, "Descrição", semestre);
    }
    UsuarioModel usuario(PerfilUsuario perfil) {
        var usuario = new UsuarioModel(); usuario.setNome("Estudante " + perfil); usuario.setEmail(UUID.randomUUID() + "@example.test");
        usuario.setSenha("hash-teste"); usuario.setPerfil(perfil); usuario.setEmailConfirmado(true);
        return usuarios.saveAndFlush(usuario);
    }
    String bearer(UsuarioModel usuario) { return "Bearer " + tokens.gerarToken(usuario); }
    UUID material(String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO materiais(id,titulo,descricao,disciplina_id,nome_arquivo,tipo_arquivo,caminho_arquivo,
                                  tamanho_arquivo,autor_id,status_moderacao,tipo_publicacao,publicado_em)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """, id, "Material " + id, "Descrição", disciplina, "teste.pdf", "application/pdf", "Uploads/" + id,
            100L, a.getId(), status, "RESUMO");
        return id;
    }
    void adicionar(UUID id, UsuarioModel usuario) throws Exception {
        mvc.perform(put("/favoritos/" + id).header("Authorization", bearer(usuario))).andExpect(status().isNoContent());
    }

    @Test void adicionaRemoveSemDuplicarENaoAlteraReacoes() throws Exception {
        UUID id = material("APROVADO");
        jdbc.update("INSERT INTO curtidas_material(material_id,usuario_id,tipo) VALUES (?,?,'CURTIDA')", id, a.getId());
        adicionar(id, a); adicionar(id, a);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE material_id = ? AND usuario_id = ?", Integer.class, id, a.getId()));
        mvc.perform(get("/favoritos").header("Authorization", bearer(a)))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "private, no-store"))
            .andExpect(jsonPath("page.totalElements").value(1)).andExpect(jsonPath("content[0].id").value(id.toString()))
            .andExpect(jsonPath("content[0].autor.email").doesNotExist());
        for (int i = 0; i < 2; i++) mvc.perform(delete("/favoritos/" + id).header("Authorization", bearer(a))).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE material_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM curtidas_material WHERE material_id = ?", Integer.class, id));
    }

    @Test void separaContasENaoConfiaNoUsuarioEnviadoPeloCliente() throws Exception {
        UUID id = material("APROVADO"); adicionar(id, a);
        mvc.perform(get("/favoritos?usuarioId=" + a.getId()).header("Authorization", bearer(b)))
            .andExpect(status().isOk()).andExpect(jsonPath("content").isEmpty());
        mvc.perform(delete("/favoritos/" + id).header("Authorization", bearer(b))).andExpect(status().isNoContent());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE usuario_id = ?", Integer.class, a.getId()));
        adicionar(id, b); adicionar(id, admin);
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE material_id = ?", Integer.class, id));
        mvc.perform(get("/favoritos/ids").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("content[0]").value(id.toString()));
    }

    @Test void exigeJwtEValidaIdentificadoresEPaginacao() throws Exception {
        UUID id = material("APROVADO");
        mvc.perform(get("/favoritos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/favoritos/ids")).andExpect(status().isUnauthorized());
        mvc.perform(put("/favoritos/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/favoritos/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/favoritos").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
        mvc.perform(put("/favoritos/invalido").header("Authorization", bearer(a)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("mensagem").exists());
        for (String parametros : List.of("page=-1", "size=0", "size=201", "page=abc"))
            mvc.perform(get("/favoritos?" + parametros).header("Authorization", bearer(a))).andExpect(status().isBadRequest());
    }

    @Test void naoPermiteFavoritarPendentesRejeitadosOuInexistentesNemParaAutorOuAdmin() throws Exception {
        for (UUID id : List.of(material("PENDENTE"), material("REJEITADO"), UUID.randomUUID())) {
            for (var usuario : List.of(a, b, admin)) {
                mvc.perform(put("/favoritos/" + id).header("Authorization", bearer(usuario))).andExpect(status().isNotFound());
                mvc.perform(delete("/favoritos/" + id).header("Authorization", bearer(usuario))).andExpect(status().isNoContent());
            }
        }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE usuario_id = ?", Integer.class, a.getId()));
    }

    @Test void consultaRevalidaVisibilidadeEDesaparecimentoDoMaterial() throws Exception {
        UUID id = material("APROVADO"); adicionar(id, a);
        jdbc.update("UPDATE materiais SET status_moderacao = 'REJEITADO' WHERE id = ?", id);
        mvc.perform(get("/favoritos").header("Authorization", bearer(a))).andExpect(jsonPath("content").isEmpty());
        mvc.perform(get("/favoritos/ids").header("Authorization", bearer(a))).andExpect(jsonPath("content").isEmpty());
        mvc.perform(delete("/favoritos/" + id).header("Authorization", bearer(a))).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE material_id = ?", Integer.class, id));
    }

    @Test void preservaFavoritosEntreSessoesEClientesIndependentes() throws Exception {
        UUID id = material("APROVADO"); adicionar(id, a);
        var outroNavegador = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        outroNavegador.perform(get("/favoritos").header("Authorization", bearer(a)))
            .andExpect(status().isOk()).andExpect(jsonPath("content[0].id").value(id.toString()));
        outroNavegador.perform(delete("/favoritos/" + id).header("Authorization", bearer(a))).andExpect(status().isNoContent());
        mvc.perform(get("/favoritos").header("Authorization", bearer(a))).andExpect(jsonPath("content").isEmpty());
    }

    @Test void paginaMateriaisEIdsSemDependerDoCatalogoGeral() throws Exception {
        for (int i = 0; i < 5; i++) adicionar(material("APROVADO"), a);
        mvc.perform(get("/favoritos?size=2&page=0").header("Authorization", bearer(a)))
            .andExpect(jsonPath("content.length()").value(2)).andExpect(jsonPath("page.totalElements").value(5))
            .andExpect(jsonPath("page.totalPages").value(3));
        mvc.perform(get("/favoritos?size=2&page=2").header("Authorization", bearer(a)))
            .andExpect(jsonPath("content.length()").value(1));
        mvc.perform(get("/favoritos/ids?size=2&page=2").header("Authorization", bearer(a)))
            .andExpect(jsonPath("content.length()").value(1)).andExpect(jsonPath("page.totalElements").value(5));
    }

    @Test void bancoGaranteUnicidadeIntegridadeECascata() throws Exception {
        UUID id = material("APROVADO"); adicionar(id, a); adicionar(id, b);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("INSERT INTO favoritos(usuario_id,material_id) VALUES (?,?)", a.getId(), id));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("INSERT INTO favoritos(usuario_id,material_id) VALUES (?,?)", a.getId(), UUID.randomUUID()));
        jdbc.update("DELETE FROM usuarios WHERE id = ?", b.getId());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE material_id = ?", Integer.class, id));
        jdbc.update("DELETE FROM materiais WHERE id = ?", id);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE material_id = ?", Integer.class, id));
    }

    @Test void cliquesConcorrentesContinuamComUmUnicoRegistro() throws Exception {
        UUID id = material("APROVADO"); String token = bearer(a);
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Future<Integer>> chamadas = new ArrayList<>();
            for (int i = 0; i < 8; i++) chamadas.add(pool.submit(() -> {
                inicio.await(); return mvc.perform(put("/favoritos/" + id).header("Authorization", token)).andReturn().getResponse().getStatus();
            }));
            inicio.countDown();
            for (var chamada : chamadas) assertEquals(204, chamada.get(20, TimeUnit.SECONDS));
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM favoritos WHERE material_id = ? AND usuario_id = ?", Integer.class, id, a.getId()));
    }
}
