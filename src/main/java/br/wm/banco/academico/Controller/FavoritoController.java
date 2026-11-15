package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Response.MaterialResponseDTO;
import br.wm.banco.academico.Exception.ErrorResponse;
import br.wm.banco.academico.Service.FavoritoService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/favoritos")
public class FavoritoController {
    private final FavoritoService favoritos;
    private final br.wm.banco.academico.Service.EstatisticasMaterialService estatisticas;
    public FavoritoController(FavoritoService favoritos, br.wm.banco.academico.Service.EstatisticasMaterialService estatisticas) {
        this.favoritos = favoritos; this.estatisticas = estatisticas;
    }
    @GetMapping
    public ResponseEntity<Page<MaterialResponseDTO>> listar(Principal principal,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        var resposta = favoritos.listar(principal.getName(), page, size);
        estatisticas.preencher(resposta.getContent());
        return ResponseEntity.ok().header("Cache-Control", "private, no-store").body(resposta);
    }
    @GetMapping("/ids")
    public ResponseEntity<Page<UUID>> ids(Principal principal,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "200") int size) {
        return ResponseEntity.ok().header("Cache-Control", "private, no-store")
            .body(favoritos.ids(principal.getName(), page, size));
    }
    @PutMapping("/{materialId}")
    public ResponseEntity<Void> adicionar(Principal principal, @PathVariable UUID materialId) {
        favoritos.adicionar(principal.getName(), materialId);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{materialId}")
    public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID materialId) {
        favoritos.remover(principal.getName(), materialId);
        return ResponseEntity.noContent().build();
    }
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> argumentoInvalido() {
        return ResponseEntity.badRequest().body(new ErrorResponse("Identificador ou paginação inválidos.", 400));
    }
}
