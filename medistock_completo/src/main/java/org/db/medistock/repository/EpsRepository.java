package org.db.medistock.repository;

import org.db.medistock.model.Eps;
import org.db.medistock.model.Farmacia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpsRepository extends JpaRepository<Eps, Long> {

    Optional<Eps> findByNombreIgnoreCase(String nombre);
    Page<Eps> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
