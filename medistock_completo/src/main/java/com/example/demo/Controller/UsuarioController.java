package com.example.demo.Controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Model.Eps;
import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Generarcitas;
import com.example.demo.Model.Medicamento;
import com.example.demo.Model.Solicitud;
import com.example.demo.Model.Usuario;
import com.example.demo.service.ResultadoAccion;
import com.example.demo.service.ResultadoActualizacionPerfil;
import com.example.demo.service.ResultadoCita;
import com.example.demo.service.ResultadoSolicitud;
import com.example.demo.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Controller del rol Usuario. Solo recibe la petición HTTP, delega toda la
 * lógica de negocio en UsuarioService y traduce el resultado a una vista o
 * un redirect. No contiene reglas de negocio, cálculos ni acceso directo a
 * repositorios.
 */
@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/usuario/dashboard")
    public String dashboard(Model model, Authentication auth, HttpSession session) {
        if (auth == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());

        if (session.getAttribute("justLoggedIn") == null) {
            session.setAttribute("justLoggedIn", true);
        }
        if (Boolean.TRUE.equals(session.getAttribute("justLoggedIn"))) {
            usuarioService.desbloquearEpsPrimerIngreso(usuario);
            session.setAttribute("justLoggedIn", false);
        }

        model.addAttribute("usuario", usuario);

        if (usuario.getEps() != null) {
            Eps eps = usuario.getEps();
            model.addAttribute("farmacias", eps.getFarmacias());
            model.addAttribute("nombreEps", eps.getNombre());

            String nombreEps = eps.getNombre().toLowerCase();
            if (nombreEps.contains("sanitas")) {
                return "usuario/dashboard-sanitas";
            } else if (nombreEps.contains("sura")) {
                return "usuario/dashboard-sura";
            }
        }
        return "usuario/dashboard-generico";
    }

    @GetMapping("/usuario/generar-cita")
    public String verGenerarCita(Authentication auth, Model model) {
        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        Optional<Generarcitas> cita = usuarioService.obtenerPrimeraCita(usuario);
        cita.ifPresent(c -> model.addAttribute("cita", c));
        return "usuario/generar-cita";
    }

    @PostMapping("/usuario/generar-cita")
    public String generarCita(Authentication auth, Model model) {
        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());

        ResultadoCita resultado = usuarioService.generarCita(usuario);
        model.addAttribute("cita", resultado.getCita());
        model.addAttribute("msg", resultado.getMensaje());

        return "usuario/generar-cita";
    }

    @GetMapping("/usuario/farmacias")
    public String verFarmaciasUsuario(@RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model, Authentication auth) {

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        Page<Farmacia> farmaciasPage = usuarioService.listarFarmaciasDeUsuario(
                usuario, nombre, PageRequest.of(page, 5));

        if (farmaciasPage == null) {
            model.addAttribute("error", "El usuario no tiene EPS asignada");
            return "usuario/farmacias";
        }

        model.addAttribute("farmacias", farmaciasPage.getContent());
        model.addAttribute("currentPage", farmaciasPage.getNumber() + 1);
        model.addAttribute("totalPages", farmaciasPage.getTotalPages());
        model.addAttribute("nombre", nombre);
        model.addAttribute("nombreEps", usuario.getEps().getNombre());

        return "usuario/farmacias";
    }

    @GetMapping("/usuario/farmacia/{id}")
    public String verMedicamentosPorFarmacia(
            @PathVariable("id") Long farmaciaId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "nombre", required = false) String nombre,
            @RequestParam(name = "laboratorio", required = false) String laboratorio,
            @RequestParam(name = "concentracion", required = false) String concentracion,
            @RequestParam(name = "presentacion", required = false) String presentacion,
            @RequestParam(name = "descripcion", required = false) String descripcion,
            Model model, Authentication auth) {

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());

        if (usuario.getEps() == null) {
            model.addAttribute("error", "El usuario no tiene EPS asignada");
            return "usuario/farmacias";
        }

        Farmacia farmacia = usuarioService.verificarAccesoFarmacia(usuario, farmaciaId);
        if (farmacia == null) {
            model.addAttribute("error", "No tiene acceso a esta farmacia");
            return "usuario/farmacias";
        }

        Page<Medicamento> medicamentosPage = usuarioService.listarMedicamentosFarmacia(
                farmacia, nombre, laboratorio, concentracion, presentacion, descripcion,
                PageRequest.of(page, 10, Sort.by("nombre").ascending()));

        model.addAttribute("laboratorios", usuarioService.laboratoriosDeFarmacia(farmacia));
        model.addAttribute("concentraciones", usuarioService.concentracionesDeFarmacia(farmacia));
        model.addAttribute("presentaciones", usuarioService.presentacionesDeFarmacia(farmacia));
        model.addAttribute("descripciones", usuarioService.descripcionesDeFarmacia(farmacia));

        model.addAttribute("medicamentos", medicamentosPage.getContent());
        model.addAttribute("farmacia", farmacia);
        model.addAttribute("farmaciaId", farmaciaId);
        model.addAttribute("currentPage", medicamentosPage.getNumber() + 1);
        model.addAttribute("totalPages", medicamentosPage.getTotalPages());
        model.addAttribute("nombre", nombre);
        model.addAttribute("laboratorio", laboratorio);
        model.addAttribute("concentracion", concentracion);
        model.addAttribute("presentacion", presentacion);
        model.addAttribute("descripcion", descripcion);
        model.addAttribute("isManager", false);

        return "usuario/medicamentos";
    }

    @GetMapping("/usuario/solicitudes")
    public String verSolicitudes(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "estado", required = false) String estado,
            Model model, Authentication auth) {

        boolean manager = usuarioService.esManager(auth);
        if (manager) {
            return "redirect:/manager/solicitudes";
        }

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        Page<Solicitud> solicitudesPage = usuarioService.listarSolicitudes(usuario, estado, page);

        model.addAttribute("solicitudes", solicitudesPage.getContent());
        model.addAttribute("currentPage", solicitudesPage.getNumber() + 1);
        model.addAttribute("totalPages", solicitudesPage.getTotalPages());
        model.addAttribute("isManager", manager);
        model.addAttribute("estado", estado);

        return "usuario/solicitudes";
    }

    @GetMapping("/usuario/solicitudes/buscar")
    public String buscarSolicitudes(@RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model, Authentication auth) {

        boolean manager = usuarioService.esManager(auth);
        if (manager) {
            return "redirect:/manager/solicitudes";
        }

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        Page<Solicitud> solicitudesPage = usuarioService.buscarSolicitudes(usuario, keyword, page);

        model.addAttribute("solicitudes", solicitudesPage.getContent());
        model.addAttribute("currentPage", solicitudesPage.getNumber() + 1);
        model.addAttribute("totalPages", solicitudesPage.getTotalPages());
        model.addAttribute("isManager", manager);
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("keyword", keyword);
        }

        return "usuario/solicitudes";
    }

    @PostMapping("/usuario/solicitar-medicamento/{idMedicamento}")
    public String solicitarMedicamento(@PathVariable Long idMedicamento,
            @RequestParam int cantidad,
            @RequestParam String codigo,
            @RequestParam int page,
            @RequestParam(required = false) String nombre,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        if (usuarioService.esManager(auth)) {
            return "redirect:/manager/medicamentos";
        }

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        ResultadoSolicitud resultado = usuarioService.solicitarMedicamento(usuario, idMedicamento, cantidad, codigo);

        redirectAttributes.addFlashAttribute(resultado.isExito() ? "msg" : "error", resultado.getMensaje());

        return "redirect:/usuario/farmacia/" + resultado.getFarmaciaId()
                + "?page=" + (page - 1)
                + (nombre != null && !nombre.isEmpty() ? "&nombre=" + nombre : "");
    }

    @GetMapping("/usuario/perfil")
    public String verPerfil(Authentication auth, Model model, HttpSession session) {
        if (auth == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());

        Boolean justLoggedIn = (Boolean) session.getAttribute("justLoggedIn");
        if (Boolean.TRUE.equals(justLoggedIn)) {
            usuarioService.desbloquearEpsPrimerIngreso(usuario);
            session.setAttribute("justLoggedIn", false);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("epsList", usuarioService.listarTodasEps());
        return "usuario/perfil";
    }

    @GetMapping("/usuario/editar/{id}")
    public String editarPerfil(@PathVariable Long id, Model model, Authentication auth) {
        if (auth == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioService.obtenerUsuarioPorId(id);
        if (usuario == null) {
            return "redirect:/usuario/perfil?error=UsuarioNoEncontrado";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("epsList", usuarioService.listarTodasEps());
        return "usuario/editar-perfil";
    }

    @PostMapping("/usuario/editar/{id}")
    public String actualizarPerfil(@PathVariable Long id, @ModelAttribute Usuario usuarioEditado,
            HttpServletRequest request) {

        ResultadoActualizacionPerfil resultado = usuarioService.actualizarPerfil(id, usuarioEditado);

        if (!resultado.isEncontrado()) {
            return "redirect:/usuario/perfil?error=UsuarioNoEncontrado";
        }

        if (resultado.isCorreoCambiado()) {
            request.getSession().invalidate();
            return "redirect:/login?correoCambiado=true";
        }

        return "redirect:/usuario/perfil?success=Datos actualizados correctamente";
    }

    @PostMapping("/usuario/editar-eps/{id}")
    public String actualizarSoloEps(
            @PathVariable Long id,
            @RequestParam("eps.id") Long epsId,
            Model model) {

        ResultadoAccion resultado = usuarioService.actualizarSoloEps(id, epsId);

        if ("error".equals(resultado.getTipo())) {
            return "redirect:/usuario/perfil?error=UsuarioNoEncontrado";
        }

        Usuario usuario = usuarioService.obtenerUsuarioPorId(id);
        model.addAttribute("usuario", usuario);
        model.addAttribute("epsList", usuarioService.listarTodasEps());
        model.addAttribute("mensajeExito", resultado.getMensaje());

        return "usuario/editar-perfil";
    }

}
