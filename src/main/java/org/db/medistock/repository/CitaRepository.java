package org.db.medistock.repository;

import org.db.medistock.model.Cita;
import org.db.medistock.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE Cita c SET c.usuario = null WHERE c.usuario.id = :usuarioId")
    int desvincularUsuario(@Param("usuarioId") Long usuarioId);

    List<Cita> findByUsuario(Usuario usuario);
    Optional<Cita> findByCodigo(String codigo);
}
