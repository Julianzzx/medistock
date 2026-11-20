package org.db.medistock.controller;

import org.db.medistock.service.OptimizacionOperativaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/manager/optimizacion")
public class ManagerOptimizacionController {

    private final OptimizacionOperativaService optimizacionService;

    public ManagerOptimizacionController(OptimizacionOperativaService optimizacionService) {
        this.optimizacionService = optimizacionService;
    }

    @GetMapping("/resolver")
    public ResponseEntity<?> resolver(
            @RequestParam(defaultValue = "100") int capacidadPorAdmin,
            @RequestParam(required = false) Integer maxSolicitudes,
            @RequestParam(required = false) Integer maxAdmins,
            @RequestParam(defaultValue = "B") String objetivo) {
        try {
            if (capacidadPorAdmin <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "capacidadPorAdmin debe ser mayor que 0."));
            }

            int solicitudesReales = (maxSolicitudes != null && maxSolicitudes > 0)
                    ? maxSolicitudes
                    : optimizacionService.obtenerTotalSolicitudesReales();
            int adminsReales = (maxAdmins != null && maxAdmins > 0)
                    ? maxAdmins
                    : optimizacionService.obtenerTotalAdminsReales();

            String objetivoNormalizado = (objetivo == null || objetivo.isBlank())
                    ? "B"
                    : objetivo.trim().toUpperCase();

            return ResponseEntity.ok(optimizacionService.resolver(
                    capacidadPorAdmin, solicitudesReales, adminsReales, objetivoNormalizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        }
    }
}