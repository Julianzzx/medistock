package org.db.medistock.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.db.medistock.model.Cita;
import org.db.medistock.model.Eps;
import org.db.medistock.model.Usuario;
import org.db.medistock.repository.EpsRepository;
import org.db.medistock.repository.UsuarioRepository;
import org.db.medistock.service.CitaService;
import org.db.medistock.service.EpsService;
import org.db.medistock.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final CitaService citaService;
    private final EpsService epsService;
    private final UsuarioRepository usuarioRepository;
    private final EpsRepository epsRepository;

    public UsuarioController(UsuarioService usuarioService,
                             CitaService citaService,
                             EpsService epsService,
                             UsuarioRepository usuarioRepository,
                             EpsRepository epsRepository) {
        this.usuarioService = usuarioService;
        this.citaService = citaService;
        this.epsService = epsService;
        this.usuarioRepository = usuarioRepository;
        this.epsRepository = epsRepository;
    }

    private Usuario obtenerUsuarioAutenticado(Authentication auth) {
        return usuarioService.buscarPorEmail(auth.getName());
    }

    private boolean esUsuarioOAuth(Authentication auth) {
        return auth instanceof OAuth2AuthenticationToken || (auth != null && auth.getPrincipal() instanceof OAuth2User);
    }

    // Mostrar Formulario De Registro

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("epsList", epsService.listarTodasEps());
        return "registro";
    }

    // Registrar Usuario Nuevo

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario) {
        usuarioService.registrarUsuario(usuario);
        return "redirect:/index?registrado=true";
    }

    // Ver Vista de Cita

    @GetMapping("/usuario/generar-cita")
    public String verGenerarCita(Authentication auth, Model model) {
        Usuario usuario = obtenerUsuarioAutenticado(auth);
        Cita cita = citaService.obtenerCitaActual(usuario);
        if (cita != null) {
            model.addAttribute("cita", cita);
        }
        return "usuario/generar-cita";
    }

    // Cita Nueva

    @PostMapping("/usuario/generar-cita")
    public String generarCita(Authentication auth, Model model) {
        Usuario usuario = obtenerUsuarioAutenticado(auth);
        CitaService.CitaResultado resultado = citaService.generarNuevaCita(usuario);

        model.addAttribute("cita", resultado.getCita());
        model.addAttribute("msg", resultado.getMensaje());

        return "usuario/generar-cita";
    }

    // Ver Perfil

    @GetMapping("/usuario/perfil")
    public String verPerfil(Authentication auth, Model model) {
        if (auth == null) {
            return "redirect:/login";
        }

        Usuario usuario = obtenerUsuarioAutenticado(auth);
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("epsList", epsRepository.findAll());
        return "usuario/perfil";
    }

    // Formulario Editar Perfil

    @GetMapping("/usuario/editar")
    public String editarPerfil(Model model, Authentication auth) {
        if (auth == null)
            return "redirect:/login";

        Usuario usuario = obtenerUsuarioAutenticado(auth);
        if (usuario == null)
            return "redirect:/login";

        usuario.setPassword("");

        boolean isOAuthUser = esUsuarioOAuth(auth);

        model.addAttribute("usuario", usuario);
        model.addAttribute("isOAuthUser", isOAuthUser);
        model.addAttribute("epsList", epsRepository.findAll());
        return "usuario/editar-perfil";
    }

    @PostMapping("/usuario/editar")
    public String actualizarPerfil(@ModelAttribute Usuario usuarioEditado,
                                   Authentication auth,
                                   HttpServletRequest request,
                                   Model model) {
        if (auth == null)
            return "redirect:/login";

        Usuario usuarioActual = obtenerUsuarioAutenticado(auth);
        if (usuarioActual == null)
            return "redirect:/login";

        boolean isOAuthUser = esUsuarioOAuth(auth);

        if (isOAuthUser) {
            usuarioEditado.setEmail(usuarioActual.getEmail());
            usuarioEditado.setPassword(null);
        }

        boolean correoCambiado = !usuarioActual.getEmail().equals(usuarioEditado.getEmail());
        boolean passwordCambiada = !isOAuthUser && usuarioEditado.getPassword() != null && !usuarioEditado.getPassword().isBlank();

        usuarioService.actualizarPerfilUsuario(usuarioActual.getId(), usuarioEditado);

        if (correoCambiado) {
            request.getSession().invalidate();
            return "redirect:/login?correoCambiado=true";
        }

        if (passwordCambiada) {
            request.getSession().invalidate();
            return "redirect:/login?passwordCambiado=true";
        }

        Usuario usuarioActualizado = obtenerUsuarioAutenticado(auth);
        model.addAttribute("usuario", usuarioActualizado);
        model.addAttribute("isOAuthUser", isOAuthUser);
        model.addAttribute("epsList", epsRepository.findAll());
        model.addAttribute("mensajeExito", "Datos actualizados correctamente.");

        return "usuario/editar-perfil";
    }
}