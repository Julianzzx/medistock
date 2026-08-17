package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.Model.EstadoSolicitud;
import com.example.demo.Model.SolicitudReabastecimiento;

public interface SolicitudReabastecimientoRepository extends JpaRepository<SolicitudReabastecimiento, Long> {

    List<SolicitudReabastecimiento> findByEstado(EstadoSolicitud estado);

    long countByEstado(EstadoSolicitud estado);

    Page<SolicitudReabastecimiento> findByEstado(EstadoSolicitud estado, Pageable pageable);

    Page<SolicitudReabastecimiento> findByEstadoAndMedicamento_Farmacia_Id(
            EstadoSolicitud estado, Long farmaciaId, Pageable pageable);
}
