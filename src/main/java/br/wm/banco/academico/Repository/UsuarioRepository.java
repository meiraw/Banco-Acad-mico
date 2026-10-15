package br.wm.banco.academico.Repository;

import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository  extends JpaRepository<UsuarioModel, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UsuarioModel u where u.id = :id")
    Optional<UsuarioModel> bloquearPorId(@org.springframework.data.repository.query.Param("id") UUID id);
    Optional<UsuarioModel> findByEmail(String email); // buscar um usuario pelo email com o findByEmail
}
