package br.wm.banco.academico.Controller;

import br.wm.banco.academico.Service.FotoPerfilService;
import br.wm.banco.academico.Service.ForumService;
import br.wm.banco.academico.Service.MaterialService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class FotoAutorController {
    private final MaterialService materiais;
    private final ForumService forum;
    private final FotoPerfilService fotos;
    private final JdbcTemplate jdbc;

    public FotoAutorController(MaterialService materiais, ForumService forum, FotoPerfilService fotos, JdbcTemplate jdbc) {
        this.materiais = materiais;
        this.forum = forum;
        this.fotos = fotos;
        this.jdbc = jdbc;
    }

    @GetMapping("/material/{id}/foto-autor")
    public ResponseEntity<byte[]> foto(@PathVariable UUID id) {
        var autor = materiais.buscarPorId(id).getAutor();
        return imagem(autor == null ? null : autor.getId());
    }

    @GetMapping("/material/{materialId}/comentarios/{comentarioId}/foto-autor")
    public ResponseEntity<byte[]> fotoComentario(@PathVariable UUID materialId, @PathVariable UUID comentarioId) {
        materiais.buscarPorId(materialId);
        var autores = jdbc.queryForList("SELECT usuario_id FROM comentarios_material WHERE id = ? AND material_id = ?",
                UUID.class, comentarioId, materialId);
        return imagem(autores.isEmpty() ? null : autores.getFirst());
    }

    @GetMapping("/material/{materialId}/comentarios/{comentarioId}/respostas/{respostaId}/foto-autor")
    public ResponseEntity<byte[]> fotoResposta(@PathVariable UUID materialId, @PathVariable UUID comentarioId,
            @PathVariable UUID respostaId) {
        materiais.buscarPorId(materialId);
        var autores = jdbc.queryForList("""
                SELECT r.usuario_id FROM respostas_comentario r
                JOIN comentarios_material c ON c.id = r.comentario_id
                WHERE r.id = ? AND c.id = ? AND c.material_id = ?
                """, UUID.class, respostaId, comentarioId, materialId);
        return imagem(autores.isEmpty() ? null : autores.getFirst());
    }

    @GetMapping("/forum/discussoes/{discussaoId}/foto-autor")
    public ResponseEntity<byte[]> fotoDiscussao(@PathVariable UUID discussaoId) {
        return imagem(forum.autorDiscussao(discussaoId));
    }

    @GetMapping("/forum/discussoes/{discussaoId}/respostas/{respostaId}/foto-autor")
    public ResponseEntity<byte[]> fotoRespostaForum(@PathVariable UUID discussaoId, @PathVariable UUID respostaId) {
        return imagem(forum.autorResposta(discussaoId, respostaId));
    }

    private ResponseEntity<byte[]> imagem(UUID autorId) {
        if (autorId == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .body(fotos.buscarPorUsuario(autorId));
    }
}
