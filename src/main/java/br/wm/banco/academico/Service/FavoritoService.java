package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Response.MaterialResponseDTO;
import br.wm.banco.academico.Enum.StatusModeracao;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Repository.FavoritoRepository;
import br.wm.banco.academico.Repository.MaterialRepository;
import br.wm.banco.academico.Repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class FavoritoService {
    private final FavoritoRepository favoritos;
    private final MaterialRepository materiais;
    private final UsuarioRepository usuarios;
    public FavoritoService(FavoritoRepository favoritos, MaterialRepository materiais, UsuarioRepository usuarios) {
        this.favoritos = favoritos; this.materiais = materiais; this.usuarios = usuarios;
    }
    private UUID usuario(String email) {
        return usuarios.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).getId();
    }
    private PageRequest pagina(int numero, int tamanho) {
        if (numero < 0 || tamanho < 1 || tamanho > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginação inválida. Use de 1 a 200 itens por página.");
        return PageRequest.of(numero, tamanho);
    }
    @Transactional(readOnly = true)
    public Page<MaterialResponseDTO> listar(String email, int numero, int tamanho) {
        return materiais.favoritosDoUsuario(usuario(email), pagina(numero, tamanho)).map(MaterialResponseDTO::new);
    }
    @Transactional(readOnly = true)
    public Page<UUID> ids(String email, int numero, int tamanho) {
        return favoritos.listarIds(usuario(email), pagina(numero, tamanho));
    }
    @Transactional
    public void adicionar(String email, UUID materialId) {
        UUID usuarioId = usuario(email);
        // Mesmo bloqueio utilizado para exclusão/moderação, antes de verificar o status.
        var material = materiais.bloquearPorId(materialId)
            .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado."));
        if (material.getStatusModeracao() != StatusModeracao.APROVADO)
            throw new ResourceNotFoundException("Material não encontrado.");
        favoritos.adicionar(usuarioId, materialId);
    }
    @Transactional
    public void remover(String email, UUID materialId) {
        // A remoção não revela se o material existe ou se está visível.
        favoritos.remover(usuario(email), materialId);
    }
}
