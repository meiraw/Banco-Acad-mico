package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.DTOs.Response.MaterialResponseDTO;
import br.wm.banco.academico.Model.MaterialModel;
import br.wm.banco.academico.Service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@RestController
@RequestMapping("/material")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController (MaterialService materialService){
        this.materialService = materialService;
    }

    @PostMapping
    public ResponseEntity <MaterialResponseDTO> criar (@Valid @RequestPart("dados") MaterialRequestDTO dto, @RequestPart ("arquivo")MultipartFile arquivo, Principal principal ){
        MaterialModel material = materialService.criar(dto,arquivo,principal.getName());
        return ResponseEntity.status(201).body(new MaterialResponseDTO(material));
    }

    @GetMapping
    public ResponseEntity<Page<MaterialResponseDTO>> listar(Pageable pageable){
        Page<MaterialModel> listar = materialService.listarTudo(pageable);
        Page<MaterialResponseDTO> resposta = listar.map(MaterialResponseDTO:: new);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponseDTO> buscarPorId(@PathVariable UUID id ){
        MaterialModel  buscar = materialService.buscarPorId(id);
        return ResponseEntity.ok(new MaterialResponseDTO (buscar));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponseDTO> atualizarMaterial(@Valid @PathVariable UUID id , @RequestBody MaterialRequestDTO dto){
        MaterialModel atualizar = materialService.atualizar(dto, id);
        return ResponseEntity.ok(new MaterialResponseDTO(atualizar));
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<Resource> arquivo(@PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean download) {
        var material = materialService.buscarPorId(id);
        var resource = materialService.arquivo(id);
        // Apenas PDF e imagens raster podem ser exibidos dentro do navegador.
        String tipo = material.getTipoArquivo();
        boolean previa = Set.of("application/pdf", "image/jpeg", "image/png", "image/gif", "image/webp").contains(tipo);
        var disposition = (download || !previa ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(material.getNomeArquivo(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header("Content-Disposition", disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", previa ? "frame-ancestors 'self'" : "sandbox; default-src 'none'")
                .contentType(MediaType.parseMediaType(previa ? tipo : "application/octet-stream"))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <Void> deletar (@PathVariable UUID id ){
        materialService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
