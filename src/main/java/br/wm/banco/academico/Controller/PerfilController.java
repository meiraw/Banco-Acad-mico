package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Response.UsuarioResponseDTO;
import br.wm.banco.academico.Service.FotoPerfilService;
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

    public PerfilController(FotoPerfilService fotos) { this.fotos = fotos; }

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
