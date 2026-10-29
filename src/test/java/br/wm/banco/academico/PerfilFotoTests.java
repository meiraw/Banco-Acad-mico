package br.wm.banco.academico;

import br.wm.banco.academico.Config.SecurityConfig;
import br.wm.banco.academico.Controller.PerfilController;
import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Exception.GlobalExpcetionHandler;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.FotoPerfilService;
import br.wm.banco.academico.Service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(PerfilFotoTests.Config.class)
@WebAppConfiguration
class PerfilFotoTests {
    @Configuration
    @EnableWebMvc
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    @Import({SecurityConfig.class, PerfilController.class, FotoPerfilService.class, GlobalExpcetionHandler.class})
    static class Config {
        @Bean UsuarioRepository usuarios() { return mock(UsuarioRepository.class); }
        @Bean TokenService tokens() { return new TokenService(AuthenticationTests.SECRET); }
        @Bean JdbcTemplate jdbc() throws Exception {
            var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                    "jdbc:h2:mem:fotos-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""));
            jdbc.execute("CREATE TABLE usuarios(id UUID PRIMARY KEY)");
            try (var stream = PerfilFotoTests.class.getResourceAsStream("/db/migration/V10__fotos_perfil.sql")) {
                for (String sql : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split(";"))
                    if (!sql.isBlank()) jdbc.execute(sql);
            }
            return jdbc;
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenService tokens;
    @Autowired JdbcTemplate jdbc;
    UsuarioModel usuario, outro;
    MockMvc mvc;

    @BeforeEach void preparar() {
        reset(usuarios);
        jdbc.update("DELETE FROM fotos_perfil");
        jdbc.update("DELETE FROM usuarios");
        usuario = criarUsuario("maria@example.org");
        outro = criarUsuario("outro@example.org");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private UsuarioModel criarUsuario(String email) {
        var conta = new UsuarioModel();
        ReflectionTestUtils.setField(conta, "id", UUID.randomUUID());
        conta.setNome("Maria Silva"); conta.setEmail(email); conta.setPerfil(PerfilUsuario.USER);
        jdbc.update("INSERT INTO usuarios(id) VALUES (?)", conta.getId());
        when(usuarios.findByEmail(email)).thenReturn(Optional.of(conta));
        when(usuarios.bloquearPorId(conta.getId())).thenReturn(Optional.of(conta));
        return conta;
    }

    private MockMultipartFile imagem(String formato, int largura, int altura) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB), formato, bytes);
        return new MockMultipartFile("foto", "foto." + formato, "image/" + formato, bytes.toByteArray());
    }

    private String bearer(UsuarioModel conta) { return "Bearer " + tokens.gerarToken(conta); }

    @Test void usuarioPodeAdicionarTrocarERelerSuaFoto() throws Exception {
        mvc.perform(get("/perfil").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk()).andExpect(jsonPath("nome").value("Maria Silva"))
                .andExpect(jsonPath("possuiFoto").value(false)).andExpect(jsonPath("senha").doesNotExist());
        mvc.perform(multipart("/perfil/foto").file(imagem("png", 800, 400))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", bearer(usuario)))
                .andExpect(status().isOk());
        var resposta = mvc.perform(get("/perfil/foto").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse().getContentAsByteArray();
        var reduzida = ImageIO.read(new ByteArrayInputStream(resposta));
        assertEquals(512, reduzida.getWidth()); assertEquals(256, reduzida.getHeight());
        mvc.perform(multipart("/perfil/foto").file(imagem("jpeg", 40, 40))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", bearer(usuario)))
                .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM fotos_perfil", Integer.class));
        mvc.perform(get("/perfil").header("Authorization", bearer(usuario)))
                .andExpect(jsonPath("possuiFoto").value(true));
    }

    @Test void adminTambemPodeSalvarFoto() throws Exception {
        usuario.setPerfil(PerfilUsuario.ADMIN);
        mvc.perform(multipart("/perfil/foto").file(imagem("png", 20, 20))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", bearer(usuario)))
                .andExpect(status().isOk());
    }

    @Test void anonimoNaoAcessaNemAlteraFotos() throws Exception {
        mvc.perform(get("/perfil")).andExpect(status().isUnauthorized());
        mvc.perform(get("/perfil/foto")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/perfil/foto").file(imagem("png", 20, 20))
                .with(r -> { r.setMethod("PUT"); return r; })).andExpect(status().isUnauthorized());
    }

    @Test void fotoFicaRestritaAContaAutenticada() throws Exception {
        mvc.perform(multipart("/perfil/foto").file(imagem("png", 20, 20))
                .param("usuarioId", outro.getId().toString())
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", bearer(usuario)))
                .andExpect(status().isOk());
        mvc.perform(get("/perfil/foto").header("Authorization", bearer(outro))).andExpect(status().isNotFound());
        assertEquals(usuario.getId(), jdbc.queryForObject("SELECT usuario_id FROM fotos_perfil", UUID.class));
    }

    @Test void rejeitaConteudoInvalidoVazioETamanhoExcessivo() throws Exception {
        for (byte[] conteudo : new byte[][]{new byte[0], "<svg/>".getBytes(StandardCharsets.UTF_8)}) {
            mvc.perform(multipart("/perfil/foto").file(new MockMultipartFile("foto", "foto.png", "image/png", conteudo))
                    .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", bearer(usuario)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/perfil/foto").file(new MockMultipartFile("foto", "foto.png", "image/png", new byte[2097153]))
                .with(r -> { r.setMethod("PUT"); return r; }).header("Authorization", bearer(usuario)))
                .andExpect(status().isPayloadTooLarge());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM fotos_perfil", Integer.class));
    }

    @Test void migrationNaoExigeFotoDeContaAntigaEExcluiFotoEmCascata() throws Exception {
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class));
        mvc.perform(get("/perfil/foto").header("Authorization", bearer(usuario))).andExpect(status().isNotFound());
        jdbc.update("INSERT INTO fotos_perfil(usuario_id, conteudo) VALUES (?, ?)", usuario.getId(), new byte[]{1});
        jdbc.update("DELETE FROM usuarios WHERE id = ?", usuario.getId());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM fotos_perfil", Integer.class));
    }
}
