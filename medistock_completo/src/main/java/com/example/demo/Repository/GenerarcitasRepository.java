package com.example.demo.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.Model.Generarcitas;
import com.example.demo.Model.Usuario;

public interface GenerarcitasRepository extends JpaRepository<Generarcitas, Long> {
    Optional<Generarcitas> findByCodigo(String codigo);

    List<Generarcitas> findByUsuario(Usuario usuario);
}
