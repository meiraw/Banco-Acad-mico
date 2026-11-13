package br.wm.banco.academico.Repository;
import br.wm.banco.academico.Model.DiscussaoModel;
import br.wm.banco.academico.DTOs.Response.DiscussaoResumoDTO;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface DiscussaoRepository extends JpaRepository<DiscussaoModel, UUID> {
    @Query(value = """
        select new br.wm.banco.academico.DTOs.Response.DiscussaoResumoDTO(
            d.id, d.titulo, substring(d.conteudo, 1, 240), disc.id, disc.nome,
            a.id, coalesce(a.nome, 'Conta excluída'), d.criadoEm,
            (select count(r.id) from RespostaForumModel r where r.discussao = d and r.excluidoEm is null))
        from DiscussaoModel d join d.disciplina disc left join d.autor a
        where d.excluidoEm is null and (:disciplina is null or disc.id = :disciplina)
        and (:autor is null or a.id = :autor)
        and (lower(d.titulo) like :busca escape '!' or lower(d.conteudo) like :busca escape '!'
            or lower(disc.nome) like :busca escape '!')
        """, countQuery = """
        select count(d.id) from DiscussaoModel d join d.disciplina disc left join d.autor a
        where d.excluidoEm is null and (:disciplina is null or disc.id = :disciplina)
        and (:autor is null or a.id = :autor)
        and (lower(d.titulo) like :busca escape '!' or lower(d.conteudo) like :busca escape '!'
            or lower(disc.nome) like :busca escape '!')
        """)
    Page<DiscussaoResumoDTO> pesquisar(UUID disciplina, UUID autor, String busca, Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DiscussaoModel d where d.id = :id and d.excluidoEm is null")
    Optional<DiscussaoModel> bloquear(UUID id);
    Optional<DiscussaoModel> findByIdAndExcluidoEmIsNull(UUID id);
    boolean existsByDisciplinaId(UUID id);
}
