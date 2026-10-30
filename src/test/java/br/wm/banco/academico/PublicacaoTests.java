package br.wm.banco.academico;

import br.wm.banco.academico.Config.SecurityConfig;
import br.wm.banco.academico.Controller.ComentarioMaterialController;
import br.wm.banco.academico.Controller.MaterialController;
import br.wm.banco.academico.Controller.PerfilMaterialController;
import br.wm.banco.academico.Controller.CurtidaMaterialController;
import br.wm.banco.academico.Controller.RespostaComentarioController;
import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Exception.GlobalExpcetionHandler;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.*;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.*;
import br.wm.banco.academico.Service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(PublicacaoTests.Config.class)
@WebAppConfiguration
class PublicacaoTests {
    @Configuration
    @EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    @Import({SecurityConfig.class, ComentarioMaterialController.class, MaterialController.class, PerfilMaterialController.class,
            CurtidaMaterialController.class, RespostaComentarioController.class, GlobalExpcetionHandler.class})
    static class Config {
        @Bean UsuarioRepository usuarios() { return mock(UsuarioRepository.class); }
        @Bean MaterialService materiais() { return mock(MaterialService.class); }
        @Bean TokenService tokens() { return new TokenService(AuthenticationTests.SECRET); }
        @Bean JdbcTemplate jdbc() throws Exception {
            var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                    "jdbc:h2:mem:publicacoes;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
            jdbc.execute("CREATE TABLE usuarios(id UUID PRIMARY KEY, nome VARCHAR(200))");
            jdbc.execute("CREATE TABLE materiais(id UUID PRIMARY KEY, titulo VARCHAR(255))");
            for (String migration : List.of("V11__publicacoes_e_comentarios.sql", "V12__curtidas_e_respostas.sql", "V13__conversa_entre_respostas.sql")) {
            try (var stream = PublicacaoTests.class.getResourceAsStream("/db/migration/" + migration)) {
                for (String sql : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";"))
                    if (!sql.isBlank()) jdbc.execute(sql);
            }
            }
            return jdbc;
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired MaterialService materiais;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenService tokens;
    MockMvc mvc;
    UsuarioModel usuario;
    MaterialModel material;
    UUID id;
    String bearer;

    @BeforeEach void preparar() {
        reset(materiais, usuarios);
        jdbc.update("DELETE FROM comentarios_material");
        jdbc.update("DELETE FROM materiais");
        jdbc.update("DELETE FROM usuarios");
        usuario = new UsuarioModel();
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.setNome("Maria Silva"); usuario.setEmail("maria@unemat.br"); usuario.setPerfil(PerfilUsuario.USER);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        jdbc.update("INSERT INTO usuarios(id, nome) VALUES (?, ?)", usuario.getId(), usuario.getNome());
        id = UUID.randomUUID();
        jdbc.update("INSERT INTO materiais(id, titulo) VALUES (?, ?)", id, "Resumo antigo");
        material = new MaterialModel(); material.setId(id); material.setTitulo("Resumo");
        material.setNomeArquivo("resumo.pdf"); material.setTipoArquivo("application/pdf");
        var disciplina = new DisciplinasModel(); ReflectionTestUtils.setField(disciplina, "id", UUID.randomUUID()); material.setDisciplina(disciplina);
        when(materiais.buscarPorId(id)).thenReturn(material);
        bearer = "Bearer " + tokens.gerarToken(usuario);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void comentariosExigemLoginValidamTextoEPersistemAutorAutenticado() throws Exception {
        String path = "/material/" + id + "/comentarios";
        mvc.perform(post(path).contentType("application/json").content("{\"texto\":\"Olá\"}"))
                .andExpect(status().isUnauthorized());
        for (String texto : new String[]{"   ", "x".repeat(2001)})
            mvc.perform(post(path).header("Authorization", bearer).contentType("application/json")
                    .content("{\"texto\":\"" + texto + "\"}" )).andExpect(status().isBadRequest());
        mvc.perform(post(path).header("Authorization", bearer).contentType("application/json")
                .content("{\"texto\":\"  Muito útil!  \",\"autorNome\":\"Impostor\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("autorNome").value("Maria Silva"))
                .andExpect(jsonPath("texto").value("Muito útil!"));
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$[0].texto").value("Muito útil!"));
        assertEquals(usuario.getId(), jdbc.queryForObject("SELECT usuario_id FROM comentarios_material", UUID.class));
    }

    @Test void curtidasPersistemPorContaSemDuplicacaoEPermitemDescurtir() throws Exception {
        String path = "/material/" + id + "/curtida";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(put(path)).andExpect(status().isUnauthorized());
        mvc.perform(delete(path)).andExpect(status().isUnauthorized());
        for (int i = 0; i < 2; i++) mvc.perform(put(path).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("total").value(1)).andExpect(jsonPath("curtido").value(true));
        var outro = new UsuarioModel();
        ReflectionTestUtils.setField(outro, "id", UUID.randomUUID());
        outro.setNome("Outro usuário"); outro.setEmail("outro@unemat.br"); outro.setPerfil(PerfilUsuario.ADMIN);
        jdbc.update("INSERT INTO usuarios(id, nome) VALUES (?, ?)", outro.getId(), outro.getNome());
        when(usuarios.findByEmail(outro.getEmail())).thenReturn(Optional.of(outro));
        String outroBearer = "Bearer " + tokens.gerarToken(outro);
        mvc.perform(get(path).header("Authorization", outroBearer)).andExpect(jsonPath("curtido").value(false));
        mvc.perform(put(path).header("Authorization", outroBearer)).andExpect(jsonPath("total").value(2));
        mvc.perform(delete(path).header("Authorization", bearer)).andExpect(jsonPath("total").value(1)).andExpect(jsonPath("curtido").value(false));
        mvc.perform(delete(path).header("Authorization", bearer)).andExpect(jsonPath("total").value(1));
        mvc.perform(get(path).header("Authorization", outroBearer)).andExpect(jsonPath("curtido").value(true));
        jdbc.update("DELETE FROM materiais WHERE id = ?", id);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM curtidas_material", Integer.class));
    }

    @Test void respostasValidamContaTextoEVinculoComPublicacao() throws Exception {
        UUID comentario = UUID.randomUUID();
        UUID autorPergunta = UUID.randomUUID();
        jdbc.update("INSERT INTO usuarios(id, nome) VALUES (?, ?)", autorPergunta, usuario.getNome());
        jdbc.update("INSERT INTO comentarios_material VALUES (?, ?, ?, ?, ?)", comentario, id, autorPergunta,
                "Dúvida", java.sql.Timestamp.from(java.time.Instant.now()));
        String path = "/material/" + id + "/comentarios/" + comentario + "/respostas";
        mvc.perform(post(path).contentType("application/json").content("{\"texto\":\"Resposta\"}"))
                .andExpect(status().isUnauthorized());
        for (String texto : List.of("   ", "x".repeat(2001))) mvc.perform(post(path).header("Authorization", bearer)
                .contentType("application/json").content("{\"texto\":\"" + texto + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path).header("Authorization", bearer).contentType("application/json")
                .content("{\"texto\":\"  Posso ajudar!  \",\"autorNome\":\"Impostor\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("autorNome").value(usuario.getNome()))
                .andExpect(jsonPath("texto").value("Posso ajudar!"));
        mvc.perform(get(path)).andExpect(jsonPath("$[0].texto").value("Posso ajudar!"));
        mvc.perform(get("/material/" + id + "/comentarios")).andExpect(jsonPath("$[0].totalRespostas").value(1));
        UUID outroMaterial = UUID.randomUUID();
        when(materiais.buscarPorId(outroMaterial)).thenReturn(material);
        String caminhoErrado = "/material/" + outroMaterial + "/comentarios/" + comentario + "/respostas";
        mvc.perform(get(caminhoErrado)).andExpect(status().isNotFound());
        mvc.perform(post(caminhoErrado).header("Authorization", bearer).contentType("application/json")
                .content("{\"texto\":\"Resposta\"}")).andExpect(status().isNotFound());
        mvc.perform(get(path + "?pagina=-1")).andExpect(status().isBadRequest());
        jdbc.update("DELETE FROM comentarios_material WHERE id = ?", comentario);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM respostas_comentario", Integer.class));
    }

    @Test void respostasAparecemDaMaisAntigaParaMaisRecenteMesmoComPaginacao() throws Exception {
        UUID pergunta = UUID.randomUUID();
        var inicio = java.time.Instant.parse("2026-01-01T10:00:00Z");
        jdbc.update("INSERT INTO comentarios_material VALUES (?, ?, ?, ?, ?)", pergunta, id, usuario.getId(),
                "Pergunta", java.sql.Timestamp.from(inicio));
        for (int i = 50; i >= 0; i--) {
            jdbc.update("INSERT INTO respostas_comentario(id, comentario_id, usuario_id, texto, criado_em) VALUES (?, ?, ?, ?, ?)",
                    UUID.randomUUID(), pergunta, usuario.getId(), "Resposta " + i, java.sql.Timestamp.from(inicio.plusSeconds(i)));
        }
        String path = "/material/" + id + "/comentarios/" + pergunta + "/respostas";
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(50))
                .andExpect(jsonPath("$[0].texto").value("Resposta 0"))
                .andExpect(jsonPath("$[49].texto").value("Resposta 49"));
        mvc.perform(get(path + "?pagina=1")).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].texto").value("Resposta 50"));
    }

    @Test void usuarioEAdminNaoPodemResponderAoProprioComentario() throws Exception {
        UUID comentario = UUID.randomUUID();
        jdbc.update("INSERT INTO comentarios_material VALUES (?, ?, ?, ?, ?)", comentario, id, usuario.getId(),
                "Minha pergunta", java.sql.Timestamp.from(java.time.Instant.now()));
        mvc.perform(get("/material/" + id + "/comentarios"))
                .andExpect(jsonPath("$[0].autorId").value(usuario.getId().toString()));
        for (var perfil : new PerfilUsuario[]{PerfilUsuario.USER, PerfilUsuario.ADMIN}) {
            usuario.setPerfil(perfil);
            mvc.perform(post("/material/" + id + "/comentarios/" + comentario + "/respostas")
                    .header("Authorization", "Bearer " + tokens.gerarToken(usuario)).contentType("application/json")
                    .content("{\"texto\":\"Minha resposta\",\"autorId\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("mensagem").value("Você não pode responder ao próprio comentário."));
        }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM respostas_comentario", Integer.class));
    }

    @Test void autorDaPerguntaPodeResponderOutraPessoaNaConversaMasNaoASiMesmo() throws Exception {
        UUID pergunta = UUID.randomUUID(), outro = UUID.randomUUID(), respostaOutro = UUID.randomUUID();
        var agora = java.sql.Timestamp.from(java.time.Instant.now());
        jdbc.update("INSERT INTO usuarios(id, nome) VALUES (?, ?)", outro, "Colega");
        jdbc.update("INSERT INTO comentarios_material VALUES (?, ?, ?, ?, ?)", pergunta, id, usuario.getId(), "Minha pergunta", agora);
        jdbc.update("INSERT INTO respostas_comentario(id, comentario_id, usuario_id, texto, criado_em) VALUES (?, ?, ?, ?, ?)",
                respostaOutro, pergunta, outro, "Resposta do colega", agora);
        String path = "/material/" + id + "/comentarios/" + pergunta + "/respostas";
        for (var perfil : new PerfilUsuario[]{PerfilUsuario.USER, PerfilUsuario.ADMIN}) {
            usuario.setPerfil(perfil);
            String token = "Bearer " + tokens.gerarToken(usuario);
            mvc.perform(post(path).header("Authorization", token).contentType("application/json")
                    .content("{\"texto\":\"Obrigado, colega!\",\"respostaPaiId\":\"" + respostaOutro + "\"}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("respondendoA").value("Colega"))
                    .andExpect(jsonPath("autorId").value(usuario.getId().toString()))
                    .andExpect(jsonPath("respostaPaiId").value(respostaOutro.toString()));
            UUID propriaResposta = jdbc.queryForList("SELECT id FROM respostas_comentario WHERE usuario_id = ?", UUID.class, usuario.getId()).getFirst();
            mvc.perform(post(path).header("Authorization", token).contentType("application/json")
                    .content("{\"texto\":\"Respondendo a mim\",\"respostaPaiId\":\"" + propriaResposta + "\"}"))
                    .andExpect(status().isForbidden());
        }
        UUID outraPergunta = UUID.randomUUID(), respostaFora = UUID.randomUUID();
        jdbc.update("INSERT INTO comentarios_material VALUES (?, ?, ?, ?, ?)", outraPergunta, id, outro, "Outra pergunta", agora);
        jdbc.update("INSERT INTO respostas_comentario(id, comentario_id, usuario_id, texto, criado_em) VALUES (?, ?, ?, ?, ?)",
                respostaFora, outraPergunta, outro, "Outra conversa", agora);
        mvc.perform(post(path).header("Authorization", "Bearer " + tokens.gerarToken(usuario)).contentType("application/json")
                .content("{\"texto\":\"Conversa errada\",\"respostaPaiId\":\"" + respostaFora + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(path)).andExpect(jsonPath("$.length()").value(3));
        jdbc.update("DELETE FROM comentarios_material WHERE id = ?", pergunta);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM respostas_comentario WHERE comentario_id = ?", Integer.class, pergunta));
    }

    @Test void publicacoesDoPerfilUsamContaAutenticadaParaUserEAdmin() throws Exception {
        mvc.perform(get("/perfil/publicacoes")).andExpect(status().isUnauthorized());
        verify(materiais, never()).minhasPublicacoes(anyString(), anyInt());
        when(materiais.minhasPublicacoes(usuario.getEmail(), 1)).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(new br.wm.banco.academico.DTOs.Response.MaterialResponseDTO(material)),
                        org.springframework.data.domain.PageRequest.of(1, 12), 13));
        for (var perfil : new PerfilUsuario[]{PerfilUsuario.USER, PerfilUsuario.ADMIN}) {
            usuario.setPerfil(perfil);
            mvc.perform(get("/perfil/publicacoes?pagina=1&email=outra@unemat.br")
                            .header("Authorization", "Bearer " + tokens.gerarToken(usuario)))
                    .andExpect(status().isOk()).andExpect(jsonPath("content[0].id").value(id.toString()));
        }
        verify(materiais, times(2)).minhasPublicacoes(usuario.getEmail(), 1);
        verify(materiais, never()).minhasPublicacoes(eq("outra@unemat.br"), anyInt());
    }

    @Test void consultaDoPerfilFiltraPorAutorEPaginaSemAlterarMateriais() {
        var repo = mock(MaterialRepository.class);
        var service = new MaterialService(repo, mock(DisciplinasService.class), usuarios);
        var pagina = org.springframework.data.domain.PageRequest.of(2, 12);
        when(repo.findByAutorEmailOrderByPublicadoEmDescIdDesc(usuario.getEmail(), pagina))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(material)));
        assertEquals(id, service.minhasPublicacoes(usuario.getEmail(), 2).getContent().getFirst().getId());
        verify(repo).findByAutorEmailOrderByPublicadoEmDescIdDesc(usuario.getEmail(), pagina);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.minhasPublicacoes(usuario.getEmail(), -1));
        verifyNoMoreInteractions(repo);
    }

    @Test void comentariosSaoSeparadosPorMaterialEPaginados() throws Exception {
        UUID outro = UUID.randomUUID();
        jdbc.update("INSERT INTO materiais(id) VALUES (?)", outro);
        when(materiais.buscarPorId(outro)).thenReturn(material);
        var agora = java.sql.Timestamp.from(java.time.Instant.now());
        for (int i = 0; i < 51; i++) jdbc.update(
                "INSERT INTO comentarios_material VALUES (?, ?, ?, ?, ?)", UUID.randomUUID(), id, usuario.getId(), "Comentário " + i, agora);
        mvc.perform(get("/material/" + id + "/comentarios")).andExpect(jsonPath("$.length()").value(50));
        mvc.perform(get("/material/" + id + "/comentarios?pagina=1")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/material/" + outro + "/comentarios")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/material/" + id + "/comentarios?pagina=-1")).andExpect(status().isBadRequest());
        UUID ausente = UUID.randomUUID();
        when(materiais.buscarPorId(ausente)).thenThrow(new ResourceNotFoundException("Material não encontrado"));
        mvc.perform(get("/material/" + ausente + "/comentarios")).andExpect(status().isNotFound());
        jdbc.update("DELETE FROM materiais WHERE id = ?", id);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM comentarios_material", Integer.class));
    }

    @Test void detalhePreservaLegadoEMostraAutorSemDadosPrivados() throws Exception {
        mvc.perform(get("/material/" + id)).andExpect(status().isOk()).andExpect(jsonPath("autorNome").isEmpty());
        material.setAutor(usuario); material.setPublicadoEm(java.time.Instant.now());
        mvc.perform(get("/material/" + id)).andExpect(jsonPath("autorNome").value("Maria Silva"))
                .andExpect(jsonPath("publicadoEm").exists()).andExpect(jsonPath("autor.email").doesNotExist());
        assertNull(jdbc.queryForObject("SELECT autor_id FROM materiais WHERE id = ?", UUID.class, id));
        assertEquals("Resumo antigo", jdbc.queryForObject("SELECT titulo FROM materiais WHERE id = ?", String.class, id));
    }

    @Test void arquivoPodeSerVisualizadoEBaixadoMasHtmlNaoExecuta() throws Exception {
        when(materiais.arquivo(id)).thenReturn(new org.springframework.core.io.ByteArrayResource("%PDF-teste".getBytes()));
        mvc.perform(get("/material/" + id + "/arquivo")).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline")));
        mvc.perform(get("/material/" + id + "/arquivo?download=true"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
        material.setTipoArquivo("text/html");
        mvc.perform(get("/material/" + id + "/arquivo"))
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
    }

    @Test void publicacaoRegistraAutorEProtegeCaminhoDoArquivo() throws Exception {
        var repo = mock(MaterialRepository.class);
        var disciplinas = mock(DisciplinasService.class);
        var service = new MaterialService(repo, disciplinas, usuarios);
        var disciplina = new DisciplinasModel(); ReflectionTestUtils.setField(disciplina, "id", UUID.randomUUID());
        when(disciplinas.buscarPorId(disciplina.getId())).thenReturn(disciplina);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var criado = service.criar(new MaterialRequestDTO("Resumo", "Descrição", disciplina.getId()),
                new MockMultipartFile("arquivo", "../../teste.pdf", "application/pdf", "%PDF".getBytes()), usuario.getEmail());
        Path arquivoCriado = Path.of(criado.getCaminhoArquivo());
        try {
            assertEquals(usuario, criado.getAutor()); assertNotNull(criado.getPublicadoEm());
            assertEquals(Path.of("Uploads"), Path.of(criado.getCaminhoArquivo()).getParent());
            when(repo.findById(id)).thenReturn(Optional.of(criado));
            assertTrue(service.arquivo(id).exists());
            criado.setCaminhoArquivo("pom.xml");
            assertThrows(ResourceNotFoundException.class, () -> service.arquivo(id));
        } finally {
            Files.deleteIfExists(arquivoCriado);
        }
    }
}
