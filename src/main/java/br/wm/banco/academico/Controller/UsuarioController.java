package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.UsuarioRequestDTO;
import br.wm.banco.academico.DTOs.Request.CadastroUsuarioRequestDTO;
import br.wm.banco.academico.DTOs.Response.UsuarioResponseDTO;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Service.UsuarioService;
import br.wm.banco.academico.Service.RecuperacaoLimiteService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RecuperacaoLimiteService limites;

    public UsuarioController (UsuarioService usuarioService, RecuperacaoLimiteService limites){
        this.usuarioService = usuarioService;
        this.limites = limites;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar (@Valid @RequestBody CadastroUsuarioRequestDTO dto, HttpServletRequest request){
        String email = dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (!limites.permitir("cadastro-ip:" + request.getRemoteAddr(), 10)
                || !limites.permitir("cadastro-email:" + email, 3)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de cadastro. Aguarde até 15 minutos.");
        }
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
