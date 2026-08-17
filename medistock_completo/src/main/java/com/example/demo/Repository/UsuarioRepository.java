package com.example.demo.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.Model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Page<Usuario> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrIdentificacionContainingIgnoreCase(
            String nombre, String apellido, String identificacion, Pageable pageable);

    List<Usuario> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(String nombre, String apellido);

    Usuario findByIdentificacion(String identificacion);

    Usuario findByCorreo(String correo);

    Optional<Usuario> findOptionalByCorreo(String correo);

    Optional<Usuario> findOptionalByIdentificacion(String identificacion);

}
