package org.db.medistock.service;

import org.db.medistock.model.Solicitud;
import org.db.medistock.model.Usuario;
import org.db.medistock.repository.SolicitudRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;

    public SolicitudService(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
    }

    // Buscar por Id

    @Transactional(readOnly = true)
    public Solicitud buscarSolicitud(Long id){
        return solicitudRepository.findById(id).orElseThrow(()-> new RuntimeException("No se ha encontrado Ninguna Solicitud"));
    }

    // Buscar Por Usuario

    @Transactional(readOnly = true)
    public Page<Solicitud> buscarUsuario(Usuario usuario, Pageable pageable){
        return solicitudRepository.findByUsuario(usuario, pageable);
    }

    // Mostrar Solicitud

    @Transactional(readOnly = true)
    public List<Solicitud> buscarSolicitudes(Usuario usuario){
        return solicitudRepository.findByUsuario(usuario);
    }

    // Buscar Por Keyword

    @Transactional(readOnly = true)
    public Page<Solicitud> buscarPorKeyword(Usuario usuario, String keyword, Pageable pageable){
        return solicitudRepository.findByUsuarioAndMedicamento_NombreContainingIgnoreCase(usuario, keyword, pageable);
    }

    // : Buscar Por Usuario y Estado

    @Transactional(readOnly = true)
    public Page<Solicitud> buscarPorEstado(Usuario usuario, Solicitud.EstadoSolicitud estado, Pageable pageable) {
        return solicitudRepository.findByUsuarioAndEstado(usuario, estado, pageable);
    }

    //  Buscar Por Usuario, Keyword y Estado

    @Transactional(readOnly = true)
    public Page<Solicitud> buscarPorKeywordYEstado(Usuario usuario, String keyword, Solicitud.EstadoSolicitud estado, Pageable pageable) {
        return solicitudRepository.findByUsuarioAndMedicamento_NombreContainingIgnoreCaseAndEstado(usuario, keyword, estado, pageable);
    }

    // Buscar Por Estado Y Eps

    @Transactional(readOnly = true)
    public Page<Solicitud> buscarPorEstadoEps(Long epsId, Solicitud.EstadoSolicitud estado, Pageable pageable){
        return solicitudRepository.findByEstadoAndMedicamentoFarmaciaEpsId(estado, epsId, pageable);
    }

    // Buscar Por Estado Y Eps

    @Transactional(readOnly = true)
    public List<Solicitud> buscarPorEstado(Long epsId, Solicitud.EstadoSolicitud estado){
        return solicitudRepository.findByEstadoAndMedicamentoFarmaciaEpsId(estado, epsId);
    }

    // Busqueda Para Solicitud Admin

    @Transactional(readOnly = true)
    public Page<Solicitud> buscarPorKeywordAdmin(Solicitud.EstadoSolicitud estado, String keyword, Long epsId, Pageable pageable){
        return solicitudRepository.buscarPorNombreApellidoCompletoYMedicamento(estado, epsId, keyword, pageable);
    }

    // Crear Solicitud

    @Transactional
    public Solicitud crearSolicitud(Solicitud solicitud, Usuario usuario) {
        solicitud.setUsuario(usuario);
        solicitud.setEstado(Solicitud.EstadoSolicitud.EN_PROCESO);
        return solicitudRepository.save(solicitud);
    }

    // Guardar Solicitud

    @Transactional
    public Solicitud guardarSolicitud(Solicitud solicitud){
        return solicitudRepository.save(solicitud);
    }

    // Aceptar Solicitud

    @Transactional
    public Solicitud aceptarSolicitud(Long id){
        Solicitud solicitud = buscarSolicitud(id);
        solicitud.setEstado(Solicitud.EstadoSolicitud.ACEPTADA);
        return solicitudRepository.save(solicitud);
    }

    // Rechazar Solicitud

    @Transactional
    public Solicitud rechazarSolicitud(Long id){
        Solicitud solicitud = buscarSolicitud(id);
        solicitud.setEstado(Solicitud.EstadoSolicitud.RECHAZADA);
        return solicitudRepository.save(solicitud);
    }
}