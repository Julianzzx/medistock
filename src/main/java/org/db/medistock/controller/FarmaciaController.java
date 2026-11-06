package org.db.medistock.controller;

import org.db.medistock.model.Farmacia;
import org.db.medistock.model.Medicamento;
import org.db.medistock.model.Usuario;
import org.db.medistock.service.FarmaciaService;
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

@Controller
@RequestMapping("/usuario")
public class FarmaciaController {

    private final FarmaciaService farmaciaService;
    private final MedicamentoService medicamentoService;
    private final UsuarioService usuarioService;

    public FarmaciaController(FarmaciaService farmaciaService,
                              MedicamentoService medicamentoService,
                              UsuarioService usuarioService) {
        this.farmaciaService = farmaciaService;
        this.medicamentoService = medicamentoService;
        this.usuarioService = usuarioService;
    }

    private Usuario obtenerUsuarioAutenticado(Authentication auth) {
        return usuarioService.buscarPorEmail(auth.getName());
    }

    // Listar Farmacias

    @GetMapping("/farmacias")
    public String listarFarmaciasUsuario(
            Authentication auth,
            Model model,
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "1") int page) {

        if (auth == null) {
            return "redirect:/login";
        }

        Usuario usuario = obtenerUsuarioAutenticado(auth);
        if (usuario == null || usuario.getEps() == null) {
            model.addAttribute("error", "No tienes una EPS asignada en tu perfil.");
            model.addAttribute("farmacias", Page.empty());
            model.addAttribute("nombreEps", "Sin EPS");
            return "usuario/farmacias";
        }

        Long epsId = usuario.getEps().getId();
        int pageSize = 6;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("nombre").ascending());
        Page<Farmacia> farmaciaPage;

        if (nombre != null && !nombre.trim().isEmpty()) {
            farmaciaPage = farmaciaService.buscarFarmaciasPorEps(epsId, nombre.trim(), pageable);
        } else {
            farmaciaPage = farmaciaService.buscarFarmaciasPorEps(epsId, null, pageable);
        }

        model.addAttribute("farmacias", farmaciaPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", farmaciaPage.getTotalPages());
        model.addAttribute("nombre", nombre);
        model.addAttribute("nombreEps", usuario.getEps().getNombre());

        return "usuario/farmacias";
    }

    // Redirigir a la vista de medicamentos de la farmacia seleccionada

    @GetMapping("/farmacia/{id}")
    public String verMedicamentosPorFarmacia(
            @PathVariable("id") Long farmaciaId,
            Authentication auth) {

        if (auth == null) {
            return "redirect:/login";
        }

        Usuario usuario = obtenerUsuarioAutenticado(auth);
        if (usuario == null || usuario.getEps() == null) {
            return "redirect:/usuario/farmacias";
        }

        Farmacia farmacia = medicamentoService.buscarFarmacia(farmaciaId);
        if (farmacia == null || farmacia.getEps() == null || !farmacia.getEps().getId().equals(usuario.getEps().getId())) {
            return "redirect:/usuario/farmacias?error=AccesoDenegado";
        }

        // Redirige a la ruta MedicamentoController

        return "redirect:/usuario/farmacias/" + farmaciaId + "/medicamentos";
    }
}