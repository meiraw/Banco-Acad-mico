package br.wm.banco.academico.Repository;

import br.wm.banco.academico.Model.MaterialModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MaterialRepository extends JpaRepository<MaterialModel, UUID> {
}
