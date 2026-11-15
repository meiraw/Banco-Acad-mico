package br.wm.banco.academico;

import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Enum.StatusModeracao;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.MaterialRepository;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.MaterialService;
import br.wm.banco.academico.Service.TokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:moderacao;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "security.jwt.secret=test-only-secret-with-at-least-32-bytes",
    "app.moderacao.email-admin=admin@example.test", "app.recuperacao.remetente=sistema@example.test",
    "management.health.mail.enabled=false"
})
class ModeracaoTests {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired MaterialRepository materiais;
    @Autowired UsuarioRepository usuarios;
    @Autowired MaterialService service;
    @Autowired br.wm.banco.academico.Service.LimpezaArquivoService limpeza;
    @Autowired TokenService tokens;
    @Autowired br.wm.banco.academico.Service.ArmazenamentoService armazenamento;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JavaMailSender mail;
    MockMvc mvc;
    UsuarioModel autor, colega, admin;
    UUID disciplina;
    List<Path> arquivos = new ArrayList<>();

    @BeforeEach void preparar() {
        reset(mail);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        autor = usuario(PerfilUsuario.USER); colega = usuario(PerfilUsuario.USER); admin = usuario(PerfilUsuario.ADMIN);
        UUID curso = UUID.randomUUID(), semestre = UUID.randomUUID(); disciplina = UUID.randomUUID();
        jdbc.update("INSERT INTO cursos(id,nome) VALUES (?,?)", curso, "Computação " + curso);
        jdbc.update("INSERT INTO semestres(id,nome,numero,curso_id) VALUES (?,?,?,?)", semestre, "Primeiro", 1, curso);
        jdbc.update("INSERT INTO disciplinas(id,nome,descricao,semestre_id) VALUES (?,?,?,?)", disciplina, "Algoritmos " + disciplina, "Disciplina", semestre);
    }

    UsuarioModel usuario(PerfilUsuario perfil) {
        var u = new UsuarioModel(); u.setNome("Estudante " + perfil); u.setEmail(UUID.randomUUID() + "@example.test");
        u.setSenha("hash-de-teste"); u.setPerfil(perfil); u.setEmailConfirmado(true);
        return usuarios.saveAndFlush(u);
    }
    String bearer(UsuarioModel u) { return "Bearer " + tokens.gerarToken(u); }
    void autenticar(UsuarioModel u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            u.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name()))));
    }
    UUID enviar() throws Exception {
        return enviar("RESUMO", null);
    }
    MockMultipartFile dadosPublicacao(String tipo, String referencia) {
        String extras = (tipo == null ? "" : ",\"tipoPublicacao\":\"" + tipo + "\"")
                + (referencia == null ? "" : ",\"referenciaBibliografica\":\"" + referencia + "\"");
        return new MockMultipartFile("dados", "", "application/json",
            ("{\"titulo\":\"Resumo de teste\",\"descricao\":\"Descrição do material\",\"disciplinaId\":\"" + disciplina + "\"" + extras + "}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    UUID enviar(String tipo, String referencia) throws Exception {
        var dados = dadosPublicacao(tipo, referencia);
        mvc.perform(multipart("/material").file(dados)
                .file(new MockMultipartFile("arquivo", "resumo.pdf", "application/pdf", UploadFixtures.pdf()))
                .header("Authorization", bearer(autor)))
            .andExpect(status().isCreated()).andExpect(jsonPath("statusModeracao").value("PENDENTE"));
        var m = materiais.findByAutorEmailOrderByPublicadoEmDescIdDesc(autor.getEmail(), org.springframework.data.domain.PageRequest.of(0, 12)).getContent().getFirst();
        arquivos.add(armazenamento.permitido(m.getCaminhoArquivo()));
        return m.getId();
    }

    @AfterEach void limpar() throws Exception {
        SecurityContextHolder.clearContext();
        for (Path arquivo : arquivos) Files.deleteIfExists(arquivo);
    }

    @Test void totaisDeInteracoesSaoPorMaterialEIncluidosNasListagens() throws Exception {
        UUID id = enviar(), vazio = enviar();
        jdbc.update("UPDATE materiais SET status_moderacao = 'APROVADO' WHERE id IN (?, ?)", id, vazio);
        jdbc.update("INSERT INTO curtidas_material(material_id,usuario_id,tipo) VALUES (?,?,'CURTIDA')", id, autor.getId());
        jdbc.update("INSERT INTO curtidas_material(material_id,usuario_id,tipo) VALUES (?,?,'DESLIKE')", id, colega.getId());
        jdbc.update("INSERT INTO favoritos(usuario_id,material_id) VALUES (?,?)", autor.getId(), id);
        jdbc.update("INSERT INTO favoritos(usuario_id,material_id) VALUES (?,?)", colega.getId(), id);
        mvc.perform(get("/material/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("totalCurtidas").value(1)).andExpect(jsonPath("totalDeslikes").value(1))
            .andExpect(jsonPath("totalFavoritos").value(2));
        mvc.perform(get("/material/" + vazio)).andExpect(status().isOk())
            .andExpect(jsonPath("totalCurtidas").value(0)).andExpect(jsonPath("totalDeslikes").value(0))
            .andExpect(jsonPath("totalFavoritos").value(0));
        for (String caminho : List.of("/material?size=500", "/favoritos", "/perfil/publicacoes")) {
            mvc.perform(get(caminho).header("Authorization", bearer(autor))).andExpect(status().isOk())
                .andExpect(jsonPath("content[?(@.id == '" + id + "')].totalCurtidas").value(org.hamcrest.Matchers.contains(1)))
                .andExpect(jsonPath("content[?(@.id == '" + id + "')].totalDeslikes").value(org.hamcrest.Matchers.contains(1)))
                .andExpect(jsonPath("content[?(@.id == '" + id + "')].totalFavoritos").value(org.hamcrest.Matchers.contains(2)));
        }
        jdbc.update("DELETE FROM favoritos WHERE usuario_id = ? AND material_id = ?", colega.getId(), id);
        jdbc.update("UPDATE curtidas_material SET tipo = 'CURTIDA' WHERE usuario_id = ? AND material_id = ?", colega.getId(), id);
        mvc.perform(get("/material/" + id)).andExpect(jsonPath("totalCurtidas").value(2))
            .andExpect(jsonPath("totalDeslikes").value(0)).andExpect(jsonPath("totalFavoritos").value(1));
    }
    @Test void envioPendenteOcultoComEmailUnicoEArquivoRestrito() throws Exception {
        UUID id = enviar();
        assertEquals(StatusModeracao.PENDENTE, materiais.findById(id).orElseThrow().getStatusModeracao());
        mvc.perform(get("/material?size=500")).andExpect(status().isOk())
            .andExpect(jsonPath("content[?(@.id == '" + id + "')]").isEmpty());
        for (String path : List.of("/material/" + id, "/material/" + id + "/arquivo", "/material/" + id + "/arquivo?download=true",
                "/material/" + id + "/comentarios")) {
            mvc.perform(get(path)).andExpect(status().isNotFound());
            mvc.perform(get(path).header("Authorization", bearer(colega))).andExpect(status().isNotFound());
        }
        mvc.perform(get("/material/" + id + "/arquivo").header("Authorization", bearer(autor)))
            .andExpect(status().isOk()).andExpect(content().bytes(UploadFixtures.pdf()))
            .andExpect(header().string("Cache-Control", "private, no-store"));
        mvc.perform(get("/material/" + id + "/arquivo").header("Authorization", bearer(admin))).andExpect(status().isOk());
        mvc.perform(get("/perfil/publicacoes").header("Authorization", bearer(autor)))
            .andExpect(jsonPath("content[0].statusModeracao").value("PENDENTE"));
        mvc.perform(get("/admin/materiais/pendentes").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("content").isArray());
        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(1)).send(captor.capture());
        var mensagem = captor.getValue();
        assertArrayEquals(new String[]{"admin@example.test"}, mensagem.getTo());
        assertEquals("Banco Acadêmico — Novo material aguardando aprovação", mensagem.getSubject());
        for (String trecho : List.of("Resumo de teste", "Algoritmos", autor.getNome(), "Data de envio:")) assertTrue(mensagem.getText().contains(trecho));
        assertFalse(mensagem.getText().contains(bearer(autor)));
    }

    @Test void aprovarRegistraAuditoriaLiberaArquivoEImpedeNovaDecisao() throws Exception {
        UUID id = enviar();
        mvc.perform(patch("/admin/materiais/" + id + "/aprovar").header("Authorization", bearer(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("statusModeracao").value("APROVADO"))
            .andExpect(jsonPath("decisaoEm").exists()).andExpect(jsonPath("moderadorId").value(admin.getId().toString()));
        mvc.perform(get("/material?size=500")).andExpect(jsonPath("content[?(@.id == '" + id + "')]").isNotEmpty());
        mvc.perform(get("/material/" + id + "/arquivo")).andExpect(status().isOk());
        mvc.perform(patch("/admin/materiais/" + id + "/aprovar").header("Authorization", bearer(admin))).andExpect(status().isConflict());
        mvc.perform(patch("/admin/materiais/" + id + "/rejeitar").header("Authorization", bearer(admin))
            .contentType("application/json").content("{\"justificativa\":\"Outra decisão\"}")).andExpect(status().isConflict());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test void rejeicaoExigeMotivoMantemArquivoEOcultaDosColegas() throws Exception {
        UUID id = enviar(); String path = "/admin/materiais/" + id + "/rejeitar";
        for (String motivo : List.of("", "   ", "x".repeat(2001))) mvc.perform(patch(path).header("Authorization", bearer(admin))
            .contentType("application/json").content("{\"justificativa\":\"" + motivo + "\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch(path).header("Authorization", bearer(admin)).contentType("application/json")
            .content("{\"justificativa\":\"  Autorização não identificada.  \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("statusModeracao").value("REJEITADO"));
        var material = materiais.findById(id).orElseThrow();
        assertNotNull(material.getDecisaoEm()); assertEquals(admin.getId(), material.getModerador().getId());
        assertTrue(Files.exists(armazenamento.permitido(material.getCaminhoArquivo())));
        mvc.perform(get("/material/" + id + "/arquivo").header("Authorization", bearer(colega))).andExpect(status().isNotFound());
        mvc.perform(get("/material?size=500")).andExpect(jsonPath("content[?(@.id == '" + id + "')]").isEmpty());
        mvc.perform(get("/perfil/publicacoes").header("Authorization", bearer(autor)))
            .andExpect(jsonPath("content[0].justificativaRejeicao").value("Autorização não identificada."));
        mvc.perform(patch(path).header("Authorization", bearer(admin)).contentType("application/json")
            .content("{\"justificativa\":\"Repetição\"}")).andExpect(status().isConflict());
    }

    @Test void usuarioComumNaoModeraNemAlteraStatusPeloPut() throws Exception {
        UUID id = enviar();
        mvc.perform(get("/admin/materiais/pendentes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/materiais/pendentes").header("Authorization", bearer(autor))).andExpect(status().isForbidden());
        for (String acao : List.of("aprovar", "rejeitar")) mvc.perform(patch("/admin/materiais/" + id + "/" + acao)
            .header("Authorization", bearer(autor)).contentType("application/json").content("{\"justificativa\":\"Tentativa\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/material/" + id).header("Authorization", bearer(autor)).contentType("application/json")
            .content("{\"statusModeracao\":\"APROVADO\"}")).andExpect(status().isForbidden());
        assertEquals(StatusModeracao.PENDENTE, materiais.findById(id).orElseThrow().getStatusModeracao());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"autenticacao", "timeout", "conexao", "preparacao", "rejeicao"})
    void falhaSmtpNaoReverteMaterialNemRepeteEnvioEmConsultas(String falha) throws Exception {
        doThrow(EmailFalhasTests.falha(falha)).when(mail).send(any(SimpleMailMessage.class));
        UUID id = enviar();
        assertTrue(materiais.existsById(id));
        mvc.perform(get("/material/" + id).header("Authorization", bearer(autor))).andExpect(status().isOk());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test void rollbackNaoEnviaEmailELimpaArquivo() {
        var caminho = new java.util.concurrent.atomic.AtomicReference<Path>();
        var id = new java.util.concurrent.atomic.AtomicReference<UUID>();
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
            var m = service.criar(UploadFixtures.dados("Rollback", "Descrição", disciplina, br.wm.banco.academico.Enum.TipoPublicacao.RESUMO, null),
                new MockMultipartFile("arquivo", "teste.pdf", "application/pdf", UploadFixtures.pdf()), autor.getEmail());
            caminho.set(armazenamento.permitido(m.getCaminhoArquivo())); id.set(m.getId());
            verifyNoInteractions(mail);
            throw new IllegalStateException("Reverter teste");
        }));
        assertFalse(materiais.existsById(id.get())); assertFalse(Files.exists(caminho.get()));
        verifyNoInteractions(mail);
    }

    @Test void decisoesConcorrentesApenasUmaVence() throws Exception {
        UUID id = enviar(); var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> decidir = () -> {
                inicio.await(); autenticar(admin);
                try { service.decidir(id, true, null); return 200; }
                catch (org.springframework.web.server.ResponseStatusException ex) { return ex.getStatusCode().value(); }
                finally { SecurityContextHolder.clearContext(); }
            };
            var a = pool.submit(decidir); var b = pool.submit(decidir); inicio.countDown();
            var resultados = new ArrayList<>(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)));
            Collections.sort(resultados); assertEquals(List.of(200, 409), resultados);
        }
    }

    @Test void tiposDePublicacaoPersistemSeparadosDoMimeEReferenciaDoLivro() throws Exception {
        for (var tipo : br.wm.banco.academico.Enum.TipoPublicacao.values()) {
            String referencia = tipo == br.wm.banco.academico.Enum.TipoPublicacao.LIVRO
                    || tipo == br.wm.banco.academico.Enum.TipoPublicacao.ARTIGO ? "Autor. Publicação. 2026." : null;
            UUID id = enviar(tipo.name(), referencia);
            var salvo = materiais.findById(id).orElseThrow();
            assertEquals(tipo, salvo.getTipoPublicacao());
            assertEquals("application/pdf", salvo.getTipoArquivo());
            assertEquals(referencia, salvo.getReferenciaBibliografica());
            assertEquals(StatusModeracao.PENDENTE, salvo.getStatusModeracao());
            mvc.perform(get("/material/" + id).header("Authorization", bearer(autor)))
                .andExpect(status().isOk()).andExpect(jsonPath("tipoPublicacao").value(tipo.name()));
            if (referencia != null) mvc.perform(get("/material/" + id).header("Authorization", bearer(autor)))
                .andExpect(jsonPath("referenciaBibliografica").value(referencia));
        }
        UUID semReferencia = enviar("LIVRO", null);
        assertNull(materiais.findById(semReferencia).orElseThrow().getReferenciaBibliografica());
    }

    @Test void validaTipoObrigatorioEReferenciaSomenteParaLivroAntesDoUpload() throws Exception {
        long antes = materiais.count();
        for (String tipo : new String[]{null, "INVALIDO"}) mvc.perform(multipart("/material")
            .file(dadosPublicacao(tipo, null)).file(new MockMultipartFile("arquivo", "teste.pdf", "application/pdf", UploadFixtures.pdf()))
            .header("Authorization", bearer(autor))).andExpect(status().isBadRequest());
        for (var tipo : br.wm.banco.academico.Enum.TipoPublicacao.values()) {
            if (tipo == br.wm.banco.academico.Enum.TipoPublicacao.LIVRO
                    || tipo == br.wm.banco.academico.Enum.TipoPublicacao.ARTIGO) continue;
            mvc.perform(multipart("/material").file(dadosPublicacao(tipo.name(), "Referência indevida"))
                .file(new MockMultipartFile("arquivo", "teste.pdf", "application/pdf", UploadFixtures.pdf()))
                .header("Authorization", bearer(autor))).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/material").file(dadosPublicacao("LIVRO", "x".repeat(501)))
            .file(new MockMultipartFile("arquivo", "teste.pdf", "application/pdf", UploadFixtures.pdf()))
            .header("Authorization", bearer(autor))).andExpect(status().isBadRequest());
        assertEquals(antes, materiais.count()); verifyNoInteractions(mail);
    }

    @Test void materialLegadoSemTipoContinuaAcessivel() throws Exception {
        UUID id = enviar();
        jdbc.update("UPDATE materiais SET tipo_publicacao = NULL, status_moderacao = 'APROVADO' WHERE id = ?", id);
        mvc.perform(get("/material/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("tipoPublicacao").isEmpty()).andExpect(jsonPath("tipoArquivo").value("application/pdf"));
        mvc.perform(get("/material/" + id + "/arquivo")).andExpect(status().isOk());
    }

    @Test void rejeitaArquivoDisfarcadoOuAusenteSemCriarPublicacao() throws Exception {
        long antes = materiais.count();
        mvc.perform(multipart("/material").file(dadosPublicacao("RESUMO", null))
            .file(new MockMultipartFile("arquivo", "falso.pdf", "application/pdf", "<html>inválido</html>".getBytes()))
            .header("Authorization", bearer(autor))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/material").file(dadosPublicacao("RESUMO", null))
            .header("Authorization", bearer(autor))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("mensagem").exists());
        assertEquals(antes, materiais.count()); verifyNoInteractions(mail);
    }

    @Test void atualizacaoValidaReferenciaELimpaReferenciaAoTrocarTipoSemAlterarModeracao() throws Exception {
        UUID id = enviar("LIVRO", "Referência do livro");
        String base = "{\"titulo\":\"Resumo atualizado\",\"descricao\":\"Descrição atualizada\",\"disciplinaId\":\"" + disciplina + "\",\"tipoPublicacao\":\"SLIDE\"";
        mvc.perform(put("/material/" + id).header("Authorization", bearer(admin)).contentType("application/json")
            .content(base + ",\"referenciaBibliografica\":\"Referência indevida\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/material/" + id).header("Authorization", bearer(admin)).contentType("application/json")
            .content(base + "}")).andExpect(status().isOk()).andExpect(jsonPath("tipoPublicacao").value("SLIDE"))
            .andExpect(jsonPath("referenciaBibliografica").isEmpty()).andExpect(jsonPath("statusModeracao").value("PENDENTE"));
        assertNull(materiais.findById(id).orElseThrow().getReferenciaBibliografica());
    }

    @Test void artigoExigeReferenciaNaCriacaoSemPersistirTentativasInvalidas() throws Exception {
        long antes = materiais.count();
        for (String referencia : new String[]{null, "", "   "}) {
            mvc.perform(multipart("/material").file(dadosPublicacao("ARTIGO", referencia))
                .file(new MockMultipartFile("arquivo", "artigo.pdf", "application/pdf", UploadFixtures.pdf()))
                .header("Authorization", bearer(autor))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("mensagem").value("Informe a referência bibliográfica do artigo."));
        }
        assertEquals(antes, materiais.count()); verifyNoInteractions(mail);
        UUID id = enviar("ARTIGO", " Autor. Artigo. 2026. ");
        assertEquals("Autor. Artigo. 2026.", materiais.findById(id).orElseThrow().getReferenciaBibliografica());
    }

    @Test void atualizacaoExigeReferenciaParaArtigoEMantemLivroOpcional() throws Exception {
        UUID id = enviar("LIVRO", null);
        String base = "{\"titulo\":\"Título atualizado\",\"descricao\":\"Descrição atualizada\",\"disciplinaId\":\"" + disciplina + "\",\"tipoPublicacao\":\"ARTIGO\"";
        for (String extra : new String[]{"", ",\"referenciaBibliografica\":null", ",\"referenciaBibliografica\":\"\"", ",\"referenciaBibliografica\":\"   \""}) {
            mvc.perform(put("/material/" + id).header("Authorization", bearer(admin))
                .contentType("application/json").content(base + extra + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("mensagem").value("Informe a referência bibliográfica do artigo."));
        }
        assertEquals(br.wm.banco.academico.Enum.TipoPublicacao.LIVRO, materiais.findById(id).orElseThrow().getTipoPublicacao());
        mvc.perform(put("/material/" + id).header("Authorization", bearer(admin)).contentType("application/json")
            .content(base + ",\"referenciaBibliografica\":\" Autor. Artigo. 2026. \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("referenciaBibliografica").value("Autor. Artigo. 2026."))
            .andExpect(jsonPath("statusModeracao").value("PENDENTE"));
        mvc.perform(put("/material/" + id).header("Authorization", bearer(admin)).contentType("application/json")
            .content(base.replace("ARTIGO", "LIVRO") + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("referenciaBibliografica").isEmpty());
    }

    @Test void artigoLegadoSemReferenciaContinuaLegivelESoExigeCorrecaoAoAtualizar() throws Exception {
        UUID id = enviar("ARTIGO", "Autor. Artigo. 2026.");
        jdbc.update("UPDATE materiais SET referencia_bibliografica = NULL, status_moderacao = 'APROVADO' WHERE id = ?", id);
        mvc.perform(get("/material/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("tipoPublicacao").value("ARTIGO")).andExpect(jsonPath("referenciaBibliografica").isEmpty());
        mvc.perform(get("/material/" + id + "/arquivo")).andExpect(status().isOk());
        String base = "{\"titulo\":\"Artigo legado\",\"descricao\":\"Descrição atualizada\",\"disciplinaId\":\"" + disciplina + "\",\"tipoPublicacao\":\"ARTIGO\"";
        mvc.perform(put("/material/" + id).header("Authorization", bearer(admin)).contentType("application/json").content(base + "}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/material/" + id).header("Authorization", bearer(admin)).contentType("application/json")
            .content(base + ",\"referenciaBibliografica\":\"Autor. Artigo. 2026.\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("statusModeracao").value("APROVADO"));
    }

    @Test void autorExcluiPostagemArquivoEInteracoesEmQualquerStatusMasColegaNao() throws Exception {
        for (var status : StatusModeracao.values()) {
            UUID id = enviar();
            jdbc.update("UPDATE materiais SET status_moderacao = ? WHERE id = ?", status.name(), id);
            Path arquivo = armazenamento.permitido(materiais.findById(id).orElseThrow().getCaminhoArquivo());
            UUID comentario = UUID.randomUUID();
            var agora = java.sql.Timestamp.from(java.time.Instant.now());
            jdbc.update("INSERT INTO comentarios_material(id,material_id,usuario_id,texto,criado_em) VALUES (?,?,?,?,?)", comentario, id, colega.getId(), "Comentário", agora);
            jdbc.update("INSERT INTO respostas_comentario(id,comentario_id,usuario_id,texto,criado_em) VALUES (?,?,?,?,?)", UUID.randomUUID(), comentario, autor.getId(), "Resposta", agora);
            jdbc.update("INSERT INTO curtidas_material(material_id,usuario_id) VALUES (?,?)", id, colega.getId());
            mvc.perform(delete("/material/" + id)).andExpect(status().isUnauthorized());
            mvc.perform(delete("/material/" + id).header("Authorization", bearer(colega))).andExpect(status().isForbidden());
            assertTrue(Files.exists(arquivo)); assertTrue(materiais.existsById(id));
            mvc.perform(delete("/material/" + id).header("Authorization", bearer(autor))).andExpect(status().isNoContent());
            assertFalse(materiais.existsById(id)); assertFalse(Files.exists(arquivo));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comentarios_material WHERE material_id = ?", Integer.class, id));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM respostas_comentario WHERE comentario_id = ?", Integer.class, comentario));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM curtidas_material WHERE material_id = ?", Integer.class, id));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM arquivos_para_excluir WHERE material_id = ?", Integer.class, id));
            mvc.perform(get("/material/" + id + "/arquivo").header("Authorization", bearer(autor))).andExpect(status().isNotFound());
            mvc.perform(delete("/material/" + id).header("Authorization", bearer(autor))).andExpect(status().isNotFound());
        }
    }

    @Test void adminExcluiMaterialSemAutorMesmoSeArquivoJaNaoExiste() throws Exception {
        UUID id = enviar(); var arquivo = armazenamento.permitido(materiais.findById(id).orElseThrow().getCaminhoArquivo());
        jdbc.update("UPDATE materiais SET autor_id = NULL WHERE id = ?", id);
        Files.delete(arquivo);
        mvc.perform(delete("/material/" + id).header("Authorization", bearer(admin))).andExpect(status().isNoContent());
        assertFalse(materiais.existsById(id));
    }

    @Test void rollbackDaExclusaoPreservaPublicacaoEArquivo() throws Exception {
        UUID id = enviar(); Path arquivo = armazenamento.permitido(materiais.findById(id).orElseThrow().getCaminhoArquivo());
        autenticar(autor);
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
            service.excluir(id);
            assertTrue(Files.exists(arquivo));
            throw new IllegalStateException("Rollback de teste");
        }));
        assertTrue(materiais.existsById(id)); assertTrue(Files.exists(arquivo));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM arquivos_para_excluir WHERE material_id = ?", Integer.class, id));
    }

    @Test void falhaNoDiscoFicaNaFilaEPermiteNovaTentativa() throws Exception {
        UUID id = enviar(); Path arquivo = armazenamento.permitido(materiais.findById(id).orElseThrow().getCaminhoArquivo());
        Files.delete(arquivo); Files.createDirectory(arquivo);
        Path bloqueio = Files.writeString(arquivo.resolve("bloqueio-teste"), "teste");
        try {
            mvc.perform(delete("/material/" + id).header("Authorization", bearer(autor))).andExpect(status().isNoContent());
            assertFalse(materiais.existsById(id)); assertTrue(Files.exists(arquivo));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM arquivos_para_excluir WHERE material_id = ?", Integer.class, id));
        } finally { Files.deleteIfExists(bloqueio); }
        limpeza.tentarLimpar(id);
        assertFalse(Files.exists(arquivo));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM arquivos_para_excluir WHERE material_id = ?", Integer.class, id));
        limpeza.tentarLimpar(id); // Retentativa após conclusão é inofensiva.
    }

    @Test void naoRemoveArquivoForaDoArmazenamento() throws Exception {
        UUID id = enviar();
        jdbc.update("UPDATE materiais SET caminho_arquivo = '../pom.xml' WHERE id = ?", id);
        mvc.perform(delete("/material/" + id).header("Authorization", bearer(autor))).andExpect(status().isNotFound());
        assertTrue(Files.exists(Path.of("pom.xml"))); assertTrue(materiais.existsById(id));
    }

    @Test void deslikeRespeitaModeracaoEReacoesConcorrentesNaoDuplicam() throws Exception {
        UUID id = enviar();
        String path = "/material/" + id + "/deslike";
        mvc.perform(get(path).header("Authorization", bearer(colega))).andExpect(status().isNotFound());
        mvc.perform(put(path).header("Authorization", bearer(colega))).andExpect(status().isNotFound());
        mvc.perform(delete(path).header("Authorization", bearer(colega))).andExpect(status().isNotFound());
        mvc.perform(patch("/admin/materiais/" + id + "/aprovar").header("Authorization", bearer(admin))).andExpect(status().isOk());
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Void> positiva = () -> { inicio.await(); mvc.perform(put("/material/" + id + "/curtida")
                .header("Authorization", bearer(colega))).andExpect(status().isOk()); return null; };
            Callable<Void> negativa = () -> { inicio.await(); mvc.perform(put(path)
                .header("Authorization", bearer(colega))).andExpect(status().isOk()); return null; };
            var a = pool.submit(positiva); var b = pool.submit(negativa); inicio.countDown();
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM curtidas_material WHERE material_id = ?", Integer.class, id));
        mvc.perform(put(path).header("Authorization", bearer(autor))).andExpect(jsonPath("deslike").value(true));
        mvc.perform(delete(path).header("Authorization", bearer(colega))).andExpect(jsonPath("totalDeslikes").value(1));
        mvc.perform(get(path).header("Authorization", bearer(autor))).andExpect(jsonPath("deslike").value(true));
        mvc.perform(delete("/material/" + id).header("Authorization", bearer(autor))).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM curtidas_material WHERE material_id = ?", Integer.class, id));
    }
}
