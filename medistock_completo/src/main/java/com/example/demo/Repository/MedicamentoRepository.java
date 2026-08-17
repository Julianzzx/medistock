package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Medicamento;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

  Page<Medicamento> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

  Page<Medicamento> findByFarmaciaId(Long farmaciaId, Pageable pageable);

  Page<Medicamento> findByNombreContainingIgnoreCaseAndFarmaciaId(String nombre, Long farmaciaId, Pageable pageable);

  List<Medicamento> findByNombreContainingIgnoreCase(String nombre);

  List<Medicamento> findByFarmaciaId(Long farmaciaId);

  List<Medicamento> findByFarmaciaIdAndNombreContainingIgnoreCase(Long farmaciaId, String nombre);

  Page<Medicamento> findByFarmaciaIdAndNombreContainingIgnoreCase(Long farmaciaId, String nombre, Pageable pageable);

  boolean existsByNombreIgnoreCase(String nombre);

  boolean existsByNombreIgnoreCaseAndFarmacia(String nombre, Farmacia farmacia);

  Page<Medicamento> findByFarmacia(Farmacia farmacia, Pageable pageable);

  Page<Medicamento> findByFarmaciaAndNombreContainingIgnoreCase(Farmacia farmacia, String nombre, Pageable pageable);

  Page<Medicamento> findByFarmaciaIn(List<Farmacia> farmacias, Pageable pageable);

  Page<Medicamento> findByFarmaciaInAndNombreContainingIgnoreCase(List<Farmacia> farmacias, String nombre,
      Pageable pageable);

  @Query("""
          SELECT m FROM Medicamento m
          WHERE m.farmacia = :farmacia
            AND (:nombre IS NULL OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
            AND (:laboratorio IS NULL OR LOWER(m.laboratorio) = LOWER(:laboratorio))
            AND (:concentracion IS NULL OR LOWER(m.concentracion) = LOWER(:concentracion))
            AND (:presentacion IS NULL OR LOWER(m.presentacion) = LOWER(:presentacion))
            AND (:descripcion IS NULL OR LOWER(m.descripcion) LIKE LOWER(CONCAT('%', :descripcion, '%')))
      """)
  Page<Medicamento> findByFarmaciaAndFiltros(
      @Param("farmacia") Farmacia farmacia,
      @Param("nombre") String nombre,
      @Param("laboratorio") String laboratorio,
      @Param("concentracion") String concentracion,
      @Param("presentacion") String presentacion,
      @Param("descripcion") String descripcion,
      Pageable pageable);

  @Query("SELECT DISTINCT m.laboratorio FROM Medicamento m WHERE m.farmacia = :farmacia AND m.laboratorio IS NOT NULL")
  List<String> findDistinctLaboratoriosByFarmacia(@Param("farmacia") Farmacia farmacia);

  @Query("SELECT DISTINCT m.concentracion FROM Medicamento m WHERE m.farmacia = :farmacia AND m.concentracion IS NOT NULL")
  List<String> findDistinctConcentracionesByFarmacia(@Param("farmacia") Farmacia farmacia);

  @Query("SELECT DISTINCT m.presentacion FROM Medicamento m WHERE m.farmacia = :farmacia AND m.presentacion IS NOT NULL")
  List<String> findDistinctPresentacionesByFarmacia(@Param("farmacia") Farmacia farmacia);

  @Query("SELECT DISTINCT m.descripcion FROM Medicamento m WHERE m.farmacia = :farmacia AND m.descripcion IS NOT NULL")
  List<String> findDistinctDescripcionesByFarmacia(@Param("farmacia") Farmacia farmacia);
}
