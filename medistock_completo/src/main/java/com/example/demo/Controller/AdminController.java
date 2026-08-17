package com.example.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Model.Eps;
import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Medicamento;
import com.example.demo.Model.Solicitud;
import com.example.demo.service.AdminService;
import com.example.demo.service.ResultadoAccion;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller del rol Admin. Solo recibe la petición HTTP, delega toda la
 * lógica de negocio en AdminService y traduce el resultado a una vista o un
 * redirect. No contiene reglas de negocio, cálculos ni acceso directo a
 * repositorios.
 */
@Controller
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model, Authentication auth) {
        Eps eps = adminService.obtenerEpsDelAdmin(auth);

        model.addAttribute("pendientes", adminService.contarSolicitudesPendientes(eps));
        model.addAttribute("isManager", adminService.esManager(auth));

        if (eps != null && eps.getNombre().equalsIgnoreCase("SURA")) {
            return "admin/Dashboard-Sura";
        }
        return "admin/dashboard";
    }

    @GetMapping("/admin/solicitudes")
    public String listarSolicitudesUsuarios(Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "farmaciaId", required = false) Long farmaciaId,
            Authentication auth) {

        Eps eps = adminService.obtenerEpsDelAdmin(auth);
        if (eps == null) {
            model.addAttribute("error", "No se pudo determinar la EPS del administrador.");
            return "admin/solicitudes";
        }

        Page<Solicitud> solicitudesPage = adminService.listarSolicitudesEnProceso(eps, farmaciaId, page);

        model.addAttribute("solicitudes", solicitudesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", solicitudesPage.getTotalPages());
        model.addAttribute("isManager", adminService.esManager(auth));
        model.addAttribute("eps", eps.getNombre());
        model.addAttribute("farmacias", adminService.listarFarmaciasDeEps(eps));
        model.addAttribute("farmaciaId", farmaciaId);

        return "admin/solicitudes";
    }

    @GetMapping("/admin/solicitudes/buscar")
    public String buscarSolicitudesUsuarios(@RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "1") int page,
            Model model,
            Authentication auth) {

        Eps eps = adminService.obtenerEpsDelAdmin(auth);
        if (eps == null) {
            model.addAttribute("error", "No se pudo determinar la EPS del administrador.");
            return "admin/solicitudes";
        }

        Page<Solicitud> solicitudesPage = adminService.buscarSolicitudes(eps, keyword, page);

        model.addAttribute("solicitudes", solicitudesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", solicitudesPage.getTotalPages());
        model.addAttribute("isManager", adminService.esManager(auth));
        model.addAttribute("eps", eps.getNombre());
        model.addAttribute("keyword", keyword);

        return "admin/solicitudes";
    }

    @GetMapping("/admin/farmacias")
    public String listarFarmaciasPorEps(Model model,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication auth) {

        Eps eps = adminService.obtenerEpsDelAdmin(auth);
        if (eps == null) {
            model.addAttribute("error", "No se pudo determinar la EPS del administrador.");
            return "admin/farmacias";
        }

        Page<Farmacia> pageFarmacias = adminService.listarFarmacias(eps, nombre, page);

        model.addAttribute("farmacias", pageFarmacias.getContent());
        model.addAttribute("currentPage", pageFarmacias.getNumber());
        model.addAttribute("totalPages", pageFarmacias.getTotalPages());
        model.addAttribute("isManager", adminService.esManager(auth));
        model.addAttribute("eps", eps.getNombre());

        return "admin/farmacias";
    }

    @GetMapping("/admin/farmacia/{id}/medicamentos")
    public String listarMedicamentosPorFarmacia(@PathVariable Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "stock", required = false) String stock,
            Model model,
            Authentication auth) {

        Page<Medicamento> pageMedicamentos = adminService.listarMedicamentosPorFarmacia(id, nombre, stock, page);

        model.addAttribute("medicamentos", pageMedicamentos.getContent());
        model.addAttribute("currentPage", pageMedicamentos.getNumber() + 1);
        model.addAttribute("totalPages", pageMedicamentos.getTotalPages());
        model.addAttribute("farmaciaId", id);
        model.addAttribute("isManager", adminService.esManager(auth));
        model.addAttribute("stock", stock);

        return "admin/medicamentos";
    }

    @PostMapping("/admin/solicitud/{id}/aceptar")
    public String aceptarSolicitudUsuario(@PathVariable Long id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            RedirectAttributes redirectAttributes,
            Authentication auth) {

        if (adminService.esManager(auth)) {
            return "redirect:/admin/solicitudes";
        }

        aplicarResultado(adminService.aceptarSolicitud(id), redirectAttributes);

        return redirectListaSolicitudes(keyword, page);
    }

    @PostMapping("/admin/solicitud/{id}/rechazar")
    public String rechazarSolicitudUsuario(@PathVariable Long id,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            RedirectAttributes redirectAttributes,
            Authentication auth) {

        if (adminService.esManager(auth)) {
            return "redirect:/admin/solicitudes";
        }

        aplicarResultado(adminService.rechazarSolicitud(id), redirectAttributes);

        return redirectListaSolicitudes(keyword, page);
    }

    @PostMapping("/admin/solicitudes/aceptar-todas")
    public String aceptarTodasSolicitudesPorEps(Authentication auth, RedirectAttributes redirectAttributes) {
        if (adminService.esManager(auth)) {
            aplicarResultado(ResultadoAccion.error("No tienes permisos para esta acción."), redirectAttributes);
            return "redirect:/admin/solicitudes";
        }

        Eps eps = adminService.obtenerEpsDelAdmin(auth);
        if (eps == null) {
            aplicarResultado(ResultadoAccion.error("No se pudo determinar la EPS del administrador."), redirectAttributes);
            return "redirect:/admin/solicitudes";
        }

        aplicarResultado(adminService.aceptarTodasEnProceso(eps), redirectAttributes);
        return "redirect:/admin/solicitudes";
    }

    @PostMapping("/admin/solicitudes/rechazar-todas")
    public String rechazarTodasSolicitudesPorEps(Authentication auth, RedirectAttributes redirectAttributes) {
        if (adminService.esManager(auth)) {
            aplicarResultado(ResultadoAccion.error("No tienes permisos para esta acción."), redirectAttributes);
            return "redirect:/admin/solicitudes";
        }

        Eps eps = adminService.obtenerEpsDelAdmin(auth);
        if (eps == null) {
            aplicarResultado(ResultadoAccion.error("No se pudo determinar la EPS del administrador."), redirectAttributes);
            return "redirect:/admin/solicitudes";
        }

        aplicarResultado(adminService.rechazarTodasEnProceso(eps), redirectAttributes);
        return "redirect:/admin/solicitudes";
    }

    @PostMapping("/admin/solicitud-reabastecimiento/{id}")
    public String crearSolicitudReabastecimiento(
            @PathVariable Long id,
            @RequestParam int cantidad,
            RedirectAttributes redirectAttributes,
            Authentication auth,
            HttpServletRequest request) {

        if (adminService.esManager(auth)) {
            return "redirect:" + request.getHeader("Referer");
        }

        aplicarResultado(adminService.crearSolicitudReabastecimiento(id, cantidad), redirectAttributes);

        return "redirect:" + request.getHeader("Referer");
    }

    private void aplicarResultado(ResultadoAccion resultado, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("tipo", resultado.getTipo());
        redirectAttributes.addFlashAttribute("mensaje", resultado.getMensaje());
    }

    private String redirectListaSolicitudes(String keyword, int page) {
        if (keyword != null && !keyword.isEmpty()) {
            return "redirect:/admin/solicitudes/buscar?keyword=" + keyword + "&page=" + page;
        }
        return "redirect:/admin/solicitudes?page=" + page;
    }
}
