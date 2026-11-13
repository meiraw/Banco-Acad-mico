package br.wm.banco.academico.Repository;

import br.wm.banco.academico.Model.MaterialModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
@Repository
public interface MaterialRepository extends JpaRepository<MaterialModel, UUID> {
    @org.springframework.data.jpa.repository.Query(value = """
        SELECT m.* FROM materiais m JOIN favoritos f ON f.material_id = m.id
        WHERE f.usuario_id = :usuarioId AND m.status_moderacao = 'APROVADO'
        ORDER BY f.criado_em DESC, f.material_id
        """, countQuery = """
        SELECT count(*) FROM materiais m JOIN favoritos f ON f.material_id = m.id
        WHERE f.usuario_id = :usuarioId AND m.status_moderacao = 'APROVADO'
        """, nativeQuery = true)
    Page<MaterialModel> favoritosDoUsuario(@org.springframework.data.repository.query.Param("usuarioId") UUID usuarioId, Pageable pagina);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(coalesce(m.tamanhoArquivo, 20971520)), 0) from MaterialModel m where m.autor.id = :id")
    long bytesDoAutor(@org.springframework.data.repository.query.Param("id") UUID id);
    Page<MaterialModel> findByStatusModeracao(br.wm.banco.academico.Enum.StatusModeracao status, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from MaterialModel m where m.id = :id")
    java.util.Optional<MaterialModel> bloquearPorId(@org.springframework.data.repository.query.Param("id") UUID id);

    Page<MaterialModel> findByAutorEmailOrderByPublicadoEmDescIdDesc(String email, Pageable pageable);
}
