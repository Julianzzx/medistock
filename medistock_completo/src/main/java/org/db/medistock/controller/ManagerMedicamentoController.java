package org.db.medistock.controller;

import org.db.medistock.model.Farmacia;
import org.db.medistock.model.Medicamento;
import org.db.medistock.service.MedicamentoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/farmacias/{farmaciaId}/medicamentos")
public class ManagerMedicamentoController {

    private final MedicamentoService medicamentoService;

    public ManagerMedicamentoController(MedicamentoService medicamentoService) {
        this.medicamentoService = medicamentoService;
    }

    private void cargarVistaMedicamentos(Long farmaciaId, String keyword, int page, int size, Model model, Medicamento medicamentoSeleccionado) {
        Farmacia farmacia = medicamentoService.buscarFarmacia(farmaciaId);
        if (farmacia == null) return;

        Pageable pageable = PageRequest.of(Math.max(0, page), size, Sort.by("nombre").ascending());
        Page<Medicamento> medicamentoPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            medicamentoPage = medicamentoService.medicamentosPorFarmacia(farmaciaId, keyword.trim(), pageable);
        } else {
            medicamentoPage = medicamentoService.medicamentosPorFarmacia(farmaciaId, null, pageable);
        }

        model.addAttribute("farmaciaSeleccionada", farmacia);
        model.addAttribute("medicamentos", medicamentoPage.getContent());
        model.addAttribute("currentPage", medicamentoPage.getNumber());
        model.addAttribute("totalPages", medicamentoPage.getTotalPages());
        model.addAttribute("medicamento", medicamentoSeleccionado != null ? medicamentoSeleccionado : new Medicamento());
        model.addAttribute("keyword", keyword);
    }

    // Buscar Medicamentos

    @GetMapping
    public String listarMedicamentos(@PathVariable Long farmaciaId,
                                     @RequestParam(name = "keyword", required = false) String keyword,
                                     @RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "5") int size,
                                     Model model) {
        Farmacia farmacia = medicamentoService.buscarFarmacia(farmaciaId);
        if (farmacia == null) return "errorAD";

        cargarVistaMedicamentos(farmaciaId, keyword, page, size, model, new Medicamento());
        return "manager/medicamentos";
    }

    // Crear Medicamento

    @PostMapping("/crear")
    public String crearMedicamento(@PathVariable Long farmaciaId,
                                   @ModelAttribute Medicamento medicamento) {
        if (medicamento.getCantidad() < 0) {
            return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos?error=CantidadNoValida";
        }
        Farmacia farmacia = medicamentoService.buscarFarmacia(farmaciaId);
        if (farmacia == null) return "errorAD";

        medicamento.setFarmacia(farmacia);
        medicamentoService.guardarMedicamento(medicamento);
        return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos";
    }

    @GetMapping("/editar/{id}")
    public String editarMedicamentoForm(@PathVariable Long farmaciaId,
                                        @PathVariable Long id,
                                        @RequestParam(name = "keyword", required = false) String keyword,
                                        @RequestParam(name = "page", defaultValue = "0") int page,
                                        @RequestParam(name = "size", defaultValue = "5") int size,
                                        Model model) {
        Medicamento medicamento = medicamentoService.buscarMedicamento(id);
        Farmacia farmacia = medicamentoService.buscarFarmacia(farmaciaId);
        if (medicamento == null || farmacia == null) return "errorAD";

        cargarVistaMedicamentos(farmaciaId, keyword, page, size, model, medicamento);
        return "manager/medicamentos";
    }

    @PostMapping("/editar/{id}")
    public String guardarEdicionMedicamento(@PathVariable Long farmaciaId,
                                            @PathVariable Long id,
                                            @ModelAttribute Medicamento medicamentoEditado) {
        if (medicamentoEditado.getCantidad() < 0) {
            return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos/editar/" + id + "?error=CantidadNoValida";
        }

        Medicamento medicamento = medicamentoService.buscarMedicamento(id);
        Farmacia farmacia = medicamentoService.buscarFarmacia(farmaciaId);
        if (medicamento == null || farmacia == null) return "errorAD";

        medicamento.setNombre(medicamentoEditado.getNombre());
        medicamento.setDescripcion(medicamentoEditado.getDescripcion());
        medicamento.setConcentracion(medicamentoEditado.getConcentracion());
        medicamento.setPresentacion(medicamentoEditado.getPresentacion());
        medicamento.setCantidad(medicamentoEditado.getCantidad());
        medicamento.setLaboratorio(medicamentoEditado.getLaboratorio());
        medicamento.setFarmacia(farmacia);

        medicamentoService.guardarMedicamento(medicamento);
        return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos";
    }

    // Eliminar Medicamento
    @GetMapping("/eliminar/{id}")
    public String eliminarMedicamento(@PathVariable Long farmaciaId, @PathVariable Long id) {
        Medicamento medicamento = medicamentoService.buscarMedicamento(id);
        if (medicamento == null) return "errorAD";

        if (medicamento.getFarmacia() == null || !medicamento.getFarmacia().getId().equals(farmaciaId)) {
            return "errorAD";
        }

        medicamentoService.eliminarMedicamento(id);
        return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos";
    }
}