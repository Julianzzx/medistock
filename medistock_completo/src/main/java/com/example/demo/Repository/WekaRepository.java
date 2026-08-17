package com.example.demo.Repository;

import com.example.demo.Model.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WekaRepository extends JpaRepository<Solicitud, Long> {

    @Query(value = "SELECT " +
            "s.cantidad AS cantidad_sol, " +
            "COALESCE(m.nombre_agrupado, m.nombre, 'Otro_med') AS nombre_agrupado, " +
            "COALESCE(m.laboratorio, 'Genfar') AS laboratorio, " +
            "COALESCE(m.presentacion, 'Tabletas') AS presentacion, " +
            "m.cantidad AS cantidad_inv, " +
            "COALESCE(m.demanda_global, 0) AS demanda_global, " +
            "COALESCE(u.edad, 0) AS edad, " +
            "COALESCE(u.genero, 'No_especificado') AS genero, " +
            "COALESCE(u.regimen, 'No_especificado') AS regimen, " +
            "COALESCE(u.eps_id, 1) AS eps_id_usr, " +
            "COALESCE(f.id, 1) AS farmacia_id, " +
            "COALESCE(f.eps_id, 1) AS eps_id_farm " +
            "FROM solicitud s " +
            "JOIN medicamento m ON s.medicamento_id = m.id " +
            "JOIN usuario u ON s.usuario_id = u.id " +
            "JOIN farmacia f ON m.farmacia_id = f.id " +
            "WHERE s.estado = 'ACEPTADA' " +
            "ORDER BY s.fecha_solicitud DESC",
            countQuery = "SELECT COUNT(*) FROM solicitud WHERE estado = 'ACEPTADA'",
            nativeQuery = true)
    Page<Object[]> obtenerDatosParaPrediccionPage(Pageable pageable);

    /**
     * Obtiene los datos de la solicitud más reciente aceptada que coincida
     * con los filtros de presentación y/o laboratorio proporcionados.
     * Si un filtro es nulo o vacío, no se aplica ese filtro.
     */
    @Query(value = "SELECT " +
            "s.cantidad AS cantidad_sol, " +
            "COALESCE(m.nombre_agrupado, m.nombre, 'Otro_med') AS nombre_agrupado, " +
            "COALESCE(m.laboratorio, 'Genfar') AS laboratorio, " +
            "COALESCE(m.presentacion, 'Tabletas') AS presentacion, " +
            "m.cantidad AS cantidad_inv, " +
            "COALESCE(m.demanda_global, 0) AS demanda_global, " +
            "COALESCE(u.edad, 0) AS edad, " +
            "COALESCE(u.genero, 'No_especificado') AS genero, " +
            "COALESCE(u.regimen, 'No_especificado') AS regimen, " +
            "COALESCE(u.eps_id, 1) AS eps_id_usr, " +
            "COALESCE(f.id, 1) AS farmacia_id, " +
            "COALESCE(f.eps_id, 1) AS eps_id_farm " +
            "FROM solicitud s " +
            "JOIN medicamento m ON s.medicamento_id = m.id " +
            "JOIN usuario u ON s.usuario_id = u.id " +
            "JOIN farmacia f ON m.farmacia_id = f.id " +
            "WHERE s.estado = 'ACEPTADA' " +
            "AND (:presentacion IS NULL OR :presentacion = '' OR m.presentacion = :presentacion) " +
            "AND (:laboratorio IS NULL OR :laboratorio = '' OR m.laboratorio = :laboratorio) " +
            "ORDER BY s.fecha_solicitud DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Object[] obtenerDatosConFiltros(@Param("presentacion") String presentacion,
                                    @Param("laboratorio") String laboratorio);

    /** Top 10 histórico filtrado por presentación y/o laboratorio */
    @Query(value = "SELECT " +
            "COALESCE(m.nombre_agrupado, m.nombre) AS medicamento, " +
            "COUNT(*) AS total_solicitudes, " +
            "SUM(s.cantidad) AS total_unidades " +
            "FROM solicitud s " +
            "JOIN medicamento m ON s.medicamento_id = m.id " +
            "WHERE s.estado = 'ACEPTADA' " +
            "AND (:presentacion IS NULL OR :presentacion = '' OR m.presentacion = :presentacion) " +
            "AND (:laboratorio IS NULL OR :laboratorio = '' OR m.laboratorio = :laboratorio) " +
            "GROUP BY COALESCE(m.nombre_agrupado, m.nombre) " +
            "ORDER BY total_solicitudes DESC " +
            "LIMIT 10",
            nativeQuery = true)
    List<Object[]> topHistoricoFiltrado(@Param("presentacion") String presentacion,
                                        @Param("laboratorio") String laboratorio);

    /** Top 10 del mes actual filtrado por presentación y/o laboratorio */
    @Query(value = "SELECT " +
            "COALESCE(m.nombre_agrupado, m.nombre) AS medicamento, " +
            "COUNT(*) AS total_solicitudes, " +
            "SUM(s.cantidad) AS total_unidades " +
            "FROM solicitud s " +
            "JOIN medicamento m ON s.medicamento_id = m.id " +
            "WHERE s.estado = 'ACEPTADA' " +
            "AND MONTH(s.fecha_solicitud) = :mes " +
            "AND YEAR(s.fecha_solicitud) = :anio " +
            "AND (:presentacion IS NULL OR :presentacion = '' OR m.presentacion = :presentacion) " +
            "AND (:laboratorio IS NULL OR :laboratorio = '' OR m.laboratorio = :laboratorio) " +
            "GROUP BY COALESCE(m.nombre_agrupado, m.nombre) " +
            "ORDER BY total_solicitudes DESC " +
            "LIMIT 10",
            nativeQuery = true)
    List<Object[]> topDelMesFiltrado(@Param("mes") int mes,
                                     @Param("anio") int anio,
                                     @Param("presentacion") String presentacion,
                                     @Param("laboratorio") String laboratorio);

    /** Top 10 histórico — todos los tiempos (sin filtros) */
    @Query(value = "SELECT " +
            "COALESCE(m.nombre_agrupado, m.nombre) AS medicamento, " +
            "COUNT(*) AS total_solicitudes, " +
            "SUM(s.cantidad) AS total_unidades " +
            "FROM solicitud s " +
            "JOIN medicamento m ON s.medicamento_id = m.id " +
            "WHERE s.estado = 'ACEPTADA' " +
            "GROUP BY COALESCE(m.nombre_agrupado, m.nombre) " +
            "ORDER BY total_solicitudes DESC " +
            "LIMIT 10",
            nativeQuery = true)
    List<Object[]> topHistorico();

    /** Top 10 del mes actual (sin filtros) */
    @Query(value = "SELECT " +
            "COALESCE(m.nombre_agrupado, m.nombre) AS medicamento, " +
            "COUNT(*) AS total_solicitudes, " +
            "SUM(s.cantidad) AS total_unidades " +
            "FROM solicitud s " +
            "JOIN medicamento m ON s.medicamento_id = m.id " +
            "WHERE s.estado = 'ACEPTADA' " +
            "AND MONTH(s.fecha_solicitud) = :mes " +
            "AND YEAR(s.fecha_solicitud) = :anio " +
            "GROUP BY COALESCE(m.nombre_agrupado, m.nombre) " +
            "ORDER BY total_solicitudes DESC " +
            "LIMIT 10",
            nativeQuery = true)
    List<Object[]> topDelMes(@Param("mes") int mes, @Param("anio") int anio);

    /** Medicamento #1 real (histórico) */
    @Query(value = "SELECT " +
            "COALESCE(m.nombre_agrupado, m.nombre) AS medicamento, " +
            "COUNT(*) AS total " +
            "FROM solicitud s " +
            "JOIN medicamento m ON s.medicamento_id = m.id " +
            "WHERE s.estado = 'ACEPTADA' " +
            "GROUP BY COALESCE(m.nombre_agrupado, m.nombre) " +
            "ORDER BY total DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Object[] medicamentoMasSolicitadoReal();

    @Query(value = "SELECT COUNT(*) FROM solicitud WHERE estado = 'ACEPTADA'",
            nativeQuery = true)
    Long contarSolicitudesAceptadas();

    /** Obtener presentaciones únicas de medicamentos en BD */
    @Query(value = "SELECT DISTINCT m.presentacion " +
            "FROM medicamento m " +
            "WHERE m.presentacion IS NOT NULL AND m.presentacion <> '' " +
            "ORDER BY m.presentacion",
            nativeQuery = true)
    List<String> obtenerPresentacionesUnicas();

    /** Obtener laboratorios únicos de medicamentos en BD */
    @Query(value = "SELECT DISTINCT m.laboratorio " +
            "FROM medicamento m " +
            "WHERE m.laboratorio IS NOT NULL AND m.laboratorio <> '' " +
            "ORDER BY m.laboratorio",
            nativeQuery = true)
    List<String> obtenerLaboratoriosUnicos();
}
