package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.LoginRequestDTO;
import br.wm.banco.academico.DTOs.Response.LoginResponseDTO;
import br.wm.banco.academico.DTOs.Response.UsuarioResponseDTO;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Service.TokenService;
import br.wm.banco.academico.Service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsuarioService usuarioService;
    private final TokenService tokenService;

    public AuthController(UsuarioService usuarioService, TokenService tokenService) {

        this.usuarioService = usuarioService;
        this.tokenService = tokenService;
    }//injeção

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto) {

        UsuarioModel usuario = usuarioService.login(dto);

        String token = tokenService.gerarToken(usuario);

        return ResponseEntity.ok(
                new LoginResponseDTO(token)
        );
    }
}

