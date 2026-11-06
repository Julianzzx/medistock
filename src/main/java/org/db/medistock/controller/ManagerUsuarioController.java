package org.db.medistock.controller;

import org.db.medistock.model.Usuario;
import org.db.medistock.service.EpsService;
import org.db.medistock.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/usuarios")
public class ManagerUsuarioController {

    private final UsuarioService usuarioService;
    private final EpsService epsService;

    public ManagerUsuarioController(UsuarioService usuarioService, EpsService epsService) {
        this.usuarioService = usuarioService;
        this.epsService = epsService;
    }

    private void cargarVistaUsuarios(String keyword, int page, int size, Model model, Usuario usuarioSeleccionado) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by("nombre").ascending());
        Page<Usuario> usuarioPage = usuarioService.mostrarUsuarios(keyword, pageable);

        model.addAttribute("usuarios", usuarioPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usuarioPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("usuario", usuarioSeleccionado != null ? usuarioSeleccionado : new Usuario());
        model.addAttribute("epsList", epsService.listarTodasEps());
    }

    //Buscar Usuarios

    @GetMapping
    public String listarUsuarios(
            Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword) {

        cargarVistaUsuarios(keyword, page, size, model, new Usuario());
        return "manager/usuarios";
    }

    // Crear Usuario

    @PostMapping("/crear")
    public String crearUsuario(@ModelAttribute Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
            usuario.setRol("USER");
        }
        usuarioService.crearUsuario(usuario);
        return "redirect:/manager/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String editarUsuarioForm(@PathVariable Long id,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "5") int size,
                                    @RequestParam(required = false) String keyword,
                                    Model model) {
        Usuario usuario = usuarioService.buscarUsuario(id);
        if (usuario == null) {
            return "redirect:/manager/usuarios?error=UsuarioNoEncontrado";
        }

        cargarVistaUsuarios(keyword, page, size, model, usuario);
        return "manager/usuarios";
    }

    @PostMapping("/editar/{id}")
    public String editarUsuario(@PathVariable Long id, @ModelAttribute Usuario usuarioEditado) {
        usuarioService.editarUsuario(id, usuarioEditado);
        return "redirect:/manager/usuarios";
    }

    // Eliminar Usuario

    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return "redirect:/manager/usuarios";
    }
}