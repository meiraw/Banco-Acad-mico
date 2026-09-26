package br.wm.banco.academico.Repository;

import br.wm.banco.academico.Model.SemestreModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SemestreRepository extends JpaRepository<SemestreModel,UUID>{
}
