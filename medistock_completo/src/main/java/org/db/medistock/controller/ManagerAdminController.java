package org.db.medistock.controller;

import org.db.medistock.model.Admin;
import org.db.medistock.repository.AdminRepository;
import org.db.medistock.repository.EpsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager")
public class ManagerAdminController {

    private final AdminRepository adminRepo;
    private final EpsRepository epsRepo;
    private final PasswordEncoder passwordEncoder;

    public ManagerAdminController(AdminRepository adminRepo,
                                  EpsRepository epsRepo,
                                  PasswordEncoder passwordEncoder) {
        this.adminRepo = adminRepo;
        this.epsRepo = epsRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/admins")
    public String admins(@RequestParam(name = "keyword", required = false) String keyword,
                         @RequestParam(name = "page", defaultValue = "1") int page,
                         @RequestParam(name = "size", defaultValue = "5") int size,
                         Model model) {

        Page<Admin> adminPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            adminPage = adminRepo
                    .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrUsuarioContainingIgnoreCase(
                            keyword.trim(), keyword.trim(), keyword.trim(),
                            PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre")));
        } else {
            adminPage = adminRepo.findAll(PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre")));
        }

        model.addAttribute("admins", adminPage.getContent());
        model.addAttribute("adminPage", adminPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", adminPage.getTotalPages());
        model.addAttribute("admin", new Admin());
        model.addAttribute("epsList", epsRepo.findAll());
        model.addAttribute("keyword", keyword);

        return "manager/admins";
    }

    @PostMapping("/admins/crear")
    public String crearAdmin(@ModelAttribute Admin admin,
                             @RequestParam(required = false) Long epsId) {

        if (admin.getPassword() != null && !admin.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }

        admin.setRol("ADMIN");

        if (epsId != null) {
            admin.setEps(epsRepo.findById(epsId).orElse(null));
        } else {
            admin.setEps(null);
        }

        adminRepo.save(admin);
        return "redirect:/manager/admins";
    }

    @GetMapping("/admins/editar/{id}")
    public String editarAdminForm(@PathVariable Long id,
                                  @RequestParam(name = "keyword", required = false) String keyword,
                                  @RequestParam(name = "page", defaultValue = "1") int page,
                                  @RequestParam(name = "size", defaultValue = "5") int size,
                                  Model model) {
        Admin admin = adminRepo.findById(id).orElse(null);
        if (admin == null) {
            return "redirect:/manager/admins";
        }

        Page<Admin> adminPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            adminPage = adminRepo
                    .findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrUsuarioContainingIgnoreCase(
                            keyword.trim(), keyword.trim(), keyword.trim(),
                            PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre")));
        } else {
            adminPage = adminRepo.findAll(PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre")));
        }

        model.addAttribute("admins", adminPage.getContent());
        model.addAttribute("adminPage", adminPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", adminPage.getTotalPages());
        model.addAttribute("admin", admin); // Carga el admin a editar en el formulario
        model.addAttribute("epsList", epsRepo.findAll());
        model.addAttribute("keyword", keyword);

        return "manager/admins";
    }

    @PostMapping("/admins/editar/{id}")
    public String actualizarAdmin(@PathVariable Long id,
                                  @ModelAttribute Admin adminForm,
                                  @RequestParam(required = false) Long epsId) {
        Admin adminExistente = adminRepo.findById(id).orElse(null);
        if (adminExistente == null) {
            return "redirect:/manager/admins";
        }

        adminExistente.setNombre(adminForm.getNombre());
        adminExistente.setApellido(adminForm.getApellido());
        adminExistente.setUsuario(adminForm.getUsuario());
        adminExistente.setIdentificacion(adminForm.getIdentificacion());

        if (adminForm.getPassword() != null && !adminForm.getPassword().isBlank()) {
            adminExistente.setPassword(passwordEncoder.encode(adminForm.getPassword()));
        }

        if (epsId != null) {
            adminExistente.setEps(epsRepo.findById(epsId).orElse(null));
        } else {
            adminExistente.setEps(null);
        }

        adminRepo.save(adminExistente);
        return "redirect:/manager/admins";
    }

    @GetMapping("/admins/eliminar/{id}")
    public String eliminarAdmin(@PathVariable Long id) {
        Admin admin = adminRepo.findById(id).orElse(null);
        if (admin == null)
            return "errorAD";
        adminRepo.delete(admin);
        return "redirect:/manager/admins";
    }

    @GetMapping("/powerbi")
    public String mostrarPowerBi() {
        return "manager/powerbi";
    }
}