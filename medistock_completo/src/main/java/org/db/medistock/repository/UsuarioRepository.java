package org.db.medistock.repository;

import org.db.medistock.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Page<Usuario> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrIdentificacionContainingIgnoreCase(String nombre, String apellido, String identificacion, Pageable pageable);
    Optional<Usuario> findByEmail(String email);
}
