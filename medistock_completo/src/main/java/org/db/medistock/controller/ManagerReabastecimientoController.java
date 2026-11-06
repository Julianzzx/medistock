package org.db.medistock.controller;

import org.db.medistock.model.Reabastecimiento;
import org.db.medistock.service.ReabastecimientoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/reabastecimientos")
public class ManagerReabastecimientoController {

    private final ReabastecimientoService reabastecimientoService;

    public ManagerReabastecimientoController(ReabastecimientoService reabastecimientoService) {
        this.reabastecimientoService = reabastecimientoService;
    }

    @GetMapping
    public String listarReabastecimientos(
            Model model,
            @RequestParam(defaultValue = "0") int pagePendientes,
            @RequestParam(defaultValue = "0") int pageAceptadas,
            @RequestParam(defaultValue = "0") int pageRechazadas) {

        int pageSize = 5;

        Pageable pageableP = PageRequest.of(Math.max(0, pagePendientes), pageSize, Sort.by("id").descending());
        Pageable pageableA = PageRequest.of(Math.max(0, pageAceptadas), pageSize, Sort.by("id").descending());
        Pageable pageableR = PageRequest.of(Math.max(0, pageRechazadas), pageSize, Sort.by("id").descending());

        Page<Reabastecimiento> pendientes = reabastecimientoService.buscarPorEstado(Reabastecimiento.EstadoReabastecimiento.EN_PROCESO, pageableP);
        Page<Reabastecimiento> aceptadas = reabastecimientoService.buscarPorEstado(Reabastecimiento.EstadoReabastecimiento.ACEPTADA, pageableA);
        Page<Reabastecimiento> rechazadas = reabastecimientoService.buscarPorEstado(Reabastecimiento.EstadoReabastecimiento.RECHAZADA, pageableR);

        model.addAttribute("solicitudesPendientes", pendientes.getContent());
        model.addAttribute("pendientesPage", pendientes);

        model.addAttribute("solicitudesAceptadas", aceptadas.getContent());
        model.addAttribute("aceptadasPage", aceptadas);

        model.addAttribute("solicitudesRechazadas", rechazadas.getContent());
        model.addAttribute("rechazadasPage", rechazadas);

        return "manager/reabastecimiento";
    }

    @GetMapping("/aceptar/{id}")
    public String aceptarReabastecimiento(@PathVariable Long id) {
        reabastecimientoService.aceptarReabastecimiento(id);
        return "redirect:/manager/reabastecimientos";
    }

    @GetMapping("/rechazar/{id}")
    public String rechazarReabastecimiento(@PathVariable Long id) {
        reabastecimientoService.rechazarReabastecimiento(id);
        return "redirect:/manager/reabastecimientos";
    }
}