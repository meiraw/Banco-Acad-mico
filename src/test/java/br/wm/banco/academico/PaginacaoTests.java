package br.wm.banco.academico;

import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import br.wm.banco.academico.Service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:paginacao;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "security.jwt.secret=test-only-secret-with-at-least-32-bytes", "management.health.mail.enabled=false"
})
@ExtendWith(OutputCaptureExtension.class)
class PaginacaoTests {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenService tokens;
    @MockitoBean JavaMailSender mail;

    private UsuarioModel usuario(String nome, PerfilUsuario perfil) {
        var conta = new UsuarioModel(); conta.setNome(nome); conta.setEmail(UUID.randomUUID() + "@example.test");
        conta.setSenha("hash-de-teste"); conta.setPerfil(perfil); conta.setEmailConfirmado(true);
        return usuarios.saveAndFlush(conta);
    }

    private void verificarPagina(ResultActions resposta, int numero, int tamanho, int total, int itens) throws Exception {
        resposta.andExpect(status().isOk()).andExpect(jsonPath("content.length()").value(itens))
            .andExpect(jsonPath("page.number").value(numero)).andExpect(jsonPath("page.size").value(tamanho))
            .andExpect(jsonPath("page.totalElements").value(total))
            .andExpect(jsonPath("page.totalPages").value((total + tamanho - 1) / tamanho))
            .andExpect(jsonPath("totalElements").doesNotExist()).andExpect(jsonPath("pageable").doesNotExist());
    }

    @Test void cincoEndpointsUsamDtoEstavelPreservandoPaginasOrdenacaoETamanhos(CapturedOutput output) throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var autor = usuario("Autor", PerfilUsuario.USER);
        var colega = usuario("Colega", PerfilUsuario.USER);
        var admin = usuario("Administrador", PerfilUsuario.ADMIN);
        String tokenAutor = "Bearer " + tokens.gerarToken(autor), tokenAdmin = "Bearer " + tokens.gerarToken(admin);
        UUID curso = UUID.randomUUID(), semestre = UUID.randomUUID();
        jdbc.update("INSERT INTO cursos(id,nome) VALUES (?,?)", curso, "Computação");
        jdbc.update("INSERT INTO semestres(id,nome,numero,curso_id) VALUES (?,?,?,?)", semestre, "Primeiro", 1, curso);
        UUID disciplina = null;
        for (int i = 0; i < 3; i++) {
            disciplina = UUID.randomUUID();
            jdbc.update("INSERT INTO disciplinas(id,nome,descricao,semestre_id) VALUES (?,?,?,?)", disciplina, "Disciplina " + i, "Descrição", semestre);
        }
        for (int i = 0; i < 16; i++) {
            UUID id = UUID.randomUUID();
            jdbc.update("""
                INSERT INTO materiais(id,titulo,descricao,disciplina_id,nome_arquivo,tipo_arquivo,caminho_arquivo,
                    autor_id,publicado_em,status_moderacao,tipo_publicacao)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """, id, "Material " + String.format("%02d", i), "Descrição", disciplina,
                "teste.pdf", "application/pdf", "Uploads/" + id, autor.getId(),
                java.sql.Timestamp.from(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(i)),
                i < 3 ? "APROVADO" : "PENDENTE", "RESUMO");
        }

        verificarPagina(mvc.perform(get("/material?page=0&size=2&sort=titulo,asc")), 0, 2, 3, 2);
        verificarPagina(mvc.perform(get("/material?page=1&size=2&sort=titulo,asc"))
            .andExpect(jsonPath("content[0].titulo").value("Material 02")), 1, 2, 3, 1);
        verificarPagina(mvc.perform(get("/material?page=99&size=2")), 99, 2, 3, 0);
        verificarPagina(mvc.perform(get("/material")), 0, 20, 3, 3);
        verificarPagina(mvc.perform(get("/material?size=500")), 0, 500, 3, 3);

        verificarPagina(mvc.perform(get("/disciplinas?page=1&size=2&sort=nome,asc"))
            .andExpect(jsonPath("content[0].nome").value("Disciplina 2")), 1, 2, 3, 1);
        verificarPagina(mvc.perform(get("/disciplinas?size=500")), 0, 500, 3, 3);
        verificarPagina(mvc.perform(get("/usuario?page=1&size=2&sort=nome,asc").header("Authorization", tokenAdmin)), 1, 2, 3, 1);

        verificarPagina(mvc.perform(get("/perfil/publicacoes?pagina=0").header("Authorization", tokenAutor)), 0, 12, 16, 12);
        verificarPagina(mvc.perform(get("/perfil/publicacoes?pagina=1").header("Authorization", tokenAutor))
            .andExpect(jsonPath("content[0].titulo").value("Material 03")), 1, 12, 16, 4);
        verificarPagina(mvc.perform(get("/perfil/publicacoes").header("Authorization", "Bearer " + tokens.gerarToken(colega))), 0, 12, 0, 0);
        verificarPagina(mvc.perform(get("/admin/materiais/pendentes?pagina=0").header("Authorization", tokenAdmin)), 0, 12, 13, 12);
        verificarPagina(mvc.perform(get("/admin/materiais/pendentes?pagina=1").header("Authorization", tokenAdmin))
            .andExpect(jsonPath("content[0].titulo").value("Material 15")), 1, 12, 13, 1);
        mvc.perform(get("/semestres")).andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Primeiro"));
        mvc.perform(get("/curso")).andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Computação"));
        assertFalse(output.getAll().contains("Serializing PageImpl"));
        assertFalse(output.getAll().contains("For a stable JSON structure"));
    }
}
