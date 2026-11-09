package br.wm.banco.academico.Repository;

import br.wm.banco.academico.Model.MaterialModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
@Repository
public interface MaterialRepository extends JpaRepository<MaterialModel, UUID> {
    Page<MaterialModel> findByStatusModeracao(br.wm.banco.academico.Enum.StatusModeracao status, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from MaterialModel m where m.id = :id")
    java.util.Optional<MaterialModel> bloquearPorId(@org.springframework.data.repository.query.Param("id") UUID id);

    Page<MaterialModel> findByAutorEmailOrderByPublicadoEmDescIdDesc(String email, Pageable pageable);
}
