package com.example.demo.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.demo.Model.EstadoSolicitud;
import com.example.demo.Model.Solicitud;
import com.example.demo.Model.Usuario;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    List<Solicitud> findByUsuario(Usuario usuario);

    Page<Solicitud> findByUsuario(Usuario usuario, Pageable pageable);

    List<Solicitud> findByEstado(EstadoSolicitud estado);

    Page<Solicitud> findByEstado(EstadoSolicitud estado, Pageable pageable);

    long countByEstado(EstadoSolicitud estado);

    Page<Solicitud> findByEstadoAndMedicamentoFarmaciaEpsId(EstadoSolicitud estado, Long epsId, Pageable pageable);

    boolean existsByUsuarioIdAndEstado(Long usuarioId, EstadoSolicitud estado);

    long countByEstadoAndMedicamentoFarmaciaEpsId(EstadoSolicitud estado, Long epsId);

    boolean existsByUsuarioIdAndGenerarcitasId(Long usuarioId, Long generarcitasId);

    boolean existsByUsuario(Usuario usuario);

    Page<Solicitud> findByEstadoAndMedicamentoFarmaciaId(
            EstadoSolicitud estado,
            Long farmaciaId,
            Pageable pageable);

    Page<Solicitud> findByEstadoAndMedicamentoFarmaciaEpsIdAndUsuarioNombreContainingIgnoreCaseOrMedicamentoNombreContainingIgnoreCase(
            EstadoSolicitud estado, Long epsId, String usuarioNombre, String medicamentoNombre,
            Pageable pageable);

    List<Solicitud> findByEstadoAndMedicamentoFarmaciaEpsId(EstadoSolicitud estado, Long epsId);

    Page<Solicitud> findByUsuarioAndMedicamento_NombreContainingIgnoreCase(Usuario usuario, String nombre,
            Pageable pageable);

    Page<Solicitud> findByUsuarioAndEstado(Usuario usuario, EstadoSolicitud estado, Pageable pageable);

    @Query("""
            SELECT s FROM Solicitud s
            WHERE s.estado = :estado
            AND s.medicamento.farmacia.eps.id = :epsId
            AND (
                LOWER(s.usuario.nombre) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.usuario.apellido) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(CONCAT(s.usuario.nombre, ' ', s.usuario.apellido)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.medicamento.nombre) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Solicitud> buscarPorNombreApellidoCompletoYMedicamento(
            EstadoSolicitud estado,
            Long epsId,
            String keyword,
            Pageable pageable);
}
