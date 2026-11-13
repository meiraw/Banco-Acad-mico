package br.wm.banco.academico;

import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.*;
import br.wm.banco.academico.DTOs.Request.RespostaForumRequestDTO;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:forum;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "security.jwt.secret=test-only-secret-with-at-least-32-bytes", "management.health.mail.enabled=false"
})
class ForumTests {
    @DynamicPropertySource static void postgresOpcional(DynamicPropertyRegistry p) {
        String url = System.getenv("FORUM_TEST_JDBC_URL");
        if (url == null) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/forum_test"))
            throw new IllegalArgumentException("Exigido banco forum_test isolado em loopback.");
        p.add("spring.datasource.url", () -> url);
        p.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        p.add("spring.datasource.username", () -> "postgres");
        p.add("spring.datasource.password", () -> System.getenv("FORUM_TEST_DB_PASSWORD"));
    }
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenService tokens;
    @Autowired ForumService forum;
    @MockitoBean JavaMailSender mail;
    MockMvc mvc;
    UsuarioModel a, b, admin;
    UUID disciplina, outraDisciplina;
    String prefixo;
    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        a = usuario(PerfilUsuario.USER); b = usuario(PerfilUsuario.USER); admin = usuario(PerfilUsuario.ADMIN);
        prefixo = UUID.randomUUID().toString();
        UUID curso = UUID.randomUUID(), semestre = UUID.randomUUID();
        jdbc.update("INSERT INTO cursos(id,nome) VALUES (?,?)", curso, "Curso " + prefixo);
        jdbc.update("INSERT INTO semestres(id,nome,numero,curso_id) VALUES (?,?,?,?)", semestre, "Primeiro", 1, curso);
        disciplina = disciplina(semestre, "Algoritmos " + prefixo);
        outraDisciplina = disciplina(semestre, "Redes " + prefixo);
    }
    UsuarioModel usuario(PerfilUsuario perfil) {
        var u = new UsuarioModel(); u.setNome("Estudante " + perfil); u.setEmail(UUID.randomUUID() + "@example.test");
        u.setSenha("hash-teste"); u.setPerfil(perfil); u.setEmailConfirmado(true); return usuarios.saveAndFlush(u);
    }
    UUID disciplina(UUID semestre, String nome) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO disciplinas(id,nome,descricao,semestre_id) VALUES (?,?,?,?)", id, nome, "Disciplina", semestre);
        return id;
    }
    String bearer(UsuarioModel u) { return "Bearer " + tokens.gerarToken(u); }
    String json(String titulo, UUID disc, String conteudo, Long versao) {
        var dados = new HashMap<String, Object>(); dados.put("titulo", titulo); dados.put("disciplinaId", disc);
        dados.put("conteudo", conteudo); dados.put("versao", versao);
        return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(dados);
    }
    UUID criar(UsuarioModel u, UUID disc, String titulo, String conteudo) throws Exception {
        String resposta = mvc.perform(post("/forum/discussoes").header("Authorization", bearer(u)).contentType(MediaType.APPLICATION_JSON)
            .content(json(titulo, disc, conteudo, null))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }
    UUID criar() throws Exception { return criar(a, disciplina, "Dúvida " + prefixo, "Conteúdo da dúvida"); }
    UUID responder(UUID id, UsuarioModel u) throws Exception {
        String resposta = mvc.perform(post("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(u))
            .contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Uma explicação\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    @Test void todasOperacoesExigemAutenticacao() throws Exception {
        String base = "/forum/discussoes", id = UUID.randomUUID().toString();
        for (String url : List.of(base, base + "/" + id, base + "/" + id + "/respostas")) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        }
        for (String url : List.of(base + "/" + id, base + "/" + id + "/respostas/" + id)) {
            mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
            mvc.perform(delete(url)).andExpect(status().isUnauthorized());
        }
    }
    @Test void criarUsaAutorAutenticadoERegistraData() throws Exception {
        String dados = json("  Título  ", disciplina, "  Conteúdo  ", null);
        dados = dados.substring(0, dados.length() - 1) + ",\"autorId\":\"" + b.getId() + "\"}";
        mvc.perform(post("/forum/discussoes").header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON).content(dados))
            .andExpect(status().isCreated()).andExpect(jsonPath("autorId").value(a.getId().toString()))
            .andExpect(jsonPath("titulo").value("Título")).andExpect(jsonPath("conteudo").value("Conteúdo"))
            .andExpect(jsonPath("versao").value(0)).andExpect(jsonPath("criadoEm").isNotEmpty())
            .andExpect(jsonPath("totalRespostas").value(0)).andExpect(jsonPath("autor.senha").doesNotExist());
    }
    @Test void validaCamposObrigatoriosTamanhosEDisciplina() throws Exception {
        for (String dados : List.of(json(null, disciplina, "Conteúdo", null), json("", disciplina, "Conteúdo", null),
            json("Título", null, "Conteúdo", null), json("Título", disciplina, "  \n  ", null),
            json("Título", disciplina, "\u00a0\u00a0", null), json("T".repeat(201), disciplina, "Conteúdo", null),
            json("Título", disciplina, "C".repeat(10001), null))) {
            mvc.perform(post("/forum/discussoes").header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON).content(dados))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("mensagem").isNotEmpty());
        }
        mvc.perform(post("/forum/discussoes").header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
            .content(json("Título", UUID.randomUUID(), "Conteúdo", null))).andExpect(status().isNotFound());
    }
    @Test void pesquisaTituloConteudoDisciplinaEFiltraAutor() throws Exception {
        UUID um = criar(a, disciplina, "Título " + prefixo, "BuscaNoConteudo");
        criar(b, outraDisciplina, "Outro " + prefixo, "Outro conteúdo");
        for (String busca : List.of("Título " + prefixo, "BuscaNoConteudo", "Algoritmos " + prefixo)) {
            mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("q", busca).param("disciplinaId", disciplina.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("page.totalElements").value(1))
                .andExpect(jsonPath("content[0].id").value(um.toString()));
        }
        mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("q", prefixo).param("minhas", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("page.totalElements").value(1));
        mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("q", prefixo))
            .andExpect(status().isOk()).andExpect(jsonPath("page.totalElements").value(2));
    }
    @Test void buscaTrataCuringasComoTextoLiteral() throws Exception {
        criar(a, disciplina, "100%_ " + prefixo, "literal");
        criar(a, disciplina, "100XY " + prefixo, "outro");
        mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("q", "100%_").param("disciplinaId", disciplina.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("page.totalElements").value(1));
        mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("q", "' OR 1=1 --"))
            .andExpect(status().isOk()).andExpect(jsonPath("page.totalElements").value(0));
    }
    @Test void paginaOrdenaComDesempateELimitaParametros() throws Exception {
        var ids = new ArrayList<UUID>();
        for (int i = 0; i < 3; i++) ids.add(criar(a, disciplina, prefixo + i, "Conteúdo"));
        jdbc.update("UPDATE discussoes_forum SET criado_em = CURRENT_TIMESTAMP WHERE disciplina_id = ?", disciplina);
        ids.sort(Comparator.naturalOrder());
        // A representação textual preserva a ordem UUID do PostgreSQL e H2.
        ids.sort(Comparator.comparing(UUID::toString));
        for (int page = 0; page < 3; page++) {
            mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("disciplinaId", disciplina.toString())
                .param("page", String.valueOf(page)).param("size", "1").param("ordem", "antigas"))
                .andExpect(status().isOk()).andExpect(jsonPath("page.totalPages").value(3))
                .andExpect(jsonPath("content[0].id").value(ids.get(page).toString()));
        }
        for (var par : List.of(new String[]{"page", "-1"}, new String[]{"page", "10001"}, new String[]{"size", "51"},
            new String[]{"size", "0"}, new String[]{"ordem", "sql"}, new String[]{"disciplinaId", "invalido"}, new String[]{"q", "q".repeat(201)})) {
            mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param(par[0], par[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("mensagem").isNotEmpty());
        }
        mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("disciplinaId", ""))
            .andExpect(status().isOk());
    }
    @Test void autorEditaDiscussaoEConflitoNaoSobrescreve() throws Exception {
        UUID id = criar();
        mvc.perform(put("/forum/discussoes/" + id).header("Authorization", bearer(b)).contentType(MediaType.APPLICATION_JSON)
            .content(json("Alterado", disciplina, "Conteúdo", 0L))).andExpect(status().isForbidden());
        mvc.perform(put("/forum/discussoes/" + id).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
            .content(json("Alterado", disciplina, "Conteúdo", 0L))).andExpect(status().isForbidden());
        mvc.perform(put("/forum/discussoes/" + id).header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
            .content(json("Alterado", outraDisciplina, "Novo", 0L))).andExpect(status().isOk())
            .andExpect(jsonPath("versao").value(1)).andExpect(jsonPath("disciplinaId").value(outraDisciplina.toString()));
        mvc.perform(put("/forum/discussoes/" + id).header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
            .content(json("Perdido", disciplina, "Velho", 0L))).andExpect(status().isConflict());
        mvc.perform(put("/forum/discussoes/" + id).header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
            .content(json("Perdido", disciplina, "Velho", null))).andExpect(status().isBadRequest());
    }
    @Test void respostasPersistemContamEAutorPodeEditarOuExcluir() throws Exception {
        UUID id = criar(), resposta = responder(id, b);
        mvc.perform(get("/forum/discussoes/" + id).header("Authorization", bearer(a))).andExpect(jsonPath("totalRespostas").value(1));
        String url = "/forum/discussoes/" + id + "/respostas/" + resposta;
        for (var conta : List.of(a, admin)) mvc.perform(put(url).header("Authorization", bearer(conta))
            .contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Alterada\",\"versao\":0}"))
            .andExpect(status().isForbidden());
        mvc.perform(put(url).header("Authorization", bearer(b)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"conteudo\":\"Alterada\",\"versao\":0}")).andExpect(status().isOk()).andExpect(jsonPath("versao").value(1));
        mvc.perform(put(url).header("Authorization", bearer(b)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"conteudo\":\"Velha\",\"versao\":0}")).andExpect(status().isConflict());
        mvc.perform(delete(url).header("Authorization", bearer(a))).andExpect(status().isForbidden());
        mvc.perform(delete(url).header("Authorization", bearer(b))).andExpect(status().isNoContent());
        mvc.perform(get("/forum/discussoes/" + id).header("Authorization", bearer(a))).andExpect(jsonPath("totalRespostas").value(0));
        mvc.perform(get("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(a)))
            .andExpect(status().isOk()).andExpect(jsonPath("content").isEmpty());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM respostas_forum WHERE id = ? AND excluido_em IS NOT NULL", Integer.class, resposta));
    }
    @Test void respostasSaoPaginadasEContagemNaoDependeDaPagina() throws Exception {
        UUID id = criar();
        for (int i = 0; i < 21; i++) forum.responder(b.getEmail(), id, new RespostaForumRequestDTO("Resposta " + i, null));
        mvc.perform(get("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(a)))
            .andExpect(status().isOk()).andExpect(jsonPath("content.length()").value(20))
            .andExpect(jsonPath("page.totalElements").value(21)).andExpect(jsonPath("page.totalPages").value(2));
        mvc.perform(get("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(a)).param("page", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("content.length()").value(1))
            .andExpect(jsonPath("content[0].conteudo").value("Resposta 20"));
        mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("disciplinaId", disciplina.toString()))
            .andExpect(jsonPath("content[0].totalRespostas").value(21));
        mvc.perform(get("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(a)).param("size", "51"))
            .andExpect(status().isBadRequest());
    }
    @Test void validaRespostaEPertencimento() throws Exception {
        UUID id = criar(), outro = criar(), resposta = responder(id, a);
        for (String conteudo : List.of("", "   ", "x".repeat(5001))) {
            mvc.perform(post("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(b))
                .contentType(MediaType.APPLICATION_JSON).content(tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(Map.of("conteudo", conteudo))))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(delete("/forum/discussoes/" + outro + "/respostas/" + resposta).header("Authorization", bearer(a)))
            .andExpect(status().isNotFound());
        // O fórum permite complementar a própria discussão, sem herdar a restrição dos comentários de materiais.
        mvc.perform(get("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(a)))
            .andExpect(jsonPath("content[0].autorId").value(a.getId().toString()));
    }
    @Test void exclusaoLogicaOcultaConversaSemApagarRespostas() throws Exception {
        UUID id = criar(), resposta = responder(id, b);
        mvc.perform(delete("/forum/discussoes/" + id).header("Authorization", bearer(b))).andExpect(status().isForbidden());
        mvc.perform(delete("/forum/discussoes/" + id).header("Authorization", bearer(a))).andExpect(status().isNoContent());
        mvc.perform(get("/forum/discussoes/" + id).header("Authorization", bearer(admin))).andExpect(status().isNotFound());
        mvc.perform(get("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(b))).andExpect(status().isNotFound());
        mvc.perform(post("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(b)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"conteudo\":\"Nova\"}")).andExpect(status().isNotFound());
        mvc.perform(get("/forum/discussoes").header("Authorization", bearer(a)).param("disciplinaId", disciplina.toString()))
            .andExpect(jsonPath("page.totalElements").value(0));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM respostas_forum WHERE id = ?", Integer.class, resposta));
    }
    @Test void adminModeraSemApagarFisicamente() throws Exception {
        UUID id = criar(), resposta = responder(id, b);
        mvc.perform(delete("/forum/discussoes/" + id + "/respostas/" + resposta).header("Authorization", bearer(admin))).andExpect(status().isNoContent());
        mvc.perform(delete("/forum/discussoes/" + id).header("Authorization", bearer(admin))).andExpect(status().isNoContent());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM discussoes_forum WHERE id = ?", Integer.class, id));
    }
    @Test void excluirContaPreservaDiscussaoERespostaAnonimizadas() throws Exception {
        UUID id = criar(), resposta = responder(id, a);
        jdbc.update("DELETE FROM usuarios WHERE id = ?", a.getId());
        mvc.perform(get("/forum/discussoes/" + id).header("Authorization", bearer(b)))
            .andExpect(status().isOk()).andExpect(jsonPath("autorId").isEmpty()).andExpect(jsonPath("autorNome").value("Conta excluída"));
        mvc.perform(get("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(b)))
            .andExpect(jsonPath("content[0].id").value(resposta.toString())).andExpect(jsonPath("content[0].autorNome").value("Conta excluída"));
    }
    @Test void disciplinaComHistoricoNaoPodeSerExcluida() throws Exception {
        UUID id = criar();
        forum.excluir(a.getEmail(), id);
        mvc.perform(delete("/disciplinas/" + disciplina).header("Authorization", bearer(admin)))
            .andExpect(status().isConflict()).andExpect(jsonPath("mensagem").value("Esta disciplina possui histórico no fórum e não pode ser excluída."));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("DELETE FROM disciplinas WHERE id = ?", disciplina));
    }
    @Test void responderConcorrenteComExclusaoNaoReabreConversa() throws Exception {
        UUID id = criar();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var largada = new CountDownLatch(1);
            var envio = executor.submit(() -> {
                largada.await();
                try { forum.responder(b.getEmail(), id, new RespostaForumRequestDTO("Concorrente", null)); }
                catch (org.springframework.web.server.ResponseStatusException e) { assertEquals(404, e.getStatusCode().value()); }
                return null;
            });
            var exclusao = executor.submit(() -> { largada.await(); forum.excluir(a.getEmail(), id); return null; });
            largada.countDown(); envio.get(10, TimeUnit.SECONDS); exclusao.get(10, TimeUnit.SECONDS);
        }
        mvc.perform(get("/forum/discussoes/" + id + "/respostas").header("Authorization", bearer(b))).andExpect(status().isNotFound());
    }
    @Test void tokenInvalidoEIdMalformadoNaoExpoemDetalhes() throws Exception {
        mvc.perform(get("/forum/discussoes").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
        mvc.perform(get("/forum/discussoes/invalido").header("Authorization", bearer(a))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("mensagem").value("Parâmetro inválido.")).andExpect(jsonPath("trace").doesNotExist());
    }
}
