package org.db.medistock.repository;

import org.db.medistock.model.Farmacia;
import org.db.medistock.model.Medicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    Page<Medicamento> findByFarmaciaId(Long id, Pageable pageable);
    Page<Medicamento> findByFarmaciaIdAndNombreContainingIgnoreCase(Long farmaciaId, String nombre, Pageable pageable);
    Page<Medicamento> findByFarmacia(Farmacia farmacia, Pageable pageable);
    List<Medicamento> findByFarmacia(Farmacia farmacia);
    List<Medicamento> findByFarmaciaId(Long farmaciaId);

    @Query("SELECT m FROM Medicamento m WHERE m.farmacia = :farmacia " +
            "AND (:nombre IS NULL OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
            "AND (:laboratorio IS NULL OR LOWER(m.laboratorio) LIKE LOWER(CONCAT('%', :laboratorio, '%'))) " +
            "AND (:concentracion IS NULL OR LOWER(m.concentracion) LIKE LOWER(CONCAT('%', :concentracion, '%'))) " +
            "AND (:presentacion IS NULL OR LOWER(m.presentacion) LIKE LOWER(CONCAT('%', :presentacion, '%'))) " +
            "AND (:descripcion IS NULL OR LOWER(m.descripcion) LIKE LOWER(CONCAT('%', :descripcion, '%')))")

    Page<Medicamento> findByFarmaciaAndFiltros(
            @Param("farmacia") Farmacia farmacia,
            @Param("nombre") String nombre,
            @Param("laboratorio") String laboratorio,
            @Param("concentracion") String concentracion,
            @Param("presentacion") String presentacion,
            @Param("descripcion") String descripcion,
            Pageable pageable
    );

    @Query("SELECT DISTINCT m.laboratorio FROM Medicamento m WHERE m.farmacia = :farmacia")
    List<String> findDistinctLaboratoriosByFarmacia(@Param("farmacia") Farmacia farmacia);

    @Query("SELECT DISTINCT m.concentracion FROM Medicamento m WHERE m.farmacia = :farmacia")
    List<String> findDistinctConcentracionesByFarmacia(@Param("farmacia") Farmacia farmacia);

    @Query("SELECT DISTINCT m.presentacion FROM Medicamento m WHERE m.farmacia = :farmacia")
    List<String> findDistinctPresentacionesByFarmacia(@Param("farmacia") Farmacia farmacia);

    @Query("SELECT DISTINCT m.descripcion FROM Medicamento m WHERE m.farmacia = :farmacia")
    List<String> findDistinctDescripcionesByFarmacia(@Param("farmacia") Farmacia farmacia);
}
