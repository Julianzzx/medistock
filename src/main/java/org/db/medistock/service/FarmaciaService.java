package org.db.medistock.service;


import org.db.medistock.model.Eps;
import org.db.medistock.model.Farmacia;
import org.db.medistock.repository.EpsRepository;
import org.db.medistock.repository.FarmaciaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmaciaService {

    private final FarmaciaRepository farmaciaRepository;
    private final EpsRepository epsRepository;

    public FarmaciaService(EpsRepository epsRepository, FarmaciaRepository farmaciaRepository) {
        this.epsRepository = epsRepository;
        this.farmaciaRepository = farmaciaRepository;
    }

    //Buscar Farmacia

    @Transactional(readOnly = true)
    public Farmacia buscarFarmacia(Long id){
        return farmaciaRepository.findById(id).orElseThrow(() -> new  RuntimeException("No Se Ha Encontrado Farmacia"));
    }

    //Buscar Por Nombre y Eps

    @Transactional(readOnly = true)
    public Page<Farmacia> buscarFarmaciasPorEps(Long epsId, String nombre, Pageable pageable) {
        if (nombre != null && !nombre.trim().isEmpty()) {
            return farmaciaRepository.findByEpsIdAndNombreContainingIgnoreCase(epsId, nombre.trim(), pageable);
        }
        return farmaciaRepository.findByEpsId(epsId, pageable);
    }

    //Mostrar Farmacia

    @Transactional(readOnly = true)
    public Page<Farmacia> mostrarFarmacias(Pageable pageable, String keyword) {

        if(keyword !=null && !keyword.isEmpty()){
            return farmaciaRepository.findByNombreContainingIgnoreCase(keyword, pageable);
        }
        else {
            return farmaciaRepository.findAll(pageable);
        }
    }

    //Guardar Farmacia

    @Transactional
    public Farmacia guardarFarmacia(Farmacia farmacia, Long epsId){
        Eps eps=epsRepository.findById(epsId).orElseThrow(()->new  RuntimeException("No Se Ha Encontrado Eps "));
        farmacia.setEps(eps);

        return farmaciaRepository.save(farmacia);
    }

    //Editar Farmacia

    @Transactional
    public Farmacia editarFarmacia(Long id, Farmacia farmacia, Long epsId){
        Farmacia farmaciaExistente = buscarFarmacia(id);
        Eps eps=epsRepository.findById(epsId).orElseThrow(()->new  RuntimeException("No Se Ha Encontrado Eps "));

        farmaciaExistente.setNombre(farmacia.getNombre());
        farmaciaExistente.setDireccion(farmacia.getDireccion());
        farmaciaExistente.setTelefono(farmacia.getTelefono());
        farmaciaExistente.setEps(eps);

        return farmaciaRepository.save(farmaciaExistente);
    }

    //Eliminar

    @Transactional
    public Boolean  eliminarFarmacia(Long id){
        Farmacia farmaciaExistente = buscarFarmacia(id);

        farmaciaRepository.delete(farmaciaExistente);
        return true;
    }
}
