package com.example.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Generarcitas;
import com.example.demo.Model.Medicamento;
import com.example.demo.Model.Solicitud;
import com.example.demo.Model.Usuario;
import com.example.demo.service.ResultadoCita;
import com.example.demo.service.ResultadoSolicitud;
import com.example.demo.service.UsuarioService;

/**
 * Único componente REST del proyecto. Reutiliza exactamente las mismas
 * reglas de negocio que UsuarioController (mismo UsuarioService) en vez de
 * duplicarlas: antes esta clase repetía getOrCreateUsuario, la validación
 * de acceso a farmacia y la validación del código de cita ya existentes en
 * UsuarioController.
 *
 * Nota de arquitectura (fuera del alcance de este cambio): sigue
 * devolviendo entidades @Entity directamente en vez de DTOs.
 */
@RestController
@RequestMapping("/api/usuario")
public class UsuarioRestController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/farmacias")
    public Page<Farmacia> listarFarmacias(
            Authentication auth,
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int page) {

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        Pageable pageable = PageRequest.of(page, 10);

        Page<Farmacia> resultado = usuarioService.listarFarmaciasDeUsuario(usuario, nombre, pageable);
        return resultado != null ? resultado : Page.empty();
    }

    @GetMapping("/farmacia/{id}/medicamentos")
    public Page<Medicamento> listarMedicamentos(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String laboratorio,
            @RequestParam(required = false) String concentracion,
            @RequestParam(required = false) String presentacion,
            @RequestParam(required = false) String descripcion,
            @RequestParam(defaultValue = "0") int page) {

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());

        Farmacia farmacia = usuarioService.verificarAccesoFarmacia(usuario, id);
        if (farmacia == null) {
            return Page.empty();
        }

        Pageable pageable = PageRequest.of(page, 10);
        return usuarioService.listarMedicamentosFarmacia(
                farmacia, nombre, laboratorio, concentracion, presentacion, descripcion, pageable);
    }

    @GetMapping("/solicitudes")
    public Page<Solicitud> listarSolicitudes(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page) {

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        return usuarioService.listarSolicitudesUsuario(usuario, PageRequest.of(page, 10));
    }

    @PostMapping("/solicitar-medicamento/{idMedicamento}")
    public String solicitarMedicamento(
            Authentication auth,
            @PathVariable Long idMedicamento,
            @RequestParam int cantidad,
            @RequestParam String codigo) {

        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        ResultadoSolicitud resultado = usuarioService.solicitarMedicamento(usuario, idMedicamento, cantidad, codigo);
        return resultado.getMensaje();
    }

    @GetMapping("/perfil")
    public Usuario perfil(Authentication auth) {
        return usuarioService.obtenerOCrearUsuario(auth.getName());
    }

    @GetMapping("/generar-cita")
    public Generarcitas obtenerCita(Authentication auth) {
        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        return usuarioService.obtenerPrimeraCita(usuario).orElse(null);
    }

    @PostMapping("/generar-cita")
    public Generarcitas generarCita(Authentication auth) {
        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());
        ResultadoCita resultado = usuarioService.generarCita(usuario);
        return resultado.getCita();
    }
}
