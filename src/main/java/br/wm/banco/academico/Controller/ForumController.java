package br.wm.banco.academico.Controller;
import br.wm.banco.academico.Service.ForumService;
import br.wm.banco.academico.DTOs.Request.*;
import br.wm.banco.academico.DTOs.Response.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/forum/discussoes")
public class ForumController {
    private final ForumService forum;
    public ForumController(ForumService forum) { this.forum = forum; }
    @GetMapping
    public Page<DiscussaoResumoDTO> listar(Principal principal, @RequestParam(defaultValue = "") String q,
        @RequestParam(required = false) UUID disciplinaId, @RequestParam(defaultValue = "false") boolean minhas,
        @RequestParam(defaultValue = "recentes") String ordem, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return forum.listar(principal.getName(), q, disciplinaId, minhas, ordem, page, size);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public DiscussaoResponseDTO criar(Principal principal, @Valid @RequestBody DiscussaoRequestDTO dados) {
        return forum.criar(principal.getName(), dados);
    }
    @GetMapping("/{id}")
    public DiscussaoResponseDTO buscar(@PathVariable UUID id) { return forum.buscar(id); }
    @PutMapping("/{id}")
    public DiscussaoResponseDTO editar(Principal principal, @PathVariable UUID id, @Valid @RequestBody DiscussaoRequestDTO dados) {
        return forum.editar(principal.getName(), id, dados);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(Principal principal, @PathVariable UUID id) { forum.excluir(principal.getName(), id); }
    @GetMapping("/{id}/respostas")
    public Page<RespostaForumResponseDTO> respostas(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) { return forum.respostas(id, page, size); }
    @PostMapping("/{id}/respostas") @ResponseStatus(HttpStatus.CREATED)
    public RespostaForumResponseDTO responder(Principal principal, @PathVariable UUID id, @Valid @RequestBody RespostaForumRequestDTO dados) {
        return forum.responder(principal.getName(), id, dados);
    }
    @PutMapping("/{id}/respostas/{respostaId}")
    public RespostaForumResponseDTO editarResposta(Principal principal, @PathVariable UUID id, @PathVariable UUID respostaId,
        @Valid @RequestBody RespostaForumRequestDTO dados) { return forum.editarResposta(principal.getName(), id, respostaId, dados); }
    @DeleteMapping("/{id}/respostas/{respostaId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirResposta(Principal principal, @PathVariable UUID id, @PathVariable UUID respostaId) {
        forum.excluirResposta(principal.getName(), id, respostaId);
    }
}
