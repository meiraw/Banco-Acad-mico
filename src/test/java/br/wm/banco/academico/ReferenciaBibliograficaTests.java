package br.wm.banco.academico;

import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.Enum.TipoPublicacao;
import br.wm.banco.academico.Service.MaterialService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ReferenciaBibliograficaTests {
    MaterialRequestDTO dados(TipoPublicacao tipo, String referencia) {
        return new MaterialRequestDTO("Título", "Descrição", UUID.randomUUID(), tipo, referencia);
    }

    @Test void beanValidationRejeitaArtigoSemReferenciaOuSomenteEspacos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String referencia : new String[]{null, "", "   ", "\t\n"}) {
                var erros = validator.validate(dados(TipoPublicacao.ARTIGO, referencia));
                assertTrue(erros.stream().anyMatch(erro -> erro.getMessage().equals("Informe a referência bibliográfica do artigo.")));
            }
            assertTrue(validator.validate(dados(TipoPublicacao.ARTIGO, "Autor. Artigo. 2026.")).isEmpty());
        }
    }

    @Test void livroOpcionalEDemaisTiposSemReferenciaContinuamValidos() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (var tipo : TipoPublicacao.values()) {
                if (tipo == TipoPublicacao.ARTIGO) continue;
                for (String referencia : new String[]{null, "", "   ", "\t\n"})
                    assertTrue(validator.validate(dados(tipo, referencia)).isEmpty(), tipo.name());
            }
            assertTrue(validator.validate(dados(TipoPublicacao.LIVRO, "Autor. Livro. 2026.")).isEmpty());
            assertFalse(validator.validate(dados(TipoPublicacao.ARTIGO, "x".repeat(501))).isEmpty());
        }
    }

    @Test void servicoProtegeCriacaoEAtualizacaoSemDependerDoController() {
        // A rejeição deve ocorrer antes de qualquer acesso ao arquivo ou ao banco.
        var service = new MaterialService(null, null, null, null, null, null, null, null, null, 1);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            for (String referencia : new String[]{null, "", "   ", "\t\n"}) {
                var dto = dados(TipoPublicacao.ARTIGO, referencia);
                var criacao = assertThrows(ResponseStatusException.class, () -> service.criar(dto, null, "autor"));
                var atualizacao = assertThrows(ResponseStatusException.class, () -> service.atualizar(dto, UUID.randomUUID()));
                for (var erro : List.of(criacao, atualizacao)) {
                    assertEquals(400, erro.getStatusCode().value());
                    assertEquals("Informe a referência bibliográfica do artigo.", erro.getReason());
                }
            }
        } finally { SecurityContextHolder.clearContext(); }
    }
}
