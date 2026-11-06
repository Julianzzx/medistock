package org.db.medistock.controller;

import org.db.medistock.model.Admin;
import org.db.medistock.model.Eps;
import org.db.medistock.model.Medicamento;
import org.db.medistock.model.Solicitud;
import org.db.medistock.repository.AdminRepository;
import org.db.medistock.repository.MedicamentoRepository;
import org.db.medistock.repository.SolicitudRepository;
import org.db.medistock.service.SolicitudService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/solicitudes")
public class AdminSolicitudController {

    private final SolicitudService solicitudService;
    private final SolicitudRepository solicitudRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final AdminRepository adminRepository;

    public AdminSolicitudController(SolicitudService solicitudService,
                                    SolicitudRepository solicitudRepository,
                                    MedicamentoRepository medicamentoRepository,
                                    AdminRepository adminRepository) {
        this.solicitudService = solicitudService;
        this.solicitudRepository = solicitudRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.adminRepository = adminRepository;
    }

    private Eps obtenerEpsDelAdmin(Authentication auth) {
        if (auth == null) return null;
        Admin admin = adminRepository.findByUsuario(auth.getName());
        if (admin != null) {
            return admin.getEps();
        }
        return null;
    }

    // Listar  Solicitudes

    @GetMapping
    public String listarSolicitudesAdmin(
            Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Solicitud.EstadoSolicitud estado,
            @RequestParam(required = false) String keyword,
            Authentication auth) {

        Eps eps = obtenerEpsDelAdmin(auth);
        if (eps == null) {
            model.addAttribute("error", "No se pudo determinar la EPS asociada al administrador.");
            return "admin/solicitudes";
        }

        int pageSize = 5;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("id").descending());
        Page<Solicitud> solicitudPage;

        Solicitud.EstadoSolicitud estadoFiltro = (estado != null) ? estado : Solicitud.EstadoSolicitud.EN_PROCESO;

        if (keyword != null && !keyword.trim().isEmpty()) {
            solicitudPage = solicitudRepository.buscarPorNombreApellidoCompletoYMedicamento(
                    estadoFiltro, eps.getId(), keyword.trim(), pageable);
            model.addAttribute("keyword", keyword);
        } else {
            solicitudPage = solicitudRepository.findByEstadoAndMedicamentoFarmaciaEpsId(
                    estadoFiltro, eps.getId(), pageable);
        }

        model.addAttribute("solicitudList", solicitudPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", solicitudPage.getTotalPages());
        model.addAttribute("estados", Solicitud.EstadoSolicitud.values());
        model.addAttribute("selectedEstado", estadoFiltro);
        model.addAttribute("epsNombre", eps.getNombre());

        return "admin/solicitudes";
    }

    // Aceptar Solicitud

    @PostMapping("/aceptar/{id}")
    public String aceptarSolicitud(
            @PathVariable Long id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            RedirectAttributes redirectAttributes) {

        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        Medicamento medicamento = solicitud.getMedicamento();

        if (medicamento.getCantidad() >= solicitud.getCantidad()) {

            medicamento.setCantidad(medicamento.getCantidad() - solicitud.getCantidad());
            medicamentoRepository.save(medicamento);

            solicitud.setEstado(Solicitud.EstadoSolicitud.ACEPTADA);
            solicitudRepository.save(solicitud);

            redirectAttributes.addFlashAttribute("tipo", "success");
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud aceptada correctamente. Stock actualizado.");
        } else {
            solicitud.setEstado(Solicitud.EstadoSolicitud.RECHAZADA);
            solicitudRepository.save(solicitud);

            redirectAttributes.addFlashAttribute("tipo", "error");
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud rechazada automáticamente por falta de stock suficiente.");
        }

        String redirectUrl = "/admin/solicitudes?page=" + page;
        if (keyword != null && !keyword.isEmpty()) {
            redirectUrl += "&keyword=" + keyword;
        }
        return "redirect:" + redirectUrl;
    }

    // Rechazar Solicitud

    @PostMapping("/rechazar/{id}")
    public String rechazarSolicitud(
            @PathVariable Long id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            RedirectAttributes redirectAttributes) {

        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        solicitud.setEstado(Solicitud.EstadoSolicitud.RECHAZADA);
        solicitudRepository.save(solicitud);

        redirectAttributes.addFlashAttribute("tipo", "error");
        redirectAttributes.addFlashAttribute("mensaje", "La solicitud fue rechazada.");

        String redirectUrl = "/admin/solicitudes?page=" + page;
        if (keyword != null && !keyword.isEmpty()) {
            redirectUrl += "&keyword=" + keyword;
        }
        return "redirect:" + redirectUrl;
    }
}