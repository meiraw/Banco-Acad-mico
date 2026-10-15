package br.wm.banco.academico;

import br.wm.banco.academico.Config.SecurityConfig;
import br.wm.banco.academico.Controller.AuthController;
import br.wm.banco.academico.Controller.UsuarioController;
import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Exception.GlobalExpcetionHandler;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.TokenService;
import br.wm.banco.academico.Service.UsuarioService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(AuthenticationTests.Config.class)
@WebAppConfiguration
class AuthenticationTests {
    static final String SECRET = "test-only-secret-with-at-least-32-bytes";
    @Configuration
    @EnableWebMvc
    @org.springframework.data.web.config.EnableSpringDataWebSupport
    @org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
    @Import({SecurityConfig.class, AuthController.class, UsuarioController.class, GlobalExpcetionHandler.class,
            br.wm.banco.academico.Controller.ConfirmacaoEmailController.class,
            br.wm.banco.academico.Controller.RecuperacaoSenhaController.class,
            br.wm.banco.academico.Controller.MaterialController.class})
    static class Config {
        @Bean br.wm.banco.academico.Service.ConfirmacaoEmailService confirmacao() {
            return mock(br.wm.banco.academico.Service.ConfirmacaoEmailService.class);
        }
        @Bean br.wm.banco.academico.Service.RecuperacaoSenhaService recuperacao() {
            return mock(br.wm.banco.academico.Service.RecuperacaoSenhaService.class);
        }
        @Bean br.wm.banco.academico.Service.RecuperacaoLimiteService limites() {
            return mock(br.wm.banco.academico.Service.RecuperacaoLimiteService.class);
        }
        @Bean UsuarioRepository usuarios() { return mock(UsuarioRepository.class); }
        @Bean TokenService tokens() { return new TokenService(SECRET); }
        @Bean br.wm.banco.academico.Service.MaterialService materiais() {
            return mock(br.wm.banco.academico.Service.MaterialService.class);
        }
        @Bean UsuarioService service(UsuarioRepository repo, PasswordEncoder encoder,
                br.wm.banco.academico.Service.ConfirmacaoEmailService confirmacao) {
            return new UsuarioService(repo, encoder, confirmacao);
        }
    }
    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    UsuarioModel usuario;
    @BeforeEach void setup() {
        reset(usuarios);
        reset(context.getBean(br.wm.banco.academico.Service.ConfirmacaoEmailService.class));
        reset(context.getBean(br.wm.banco.academico.Service.RecuperacaoLimiteService.class));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        usuario = new UsuarioModel();
        usuario.setEmailConfirmado(true);
        usuario.setNome("Teste"); usuario.setEmail("teste@unemat.br");
        usuario.setSenha(encoder.encode("Senha123!")); usuario.setPerfil(PerfilUsuario.USER);
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }
    @Test void cadastroPreservaBcryptEPerfilUser() throws Exception {
        when(usuarios.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        mvc.perform(post("/usuario").contentType("application/json")
                .content("{\"nome\":\"Novo\",\"email\":\"novo@unemat.br\",\"senha\":\"Senha123!\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("senha").doesNotExist());
        verify(usuarios).saveAndFlush(argThat(u -> !u.isEmailConfirmado() && u.getPerfil() == PerfilUsuario.USER
                && encoder.matches("Senha123!", u.getSenha()) && !u.getSenha().equals("Senha123!")));
    }
    @Test void cadastroELoginAceitamDiferentesProvedoresSemEnviarEmail() throws Exception {
        when(usuarios.saveAndFlush(any())).thenAnswer(inv -> {
            UsuarioModel salvo = inv.getArgument(0);
            when(usuarios.findByEmail(salvo.getEmail())).thenReturn(Optional.of(salvo));
            return salvo;
        });
        for (String email : new String[]{"novo@gmail.com", "novo@outlook.com", "novo@unemat.br"}) {
            mvc.perform(post("/usuario").contentType("application/json")
                    .content("{\"nome\":\"Novo\",\"email\":\"" + email + "\",\"senha\":\"Senha123!\"}"))
                    .andExpect(status().isCreated());
            mvc.perform(post("/auth/login").contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"senha\":\"Senha123!\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("token").isString());
        }
        verifyNoInteractions(context.getBean(br.wm.banco.academico.Service.ConfirmacaoEmailService.class));
    }
    @Test void cadastroDuplicadoEInvalido() throws Exception {
        mvc.perform(post("/usuario").contentType("application/json")
                .content("{\"nome\":\"Teste\",\"email\":\"teste@unemat.br\",\"senha\":\"Senha123!\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/usuario").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verify(usuarios, never()).save(any());
    }
    @Test void loginGeraJwtValido() throws Exception {
        String body = mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"email\":\"teste@unemat.br\",\"senha\":\"Senha123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("token").isString())
                .andReturn().getResponse().getContentAsString();
        String token = body.split("\"")[3];
        assertEquals(usuario.getEmail(), tokens.validarToken(token));
        assertEquals("USER", JWT.decode(token).getClaim("perfil").asString());
        assertTrue(JWT.decode(token).getExpiresAtAsInstant().isAfter(Instant.now()));
    }
    @Test void loginNaoExigeConfirmacaoDeEmail() throws Exception {
        usuario.setEmailConfirmado(false);
        mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"email\":\"teste@unemat.br\",\"senha\":\"Senha123!\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/usuario").header("Authorization", "Bearer " + tokens.gerarToken(usuario)))
                .andExpect(status().isForbidden());
    }
    @Test void confirmacaoPublicaComValidacaoELimite() throws Exception {
        var limites = context.getBean(br.wm.banco.academico.Service.RecuperacaoLimiteService.class);
        when(limites.permitir(anyString(), anyInt())).thenReturn(true);
        mvc.perform(post("/auth/confirmar-email").contentType("application/json")
                .content("{\"token\":\"" + "A".repeat(43) + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/reenviar-confirmacao").contentType("application/json")
                .content("{\"email\":\"teste@unemat.br\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/confirmar-email").contentType("application/json")
                .content("{\"token\":\"curto\"}"))
                .andExpect(status().isBadRequest());
        when(limites.permitir(anyString(), anyInt())).thenReturn(false);
        mvc.perform(post("/auth/reenviar-confirmacao").contentType("application/json")
                .content("{\"email\":\"teste@unemat.br\"}"))
                .andExpect(status().isTooManyRequests());
    }
    @Test void loginRejeitaCredenciaisInvalidas() throws Exception {
        for (String email : new String[]{"teste@unemat.br", "ausente@unemat.br"}) {
            mvc.perform(post("/auth/login").contentType("application/json")
                    .content("{\"email\":\"" + email + "\",\"senha\":\"errada\"}"))
                    .andExpect(status().isConflict()); // Contrato existente preservado.
        }
    }
    String assinar(String issuer, String secret, Instant exp) {
        return JWT.create().withIssuer(issuer).withSubject(usuario.getEmail())
                .withExpiresAt(exp).sign(Algorithm.HMAC256(secret));
    }
    @Test void rejeitaTokensInvalidos() throws Exception {
        for (String token : new String[]{"invalido", assinar("banco-academico", SECRET, Instant.now().minusSeconds(60)),
                assinar("outro", SECRET, Instant.now().plusSeconds(60)),
                assinar("banco-academico", SECRET + "outro", Instant.now().plusSeconds(60)),
                JWT.create().withIssuer("banco-academico").withSubject(usuario.getEmail()).sign(Algorithm.HMAC256(SECRET))}) {
            assertThrows(JWTVerificationException.class, () -> tokens.validarToken(token));
            mvc.perform(get("/usuario").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }
    @Test void permissoesEPerfilAtualDoBanco() throws Exception {
        when(usuarios.findAll(any(Pageable.class))).thenReturn(new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));
        String token = tokens.gerarToken(usuario);
        mvc.perform(get("/usuario")).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuario").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        usuario.setPerfil(PerfilUsuario.ADMIN);
        mvc.perform(get("/usuario").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.empty());
        mvc.perform(get("/usuario").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @Test void recuperacaoPublicaEJwtAnteriorRevogado() throws Exception {
        var limites = context.getBean(br.wm.banco.academico.Service.RecuperacaoLimiteService.class);
        when(limites.permitir(anyString(), anyInt())).thenReturn(true);
        mvc.perform(post("/auth/esqueci-senha").contentType("application/json")
                .content("{\"email\":\"teste@unemat.br\"}")).andExpect(status().isOk());
        mvc.perform(post("/auth/redefinir-senha").contentType("application/json")
                .content("{\"token\":\"" + "A".repeat(43) + "\",\"novaSenha\":\"NovaSenha123!\"}"))
                .andExpect(status().isOk());
        String anterior = tokens.gerarToken(usuario);
        String legado = assinar("banco-academico", SECRET, Instant.now().plusSeconds(60));
        usuario.setVersaoCredenciais(1);
        mvc.perform(get("/usuario").header("Authorization", "Bearer " + anterior)).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuario").header("Authorization", "Bearer " + legado)).andExpect(status().isUnauthorized());
        mvc.perform(get("/usuario").header("Authorization", "Bearer " + tokens.gerarToken(usuario)))
                .andExpect(status().isForbidden());
    }
    @Test void corsPreflight() throws Exception {
        mvc.perform(options("/usuario").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/usuario").header("Origin", "https://outro.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
    @Test void envioAutenticadoEExclusaoAdministrativa() throws Exception {
        String token = tokens.gerarToken(usuario);
        mvc.perform(post("/material")).andExpect(status().isUnauthorized());
        // Sem as partes multipart, 400 confirma que USER chegou ao controller.
        mvc.perform(post("/material").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        String path = "/material/00000000-0000-0000-0000-000000000001";
        mvc.perform(delete(path).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        usuario.setPerfil(PerfilUsuario.ADMIN);
        mvc.perform(delete(path).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
    }
    @Test void catalogoPublicoEGestaoRestrita() throws Exception {
        var materiais = context.getBean(br.wm.banco.academico.Service.MaterialService.class);
        when(materiais.listarTudo(any(Pageable.class))).thenReturn(new org.springframework.data.domain.PageImpl<>(
                java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 20), 0));
        mvc.perform(get("/material")).andExpect(status().isOk());
        String token = tokens.gerarToken(usuario);
        for (String path : new String[]{"/curso", "/semestres", "/disciplinas"}) {
            mvc.perform(post(path).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(put(path + "/00000000-0000-0000-0000-000000000001")
                    .header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(delete(path + "/00000000-0000-0000-0000-000000000001")
                    .header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
    }
}
