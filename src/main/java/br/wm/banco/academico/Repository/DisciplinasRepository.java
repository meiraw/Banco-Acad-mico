package br.wm.banco.academico.Repository;

import br.wm.banco.academico.Model.DisciplinasModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface DisciplinasRepository extends JpaRepository<  DisciplinasModel, UUID> {
}
