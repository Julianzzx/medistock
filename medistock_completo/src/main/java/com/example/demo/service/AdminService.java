package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.example.demo.Model.Admin;
import com.example.demo.Model.EstadoSolicitud;
import com.example.demo.Model.Eps;
import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Medicamento;
import com.example.demo.Model.Solicitud;
import com.example.demo.Model.SolicitudReabastecimiento;
import com.example.demo.Repository.AdminRepository;
import com.example.demo.Repository.FarmaciaRepository;
import com.example.demo.Repository.MedicamentoRepository;
import com.example.demo.Repository.SolicitudReabastecimientoRepository;
import com.example.demo.Repository.SolicitudRepository;

import java.util.List;

/**
 * Lógica de negocio del rol Admin: revisión y resolución de solicitudes de
 * usuarios, gestión de medicamentos por farmacia y solicitudes de
 * reabastecimiento hacia el Manager.
 *
 * Antes esta lógica vivía repartida dentro de AdminController; se movió aquí
 * para que el controller solo reciba la petición HTTP, delegue y traduzca el
 * resultado a una vista/redirect.
 */
@Service
public class AdminService {

    private static final int STOCK_BAJO_MAX = 20;
    private static final int STOCK_MEDIO_MAX = 49;
    private static final int PAGE_SIZE_SOLICITUDES = 5;
    private static final int PAGE_SIZE_FARMACIAS = 4;
    private static final int PAGE_SIZE_MEDICAMENTOS = 4;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private SolicitudReabastecimientoRepository solicitudReabastecimientoRepository;

    @Autowired
    private FarmaciaRepository farmaciaRepository;

    @Autowired
    private AdminRepository adminRepository;

    public boolean esManager(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_MANAGER"));
    }

    public Eps obtenerEpsDelAdmin(Authentication auth) {
        Admin admin = adminRepository.findByNomUser(auth.getName());
        if (admin == null || admin.getFarmacia() == null) {
            return null;
        }
        return admin.getFarmacia().getEps();
    }

    public long contarSolicitudesPendientes(Eps eps) {
        if (eps == null) {
            return 0;
        }
        return solicitudRepository.countByEstadoAndMedicamentoFarmaciaEpsId(
                EstadoSolicitud.EN_PROCESO, eps.getId());
    }

    public Page<Solicitud> listarSolicitudesEnProceso(Eps eps, Long farmaciaId, int page) {
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE_SOLICITUDES);
        if (farmaciaId != null) {
            return solicitudRepository.findByEstadoAndMedicamentoFarmaciaId(
                    EstadoSolicitud.EN_PROCESO, farmaciaId, pageable);
        }
        return solicitudRepository.findByEstadoAndMedicamentoFarmaciaEpsId(
                EstadoSolicitud.EN_PROCESO, eps.getId(), pageable);
    }

    public Page<Solicitud> buscarSolicitudes(Eps eps, String keyword, int page) {
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE_SOLICITUDES);
        return solicitudRepository.buscarPorNombreApellidoCompletoYMedicamento(
                EstadoSolicitud.EN_PROCESO, eps.getId(), keyword, pageable);
    }

    public List<Farmacia> listarFarmaciasDeEps(Eps eps) {
        return farmaciaRepository.findByEpsId(eps.getId());
    }

    public Page<Farmacia> listarFarmacias(Eps eps, String nombre, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE_FARMACIAS);
        if (nombre != null && !nombre.isEmpty()) {
            return farmaciaRepository.findByEpsIdAndNombreContainingIgnoreCase(eps.getId(), nombre, pageable);
        }
        return farmaciaRepository.findByEpsId(eps.getId(), pageable);
    }

    /**
     * Clasifica un medicamento por nivel de stock. Es una regla de negocio
     * (los umbrales BAJO/MEDIO/FULL) y por eso vive en el Service, no en el
     * Controller.
     */
    public String clasificarStock(int cantidad) {
        if (cantidad <= STOCK_BAJO_MAX) {
            return "BAJO";
        }
        if (cantidad <= STOCK_MEDIO_MAX) {
            return "MEDIO";
        }
        return "FULL";
    }

    public Page<Medicamento> listarMedicamentosPorFarmacia(Long farmaciaId, String nombre, String stock, int page) {
        List<Medicamento> lista;
        if (nombre != null && !nombre.isEmpty()) {
            lista = medicamentoRepository.findByFarmaciaIdAndNombreContainingIgnoreCase(farmaciaId, nombre);
        } else {
            lista = medicamentoRepository.findByFarmaciaId(farmaciaId);
        }

        if (stock != null && !stock.isEmpty()) {
            lista = lista.stream()
                    .filter(m -> clasificarStock(m.getCantidad()).equals(stock))
                    .toList();
        }

        int start = page * PAGE_SIZE_MEDICAMENTOS;
        int end = Math.min(start + PAGE_SIZE_MEDICAMENTOS, lista.size());
        List<Medicamento> pageContent = start > lista.size() ? List.of() : lista.subList(start, end);

        Pageable pageable = PageRequest.of(page, PAGE_SIZE_MEDICAMENTOS);
        return new PageImpl<>(pageContent, pageable, lista.size());
    }

    /**
     * Regla de negocio central del módulo Admin: solo se acepta una solicitud
     * si hay stock suficiente; de lo contrario se rechaza automáticamente.
     */
    public ResultadoAccion aceptarSolicitud(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id).orElseThrow();
        Medicamento medicamento = solicitud.getMedicamento();

        if (medicamento.getCantidad() < solicitud.getCantidad()) {
            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
            solicitudRepository.save(solicitud);
            return ResultadoAccion.error("Solicitud rechazada. No hay stock suficiente.");
        }

        medicamento.setCantidad(medicamento.getCantidad() - solicitud.getCantidad());
        medicamentoRepository.save(medicamento);

        solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        solicitudRepository.save(solicitud);

        return ResultadoAccion.exito("Solicitud aceptada. Acérquese a la farmacia para reclamar su medicamento.");
    }

    public ResultadoAccion rechazarSolicitud(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id).orElseThrow();
        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitudRepository.save(solicitud);
        return ResultadoAccion.error("La solicitud fue rechazada.");
    }

    public ResultadoAccion aceptarTodasEnProceso(Eps eps) {
        List<Solicitud> solicitudes = solicitudRepository.findByEstadoAndMedicamentoFarmaciaEpsId(
                EstadoSolicitud.EN_PROCESO, eps.getId());

        for (Solicitud solicitud : solicitudes) {
            solicitud.setEstado(EstadoSolicitud.ACEPTADA);
        }
        solicitudRepository.saveAll(solicitudes);

        return ResultadoAccion.exito("Todas las solicitudes en proceso de su EPS fueron aceptadas correctamente.");
    }

    public ResultadoAccion rechazarTodasEnProceso(Eps eps) {
        List<Solicitud> solicitudes = solicitudRepository.findByEstadoAndMedicamentoFarmaciaEpsId(
                EstadoSolicitud.EN_PROCESO, eps.getId());

        for (Solicitud solicitud : solicitudes) {
            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        }
        solicitudRepository.saveAll(solicitudes);

        return ResultadoAccion.advertencia("Todas las solicitudes en proceso de su EPS fueron rechazadas correctamente.");
    }

    /**
     * Valida y crea una solicitud de reabastecimiento. La validación de
     * cantidad (> 0) es una regla de negocio, no un detalle de HTTP.
     */
    public ResultadoAccion crearSolicitudReabastecimiento(Long medicamentoId, int cantidad) {
        if (cantidad <= 0) {
            return ResultadoAccion.error("La cantidad debe ser mayor a cero.");
        }

        Medicamento medicamento = medicamentoRepository.findById(medicamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Medicamento no encontrado"));

        SolicitudReabastecimiento solicitud = new SolicitudReabastecimiento();
        solicitud.setMedicamento(medicamento);
        solicitud.setCantidad(cantidad);
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);
        solicitudReabastecimientoRepository.save(solicitud);

        return ResultadoAccion.advertencia("Solicitud de reabastecimiento enviada al Manager para revisión.");
    }
}
