package br.wm.banco.academico;

import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:configuracoes;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "security.jwt.secret=test-only-secret-with-at-least-32-bytes", "management.health.mail.enabled=false"
})
class ConfiguracoesTests {
    @DynamicPropertySource static void postgresOpcional(DynamicPropertyRegistry p) {
        String url = System.getenv("CONFIG_TEST_JDBC_URL");
        if (url == null) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/configuracoes_test"))
            throw new IllegalArgumentException("Exigido banco configuracoes_test isolado em loopback.");
        p.add("spring.datasource.url", () -> url); p.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        p.add("spring.datasource.username", () -> "postgres"); p.add("spring.datasource.password", () -> System.getenv("CONFIG_TEST_DB_PASSWORD"));
    }
    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired TokenService tokens;
    @MockitoBean JavaMailSender mail;
    @MockitoBean RecuperacaoLimiteService limites;
    MockMvc mvc;
    UsuarioModel a, b;
    String tokenA, tokenB, ip;
    boolean postgres;
    static AtomicInteger proximoIp = new AtomicInteger();
    static final String REDE_TESTE = "2001:db8:" + UUID.randomUUID().toString().replace("-", "").substring(0, 20)
        .replaceAll("(.{4})(?!$)", "$1:") + ":";
    static final String ANTIGA = "Antiga123!", NOVA = "NovaSenha123!";
    @BeforeEach void preparar() throws Exception {
        ip = REDE_TESTE + Integer.toHexString(proximoIp.incrementAndGet());
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
            .defaultRequest(get("/").with(r -> { r.setRemoteAddr(ip); return r; })).build();
        a = usuario(); b = usuario(); tokenA = tokens.gerarToken(a); tokenB = tokens.gerarToken(b);
        try (var c = jdbc.getDataSource().getConnection()) { postgres = c.getMetaData().getDatabaseProductName().equals("PostgreSQL"); }
        var contadores = new HashMap<String, Integer>();
        var real = new RecuperacaoLimiteService(jdbc);
        when(limites.permitir(anyString(), anyInt())).thenAnswer(i -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "Limite deve ser confirmado fora da transação da senha");
            String chave = i.getArgument(0); int max = i.getArgument(1);
            return postgres ? real.permitir(chave, max) : contadores.merge(chave, 1, Integer::sum) <= max;
        });
    }
    UsuarioModel usuario() {
        var u = new UsuarioModel(); u.setNome("Estudante"); u.setEmail(UUID.randomUUID() + "@example.test");
        u.setSenha(encoder.encode(ANTIGA)); u.setSemestreAtual(2); u.setPerfil(PerfilUsuario.USER);
        return usuarios.saveAndFlush(u);
    }
    String json(Object dados) { return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(dados); }
    ResultActions trocar(String token, String atual, String nova, String confirmacao) throws Exception {
        return mvc.perform(put("/perfil/senha").header("Authorization", "Bearer " + token).contentType("application/json")
            .content(json(Map.of("senhaAtual", atual, "novaSenha", nova, "confirmacaoNovaSenha", confirmacao))));
    }
    @Test void endpointsExigemAutenticacao() throws Exception {
        for (String url : List.of("/perfil", "/perfil/senha")) mvc.perform(put(url).contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/perfil/senha").header("Authorization", "Bearer invalido").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
    }
    @Test void atualizaSomenteSemestreEPreservaNomeEmailCursoESenha() throws Exception {
        String hash = a.getSenha();
        mvc.perform(put("/perfil").header("Authorization", "Bearer " + tokenA).contentType("application/json")
            .content(json(Map.of("nome", "  Maria Silva  ", "semestreAtual", 5, "id", b.getId(), "email", "outra@example.test",
                "curso", "Outro curso", "perfil", "ADMIN", "senha", "Ataque123!"))))
            .andExpect(status().isOk()).andExpect(jsonPath("id").value(a.getId().toString()))
            .andExpect(jsonPath("nome").value("Estudante")).andExpect(jsonPath("semestreAtual").value(5))
            .andExpect(jsonPath("curso").value("Ciência da Computação")).andExpect(jsonPath("senha").doesNotExist());
        var salvo = usuarios.findById(a.getId()).orElseThrow();
        assertEquals(hash, salvo.getSenha()); assertEquals(a.getEmail(), salvo.getEmail()); assertEquals(PerfilUsuario.USER, salvo.getPerfil());
        assertEquals(0, salvo.getVersaoCredenciais()); assertEquals("Estudante", usuarios.findById(b.getId()).orElseThrow().getNome());
        mvc.perform(get("/perfil").header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk()).andExpect(jsonPath("nome").value("Estudante"));
    }
    @Test void perfilRejeitaSemestreInvalidoOuAusente() throws Exception {
        var casos = List.of(Map.of("semestreAtual", 0), Map.of("semestreAtual", 9), Map.of("nome", "Nome"), Map.of());
        for (var dados : casos) mvc.perform(put("/perfil").header("Authorization", "Bearer " + tokenA).contentType("application/json").content(json(dados)))
            .andExpect(status().isBadRequest());
        assertEquals("Estudante", usuarios.findById(a.getId()).orElseThrow().getNome());
    }
    @Test void semestreAntigoNuloPodeSerPreenchido() throws Exception {
        jdbc.update("UPDATE usuarios SET semestre_atual = NULL WHERE id = ?", a.getId());
        mvc.perform(put("/perfil").header("Authorization", "Bearer " + tokenA).contentType("application/json")
            .content("{\"semestreAtual\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("semestreAtual").value(1));
    }
    @Test void senhaCorretaAtualizaHashRevogaRecuperacoesESessoes() throws Exception {
        String outroHash = b.getSenha();
        for (var u : List.of(a, b)) jdbc.update("INSERT INTO recuperacoes_senha VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '10' MINUTE)",
            RecuperacaoSenhaService.hash(u.getId().toString()), u.getId());
        trocar(tokenA, ANTIGA, NOVA, NOVA).andExpect(status().isOk()).andExpect(jsonPath("mensagem").isNotEmpty());
        var salvo = usuarios.findById(a.getId()).orElseThrow();
        assertTrue(encoder.matches(NOVA, salvo.getSenha())); assertFalse(encoder.matches(ANTIGA, salvo.getSenha()));
        assertNotEquals(NOVA, salvo.getSenha()); assertEquals(1, salvo.getVersaoCredenciais());
        assertEquals(outroHash, usuarios.findById(b.getId()).orElseThrow().getSenha());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM recuperacoes_senha WHERE usuario_id = ?", Integer.class, a.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM recuperacoes_senha WHERE usuario_id = ?", Integer.class, b.getId()));
        mvc.perform(get("/perfil").header("Authorization", "Bearer " + tokenA)).andExpect(status().isUnauthorized());
        mvc.perform(get("/perfil").header("Authorization", "Bearer " + tokenB)).andExpect(status().isOk());
        String resposta = mvc.perform(post("/auth/login").contentType("application/json").content(json(Map.of("email", a.getEmail(), "senha", NOVA))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/perfil").header("Authorization", "Bearer " + JsonPath.read(resposta, "$.token"))).andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType("application/json").content(json(Map.of("email", a.getEmail(), "senha", ANTIGA))))
            .andExpect(status().isConflict());
    }
    @Test void senhaAtualIncorretaNaoAlteraContaNemRecuperacao() throws Exception {
        jdbc.update("INSERT INTO recuperacoes_senha VALUES (?, ?, CURRENT_TIMESTAMP)", RecuperacaoSenhaService.hash(a.getId().toString()), a.getId());
        trocar(tokenA, "Errada123!", NOVA, NOVA).andExpect(status().isBadRequest()).andExpect(jsonPath("mensagem").value("Senha atual incorreta."));
        var salvo = usuarios.findById(a.getId()).orElseThrow();
        assertEquals(a.getSenha(), salvo.getSenha()); assertEquals(0, salvo.getVersaoCredenciais());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM recuperacoes_senha WHERE usuario_id = ?", Integer.class, a.getId()));
        mvc.perform(get("/perfil").header("Authorization", "Bearer " + tokenA)).andExpect(status().isOk());
    }
    @Test void validaNovaSenhaConfirmacaoELimiteUtf8() throws Exception {
        trocar(tokenA, ANTIGA, "curta", "curta").andExpect(status().isBadRequest());
        trocar(tokenA, ANTIGA, "        ", "        ").andExpect(status().isBadRequest());
        trocar(tokenA, ANTIGA, "\u00a0".repeat(8), "\u00a0".repeat(8)).andExpect(status().isBadRequest());
        trocar(tokenA, ANTIGA, NOVA, "Diferente123!").andExpect(status().isBadRequest());
        trocar(tokenA, ANTIGA, "á".repeat(37), "á".repeat(37)).andExpect(status().isBadRequest());
        trocar(tokenA, "á".repeat(37), NOVA, NOVA).andExpect(status().isBadRequest());
        assertEquals(a.getSenha(), usuarios.findById(a.getId()).orElseThrow().getSenha());
    }
    @Test void preservaEspacosDaSenhaESuportaLimiteDe72Bytes() throws Exception {
        String nova = " " + "a".repeat(70) + " ";
        trocar(tokenA, ANTIGA, nova, nova).andExpect(status().isOk());
        var salvo = usuarios.findById(a.getId()).orElseThrow();
        assertTrue(encoder.matches(nova, salvo.getSenha())); assertFalse(encoder.matches(nova.trim(), salvo.getSenha()));
    }
    @Test void tentativasFalhasPersistemELimiteDeContaNaoDependeDoIp() throws Exception {
        for (int i = 0; i < 5; i++) trocar(tokenA, "Errada123!", NOVA, NOVA).andExpect(status().isBadRequest());
        mvc.perform(put("/perfil/senha").with(r -> { r.setRemoteAddr("198.51.100.10"); return r; })
            .header("Authorization", "Bearer " + tokenA).contentType("application/json")
            .content(json(Map.of("senhaAtual", ANTIGA, "novaSenha", NOVA, "confirmacaoNovaSenha", NOVA))))
            .andExpect(status().isTooManyRequests());
        assertEquals(a.getSenha(), usuarios.findById(a.getId()).orElseThrow().getSenha());
        if (postgres) assertEquals(6, jdbc.queryForObject("SELECT tentativas FROM limites_recuperacao WHERE chave = ?", Integer.class,
            RecuperacaoSenhaService.hash("alterar-senha-conta:" + a.getId())));
    }
    @Test void administradorTrocaSenhaSemMudarEmailRevogaJwtERecuperacao() throws Exception {
        a.setPerfil(PerfilUsuario.ADMIN); usuarios.saveAndFlush(a);
        String administrador = tokens.gerarToken(a);
        String recuperacao = RecuperacaoSenhaService.hash("recuperacao-anterior");
        jdbc.update("INSERT INTO recuperacoes_senha(token_hash,usuario_id,expira_em) VALUES (?,?,?)",
            recuperacao, b.getId(), java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(900)));
        mvc.perform(put("/usuario/" + b.getId()).header("Authorization", "Bearer " + administrador)
                .contentType("application/json")
                .content(json(Map.of("nome", b.getNome(), "email", b.getEmail(), "senha", NOVA))))
            .andExpect(status().isOk());
        var salvo = usuarios.findById(b.getId()).orElseThrow();
        assertEquals(1, salvo.getVersaoCredenciais());
        assertEquals(b.getEmail(), salvo.getEmail());
        assertTrue(encoder.matches(NOVA, salvo.getSenha()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM recuperacoes_senha WHERE usuario_id = ?", Integer.class, b.getId()));
        mvc.perform(get("/perfil").header("Authorization", "Bearer " + tokenB)).andExpect(status().isUnauthorized());
        mvc.perform(get("/perfil").header("Authorization", "Bearer " + tokens.gerarToken(salvo))).andExpect(status().isOk());
    }
    @Test void estudanteNaoPodeTrocarSenhaAdministrativaEExcessoDeBytesNaoAlteraConta() throws Exception {
        var dados = Map.of("nome", b.getNome(), "email", b.getEmail(), "senha", NOVA);
        mvc.perform(put("/usuario/" + b.getId()).header("Authorization", "Bearer " + tokenA)
                .contentType("application/json").content(json(dados))).andExpect(status().isForbidden());
        a.setPerfil(PerfilUsuario.ADMIN); usuarios.saveAndFlush(a);
        mvc.perform(put("/usuario/" + b.getId()).header("Authorization", "Bearer " + tokens.gerarToken(a))
                .contentType("application/json")
                .content(json(Map.of("nome", b.getNome(), "email", b.getEmail(), "senha", "á".repeat(37)))))
            .andExpect(status().isBadRequest());
        var salvo = usuarios.findById(b.getId()).orElseThrow();
        assertEquals(0, salvo.getVersaoCredenciais());
        assertTrue(encoder.matches(ANTIGA, salvo.getSenha()));
        mvc.perform(get("/perfil").header("Authorization", "Bearer " + tokenB)).andExpect(status().isOk());
    }
    @Test void limiteDeIpBloqueiaAntesDaOperacao() throws Exception {
        when(limites.permitir("alterar-senha-ip:" + ip, 20)).thenReturn(false);
        trocar(tokenA, ANTIGA, NOVA, NOVA).andExpect(status().isTooManyRequests());
        verify(limites, never()).permitir("alterar-senha-conta:" + a.getId(), 5);
        assertEquals(a.getSenha(), usuarios.findById(a.getId()).orElseThrow().getSenha());
    }
}
