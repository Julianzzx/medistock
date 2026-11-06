package org.db.medistock.service;

import org.db.medistock.model.Farmacia;
import org.db.medistock.repository.FarmaciaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.db.medistock.model.Medicamento;
import org.db.medistock.repository.MedicamentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;
    private final FarmaciaRepository farmaciaRepository;

    public MedicamentoService(FarmaciaRepository farmaciaRepository, MedicamentoRepository medicamentoRepository) {
        this.farmaciaRepository = farmaciaRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    //Buscar Medicamento

    @Transactional(readOnly = true)
    public Medicamento buscarMedicamento(Long id){
        return medicamentoRepository.findById(id).orElseThrow(() -> new  RuntimeException("No Se Ha Encontrado Medicamento"));
    }

    //Guardar Medicamento

    @Transactional
    public Medicamento guardarMedicamento(Medicamento medicamento){
        if ((medicamento.getCantidad()<0)){
            throw new RuntimeException("Medicamento no puede ser negativo");
        }
        return medicamentoRepository.save(medicamento);
    }

    //Editar Medicamento

    @Transactional
    public Medicamento editarMedicamento(Long id, Medicamento medicamento){
        Medicamento medicamentoExistente= buscarMedicamento(id);

        medicamentoExistente.setNombre(medicamento.getNombre());
        medicamentoExistente.setCantidad(medicamento.getCantidad());
        medicamentoExistente.setConcentracion(medicamento.getConcentracion());
        medicamentoExistente.setDescripcion(medicamento.getDescripcion());
        medicamentoExistente.setLaboratorio(medicamento.getLaboratorio());
        medicamentoExistente.setPresentacion(medicamento.getPresentacion());

        return medicamentoRepository.save(medicamentoExistente);
    }

    //Eliminar Medicamento

    @Transactional
    public Boolean eliminarMedicamento(Long id) {
        Medicamento medicamentoExistente = buscarMedicamento(id);

        medicamentoRepository.delete(medicamentoExistente);
        return true;
    }

    //Buscar Medicamentos Por Farmacia

    @Transactional(readOnly = true)
    public Farmacia buscarFarmacia(Long id){
        return farmaciaRepository.findById(id).orElseThrow(() -> new  RuntimeException("No Se Ha Encontrado Farmacia"));
    }

    // Busqueda De Medicamentos  Por Keyword

    @Transactional(readOnly = true)
    public Page<Medicamento> medicamentosPorFarmacia(Long id, String keyword, Pageable pageable){
        Farmacia farmacia = buscarFarmacia(id);

        if(keyword != null && !keyword.isEmpty()){
            return medicamentoRepository.findByFarmaciaIdAndNombreContainingIgnoreCase(id, keyword, pageable);
        }
        else{
            return medicamentoRepository.findByFarmaciaId(id, pageable);
        }
    }

    // Búsqueda avanzada con múltiples filtros

    @Transactional(readOnly = true)
    public Page<Medicamento> buscarMedicamentosConFiltros(
            Farmacia farmacia, String nombre, String laboratorio,
            String concentracion, String presentacion, String descripcion, Pageable pageable) {

        return medicamentoRepository.findByFarmaciaAndFiltros(
                farmacia,
                (nombre != null && !nombre.isEmpty()) ? nombre : null,
                (laboratorio != null && !laboratorio.isEmpty()) ? laboratorio : null,
                (concentracion != null && !concentracion.isEmpty()) ? concentracion : null,
                (presentacion != null && !presentacion.isEmpty()) ? presentacion : null,
                (descripcion != null && !descripcion.isEmpty()) ? descripcion : null,
                pageable
        );
    }

    // Buscar Medicamentos Por Filtros

    @Transactional(readOnly = true)
    public List<String> obtenerLaboratoriosPorFarmacia(Farmacia farmacia) {
        return medicamentoRepository.findDistinctLaboratoriosByFarmacia(farmacia);
    }

    @Transactional(readOnly = true)
    public List<String> obtenerConcentracionesPorFarmacia(Farmacia farmacia) {
        return medicamentoRepository.findDistinctConcentracionesByFarmacia(farmacia);
    }

    @Transactional(readOnly = true)
    public List<String> obtenerPresentacionesPorFarmacia(Farmacia farmacia) {
        return medicamentoRepository.findDistinctPresentacionesByFarmacia(farmacia);
    }

    @Transactional(readOnly = true)
    public List<String> obtenerDescripcionesPorFarmacia(Farmacia farmacia) {
        return medicamentoRepository.findDistinctDescripcionesByFarmacia(farmacia);
    }
}
