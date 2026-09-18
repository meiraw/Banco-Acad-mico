package br.wm.banco.academico.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MaterialRepository extends JpaRepository< MaterialRepository, UUID> {
}
