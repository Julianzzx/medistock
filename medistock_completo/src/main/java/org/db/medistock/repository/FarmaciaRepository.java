package org.db.medistock.repository;

import org.db.medistock.model.Farmacia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmaciaRepository extends JpaRepository<Farmacia, Long> {

    Page<Farmacia>findByNombreContainingIgnoreCase( String nombre, Pageable pageable);
    Page<Farmacia> findByEpsId(Long epsId, Pageable pageable);
    Page<Farmacia> findByEpsIdAndNombreContainingIgnoreCase(Long epsId, String nombre, Pageable pageable);
    Page<Farmacia> findByNombreContainingIgnoreCaseAndEpsId(String keyword, Long id, Pageable pageable);
}
