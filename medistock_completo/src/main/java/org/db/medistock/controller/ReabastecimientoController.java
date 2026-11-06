package org.db.medistock.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.db.medistock.model.Admin;
import org.db.medistock.model.Eps;
import org.db.medistock.model.Farmacia;
import org.db.medistock.model.Medicamento;
import org.db.medistock.repository.AdminRepository;
import org.db.medistock.repository.FarmaciaRepository;
import org.db.medistock.repository.MedicamentoRepository;
import org.db.medistock.service.ReabastecimientoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class ReabastecimientoController {

    private final ReabastecimientoService reabastecimientoService;
    private final MedicamentoRepository medicamentoRepository;
    private final AdminRepository adminRepository;
    private final FarmaciaRepository farmaciaRepository;

    public ReabastecimientoController(ReabastecimientoService reabastecimientoService,
                                      MedicamentoRepository medicamentoRepository,
                                      AdminRepository adminRepository,
                                      FarmaciaRepository farmaciaRepository) {
        this.reabastecimientoService = reabastecimientoService;
        this.medicamentoRepository = medicamentoRepository;
        this.adminRepository = adminRepository;
        this.farmaciaRepository = farmaciaRepository;
    }

    private Eps obtenerEpsDelAdmin(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        Admin admin = adminRepository.findByUsuario(auth.getName());
        if (admin != null) {
            return admin.getEps();
        }
        return null;
    }

    // Listar las farmacias

    @GetMapping("/farmacias")
    public String listarFarmaciasPorEps(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String nombre,
            Model model,
            Authentication auth) {

        Eps eps = obtenerEpsDelAdmin(auth);
        if (eps == null) {
            model.addAttribute("error", "No se pudo determinar la EPS del administrador.");
            return "admin/farmacias";
        }

        int pageSize = 6;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by("nombre").ascending());
        Page<Farmacia> farmaciaPage;

        if (nombre != null && !nombre.trim().isEmpty()) {
            farmaciaPage = farmaciaRepository.findByEpsIdAndNombreContainingIgnoreCase(eps.getId(), nombre.trim(), pageable);
            model.addAttribute("nombre", nombre);
        } else {
            farmaciaPage = farmaciaRepository.findByEpsId(eps.getId(), pageable);
        }

        model.addAttribute("farmacias", farmaciaPage.getContent());
        model.addAttribute("currentPage", page - 1);
        model.addAttribute("totalPages", farmaciaPage.getTotalPages());
        model.addAttribute("epsNombre", eps.getNombre());

        return "admin/farmacias";
    }

    // Listar inventario de medicamentos

    @GetMapping("/farmacia/{id}/medicamentos")
    public String listarMedicamentosFarmacia(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String filtroStock,
            Model model,
            Authentication auth) {

        Eps epsAdmin = obtenerEpsDelAdmin(auth);
        if (epsAdmin == null) {
            return "redirect:/admin/dashboard?error=SinEPS";
        }

        Farmacia farmacia = farmaciaRepository.findById(id).orElse(null);
        if (farmacia == null || farmacia.getEps() == null || !farmacia.getEps().getId().equals(epsAdmin.getId())) {
            return "redirect:/admin/farmacias?error=AccesoDenegado";
        }

        List<Medicamento> todosLosMedicamentos = medicamentoRepository.findByFarmaciaId(id);

        if (nombre != null && !nombre.trim().isEmpty()) {
            String keyword = nombre.trim().toLowerCase();
            todosLosMedicamentos = todosLosMedicamentos.stream()
                    .filter(m -> m.getNombre() != null && m.getNombre().toLowerCase().contains(keyword))
                    .toList();
            model.addAttribute("nombre", nombre);
        }

        if (filtroStock != null && !filtroStock.isEmpty()) {
            todosLosMedicamentos = todosLosMedicamentos.stream().filter(m -> {
                int cantidad = m.getCantidad();
                if ("bajo".equals(filtroStock)) return cantidad <= 20;
                if ("medio".equals(filtroStock)) return cantidad > 20 && cantidad <= 50;
                if ("alto".equals(filtroStock)) return cantidad > 50;
                return true;
            }).toList();
            model.addAttribute("filtroStock", filtroStock);
        }

        int pageSize = 6;
        int totalItems = todosLosMedicamentos.size();
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        List<Medicamento> medicamentosPaginados = (startIndex > totalItems) ? List.of() : todosLosMedicamentos.subList(startIndex, endIndex);

        Page<Medicamento> medicamentoPage = new PageImpl<>(
                medicamentosPaginados,
                PageRequest.of(Math.max(0, page - 1), pageSize),
                totalItems
        );

        model.addAttribute("medicamentos", medicamentoPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", medicamentoPage.getTotalPages());
        model.addAttribute("farmaciaId", id);
        model.addAttribute("farmaciaNombre", farmacia.getNombre());
        model.addAttribute("isManager", false);

        return "admin/medicamentos";
    }

    // Crear Solicitud de Reabastecimiento

    @PostMapping("/solicitud-reabastecimiento/{id}")
    public String crearSolicitudReabastecimiento(
            @PathVariable Long id,
            @RequestParam int cantidad,
            RedirectAttributes redirectAttributes,
            Authentication auth,
            HttpServletRequest request) {

        Eps epsAdmin = obtenerEpsDelAdmin(auth);
        if (epsAdmin == null) {
            redirectAttributes.addFlashAttribute("tipo", "error");
            redirectAttributes.addFlashAttribute("mensaje", "No se pudo determinar la EPS del administrador.");
            return "redirect:" + (request.getHeader("Referer") != null ? request.getHeader("Referer") : "/admin/dashboard");
        }

        Medicamento medicamento = medicamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Medicamento no encontrado"));

        if (medicamento.getFarmacia() == null || medicamento.getFarmacia().getEps() == null ||
                !medicamento.getFarmacia().getEps().getId().equals(epsAdmin.getId())) {
            redirectAttributes.addFlashAttribute("tipo", "error");
            redirectAttributes.addFlashAttribute("mensaje", "Acceso denegado: El medicamento no pertenece a tu red EPS.");
            return "redirect:" + (request.getHeader("Referer") != null ? request.getHeader("Referer") : "/admin/dashboard");
        }

        if (cantidad <= 0) {
            redirectAttributes.addFlashAttribute("tipo", "error");
            redirectAttributes.addFlashAttribute("mensaje", "La cantidad debe ser mayor a cero.");
            return "redirect:" + (request.getHeader("Referer") != null ? request.getHeader("Referer") : "/admin/dashboard");
        }

        reabastecimientoService.crearReabastecimiento(medicamento, cantidad);

        redirectAttributes.addFlashAttribute("tipo", "warning");
        redirectAttributes.addFlashAttribute("mensaje", "Solicitud de reabastecimiento enviada al Manager para revisión.");

        return "redirect:" + (request.getHeader("Referer") != null ? request.getHeader("Referer") : "/admin/dashboard");
    }
}