package org.db.medistock.repository;

import org.db.medistock.model.Reabastecimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReabastecimientoRepository extends JpaRepository<Reabastecimiento, Long> {

    List<Reabastecimiento> findByEstado(Reabastecimiento.EstadoReabastecimiento estado);
    Page<Reabastecimiento>findByEstado(Reabastecimiento.EstadoReabastecimiento estado, Pageable pageable);
}
