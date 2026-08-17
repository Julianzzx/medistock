package com.example.demo.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.Model.Eps;

@Repository
public interface EpsRepository extends JpaRepository<Eps, Long> {

    Eps findByNombre(String nombre);

    Page<Eps> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Optional<Eps> findByNombreIgnoreCase(String nombre);
}
