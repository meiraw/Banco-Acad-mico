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

    public PerfilMaterialController(MaterialService materiais) { this.materiais = materiais; }

    @GetMapping
    public Page<MaterialResponseDTO> listar(Principal principal, @RequestParam(defaultValue = "0") int pagina) {
        return materiais.minhasPublicacoes(principal.getName(), pagina);
    }
}
