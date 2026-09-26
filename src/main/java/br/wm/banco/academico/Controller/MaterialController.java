package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.MaterialRequestDTO;
import br.wm.banco.academico.DTOs.Response.MaterialResponseDTO;
import br.wm.banco.academico.Model.MaterialModel;
import br.wm.banco.academico.Service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/material")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController (MaterialService materialService){
        this.materialService = materialService;
    }

    @PostMapping
    public ResponseEntity <MaterialResponseDTO> criar (@Valid @RequestPart("dados") MaterialRequestDTO dto, @RequestPart ("arquivo")MultipartFile arquivo ){
        MaterialModel material = materialService.criar(dto,arquivo);
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

    @DeleteMapping("/{id}")
    public ResponseEntity <Void> deletar (@PathVariable UUID id ){
        materialService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
