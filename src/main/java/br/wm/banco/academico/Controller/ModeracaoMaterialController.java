package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Response.MaterialResponseDTO;
import br.wm.banco.academico.Service.MaterialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/admin/materiais")
public class ModeracaoMaterialController {
    private final MaterialService materiais;
    public ModeracaoMaterialController(MaterialService materiais) { this.materiais = materiais; }
    public record Rejeicao(@NotBlank(message = "Informe a justificativa da rejeição.")
            @Size(max = 2000, message = "A justificativa deve ter até 2000 caracteres.") String justificativa) {}

    @GetMapping("/pendentes")
    public Page<MaterialResponseDTO> pendentes(@RequestParam(defaultValue = "0") int pagina) {
        return materiais.pendentes(pagina);
    }

    @PatchMapping("/{id}/aprovar")
    public MaterialResponseDTO aprovar(@PathVariable UUID id) { return materiais.decidir(id, true, null); }

    @PatchMapping("/{id}/rejeitar")
    public MaterialResponseDTO rejeitar(@PathVariable UUID id, @Valid @RequestBody Rejeicao dados) {
        return materiais.decidir(id, false, dados.justificativa());
    }
}
