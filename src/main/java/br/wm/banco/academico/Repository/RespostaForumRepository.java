package br.wm.banco.academico.Repository;
import br.wm.banco.academico.Model.RespostaForumModel;
import br.wm.banco.academico.DTOs.Response.RespostaForumResponseDTO;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;

public interface RespostaForumRepository extends JpaRepository<RespostaForumModel, UUID> {
    @Query("""
        select new br.wm.banco.academico.DTOs.Response.RespostaForumResponseDTO(
            r.id, r.discussao.id, a.id, coalesce(a.nome, 'Conta excluída'),
            r.conteudo, r.criadoEm, r.atualizadoEm, r.versao)
        from RespostaForumModel r left join r.autor a
        where r.discussao.id = :discussao and r.discussao.excluidoEm is null and r.excluidoEm is null
        """)
    Page<RespostaForumResponseDTO> listar(UUID discussao, Pageable pagina);
    long countByDiscussaoIdAndExcluidoEmIsNull(UUID id);
    Optional<RespostaForumModel> findByIdAndDiscussaoIdAndExcluidoEmIsNull(UUID id, UUID discussaoId);
}
