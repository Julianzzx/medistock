package com.example.demo.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.Model.Eps;
import com.example.demo.Model.Farmacia;

public interface FarmaciaRepository extends JpaRepository<Farmacia, Long> {

    Page<Farmacia> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Farmacia findByNombreIgnoreCase(String nombre);

    Page<Farmacia> findByNombreContainingIgnoreCaseAndEpsId(String nombre, Long epsId, Pageable pageable);

    Page<Farmacia> findByEpsId(Long epsId, Pageable pageable);

    List<Farmacia> findByEpsId(Long epsId);

    List<Farmacia> findByNombreContainingIgnoreCaseAndEpsId(String nombre, Long epsId);

    List<Farmacia> findByEps(Eps eps);

    Page<Farmacia> findByEpsIdAndNombreContainingIgnoreCase(Long epsId, String nombre, Pageable pageable);

}
