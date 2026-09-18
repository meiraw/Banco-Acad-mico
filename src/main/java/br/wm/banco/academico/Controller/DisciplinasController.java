package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.DisciplinasRequestDTO;
import br.wm.banco.academico.DTOs.Response.DisciplinasResponseDTO;
import br.wm.banco.academico.Model.DisciplinasModel;
import br.wm.banco.academico.Service.DisciplinasService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/disciplinas")
public class DisciplinasController {

    private final DisciplinasService disciplinasService;

    public DisciplinasController(DisciplinasService disciplinasService) {
        this.disciplinasService = disciplinasService;
    }

    @PostMapping
    public ResponseEntity<DisciplinasResponseDTO > criarDisciplinas (@Valid @RequestBody DisciplinasRequestDTO dto ){
            DisciplinasModel disciplinas = disciplinasService.criar(dto);
        return ResponseEntity.status(201).body(new DisciplinasResponseDTO(disciplinas));
    }

    @GetMapping
    public ResponseEntity<Page<DisciplinasResponseDTO>> listar (Pageable pageable ){
        Page<DisciplinasModel> lista = disciplinasService.listarTudo(pageable);
        Page<DisciplinasResponseDTO> resposta = lista.map(DisciplinasResponseDTO:: new);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplinasResponseDTO> buscarId(@PathVariable UUID id){
        DisciplinasModel buscar = disciplinasService.buscarPorId(id);
        return ResponseEntity.ok(new DisciplinasResponseDTO(buscar));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisciplinasResponseDTO> atualizar ( @PathVariable UUID id ,@Valid @RequestBody DisciplinasRequestDTO dto){
        DisciplinasModel novaDisciplinas = disciplinasService.atualizar(dto, id);
        return ResponseEntity.ok(new DisciplinasResponseDTO(novaDisciplinas));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar (@PathVariable UUID id){
        disciplinasService.excluir(id);
        return ResponseEntity.noContent().build();
    }


}


