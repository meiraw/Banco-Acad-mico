package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Response.MaterialResponseDTO;
import br.wm.banco.academico.Service.MaterialService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/perfil/publicacoes")
public class PerfilMaterialController {
    private final MaterialService materiais;
    private final br.wm.banco.academico.Service.EstatisticasMaterialService estatisticas;

    public PerfilMaterialController(MaterialService materiais, br.wm.banco.academico.Service.EstatisticasMaterialService estatisticas) {
        this.materiais = materiais; this.estatisticas = estatisticas;
    }

    @GetMapping
    public Page<MaterialResponseDTO> listar(Principal principal, @RequestParam(defaultValue = "0") int pagina) {
        var resposta = materiais.minhasPublicacoes(principal.getName(), pagina);
        estatisticas.preencher(resposta.getContent());
        return resposta;
    }
}
