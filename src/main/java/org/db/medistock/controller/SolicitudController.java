package org.db.medistock.controller;

import org.db.medistock.model.Solicitud;
import org.db.medistock.model.Usuario;
import org.db.medistock.service.SolicitudService;
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
@RequestMapping("/usuario/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final UsuarioService usuarioService;

    public SolicitudController(SolicitudService solicitudService, UsuarioService usuarioService) {
        this.solicitudService = solicitudService;
        this.usuarioService = usuarioService;
    }

    private Usuario obtenerUsuarioAutenticado(Authentication auth) {
        return usuarioService.buscarPorEmail(auth.getName());
    }

    //  Buscar Solicitudes

    @GetMapping
    public String listarSolicitudesUsuario(
            Authentication auth,
            Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String estado) {

        Usuario usuario = obtenerUsuarioAutenticado(auth);

        int pageSize = 5;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("id").descending());
        Page<Solicitud> solicitudPage;

        Solicitud.EstadoSolicitud estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = Solicitud.EstadoSolicitud.valueOf(estado);
            } catch (IllegalArgumentException e) {
                estadoEnum = null;
            }
        }

        if ((keyword != null && !keyword.isEmpty()) && estadoEnum != null) {
            solicitudPage = solicitudService.buscarPorKeywordYEstado(usuario, keyword, estadoEnum, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            solicitudPage = solicitudService.buscarPorKeyword(usuario, keyword, pageable);
        } else if (estadoEnum != null) {
            solicitudPage = solicitudService.buscarPorEstado(usuario, estadoEnum, pageable);
        } else {
            solicitudPage = solicitudService.buscarUsuario(usuario, pageable);
        }

        model.addAttribute("solicitudList", solicitudPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", solicitudPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("estado", estado); // Mantiene el estado seleccionado en el select de la vista

        return "usuario/solicitudes";
    }

    // Mostrar Formulario para Crear una Nueva Solicitud

    @GetMapping("/nuevo")
    public String formularioCrearSolicitud(Model model) {
        model.addAttribute("solicitud", new Solicitud());
        return "usuario/solicitud-form";
    }

    //  Crear Solicitud

    @PostMapping("/crear")
    public String crearSolicitud(Authentication auth, @ModelAttribute Solicitud solicitud) {
        Usuario usuario = obtenerUsuarioAutenticado(auth);
        solicitudService.crearSolicitud(solicitud, usuario);
        return "redirect:/usuario/solicitudes";
    }
}