package com.example.demo.service;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.demo.Model.Eps;
import com.example.demo.Model.EstadoSolicitud;
import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Generarcitas;
import com.example.demo.Model.Medicamento;
import com.example.demo.Model.Solicitud;
import com.example.demo.Model.Usuario;
import com.example.demo.Repository.EpsRepository;
import com.example.demo.Repository.FarmaciaRepository;
import com.example.demo.Repository.GenerarcitasRepository;
import com.example.demo.Repository.MedicamentoRepository;
import com.example.demo.Repository.SolicitudRepository;
import com.example.demo.Repository.UsuarioRepository;

/**
 * Lógica de negocio del rol Usuario: aprovisionamiento de cuentas OAuth2,
 * generación de códigos de cita, consulta de farmacias/medicamentos de su
 * EPS, solicitud de medicamentos y edición de perfil (incluida la regla de
 * "la EPS solo se puede cambiar una vez").
 *
 * Antes esta lógica vivía repartida dentro de UsuarioController; se movió
 * aquí para que el controller solo reciba la petición HTTP, delegue y
 * traduzca el resultado a una vista/redirect.
 */
@Service
public class UsuarioService {

    @Autowired
    private MedicamentoRepository medicamentoRepo;
    @Autowired
    private SolicitudRepository solicitudRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private FarmaciaRepository farmaciaRepo;
    @Autowired
    private EpsRepository epsRepo;
    @Autowired
    private GenerarcitasRepository generarcitasRepo;

    private final Random random = new Random();

    public boolean esManager(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MANAGER"));
    }

    /**
     * Aprovisiona un Usuario en el primer login OAuth2: si no existe todavía
     * un registro para ese correo, crea uno con valores por defecto y le
     * asigna una EPS al azar entre las disponibles.
     */
    public Usuario obtenerOCrearUsuario(String correo) {
        Optional<Usuario> optional = usuarioRepo.findOptionalByCorreo(correo);
        if (optional.isPresent()) {
            return optional.get();
        }

        Usuario nuevo = new Usuario();
        nuevo.setCorreo(correo);
        nuevo.setNombre("Usuario");
        nuevo.setApellido("OAuth2");
        nuevo.setRol("USER");
        nuevo.setIdentificacion("OAUTH-" + UUID.randomUUID());
        nuevo.setDireccion("No especificada");

        List<Eps> epsDisponibles = epsRepo.findAll();
        if (!epsDisponibles.isEmpty()) {
            nuevo.setEps(epsDisponibles.get(random.nextInt(epsDisponibles.size())));
        }
        return usuarioRepo.save(nuevo);
    }

    /**
     * Al primer ingreso de la sesión, se libera el bloqueo que impide volver
     * a cambiar de EPS (el bloqueo se vuelve a activar en cuanto el usuario
     * cambia de EPS una vez, ver {@link #actualizarSoloEps}).
     */
    public void desbloquearEpsPrimerIngreso(Usuario usuario) {
        usuario.setEpsBloqueada(false);
        usuarioRepo.save(usuario);
    }

    public Optional<Generarcitas> obtenerPrimeraCita(Usuario usuario) {
        List<Generarcitas> citas = generarcitasRepo.findByUsuario(usuario);
        return citas.isEmpty() ? Optional.empty() : Optional.of(citas.get(0));
    }

    /**
     * Regla de negocio: si ya existe un código de cita activo (no expirado)
     * no se genera uno nuevo; se devuelve el existente con un mensaje
     * informativo.
     */
    public ResultadoCita generarCita(Usuario usuario) {
        List<Generarcitas> existentes = generarcitasRepo.findByUsuario(usuario);
        if (!existentes.isEmpty() && !existentes.get(0).isExpirado()) {
            return new ResultadoCita(existentes.get(0), "Ya tienes un código generado");
        }

        Generarcitas generarcitas = new Generarcitas();
        generarcitas.setUsuario(usuario);
        generarcitas.setCodigo(UUID.randomUUID().toString().substring(0, 8));
        generarcitas.setExpirado(false);
        generarcitasRepo.save(generarcitas);

        return new ResultadoCita(generarcitas, "Tu código fue generado correctamente");
    }

    /**
     * Farmacias de la EPS del usuario. Devuelve null si el usuario no tiene
     * EPS asignada (el Controller decide cómo mostrarlo).
     */
    public Page<Farmacia> listarFarmaciasDeUsuario(Usuario usuario, String nombre, Pageable pageable) {
        if (usuario.getEps() == null) {
            return null;
        }
        Long epsId = usuario.getEps().getId();

        if (nombre != null && !nombre.trim().isEmpty()) {
            return farmaciaRepo.findByEpsIdAndNombreContainingIgnoreCase(epsId, nombre.trim(), pageable);
        }
        return farmaciaRepo.findByEpsId(epsId, pageable);
    }

    /**
     * Regla de negocio de acceso: un usuario solo puede ver farmacias que
     * pertenezcan a su propia EPS. Devuelve null si no tiene acceso (EPS sin
     * asignar, farmacia inexistente o de otra EPS).
     */
    public Farmacia verificarAccesoFarmacia(Usuario usuario, Long farmaciaId) {
        if (usuario.getEps() == null) {
            return null;
        }
        Farmacia farmacia = farmaciaRepo.findById(farmaciaId).orElse(null);
        if (farmacia == null || farmacia.getEps() == null
                || !farmacia.getEps().getId().equals(usuario.getEps().getId())) {
            return null;
        }
        return farmacia;
    }

    public Page<Medicamento> listarMedicamentosFarmacia(Farmacia farmacia, String nombre, String laboratorio,
            String concentracion, String presentacion, String descripcion, Pageable pageable) {

        boolean hayFiltros = tieneTexto(nombre) || tieneTexto(laboratorio) || tieneTexto(concentracion)
                || tieneTexto(presentacion) || tieneTexto(descripcion);

        if (hayFiltros) {
            return medicamentoRepo.findByFarmaciaAndFiltros(
                    farmacia,
                    tieneTexto(nombre) ? nombre : null,
                    tieneTexto(laboratorio) ? laboratorio : null,
                    tieneTexto(concentracion) ? concentracion : null,
                    tieneTexto(presentacion) ? presentacion : null,
                    tieneTexto(descripcion) ? descripcion : null,
                    pageable);
        }
        return medicamentoRepo.findByFarmacia(farmacia, pageable);
    }

    public List<String> laboratoriosDeFarmacia(Farmacia farmacia) {
        return medicamentoRepo.findDistinctLaboratoriosByFarmacia(farmacia);
    }

    public List<String> concentracionesDeFarmacia(Farmacia farmacia) {
        return medicamentoRepo.findDistinctConcentracionesByFarmacia(farmacia);
    }

    public List<String> presentacionesDeFarmacia(Farmacia farmacia) {
        return medicamentoRepo.findDistinctPresentacionesByFarmacia(farmacia);
    }

    public List<String> descripcionesDeFarmacia(Farmacia farmacia) {
        return medicamentoRepo.findDistinctDescripcionesByFarmacia(farmacia);
    }

    public Page<Solicitud> listarSolicitudes(Usuario usuario, String estado, int page) {
        Pageable pageable = PageRequest.of(page, 5);
        if (estado != null && !estado.isBlank()) {
            EstadoSolicitud estadoEnum = EstadoSolicitud.valueOf(estado);
            return solicitudRepo.findByUsuarioAndEstado(usuario, estadoEnum, pageable);
        }
        return solicitudRepo.findByUsuario(usuario, pageable);
    }

    /** Listado simple sin filtro de estado, usado por el endpoint REST. */
    public Page<Solicitud> listarSolicitudesUsuario(Usuario usuario, Pageable pageable) {
        return solicitudRepo.findByUsuario(usuario, pageable);
    }

    public Page<Solicitud> buscarSolicitudes(Usuario usuario, String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 5);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return solicitudRepo.findByUsuarioAndMedicamento_NombreContainingIgnoreCase(usuario, keyword.trim(),
                    pageable);
        }
        return solicitudRepo.findByUsuario(usuario, pageable);
    }

    /**
     * Regla de negocio central de este módulo: solo se puede solicitar un
     * medicamento con un código de cita propio, vigente y no expirado. Al
     * confirmarse la solicitud, el código de cita se marca como expirado
     * para que no pueda reutilizarse.
     */
    public ResultadoSolicitud solicitarMedicamento(Usuario usuario, Long idMedicamento, int cantidad, String codigo) {
        Medicamento medicamento = medicamentoRepo.findById(idMedicamento)
                .orElseThrow(() -> new IllegalArgumentException("Medicamento no encontrado"));

        Long farmaciaId = medicamento.getFarmacia().getId();
        Optional<Generarcitas> citaOpt = generarcitasRepo.findByCodigo(codigo);

        boolean citaValida = citaOpt.isPresent()
                && !citaOpt.get().isExpirado()
                && citaOpt.get().getUsuario().getId().equals(usuario.getId());

        if (!citaValida) {
            return new ResultadoSolicitud(false, "Código inválido o expirado", farmaciaId);
        }

        Solicitud solicitud = new Solicitud();
        solicitud.setUsuario(usuario);
        solicitud.setMedicamento(medicamento);
        solicitud.setCantidad(cantidad);
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);
        solicitudRepo.save(solicitud);

        Generarcitas cita = citaOpt.get();
        cita.setExpirado(true);
        generarcitasRepo.save(cita);

        return new ResultadoSolicitud(true, "Solicitud creada con éxito", farmaciaId);
    }

    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepo.findById(id).orElse(null);
    }

    public List<Eps> listarTodasEps() {
        return epsRepo.findAll();
    }

    /**
     * Actualiza los datos editables del perfil. Si el correo cambió, el
     * Controller debe invalidar la sesión (el correo es el identificador de
     * login), por eso el resultado lo señala explícitamente.
     */
    public ResultadoActualizacionPerfil actualizarPerfil(Long id, Usuario usuarioEditado) {
        Usuario usuario = usuarioRepo.findById(id).orElse(null);
        if (usuario == null) {
            return new ResultadoActualizacionPerfil(false, false);
        }

        boolean correoCambiado = !usuario.getCorreo().equals(usuarioEditado.getCorreo());

        usuario.setNombre(usuarioEditado.getNombre());
        usuario.setApellido(usuarioEditado.getApellido());
        usuario.setCorreo(usuarioEditado.getCorreo());
        usuario.setDireccion(usuarioEditado.getDireccion());
        usuario.setContacto(usuarioEditado.getContacto());
        usuario.setRegimen(usuarioEditado.getRegimen());

        usuarioRepo.save(usuario);

        return new ResultadoActualizacionPerfil(true, correoCambiado);
    }

    /**
     * Regla de negocio: la EPS de un usuario solo se puede cambiar una vez
     * por sesión (usuario.isEpsBloqueada()); tras el cambio queda bloqueada
     * de nuevo hasta el siguiente login.
     */
    public ResultadoAccion actualizarSoloEps(Long id, Long epsId) {
        Usuario usuario = usuarioRepo.findById(id).orElse(null);
        if (usuario == null) {
            return ResultadoAccion.error("UsuarioNoEncontrado");
        }

        if (usuario.isEpsBloqueada()) {
            return ResultadoAccion.advertencia(
                    "Ya has cambiado tu EPS. Cierra sesión e inicia nuevamente para cambiarla de nuevo.");
        }

        Eps nuevaEps = epsRepo.findById(epsId).orElse(null);
        if (nuevaEps != null) {
            usuario.setEps(nuevaEps);
            usuario.setEpsBloqueada(true);
            usuarioRepo.save(usuario);
        }

        return ResultadoAccion.exito(
                "EPS actualizada correctamente. Vuelve a iniciar sesión para cambiarla nuevamente.");
    }

    private boolean tieneTexto(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
