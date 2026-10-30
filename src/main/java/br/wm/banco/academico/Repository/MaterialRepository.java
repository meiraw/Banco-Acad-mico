package br.wm.banco.academico.Repository;

import br.wm.banco.academico.Model.MaterialModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
@Repository
public interface MaterialRepository extends JpaRepository<MaterialModel, UUID> {
    Page<MaterialModel> findByAutorEmailOrderByPublicadoEmDescIdDesc(String email, Pageable pageable);
}
