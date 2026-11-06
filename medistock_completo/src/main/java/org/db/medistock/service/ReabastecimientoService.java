package org.db.medistock.service;

import org.db.medistock.model.Medicamento;
import org.db.medistock.model.Reabastecimiento;
import org.db.medistock.repository.MedicamentoRepository;
import org.db.medistock.repository.ReabastecimientoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReabastecimientoService {

    private final ReabastecimientoRepository reabastecimientoRepository;
    private final MedicamentoRepository medicamentoRepository;

    public ReabastecimientoService(MedicamentoRepository medicamentoRepository, ReabastecimientoRepository reabastecimientoRepository) {
        this.medicamentoRepository = medicamentoRepository;
        this.reabastecimientoRepository = reabastecimientoRepository;
    }

    //Crear Solicitud De Reabastecimiento

    @Transactional
    public Reabastecimiento crearReabastecimiento(Medicamento medicamento, int cantidad){
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La Cantidad Debe Ser Mayor A Cero.");
        }
        Reabastecimiento reabastecimiento= new Reabastecimiento();
        reabastecimiento.setMedicamento(medicamento);
        reabastecimiento.setCantidad(cantidad);
        reabastecimiento.setEstado(Reabastecimiento.EstadoReabastecimiento.EN_PROCESO);

        return reabastecimientoRepository.save(reabastecimiento);
    }

    //Buscar Por Estado

    @Transactional(readOnly = true)
    public Page<Reabastecimiento> buscarPorEstado(Reabastecimiento.EstadoReabastecimiento estado, Pageable pageable) {
        return reabastecimientoRepository.findByEstado(estado, pageable);
    }

    //Aceptar Reabastecimiento

    @Transactional
    public Reabastecimiento aceptarReabastecimiento(Long id) {
       Reabastecimiento solicitud = reabastecimientoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        Medicamento medicamento = solicitud.getMedicamento();
        medicamento.setCantidad(medicamento.getCantidad() + solicitud.getCantidad());
        solicitud.setEstado(Reabastecimiento.EstadoReabastecimiento.ACEPTADA);

        medicamentoRepository.save(medicamento);
        return reabastecimientoRepository.save(solicitud);

    }

    // Rechazar Rebastecimiento}

    @Transactional
    public Reabastecimiento rechazarReabastecimiento(Long id) {
        Reabastecimiento reabastecimiento = reabastecimientoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        reabastecimiento.setEstado(Reabastecimiento.EstadoReabastecimiento.RECHAZADA);
        return reabastecimientoRepository.save(reabastecimiento);
    }
}
