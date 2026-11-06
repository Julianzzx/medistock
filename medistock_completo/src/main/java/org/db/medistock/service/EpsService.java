package org.db.medistock.service;

import org.db.medistock.model.Eps;
import org.db.medistock.model.Farmacia;
import org.db.medistock.repository.EpsRepository;
import org.db.medistock.repository.FarmaciaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class EpsService {

    private final EpsRepository epsRepository;
    private final FarmaciaRepository farmaciaRepository;

    public EpsService(EpsRepository epsRepository, FarmaciaRepository farmaciaRepo) {
        this.epsRepository = epsRepository;
        this.farmaciaRepository = farmaciaRepo;
    }

    // Mostrar Eps

    @Transactional(readOnly = true)
    public List<Eps> listarTodasEps() {
        return epsRepository.findAll();
    }

    // Buscar Eps

    @Transactional(readOnly = true)
    public Page<Eps> listarEpsPaginadas(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return epsRepository.findByNombreContainingIgnoreCase(keyword, pageable);
        }
        return epsRepository.findAll(pageable);
    }

    // Buscar Eps Por ID

    @Transactional(readOnly = true)
    public Eps buscarEpsPorId(Long id) {
        return epsRepository.findById(id).orElse(null);
    }

    // Guardar o actualizar EPS

    @Transactional
    public Eps guardarEps(Eps eps) {
        return epsRepository.save(eps);
    }

    // Eliminar EPS

    @Transactional
    public boolean eliminarEps(Long id) {
        if (!epsRepository.existsById(id)) {
            return false;
        }
        epsRepository.deleteById(id);
        return true;
    }

    // Buscar EPS Por Nombre Exacto

    @Transactional(readOnly = true)
    public Optional<Eps> buscarPorNombre(String nombre) {
        return epsRepository.findByNombreIgnoreCase(nombre);
    }

    // Listar Farmacia De Una EPS Específica

    @Transactional(readOnly = true)
    public Page<Farmacia> listarFarmaciasPorEpsNombre(String nombreEps, String keyword, Pageable pageable) {
        Optional<Eps> epsOpt = epsRepository.findByNombreIgnoreCase(nombreEps);
        if (epsOpt.isEmpty()) {
            return Page.empty();
        }

        Eps eps = epsOpt.get();
        if (keyword != null && !keyword.isEmpty()) {
            return farmaciaRepository.findByNombreContainingIgnoreCaseAndEpsId(keyword, eps.getId(), pageable);
        }
        return farmaciaRepository.findByEpsId(eps.getId(), pageable);
    }
}