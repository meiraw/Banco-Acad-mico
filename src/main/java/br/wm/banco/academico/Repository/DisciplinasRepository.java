package br.wm.banco.academico.Repository;

import br.wm.banco.academico.Model.DisciplinasModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DisciplinasRepository extends JpaRepository<  DisciplinasModel, UUID> {
}
