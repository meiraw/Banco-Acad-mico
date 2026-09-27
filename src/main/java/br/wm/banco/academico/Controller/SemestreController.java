package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.SemestreRequestDTO;
import br.wm.banco.academico.DTOs.Response.SemestreResponseDTO;
import br.wm.banco.academico.Model.SemestreModel;
import br.wm.banco.academico.Service.SemestreService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/semestres")
public class SemestreController {


    private final SemestreService semestreService;

    public SemestreController (SemestreService semestreService ){
        this.semestreService = semestreService;
    }

    @PostMapping
    public ResponseEntity <SemestreResponseDTO> criar (@Valid @RequestBody SemestreRequestDTO dto){
        SemestreModel  semestre = semestreService.criar(dto);
        return ResponseEntity.status(201).body(new SemestreResponseDTO(semestre));
    }

    @GetMapping
    public ResponseEntity <Page<SemestreResponseDTO>> listar (Pageable pageable){
        Page<SemestreModel> listar = semestreService.listar(pageable);
        Page<SemestreResponseDTO> resposta = listar.map(SemestreResponseDTO:: new);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SemestreResponseDTO> buscarId(@PathVariable UUID id ){
        SemestreModel buscar = semestreService.buscarPorId(id);
        return  ResponseEntity.ok(new SemestreResponseDTO (buscar));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SemestreResponseDTO> atualizar (@Valid @RequestBody SemestreRequestDTO dto , @PathVariable UUID id ){
        SemestreModel  novo = semestreService.atualizar(dto, id);
        return ResponseEntity.ok(new SemestreResponseDTO(novo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id){
        semestreService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
