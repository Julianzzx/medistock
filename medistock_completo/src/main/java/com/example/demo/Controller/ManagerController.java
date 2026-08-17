package com.example.demo.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.Model.Admin;
import com.example.demo.Model.Eps;
import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Medicamento;
import com.example.demo.Model.Usuario;
import com.example.demo.service.ManagerService;
import com.example.demo.service.ResultadoAccion;
import com.example.demo.service.SolicitudesReabastecimientoVista;

//----------------MANAGER------------------
/**
 * Controller del rol Manager. Solo recibe la petición HTTP, delega toda la
 * lógica de negocio en ManagerService (o WekaService para el módulo
 * predictivo) y traduce el resultado a una vista o un redirect. No contiene
 * reglas de negocio, cálculos ni acceso directo a repositorios.
 */
@Controller
@RequestMapping("/manager")
public class ManagerController {

    @Autowired
    private ManagerService managerService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "manager/dashboard";
    }

    // ----------------- USUARIOS -----------------
    @GetMapping("/usuarios")
    public String usuarios(@RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model) {

        Page<Usuario> usuarioPage = managerService.listarUsuarios(keyword, page, size);

        model.addAttribute("usuarios", usuarioPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usuarioPage.getTotalPages());
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("keyword", keyword);
        model.addAttribute("epsList", managerService.listarTodasEps());

        return "manager/usuarios";
    }

    @GetMapping("/usuarios/crear")
    public String mostrarFormularioCrearUsuario(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("epsList", managerService.listarTodasEps());
        return "manager/crear-usuario";
    }

    @PostMapping("/usuarios/crear")
    public String crearUsuario(@ModelAttribute Usuario usuario) {
        managerService.crearUsuario(usuario);
        return "redirect:/manager/usuarios";
    }

    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        if (!managerService.eliminarUsuario(id)) {
            return "errorAD";
        }
        return "redirect:/manager/usuarios";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuarioForm(@PathVariable Long id, Model model) {
        Usuario usuario = managerService.obtenerUsuario(id);
        if (usuario == null) {
            return "redirect:/manager/usuarios?error=UsuarioNoEncontrado";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("epsList", managerService.listarTodasEps());

        return "manager/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    public String guardarEdicionUsuario(@PathVariable Long id, @ModelAttribute Usuario usuarioEditado) {
        try {
            managerService.actualizarUsuario(id, usuarioEditado);
        } catch (java.util.NoSuchElementException e) {
            return "errorAD";
        }
        return "redirect:/manager/usuarios";
    }

    // ----------------- FARMACIAS -----------------
    @GetMapping("/farmacias")
    public String farmacias(@RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model) {

        Page<Farmacia> farmaciaPage = managerService.listarFarmacias(keyword, page, size);

        model.addAttribute("farmacias", farmaciaPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", farmaciaPage.getTotalPages());
        model.addAttribute("farmacia", new Farmacia());
        model.addAttribute("keyword", keyword);
        model.addAttribute("epsList", managerService.listarTodasEps());

        return "manager/farmacias";
    }

    @PostMapping("/farmacias/crear")
    public String crearFarmacia(@ModelAttribute Farmacia farmacia,
            @RequestParam("epsId") Long epsId) {
        managerService.crearFarmacia(farmacia, epsId);
        return "redirect:/manager/farmacias";
    }

    @GetMapping("/farmacias/editar/{id}")
    public String editarFarmaciaForm(@PathVariable Long id, Model model) {
        Farmacia farmacia = managerService.obtenerFarmacia(id);
        if (farmacia == null) {
            return "errorAD";
        }

        model.addAttribute("farmacias", managerService.listarTodasFarmacias());
        model.addAttribute("farmacia", farmacia);
        model.addAttribute("epsList", managerService.listarTodasEps());

        return "manager/farmacias";
    }

    @PostMapping("/farmacias/editar/{id}")
    public String guardarEdicionFarmacia(@PathVariable Long id,
            @ModelAttribute Farmacia farmaciaEditada,
            @RequestParam("epsId") Long epsId) {
        if (!managerService.actualizarFarmacia(id, farmaciaEditada, epsId)) {
            return "errorAD";
        }
        return "redirect:/manager/farmacias";
    }

    @GetMapping("/farmacias/eliminar/{id}")
    public String eliminarFarmacia(@PathVariable Long id) {
        ResultadoAccion resultado = managerService.eliminarFarmacia(id);
        if ("error".equals(resultado.getTipo())) {
            return "errorAD";
        }
        return "redirect:/manager/farmacias";
    }

    // ----------------- MEDICAMENTOS -----------------
    @GetMapping("/farmacias/{farmaciaId}/medicamentos")
    public String listarMedicamentos(@PathVariable Long farmaciaId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {

        Farmacia farmacia = managerService.obtenerFarmacia(farmaciaId);
        if (farmacia == null) {
            return "errorAD";
        }

        Page<Medicamento> medicamentos = managerService.listarMedicamentosPorFarmacia(farmaciaId, keyword, page, size);

        model.addAttribute("farmaciaSeleccionada", farmacia);
        model.addAttribute("medicamentos", medicamentos.getContent());
        model.addAttribute("currentPage", medicamentos.getNumber());
        model.addAttribute("totalPages", medicamentos.getTotalPages());
        model.addAttribute("medicamento", new Medicamento());
        model.addAttribute("keyword", keyword);

        return "manager/medicamentos";
    }

    @PostMapping("/farmacias/{farmaciaId}/medicamentos/crear")
    public String crearMedicamento(@PathVariable Long farmaciaId,
            @ModelAttribute Medicamento medicamento) {

        try {
            ResultadoAccion resultado = managerService.crearMedicamento(farmaciaId, medicamento);
            if ("error".equals(resultado.getTipo())) {
                return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos?error=" + resultado.getMensaje();
            }
            return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos";
        } catch (java.util.NoSuchElementException e) {
            return "errorAD";
        }
    }

    @GetMapping("/farmacias/{farmaciaId}/medicamentos/eliminar/{id}")
    public String eliminarMedicamento(@PathVariable Long farmaciaId, @PathVariable Long id) {
        try {
            ResultadoAccion resultado = managerService.eliminarMedicamento(farmaciaId, id);
            if ("error".equals(resultado.getTipo())) {
                return "errorAD";
            }
            return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos";
        } catch (java.util.NoSuchElementException e) {
            return "errorAD";
        }
    }

    @GetMapping("/farmacias/{farmaciaId}/medicamentos/editar/{id}")
    public String editarMedicamentoForm(@PathVariable Long farmaciaId,
            @PathVariable Long id,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {

        Medicamento medicamento = managerService.obtenerMedicamento(id);
        Farmacia farmacia = managerService.obtenerFarmacia(farmaciaId);
        if (medicamento == null || farmacia == null) {
            return "errorAD";
        }

        Page<Medicamento> medicamentoPage = managerService.listarMedicamentosPorFarmacia(farmaciaId, keyword, page - 1, 5);

        model.addAttribute("medicamentos", medicamentoPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", medicamentoPage.getTotalPages());
        model.addAttribute("medicamento", medicamento);
        model.addAttribute("farmaciaSeleccionada", farmacia);
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("keyword", keyword);
        }

        return "manager/medicamentos";
    }

    @PostMapping("/farmacias/{farmaciaId}/medicamentos/editar/{id}")
    public String guardarEdicionMedicamento(@PathVariable Long farmaciaId,
            @PathVariable Long id,
            @ModelAttribute Medicamento medicamentoEditado) {

        if (!managerService.actualizarMedicamento(farmaciaId, id, medicamentoEditado)) {
            return "errorAD";
        }
        return "redirect:/manager/farmacias/" + farmaciaId + "/medicamentos";
    }

    // ----------------- ADMINS -----------------
    @GetMapping("/admins")
    public String admins(@RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model) {

        Page<Admin> adminPage = managerService.listarAdmins(keyword, page, size);

        model.addAttribute("admins", adminPage.getContent());
        model.addAttribute("adminPage", adminPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", adminPage.getTotalPages());
        model.addAttribute("admin", new Admin());
        model.addAttribute("epsList", managerService.listarTodasEps());
        model.addAttribute("farmacias", managerService.listarTodasFarmacias());
        model.addAttribute("keyword", keyword);

        return "manager/admins";
    }

    @PostMapping("/admins/crear")
    public String crearAdmin(@ModelAttribute Admin admin,
            @RequestParam(required = false) Long farmaciaId) {
        managerService.crearAdmin(admin, farmaciaId);
        return "redirect:/manager/admins";
    }

    @GetMapping("/admins/eliminar/{id}")
    public String eliminarAdmin(@PathVariable Long id) {
        if (!managerService.eliminarAdmin(id)) {
            return "errorAD";
        }
        return "redirect:/manager/admins";
    }

    @GetMapping("/admins/editar/{id}")
    public String editarAdminForm(@PathVariable Long id,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            Model model) {

        Admin admin = managerService.obtenerAdmin(id);
        if (admin == null) {
            return "errorAD";
        }

        Page<Admin> adminPage = managerService.listarAdmins(keyword, page, size);

        model.addAttribute("admins", adminPage.getContent());
        model.addAttribute("adminPage", adminPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", adminPage.getTotalPages());
        model.addAttribute("admin", admin);
        model.addAttribute("farmacias", managerService.listarTodasFarmacias());
        model.addAttribute("keyword", keyword);

        return "manager/admins";
    }

    @PostMapping("/admins/editar/{id}")
    public String guardarEdicionAdmin(@PathVariable Long id,
            @ModelAttribute Admin adminEditado,
            @RequestParam(required = false) Long farmaciaId) {

        if (!managerService.actualizarAdmin(id, adminEditado, farmaciaId)) {
            return "errorAD";
        }
        return "redirect:/manager/admins";
    }

    // ----------------- SOLICITUDES DE REABASTECIMIENTO -----------------
    @GetMapping("/solicitudes")
    public String verSolicitudes(
            @RequestParam(defaultValue = "0") int pagePendientes,
            @RequestParam(defaultValue = "0") int pageAceptadas,
            @RequestParam(defaultValue = "0") int pageRechazadas,
            @RequestParam(required = false) Long farmaciaId,
            Model model) {

        SolicitudesReabastecimientoVista vista = managerService.listarSolicitudesReabastecimiento(
                farmaciaId, pagePendientes, pageAceptadas, pageRechazadas, 5);

        model.addAttribute("solicitudesPendientes", vista.getPendientes().getContent());
        model.addAttribute("pendientesPage", vista.getPendientes());

        model.addAttribute("solicitudesAceptadas", vista.getAceptadas().getContent());
        model.addAttribute("aceptadasPage", vista.getAceptadas());

        model.addAttribute("solicitudesRechazadas", vista.getRechazadas().getContent());
        model.addAttribute("rechazadasPage", vista.getRechazadas());

        model.addAttribute("farmacias", managerService.listarTodasFarmacias());
        model.addAttribute("farmaciaId", farmaciaId);

        return "manager/solicitudes";
    }

    @PostMapping("/solicitud/{id}/aceptar")
    public String aceptarSolicitud(@PathVariable Long id) {
        managerService.aceptarSolicitudReabastecimiento(id);
        return "redirect:/manager/solicitudes";
    }

    @PostMapping("/solicitud/{id}/rechazar")
    public String rechazarSolicitud(@PathVariable Long id) {
        managerService.rechazarSolicitudReabastecimiento(id);
        return "redirect:/manager/solicitudes";
    }

    @PostMapping("/solicitud/{id}/eliminar")
    public String eliminarSolicitud(@PathVariable Long id) {
        managerService.eliminarSolicitudReabastecimiento(id);
        return "redirect:/manager/solicitudes";
    }

    @PostMapping("/solicitudes/aceptar-todo")
    public String aceptarTodo() {
        managerService.aceptarTodasPendientes();
        return "redirect:/manager/solicitudes";
    }

    @PostMapping("/solicitudes/rechazar-todo")
    public String rechazarTodo() {
        managerService.rechazarTodasPendientes();
        return "redirect:/manager/solicitudes";
    }

    @PostMapping("/solicitudes/eliminar-todo-aceptadas")
    public String eliminarTodoAceptadas() {
        managerService.eliminarTodasAceptadas();
        return "redirect:/manager/solicitudes";
    }

    @PostMapping("/solicitudes/eliminar-todo-rechazadas")
    public String eliminarTodoRechazadas() {
        managerService.eliminarTodasRechazadas();
        return "redirect:/manager/solicitudes";
    }

    // ------------------EPS--------------------------
    @GetMapping("/eps")
    public String listarEps(
            Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword) {

        Page<Eps> epsPage = managerService.listarEps(keyword, page, 5);

        model.addAttribute("keyword", keyword);
        model.addAttribute("epsList", epsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", epsPage.getTotalPages());
        model.addAttribute("eps", new Eps());

        return "manager/eps";
    }

    @PostMapping("/eps/crear")
    public String crearEps(@ModelAttribute Eps eps) {
        managerService.crearEps(eps);
        return "redirect:/manager/eps";
    }

    @GetMapping("/eps/editar/{id}")
    public String editarEps(@PathVariable Long id, Model model,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword) {

        model.addAttribute("eps", managerService.obtenerEps(id));

        Page<Eps> epsPage = managerService.listarEps(keyword, page, 5);

        model.addAttribute("epsList", epsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", epsPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "manager/eps";
    }

    @PostMapping("/eps/editar/{id}")
    public String actualizarEps(@PathVariable Long id, @ModelAttribute Eps eps) {
        managerService.actualizarEps(id, eps);
        return "redirect:/manager/eps";
    }

    @GetMapping("/eps/eliminar/{id}")
    public String eliminarEps(@PathVariable Long id) {
        managerService.eliminarEps(id);
        return "redirect:/manager/eps";
    }

    @GetMapping("/eps/select")
    public String seleccionarEps(Model model) {
        model.addAttribute("epsList", managerService.listarTodasEps());
        return "manager/eps-select";
    }

    @GetMapping("/eps/{id}/farmacias")
    public String verFarmaciasPorEps(@PathVariable Long id, Model model) {
        Eps eps = managerService.buscarEpsPorId(id);
        if (eps == null) {
            return "redirect:/manager/eps/select";
        }

        model.addAttribute("eps", eps);

        return "manager/farmacias-por-eps";
    }

    @GetMapping("/farmaciamanager")
    public String farmaciaSanitas(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre"));
        Page<Farmacia> farmaciaPage = managerService.listarFarmaciasPorNombreEps("Sanitas", keyword, pageable);

        model.addAttribute("farmacias", farmaciaPage.getContent());
        model.addAttribute("currentPage", farmaciaPage.getNumber());
        model.addAttribute("totalPages", farmaciaPage.getTotalPages());
        model.addAttribute("keyword", keyword);

        return "manager/farmaciamanager";
    }

    @GetMapping("/Sura_farmacia")
    public String farmaciaSura(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, 6);
        Optional<Page<Farmacia>> farmaciasOpt = managerService.listarFarmaciasSura(keyword, pageable);

        if (farmaciasOpt.isPresent()) {
            Page<Farmacia> farmaciasPage = farmaciasOpt.get();
            model.addAttribute("farmacias", farmaciasPage.getContent());
            model.addAttribute("currentPage", farmaciasPage.getNumber());
            model.addAttribute("totalPages", farmaciasPage.getTotalPages());
        } else {
            model.addAttribute("farmacias", new ArrayList<>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
        }

        model.addAttribute("keyword", keyword);
        return "manager/Sura_farmacia";
    }

    @GetMapping("/powerbi")
    public String mostrarPowerBi() {
        return "manager/powerbi";
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MODELO PREDICTIVO WEKA  — acceso exclusivo MANAGER
    //  (ya estaba correctamente delegado en WekaService; sin cambios)
    // ══════════════════════════════════════════════════════════════════════

    @Autowired
    private com.example.demo.service.WekaService wekaService;

    /**
     * GET /manager/weka
     * Carga la vista y lanza predicción automática desde la BD.
     */
    @GetMapping("/weka")
    public String wekaVista(Model model) {
        com.example.demo.Model.PrediccionResultado res =
                new com.example.demo.Model.PrediccionResultado();

        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        res.setMes(String.valueOf(ahora.getMonthValue()));
        res.setAnio(String.valueOf(ahora.getYear()));

        try {
            String prediccion = wekaService.predictDesdeDB();
            res.setMedicamentoPredicho(prediccion);
        } catch (Exception e) {
            res.setError("Error al predecir: " + e.getMessage());
        }

        model.addAttribute("resultado",          res);
        model.addAttribute("medicamentoReal",    wekaService.medicamentoRealMasSolicitado());
        model.addAttribute("totalSolicitudes",   wekaService.totalSolicitudesAceptadas());
        model.addAttribute("topHistorico",       wekaService.topHistorico());
        model.addAttribute("topDelMes",          wekaService.topDelMes());
        model.addAttribute("accuracyCv",         wekaService.getAccuracyFormateado());
        model.addAttribute("kappa",              wekaService.getKappaFormateado());
        model.addAttribute("numInstancias",      wekaService.getNumInstancias());
        model.addAttribute("numAtributos",       wekaService.getNumAtributos());
        model.addAttribute("numClases",          wekaService.getNumClases());
        model.addAttribute("profundidadArbol",   wekaService.getProfundidadArbol());
        model.addAttribute("numHojas",           wekaService.getNumHojas());
        model.addAttribute("fechaEntrenamiento", wekaService.getFechaEntrenamiento());

        // Listas dinámicas desde BD para los filtros
        model.addAttribute("presentaciones",  wekaService.obtenerPresentacionesUnicas());
        model.addAttribute("laboratorios",    wekaService.obtenerLaboratoriosUnicos());

        // Filtros vacíos en carga inicial
        model.addAttribute("filtrosPresentacion", "");
        model.addAttribute("filtrosLaboratorio",  "");

        model.addAttribute("form", new com.example.demo.Model.PrediccionResultado());
        return "manager/weka";
    }

    /**
     * POST /manager/weka/predecir
     * Predicción real usando datos de la BD filtrados por presentación y/o laboratorio.
     */
    @PostMapping("/weka/predecir")
    public String wekaPredecir(
            @RequestParam(required = false) String presentacion,
            @RequestParam(required = false) String laboratorio,
            Model model) {

        // Normalizar vacíos
        String pres = (presentacion != null && !presentacion.isBlank()) ? presentacion.trim() : null;
        String lab  = (laboratorio  != null && !laboratorio.isBlank())  ? laboratorio.trim()  : null;

        // Stats del modelo (siempre se cargan)
        model.addAttribute("accuracyCv",         wekaService.getAccuracyFormateado());
        model.addAttribute("kappa",              wekaService.getKappaFormateado());
        model.addAttribute("numInstancias",      wekaService.getNumInstancias());
        model.addAttribute("numAtributos",       wekaService.getNumAtributos());
        model.addAttribute("numClases",          wekaService.getNumClases());
        model.addAttribute("profundidadArbol",   wekaService.getProfundidadArbol());
        model.addAttribute("numHojas",           wekaService.getNumHojas());
        model.addAttribute("fechaEntrenamiento", wekaService.getFechaEntrenamiento());
        model.addAttribute("medicamentoReal",    wekaService.medicamentoRealMasSolicitado());
        model.addAttribute("totalSolicitudes",   wekaService.totalSolicitudesAceptadas());

        // Listas dinámicas para los selects (con el valor seleccionado persistido)
        model.addAttribute("presentaciones",      wekaService.obtenerPresentacionesUnicas());
        model.addAttribute("laboratorios",        wekaService.obtenerLaboratoriosUnicos());
        model.addAttribute("filtrosPresentacion", pres != null ? pres : "");
        model.addAttribute("filtrosLaboratorio",  lab  != null ? lab  : "");

        // Tablas filtradas según los parámetros enviados
        model.addAttribute("topHistorico", wekaService.topHistoricoFiltrado(pres, lab));
        model.addAttribute("topDelMes",    wekaService.topDelMesFiltrado(pres, lab));

        try {
            String prediccion = wekaService.predictPorFiltros(pres, null, lab);

            com.example.demo.Model.PrediccionResultado res =
                    new com.example.demo.Model.PrediccionResultado();
            java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
            res.setMes(String.valueOf(ahora.getMonthValue()));
            res.setAnio(String.valueOf(ahora.getYear()));
            res.setMedicamentoPredicho(prediccion);

            model.addAttribute("resultado",           res);
            model.addAttribute("medicamentoPredicho", prediccion);
            model.addAttribute("prediccionExitosa",   true);

            return "manager/weka";
        } catch (Exception e) {
            model.addAttribute("prediccionExitosa", false);
            model.addAttribute("errorMsg", "Error en predicción: " + e.getMessage());
            model.addAttribute("topHistorico", wekaService.topHistorico());
            model.addAttribute("topDelMes",    wekaService.topDelMes());
            return "manager/weka";
        }
    }
}
