package org.db.medistock.controller;

import org.db.medistock.model.Cita;
import org.db.medistock.model.Farmacia;
import org.db.medistock.model.Medicamento;
import org.db.medistock.model.Solicitud;
import org.db.medistock.model.Usuario;
import org.db.medistock.repository.CitaRepository;
import org.db.medistock.repository.MedicamentoRepository;
import org.db.medistock.repository.SolicitudRepository;
import org.db.medistock.service.MedicamentoService;
import org.db.medistock.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/usuario/farmacias/{farmaciaId}/medicamentos")
public class MedicamentoController {

    private final MedicamentoService medicamentoService;
    private final MedicamentoRepository medicamentoRepo;
    private final SolicitudRepository solicitudRepo;
    private final CitaRepository citaRepo;
    private final UsuarioService usuarioService;

    public MedicamentoController(MedicamentoService medicamentoService,
                                 MedicamentoRepository medicamentoRepo,
                                 SolicitudRepository solicitudRepo,
                                 CitaRepository citaRepo,
                                 UsuarioService usuarioService) {
        this.medicamentoService = medicamentoService;
        this.medicamentoRepo = medicamentoRepo;
        this.solicitudRepo = solicitudRepo;
        this.citaRepo = citaRepo;
        this.usuarioService = usuarioService;
    }

    private Usuario obtenerUsuarioAutenticado(Authentication auth) {
        return usuarioService.buscarPorEmail(auth.getName());
    }

   //Filtros

    @GetMapping
    public String consultarMedicamentosFarmacia(
            @PathVariable Long farmaciaId,
            Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String laboratorio,
            @RequestParam(required = false) String concentracion,
            @RequestParam(required = false) String presentacion,
            @RequestParam(required = false) String descripcion) {

        Farmacia farmacia = medicamentoService.buscarFarmacia(farmaciaId);
        if (farmacia == null) {
            return "redirect:/usuario/farmacias";
        }

        int pageSize = 6;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("nombre").ascending());
        Page<Medicamento> medicamentoPage;

        if ((nombre != null && !nombre.trim().isEmpty()) ||
                (laboratorio != null && !laboratorio.trim().isEmpty()) ||
                (concentracion != null && !concentracion.trim().isEmpty()) ||
                (presentacion != null && !presentacion.trim().isEmpty()) ||
                (descripcion != null && !descripcion.trim().isEmpty())) {

            medicamentoPage = medicamentoRepo.findByFarmaciaAndFiltros(
                    farmacia,
                    (nombre != null && !nombre.isEmpty()) ? nombre : null,
                    (laboratorio != null && !laboratorio.isEmpty()) ? laboratorio : null,
                    (concentracion != null && !concentracion.isEmpty()) ? concentracion : null,
                    (presentacion != null && !presentacion.isEmpty()) ? presentacion : null,
                    (descripcion != null && !descripcion.isEmpty()) ? descripcion : null,
                    pageable);
        } else {
            medicamentoPage = medicamentoRepo.findByFarmacia(farmacia, pageable);
        }

        model.addAttribute("farmacia", farmacia);
        model.addAttribute("farmaciaId", farmaciaId);
        model.addAttribute("medicamentoList", medicamentoPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", medicamentoPage.getTotalPages());

        model.addAttribute("laboratorios", medicamentoRepo.findDistinctLaboratoriosByFarmacia(farmacia));
        model.addAttribute("concentraciones", medicamentoRepo.findDistinctConcentracionesByFarmacia(farmacia));
        model.addAttribute("presentaciones", medicamentoRepo.findDistinctPresentacionesByFarmacia(farmacia));
        model.addAttribute("descripciones", medicamentoRepo.findDistinctDescripcionesByFarmacia(farmacia));

        model.addAttribute("nombre", nombre);
        model.addAttribute("laboratorio", laboratorio);
        model.addAttribute("concentracion", concentracion);
        model.addAttribute("presentacion", presentacion);
        model.addAttribute("descripcion", descripcion);

        return "usuario/medicamentos";
    }

    @PostMapping("/solicitar/{idMedicamento}")
    public String solicitarMedicamento(
            @PathVariable Long farmaciaId,
            @PathVariable Long idMedicamento,
            @RequestParam int cantidad,
            @RequestParam String codigo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String laboratorio,
            @RequestParam(required = false) String concentracion,
            @RequestParam(required = false) String presentacion,
            @RequestParam(required = false) String descripcion,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        if (auth == null) {
            return "redirect:/login";
        }

        Usuario usuario = obtenerUsuarioAutenticado(auth);
        Medicamento medicamento = medicamentoRepo.findById(idMedicamento)
                .orElseThrow(() -> new IllegalArgumentException("Medicamento no encontrado"));

        Optional<Cita> citaOpt = citaRepo.findByCodigo(codigo);

        if (citaOpt.isEmpty() || citaOpt.get().isCodestado()
                || !citaOpt.get().getUsuario().getId().equals(usuario.getId())) {

            redirectAttributes.addFlashAttribute("error", "Código de cita inválido, ya utilizado o expirado.");
        } else {

            Solicitud solicitud = new Solicitud();
            solicitud.setUsuario(usuario);
            solicitud.setMedicamento(medicamento);
            solicitud.setCantidad(cantidad);
            solicitud.setFechasolicitud(LocalDateTime.now());
            solicitud.setEstado(Solicitud.EstadoSolicitud.EN_PROCESO);
            solicitud.setCita(citaOpt.get());
            solicitudRepo.save(solicitud);

            Cita cita = citaOpt.get();
            cita.setCodestado(true);
            citaRepo.save(cita);

            redirectAttributes.addFlashAttribute("msg", "¡Solicitud creada con éxito!");
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/usuario/farmacias/" + farmaciaId + "/medicamentos?page=" + page);
        if (nombre != null && !nombre.isEmpty()) redirectUrl.append("&nombre=").append(nombre);
        if (laboratorio != null && !laboratorio.isEmpty()) redirectUrl.append("&laboratorio=").append(laboratorio);
        if (concentracion != null && !concentracion.isEmpty()) redirectUrl.append("&concentracion=").append(concentracion);
        if (presentacion != null && !presentacion.isEmpty()) redirectUrl.append("&presentacion=").append(presentacion);
        if (descripcion != null && !descripcion.isEmpty()) redirectUrl.append("&descripcion=").append(descripcion);

        return redirectUrl.toString();
    }
}