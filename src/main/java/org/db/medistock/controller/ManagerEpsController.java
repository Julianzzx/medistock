package org.db.medistock.controller;

import org.db.medistock.model.Eps;
import org.db.medistock.service.EpsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/eps")
public class ManagerEpsController {

    private final EpsService epsService;

    public ManagerEpsController(EpsService epsService) {
        this.epsService = epsService;
    }

    // Listar y Buscar EPS

    @GetMapping
    public String listarEps(
            Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword) {

        int pageSize = 5;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by("nombre").ascending());
        Page<Eps> epsPage = epsService.listarEpsPaginadas(keyword, pageable);

        model.addAttribute("epsList", epsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", epsPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("eps", new Eps()); // Objeto vacío para el formulario de registro

        return "manager/eps";
    }

    // Guardar  EPS

    @PostMapping("/crear")
    public String crearEps(@ModelAttribute Eps eps) {
        epsService.guardarEps(eps);
        return "redirect:/manager/eps";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditarEps(@PathVariable Long id,
                                      Model model,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(required = false) String keyword) {
        Eps eps = epsService.buscarEpsPorId(id);
        if (eps == null) {
            return "redirect:/manager/eps";
        }

        int pageSize = 5;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), pageSize, Sort.by("nombre").ascending());
        Page<Eps> epsPage = epsService.listarEpsPaginadas(keyword, pageable);

        model.addAttribute("epsList", epsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", epsPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("eps", eps); // Rellena el formulario con los datos a editar

        return "manager/eps";
    }

    // Procesar la Actualización de la EPS

    @PostMapping("/editar/{id}")
    public String editarEps(@PathVariable Long id, @ModelAttribute Eps epsEditada) {
        epsEditada.setId(id);
        epsService.guardarEps(epsEditada);
        return "redirect:/manager/eps";
    }

    // Eliminar  EPS

    @GetMapping("/eliminar/{id}")
    public String eliminarEps(@PathVariable Long id) {
        epsService.eliminarEps(id);
        return "redirect:/manager/eps";
    }
}