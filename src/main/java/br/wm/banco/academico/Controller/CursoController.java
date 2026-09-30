package br.wm.banco.academico.Controller;


import br.wm.banco.academico.DTOs.Request.CursoRequestDTO;
import br.wm.banco.academico.DTOs.Response.CursoResponseDTO;
import br.wm.banco.academico.Model.CursoModel;
import br.wm.banco.academico.Service.CursoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/curso")
public class CursoController {

    private final CursoService cursoService;

    public CursoController (CursoService cursoService ){
        this.cursoService = cursoService;
    }

    @PostMapping
    public ResponseEntity<CursoResponseDTO> criar (@Valid @RequestBody CursoRequestDTO dto ){
        CursoModel curso = cursoService.criar(dto);
        return ResponseEntity.status(201).body(new CursoResponseDTO(curso));
    }

    @GetMapping
    public ResponseEntity<List<CursoResponseDTO>> listar() {

        List<CursoModel> cursos = cursoService.listar();

        List<CursoResponseDTO> resposta = cursos.stream()
                .map(CursoResponseDTO::new)
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @GetMapping ("/{id}")
    public ResponseEntity<CursoResponseDTO> buscarId (@PathVariable UUID id){
        CursoModel buscar = cursoService.buscarPorId(id);
        return ResponseEntity.ok(new CursoResponseDTO(buscar));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoResponseDTO> atualizar ( @PathVariable UUID id,@Valid @RequestBody CursoRequestDTO dto){

        CursoModel cursoNovo = cursoService.atualizar(dto,id);

        return ResponseEntity.ok(new CursoResponseDTO(cursoNovo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover (@PathVariable UUID id) {
        cursoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
