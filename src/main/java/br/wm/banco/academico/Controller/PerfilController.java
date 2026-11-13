package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Response.UsuarioResponseDTO;
import br.wm.banco.academico.Service.FotoPerfilService;
import br.wm.banco.academico.Service.PerfilService;
import br.wm.banco.academico.Service.RecuperacaoLimiteService;
import br.wm.banco.academico.DTOs.Request.AtualizarPerfilRequestDTO;
import br.wm.banco.academico.DTOs.Request.AlterarSenhaRequestDTO;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/perfil")
public class PerfilController {
    private final FotoPerfilService fotos;
    private final PerfilService contas;
    private final RecuperacaoLimiteService limites;

    public PerfilController(FotoPerfilService fotos, PerfilService contas, RecuperacaoLimiteService limites) {
        this.fotos = fotos; this.contas = contas; this.limites = limites;
    }

    @PutMapping
    public UsuarioResponseDTO atualizar(@Valid @RequestBody AtualizarPerfilRequestDTO dados, Principal principal) {
        var usuario = contas.atualizar(principal.getName(), dados);
        return new UsuarioResponseDTO(usuario, fotos.possuiFoto(usuario.getId()));
    }

    @PutMapping("/senha")
    public Map<String, String> alterarSenha(@Valid @RequestBody AlterarSenhaRequestDTO dados, Principal principal, HttpServletRequest request) {
        // Limites são confirmados antes da transação da senha: falhas não apagam tentativas.
        if (!limites.permitir("alterar-senha-ip:" + request.getRemoteAddr(), 20)) throw limite();
        var usuario = fotos.usuarioAtual(principal.getName());
        if (!limites.permitir("alterar-senha-conta:" + usuario.getId(), 5)) throw limite();
        contas.alterarSenha(principal.getName(), dados);
        return Map.of("mensagem", "Senha alterada. Entre novamente com sua nova senha.");
    }

    private static org.springframework.web.server.ResponseStatusException limite() {
        return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
            "Muitas tentativas. Aguarde até 15 minutos.");
    }

    @GetMapping
    public UsuarioResponseDTO perfil(Principal principal) {
        var usuario = fotos.usuarioAtual(principal.getName());
        return new UsuarioResponseDTO(usuario, fotos.possuiFoto(usuario.getId()));
    }

    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto(Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(fotos.buscar(principal.getName()));
    }

    @PutMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> salvar(@RequestPart("foto") MultipartFile foto, Principal principal) {
        fotos.salvar(principal.getName(), foto);
        return Map.of("mensagem", "Foto de perfil atualizada.");
    }
}
