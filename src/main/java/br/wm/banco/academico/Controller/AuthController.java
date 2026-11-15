package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.LoginRequestDTO;
import br.wm.banco.academico.DTOs.Response.LoginResponseDTO;
import br.wm.banco.academico.DTOs.Response.UsuarioResponseDTO;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Service.TokenService;
import br.wm.banco.academico.Service.UsuarioService;
import br.wm.banco.academico.Service.RecuperacaoLimiteService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final RecuperacaoLimiteService limites;

    public AuthController(UsuarioService usuarioService, TokenService tokenService, RecuperacaoLimiteService limites) {

        this.usuarioService = usuarioService;
        this.tokenService = tokenService;
        this.limites = limites;
    }//injeção

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {

        // Contabiliza antes de consultar a conta, inclusive credenciais incorretas.
        String email = dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (!limites.permitir("login-ip:" + request.getRemoteAddr(), 100)
                || !limites.permitir("login-email:" + email, 20)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas de login. Aguarde até 15 minutos.");
        }

        UsuarioModel usuario = usuarioService.login(dto);

        String token = tokenService.gerarToken(usuario);

        return ResponseEntity.ok(
                new LoginResponseDTO(token)
        );
    }
}

