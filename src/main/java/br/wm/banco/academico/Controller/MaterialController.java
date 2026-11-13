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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ResponseEntity<MaterialResponseDTO> atualizarMaterial(@PathVariable UUID id , @Valid @RequestBody MaterialRequestDTO dto){
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
        boolean previa = br.wm.banco.academico.Service.ArmazenamentoService.validado(material.getCaminhoArquivo())
                && tipo != null && Set.of("application/pdf", "image/jpeg", "image/png", "image/gif").contains(tipo);
        String nomeDownload = "material-" + id;
        try { nomeDownload = br.wm.banco.academico.Service.ValidacaoArquivoService.nomeSeguro(material.getNomeArquivo()); }
        catch (org.springframework.web.server.ResponseStatusException ignored) { /* Metadados legados não impedem o download. */ }
        var disposition = (download || !previa ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(nomeDownload, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header("Content-Disposition", disposition.toString())
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox; default-src 'none'; frame-ancestors 'self'")
                .header("Referrer-Policy", "no-referrer")
                .contentType(MediaType.parseMediaType(previa ? tipo : "application/octet-stream"))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity <Void> deletar (@PathVariable UUID id ){
        materialService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
