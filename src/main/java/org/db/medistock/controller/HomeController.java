package org.db.medistock.controller;

import org.db.medistock.model.Admin;
import org.db.medistock.model.Usuario;
import org.db.medistock.service.AdminService;
import org.db.medistock.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AdminService adminService;
    private final UsuarioService usuarioService;

    public HomeController(AdminService adminService, UsuarioService usuarioService) {
        this.adminService = adminService;
        this.usuarioService = usuarioService;
    }

    // Página de Inicio

    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    //Vista De Login

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Dashboard  Usuario

    @GetMapping("/usuario/dashboard")
    public String dashboardUsuario(Authentication auth, Model model) {
        if (auth != null && auth.getName() != null) {
            Usuario usuario = usuarioService.buscarPorEmail(auth.getName());

            if (usuario != null) {
                model.addAttribute("usuario", usuario);

                if (usuario.getEps() != null) {
                    Long epsId = usuario.getEps().getId();

                    if (epsId != null) {
                        if (epsId == 1L) {
                            return "usuario/dashboard-sanitas";
                        } else if (epsId == 2L) {
                            return "usuario/dashboard-sura";
                        }
                    }
                }
            }
        }

        return "usuario/dashboard";
    }

    // Dashboard  Administrador

    @GetMapping("/admin/dashboard")
    public String dashboardAdmin(Authentication auth, Model model) {
        if (auth != null && auth.getName() != null) {
            Admin admin = adminService.buscarPorUsuario(auth.getName());

            if (admin != null) {
                model.addAttribute("admin", admin);

                if (admin.getEps() != null) {
                    Long epsId = admin.getEps().getId();
                    model.addAttribute("epsAsociada", admin.getEps().getNombre());

                    if (epsId == 1L) {
                        return "admin/dashboard-sanitas";
                    } else if (epsId == 2L) {
                        return "admin/dashboard-sura";
                    }
                }
            }
        }
        return "admin/dashboard";
    }

    // Dashboard Manager
    @GetMapping({"/manager", "/manager/dashboard"})
    public String dashboardManager() {
        return "manager/dashboard";
    }
}