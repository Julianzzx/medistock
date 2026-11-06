package org.db.medistock.repository;

import org.db.medistock.model.Cita;
import org.db.medistock.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByUsuario(Usuario usuario);
    Optional<Cita> findByCodigo(String codigo);
}
