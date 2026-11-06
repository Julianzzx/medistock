package org.db.medistock.repository;

import org.db.medistock.model.Solicitud;
import org.db.medistock.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    Page<Solicitud> findByUsuario(Usuario usuario, Pageable pageable);
    List<Solicitud> findByUsuario(Usuario usuario);
    Page<Solicitud> findByUsuarioAndMedicamento_NombreContainingIgnoreCase(Usuario usuario, String keyword, Pageable pageable);
    Page<Solicitud> findByUsuarioAndEstado(Usuario usuario, Solicitud.EstadoSolicitud estado, Pageable pageable);
    Page<Solicitud> findByUsuarioAndMedicamento_NombreContainingIgnoreCaseAndEstado(Usuario usuario, String keyword, Solicitud.EstadoSolicitud estado, Pageable pageable);
    Page<Solicitud> findByEstadoAndMedicamentoFarmaciaEpsId(Solicitud.EstadoSolicitud estado, Long epsId, Pageable pageable);
    List<Solicitud> findByEstadoAndMedicamentoFarmaciaEpsId(Solicitud.EstadoSolicitud estado, Long epsId);

    @Query("SELECT s FROM Solicitud s WHERE s.estado = :estado AND s.medicamento.farmacia.eps.id = :epsId AND " +
            "(LOWER(s.usuario.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.usuario.apellido) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.medicamento.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')))")

    Page<Solicitud> buscarPorNombreApellidoCompletoYMedicamento(
            @Param("estado") Solicitud.EstadoSolicitud estado,
            @Param("epsId") Long epsId,
            @Param("keyword") String keyword,
            Pageable pageable);
}