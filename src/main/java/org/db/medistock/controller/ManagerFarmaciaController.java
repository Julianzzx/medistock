package org.db.medistock.controller;

import org.db.medistock.model.Eps;
import org.db.medistock.model.Farmacia;
import org.db.medistock.model.Medicamento;
import org.db.medistock.repository.EpsRepository;
import org.db.medistock.repository.FarmaciaRepository;
import org.db.medistock.repository.MedicamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/manager")
public class ManagerFarmaciaController {

    private final EpsRepository epsRepository;
    private final FarmaciaRepository farmaciaRepo;
    private final MedicamentoRepository medicamentoRepo;

    public ManagerFarmaciaController(EpsRepository epsRepository,
                                     FarmaciaRepository farmaciaRepo,
                                     MedicamentoRepository medicamentoRepo) {
        this.epsRepository = epsRepository;
        this.farmaciaRepo = farmaciaRepo;
        this.medicamentoRepo = medicamentoRepo;
    }

    @GetMapping("/farmacias")
    public String farmacias(@RequestParam(name = "keyword", required = false) String keyword,
                            @RequestParam(name = "page", defaultValue = "1") int page,
                            @RequestParam(name = "size", defaultValue = "5") int size,
                            Model model) {

        Page<Farmacia> farmaciaPage;

        if (keyword != null && !keyword.isEmpty()) {
            farmaciaPage = farmaciaRepo.findByNombreContainingIgnoreCase(keyword,
                    PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre")));
        } else {
            farmaciaPage = farmaciaRepo.findAll(PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre")));
        }

        model.addAttribute("farmacias", farmaciaPage.getContent());
        model.addAttribute("farmaciaList", farmaciaPage.getContent()); // Soporte para ambos nombres de atributo en la vista
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", farmaciaPage.getTotalPages());
        model.addAttribute("farmacia", new Farmacia());
        model.addAttribute("keyword", keyword);
        model.addAttribute("epsList", epsRepository.findAll());

        return "manager/farmacias";
    }

    @PostMapping("/farmacias/crear")
    public String crearFarmacia(@ModelAttribute Farmacia farmacia,
                                @RequestParam("epsId") Long epsId) {
        Eps eps = epsRepository.findById(epsId).orElse(null);
        if (eps != null) {
            farmacia.setEps(eps);
        }
        farmaciaRepo.save(farmacia);
        return "redirect:/manager/farmacias";
    }

    @GetMapping("/farmacias/editar/{id}")
    public String editarFarmaciaForm(@PathVariable Long id,
                                     @RequestParam(name = "keyword", required = false) String keyword,
                                     @RequestParam(name = "page", defaultValue = "1") int page,
                                     @RequestParam(name = "size", defaultValue = "5") int size,
                                     Model model) {
        Farmacia farmacia = farmaciaRepo.findById(id).orElse(null);
        if (farmacia == null)
            return "errorAD";

        Page<Farmacia> farmaciaPage;
        if (keyword != null && !keyword.isEmpty()) {
            farmaciaPage = farmaciaRepo.findByNombreContainingIgnoreCase(keyword,
                    PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre")));
        } else {
            farmaciaPage = farmaciaRepo.findAll(PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre")));
        }

        model.addAttribute("farmacias", farmaciaPage.getContent());
        model.addAttribute("farmaciaList", farmaciaPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", farmaciaPage.getTotalPages());
        model.addAttribute("farmacia", farmacia);
        model.addAttribute("epsList", epsRepository.findAll());
        model.addAttribute("keyword", keyword);

        return "manager/farmacias";
    }

    @PostMapping("/farmacias/editar/{id}")
    public String guardarEdicionFarmacia(@PathVariable Long id,
                                         @ModelAttribute Farmacia farmaciaEditada,
                                         @RequestParam("epsId") Long epsId) {
        Farmacia farmacia = farmaciaRepo.findById(id).orElse(null);
        if (farmacia == null)
            return "errorAD";

        farmacia.setNombre(farmaciaEditada.getNombre());
        farmacia.setDireccion(farmaciaEditada.getDireccion());

        Eps eps = epsRepository.findById(epsId).orElse(null);
        if (eps != null) {
            farmacia.setEps(eps);
        }

        farmaciaRepo.save(farmacia);
        return "redirect:/manager/farmacias";
    }

    @GetMapping("/farmacias/eliminar/{id}")
    public String eliminarFarmacia(@PathVariable Long id) {
        Farmacia farmacia = farmaciaRepo.findById(id).orElse(null);
        if (farmacia == null)
            return "errorAD";

        List<Medicamento> medicamentosAsignados = medicamentoRepo.findByFarmacia(farmacia);
        if (!medicamentosAsignados.isEmpty()) {
            return "errorAD"; // O redirigir a una vista/alerta indicando que no se puede eliminar porque tiene medicamentos
        }

        farmaciaRepo.deleteById(id);
        return "redirect:/manager/farmacias";
    }

    @GetMapping("/eps/select")
    public String seleccionarEps(Model model) {
        List<Eps> epsList = epsRepository.findAll();
        model.addAttribute("epsList", epsList);
        return "manager/eps-select";
    }

    @GetMapping("/eps/{id}/farmacias")
    public String verFarmaciasPorEps(@PathVariable Long id, Model model) {
        Eps eps = epsRepository.findById(id).orElse(null);

        if (eps == null) {
            return "redirect:/manager/eps/select";
        }

        model.addAttribute("eps", eps);
        return "manager/farmacias-por-eps";
    }

    @GetMapping("/sura_farmcia")
    public String farmaciaSura(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        Optional<Eps> suraOpt = epsRepository.findById(2L);

        Page<Farmacia> farmaciasPage;
        if (suraOpt.isPresent()) {
            Eps sura = suraOpt.get();
            Pageable pageable = PageRequest.of(page, 6);
            if (keyword != null && !keyword.isEmpty()) {
                farmaciasPage = farmaciaRepo.findByNombreContainingIgnoreCaseAndEpsId(keyword, sura.getId(), pageable);
            } else {
                farmaciasPage = farmaciaRepo.findByEpsId(sura.getId(), pageable);
            }

            model.addAttribute("farmacias", farmaciasPage.getContent());
            model.addAttribute("currentPage", farmaciasPage.getNumber());
            model.addAttribute("totalPages", farmaciasPage.getTotalPages());
        } else {
            model.addAttribute("farmacias", new ArrayList<>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
        }

        model.addAttribute("keyword", keyword);
        return "manager/sura_farmacia";
    }

    @GetMapping("/Sanita_farmacia")
    public String farmaciaSanitas(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model) {

        Optional<Eps> sanitasOpt = epsRepository.findById(1L);
        Page<Farmacia> farmaciaPage = Page.empty();

        if (sanitasOpt.isPresent()) {
            Eps sanitas = sanitasOpt.get();
            Pageable pageable = PageRequest.of(page, size, Sort.by("nombre"));

            if (keyword != null && !keyword.isEmpty()) {
                farmaciaPage = farmaciaRepo.findByEpsIdAndNombreContainingIgnoreCase(sanitas.getId(), keyword, pageable);
            } else {
                farmaciaPage = farmaciaRepo.findByEpsId(sanitas.getId(), pageable);
            }
        }

        model.addAttribute("farmacias", farmaciaPage.getContent());
        model.addAttribute("currentPage", farmaciaPage.getNumber());
        model.addAttribute("totalPages", farmaciaPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "manager/sanita_farmacia";
    }
}