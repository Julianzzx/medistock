package com.example.demo.service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.Model.Admin;
import com.example.demo.Model.Eps;
import com.example.demo.Model.EstadoSolicitud;
import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Medicamento;
import com.example.demo.Model.SolicitudReabastecimiento;
import com.example.demo.Model.Usuario;
import com.example.demo.Repository.AdminRepository;
import com.example.demo.Repository.EpsRepository;
import com.example.demo.Repository.FarmaciaRepository;
import com.example.demo.Repository.MedicamentoRepository;
import com.example.demo.Repository.SolicitudReabastecimientoRepository;
import com.example.demo.Repository.UsuarioRepository;

/**
 * Lógica de negocio del rol Manager: administración de usuarios, farmacias,
 * medicamentos, administradores, EPS y revisión de solicitudes de
 * reabastecimiento. Antes vivía repartida dentro de ManagerController; se
 * movió aquí para que el controller solo reciba la petición HTTP, delegue y
 * traduzca el resultado a una vista/redirect.
 *
 * El módulo WEKA ya estaba correctamente delegado en WekaService y no se
 * modificó.
 */
@Service
public class ManagerService {

    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private AdminRepository adminRepo;
    @Autowired
    private FarmaciaRepository farmaciaRepo;
    @Autowired
    private EpsRepository epsRepository;
    @Autowired
    private MedicamentoRepository medicamentoRepo;
    @Autowired
    private SolicitudReabastecimientoRepository solicitudReabastecimientoRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ===================== USUARIOS =====================

    public Page<Usuario> listarUsuarios(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("nombre"));
        if (keyword != null && !keyword.isEmpty()) {
            return usuarioRepo
                    .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrIdentificacionContainingIgnoreCase(
                            keyword, keyword, keyword, pageable);
        }
        return usuarioRepo.findAll(pageable);
    }

    public Usuario obtenerUsuario(Long id) {
        return usuarioRepo.findById(id).orElse(null);
    }

    /**
     * Crea un usuario aplicando las reglas de negocio: rol por defecto,
     * encriptación de la identificación y resolución de la EPS asociada.
     */
    public void crearUsuario(Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
            usuario.setRol("USER");
        }
        usuario.setIdentificacion(passwordEncoder.encode(usuario.getIdentificacion()));
        usuario.setEps(resolverEpsOpcional(usuario.getEps() != null ? usuario.getEps().getId() : null));
        usuarioRepo.save(usuario);
    }

    public boolean eliminarUsuario(Long id) {
        if (!usuarioRepo.existsById(id)) {
            return false;
        }
        usuarioRepo.deleteById(id);
        return true;
    }

    /**
     * Aplica una edición parcial: solo reencripta la identificación si viene
     * un valor nuevo, y solo cambia la EPS si el editor mandó una.
     */
    public void actualizarUsuario(Long id, Usuario usuarioEditado) {
        Usuario usuario = usuarioRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

        usuario.setNombre(usuarioEditado.getNombre());
        usuario.setApellido(usuarioEditado.getApellido());
        usuario.setDireccion(usuarioEditado.getDireccion());
        usuario.setCorreo(usuarioEditado.getCorreo());
        usuario.setRol(usuarioEditado.getRol());

        if (usuarioEditado.getIdentificacion() != null && !usuarioEditado.getIdentificacion().isBlank()) {
            usuario.setIdentificacion(passwordEncoder.encode(usuarioEditado.getIdentificacion()));
        }

        if (usuarioEditado.getEps() != null && usuarioEditado.getEps().getId() != null) {
            usuario.setEps(epsRepository.findById(usuarioEditado.getEps().getId()).orElse(null));
        }

        usuarioRepo.save(usuario);
    }

    // ===================== FARMACIAS =====================

    public Page<Farmacia> listarFarmacias(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("nombre"));
        if (keyword != null && !keyword.isEmpty()) {
            return farmaciaRepo.findByNombreContainingIgnoreCase(keyword, pageable);
        }
        return farmaciaRepo.findAll(pageable);
    }

    public List<Farmacia> listarTodasFarmacias() {
        return farmaciaRepo.findAll();
    }

    public Farmacia obtenerFarmacia(Long id) {
        return farmaciaRepo.findById(id).orElse(null);
    }

    public void crearFarmacia(Farmacia farmacia, Long epsId) {
        farmacia.setEps(resolverEpsOpcional(epsId));
        farmaciaRepo.save(farmacia);
    }

    public boolean actualizarFarmacia(Long id, Farmacia farmaciaEditada, Long epsId) {
        Farmacia farmacia = farmaciaRepo.findById(id).orElse(null);
        if (farmacia == null) {
            return false;
        }

        farmacia.setNombre(farmaciaEditada.getNombre());
        farmacia.setDireccion(farmaciaEditada.getDireccion());
        Eps eps = resolverEpsOpcional(epsId);
        if (eps != null) {
            farmacia.setEps(eps);
        }

        farmaciaRepo.save(farmacia);
        return true;
    }

    /**
     * Regla de negocio: no se puede eliminar una farmacia que todavía tiene
     * medicamentos asignados.
     */
    public ResultadoAccion eliminarFarmacia(Long id) {
        if (!farmaciaRepo.existsById(id)) {
            return ResultadoAccion.error("La farmacia no existe.");
        }

        List<Medicamento> medicamentosAsignados = medicamentoRepo.findByFarmaciaId(id);
        if (!medicamentosAsignados.isEmpty()) {
            return ResultadoAccion.error("No se puede eliminar una farmacia con medicamentos asignados.");
        }

        farmaciaRepo.deleteById(id);
        return ResultadoAccion.exito("Farmacia eliminada correctamente.");
    }

    // ===================== MEDICAMENTOS (vista Manager) =====================

    public Medicamento obtenerMedicamento(Long id) {
        return medicamentoRepo.findById(id).orElse(null);
    }

    public Page<Medicamento> listarMedicamentosPorFarmacia(Long farmaciaId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre"));
        if (keyword != null && !keyword.isEmpty()) {
            return medicamentoRepo.findByFarmaciaIdAndNombreContainingIgnoreCase(farmaciaId, keyword, pageable);
        }
        return medicamentoRepo.findByFarmaciaId(farmaciaId, pageable);
    }

    /**
     * Regla de negocio: la cantidad de un medicamento nuevo no puede ser
     * negativa. Si la farmacia de la ruta no existe, se lanza una excepción
     * (caso de integridad de datos, no de validación de negocio).
     */
    public ResultadoAccion crearMedicamento(Long farmaciaId, Medicamento medicamento) {
        if (medicamento.getCantidad() < 0) {
            return ResultadoAccion.error("Cantidad no puede ser negativa");
        }

        Farmacia farmacia = farmaciaRepo.findById(farmaciaId)
                .orElseThrow(() -> new NoSuchElementException("Farmacia no encontrada"));

        medicamento.setFarmacia(farmacia);
        medicamentoRepo.save(medicamento);
        return ResultadoAccion.exito("Medicamento creado.");
    }

    /**
     * Regla de negocio: el medicamento a eliminar debe pertenecer
     * efectivamente a la farmacia indicada en la ruta.
     */
    public ResultadoAccion eliminarMedicamento(Long farmaciaId, Long id) {
        Medicamento medicamento = medicamentoRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Medicamento no encontrado"));

        if (medicamento.getFarmacia() == null || !medicamento.getFarmacia().getId().equals(farmaciaId)) {
            return ResultadoAccion.error("El medicamento no pertenece a esta farmacia.");
        }

        medicamentoRepo.delete(medicamento);
        return ResultadoAccion.exito("Medicamento eliminado.");
    }

    public boolean actualizarMedicamento(Long farmaciaId, Long id, Medicamento medicamentoEditado) {
        Medicamento medicamento = medicamentoRepo.findById(id).orElse(null);
        Farmacia farmacia = farmaciaRepo.findById(farmaciaId).orElse(null);
        if (medicamento == null || farmacia == null) {
            return false;
        }

        medicamento.setNombre(medicamentoEditado.getNombre());
        medicamento.setCantidad(medicamentoEditado.getCantidad());
        medicamento.setFarmacia(farmacia);
        medicamentoRepo.save(medicamento);
        return true;
    }

    // ===================== ADMINS =====================

    public Page<Admin> listarAdmins(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("nombre"));
        if (keyword != null && !keyword.isEmpty()) {
            return adminRepo
                    .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrNomUserContainingIgnoreCase(
                            keyword, keyword, keyword, pageable);
        }
        return adminRepo.findAll(pageable);
    }

    public Admin obtenerAdmin(Long id) {
        return adminRepo.findById(id).orElse(null);
    }

    public void crearAdmin(Admin admin, Long farmaciaId) {
        if (admin.getIdentificacion() != null && !admin.getIdentificacion().isBlank()) {
            admin.setIdentificacion(passwordEncoder.encode(admin.getIdentificacion()));
        }
        admin.setFarmacia(farmaciaId != null ? farmaciaRepo.findById(farmaciaId).orElse(null) : null);
        adminRepo.save(admin);
    }

    public boolean eliminarAdmin(Long id) {
        Admin admin = adminRepo.findById(id).orElse(null);
        if (admin == null) {
            return false;
        }
        adminRepo.delete(admin);
        return true;
    }

    public boolean actualizarAdmin(Long id, Admin adminEditado, Long farmaciaId) {
        Admin admin = adminRepo.findById(id).orElse(null);
        if (admin == null) {
            return false;
        }

        admin.setNombre(adminEditado.getNombre());
        admin.setApellido(adminEditado.getApellido());
        admin.setNomUser(adminEditado.getNomUser());

        if (adminEditado.getIdentificacion() != null && !adminEditado.getIdentificacion().isBlank()) {
            admin.setIdentificacion(passwordEncoder.encode(adminEditado.getIdentificacion()));
        }

        admin.setFarmacia(farmaciaId != null ? farmaciaRepo.findById(farmaciaId).orElse(null) : null);
        adminRepo.save(admin);
        return true;
    }

    // ===================== SOLICITUDES DE REABASTECIMIENTO =====================

    public SolicitudesReabastecimientoVista listarSolicitudesReabastecimiento(Long farmaciaId,
            int pagePendientes, int pageAceptadas, int pageRechazadas, int size) {

        Pageable pageableP = PageRequest.of(pagePendientes, size);
        Pageable pageableA = PageRequest.of(pageAceptadas, size);
        Pageable pageableR = PageRequest.of(pageRechazadas, size);

        if (farmaciaId != null) {
            return new SolicitudesReabastecimientoVista(
                    solicitudReabastecimientoRepo.findByEstadoAndMedicamento_Farmacia_Id(
                            EstadoSolicitud.EN_PROCESO, farmaciaId, pageableP),
                    solicitudReabastecimientoRepo.findByEstadoAndMedicamento_Farmacia_Id(
                            EstadoSolicitud.ACEPTADA, farmaciaId, pageableA),
                    solicitudReabastecimientoRepo.findByEstadoAndMedicamento_Farmacia_Id(
                            EstadoSolicitud.RECHAZADA, farmaciaId, pageableR));
        }

        return new SolicitudesReabastecimientoVista(
                solicitudReabastecimientoRepo.findByEstado(EstadoSolicitud.EN_PROCESO, pageableP),
                solicitudReabastecimientoRepo.findByEstado(EstadoSolicitud.ACEPTADA, pageableA),
                solicitudReabastecimientoRepo.findByEstado(EstadoSolicitud.RECHAZADA, pageableR));
    }

    /**
     * Regla de negocio central: aceptar una solicitud de reabastecimiento
     * incrementa el stock del medicamento correspondiente.
     */
    public ResultadoAccion aceptarSolicitudReabastecimiento(Long id) {
        SolicitudReabastecimiento solicitud = solicitudReabastecimientoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        Medicamento medicamento = solicitud.getMedicamento();
        medicamento.setCantidad(medicamento.getCantidad() + solicitud.getCantidad());
        solicitud.setEstado(EstadoSolicitud.ACEPTADA);

        medicamentoRepo.save(medicamento);
        solicitudReabastecimientoRepo.save(solicitud);

        return ResultadoAccion.exito("Solicitud de reabastecimiento aceptada.");
    }

    public ResultadoAccion rechazarSolicitudReabastecimiento(Long id) {
        SolicitudReabastecimiento solicitud = solicitudReabastecimientoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitudReabastecimientoRepo.save(solicitud);

        return ResultadoAccion.error("Solicitud de reabastecimiento rechazada.");
    }

    public void eliminarSolicitudReabastecimiento(Long id) {
        SolicitudReabastecimiento solicitud = solicitudReabastecimientoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        solicitudReabastecimientoRepo.delete(solicitud);
    }

    public void aceptarTodasPendientes() {
        List<SolicitudReabastecimiento> pendientes = solicitudReabastecimientoRepo
                .findByEstado(EstadoSolicitud.EN_PROCESO);

        for (SolicitudReabastecimiento solicitud : pendientes) {
            Medicamento medicamento = solicitud.getMedicamento();
            medicamento.setCantidad(medicamento.getCantidad() + solicitud.getCantidad());
            solicitud.setEstado(EstadoSolicitud.ACEPTADA);

            medicamentoRepo.save(medicamento);
            solicitudReabastecimientoRepo.save(solicitud);
        }
    }

    public void rechazarTodasPendientes() {
        List<SolicitudReabastecimiento> pendientes = solicitudReabastecimientoRepo
                .findByEstado(EstadoSolicitud.EN_PROCESO);

        for (SolicitudReabastecimiento solicitud : pendientes) {
            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
            solicitudReabastecimientoRepo.save(solicitud);
        }
    }

    public void eliminarTodasAceptadas() {
        solicitudReabastecimientoRepo.deleteAll(
                solicitudReabastecimientoRepo.findByEstado(EstadoSolicitud.ACEPTADA));
    }

    public void eliminarTodasRechazadas() {
        solicitudReabastecimientoRepo.deleteAll(
                solicitudReabastecimientoRepo.findByEstado(EstadoSolicitud.RECHAZADA));
    }

    // ===================== EPS =====================

    public List<Eps> listarTodasEps() {
        return epsRepository.findAll();
    }

    public Page<Eps> listarEps(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("nombre").ascending());
        if (keyword != null && !keyword.isEmpty()) {
            return epsRepository.findByNombreContainingIgnoreCase(keyword, pageable);
        }
        return epsRepository.findAll(pageable);
    }

    public Eps obtenerEps(Long id) {
        return epsRepository.findById(id).orElseGet(Eps::new);
    }

    public void crearEps(Eps eps) {
        epsRepository.save(eps);
    }

    public void actualizarEps(Long id, Eps eps) {
        eps.setId(id);
        epsRepository.save(eps);
    }

    public void eliminarEps(Long id) {
        epsRepository.deleteById(id);
    }

    public Eps buscarEpsPorId(Long id) {
        return epsRepository.findById(id).orElse(null);
    }

    /**
     * Farmacias de una EPS identificada por nombre exacto (usado por la
     * vista fija de Sanitas). Si la EPS no existe, devuelve una página
     * vacía en vez de propagar el error a la vista.
     */
    public Page<Farmacia> listarFarmaciasPorNombreEps(String nombreEps, String keyword, Pageable pageable) {
        Optional<Eps> epsOpt = epsRepository.findByNombreIgnoreCase(nombreEps);
        if (epsOpt.isEmpty()) {
            return Page.empty(pageable);
        }

        Long epsId = epsOpt.get().getId();
        if (keyword != null && !keyword.isEmpty()) {
            return farmaciaRepo.findByEpsIdAndNombreContainingIgnoreCase(epsId, keyword, pageable);
        }
        return farmaciaRepo.findByEpsId(epsId, pageable);
    }

    /**
     * Variante usada por la vista fija de Sura, que filtra el nombre con el
     * otro método de repositorio ya existente en el proyecto.
     * Devuelve Optional.empty() si la EPS "Sura" no existe.
     */
    public Optional<Page<Farmacia>> listarFarmaciasSura(String keyword, Pageable pageable) {
        Optional<Eps> suraOpt = epsRepository.findByNombreIgnoreCase("Sura");
        if (suraOpt.isEmpty()) {
            return Optional.empty();
        }

        Long epsId = suraOpt.get().getId();
        if (keyword != null && !keyword.isEmpty()) {
            return Optional.of(farmaciaRepo.findByNombreContainingIgnoreCaseAndEpsId(keyword, epsId, pageable));
        }
        return Optional.of(farmaciaRepo.findByEpsId(epsId, pageable));
    }

    private Eps resolverEpsOpcional(Long epsId) {
        if (epsId == null) {
            return null;
        }
        return epsRepository.findById(epsId).orElse(null);
    }
}
