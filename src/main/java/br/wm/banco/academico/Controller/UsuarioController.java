package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.UsuarioRequestDTO;
import br.wm.banco.academico.DTOs.Response.UsuarioResponseDTO;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController (UsuarioService usuarioService){
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar (@Valid @RequestBody UsuarioRequestDTO dto ){
        UsuarioModel usuario = usuarioService.criar(dto);
        return ResponseEntity.status(201).body(new UsuarioResponseDTO (usuario));
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioResponseDTO>> listar ( Pageable pageable ){
        Page<UsuarioModel> listar  = usuarioService.listar(pageable);
        Page<UsuarioResponseDTO> resposta = listar.map(UsuarioResponseDTO::new);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping ("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscar (@PathVariable UUID id ){
        UsuarioModel buscar = usuarioService.buscarId(id);
        return ResponseEntity.ok(new UsuarioResponseDTO (buscar));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> novo (@PathVariable UUID id ,@Valid @RequestBody UsuarioRequestDTO dto ){
        UsuarioModel novo  = usuarioService.atualizar(dto,id);
        return ResponseEntity.ok(new UsuarioResponseDTO (novo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover ( @PathVariable UUID id ){
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
