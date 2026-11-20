package org.db.medistock.controller;

import org.db.medistock.dto.OperacionResponseDTO;
import org.db.medistock.service.ChatbotService;
import org.db.medistock.service.ChatbotService.ResultadoAccion;
import org.db.medistock.service.ChatbotService.TurnoChat;
import org.db.medistock.service.OptimizacionOperativaService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/manager/chatbot")
public class ManagerChatbotController {

    private static final int MAX_TURNOS = 10;
    private static final int MAX_TEXTO_TURNO = 2000;

    private final ChatbotService chatbotService;
    private final OptimizacionOperativaService optimizacionService;
    private final ObjectMapper objectMapper;

    public ManagerChatbotController(ChatbotService chatbotService,
                                    OptimizacionOperativaService optimizacionService,
                                    ObjectMapper objectMapper) {
        this.chatbotService = chatbotService;
        this.optimizacionService = optimizacionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/mensaje")
    public RespuestaChat mensaje(@RequestBody PeticionChat peticion) {
        if (peticion == null || peticion.pregunta() == null || peticion.pregunta().isBlank()) {
            return new RespuestaChat("Escribe una pregunta.", false);
        }
        if (peticion.historial() != null && peticion.historial().size() > MAX_TURNOS) {
            return new RespuestaChat("La conversación es demasiado larga. Inicia un chat nuevo.", false);
        }

        List<TurnoChat> historial = peticion.historial() == null
                ? List.of()
                : peticion.historial().stream()
                        .filter(turno -> turno != null && turno.texto() != null)
                        .map(turno -> new TurnoChat(
                                normalizarRol(turno.rol()),
                                turno.texto().substring(
                                        0, Math.min(turno.texto().length(), MAX_TEXTO_TURNO))))
                        .toList();

        ResultadoAccion resultado = chatbotService.responderComoAnalista(
                construirContexto(peticion.simulacion()), peticion.pregunta(), historial);
        return new RespuestaChat(resultado.mensaje(), resultado.exito());
    }

    // ------------------------------------------------------------------ contexto para el asistente

    private String construirContexto(Simulacion simulacion) {
        StringBuilder contexto = new StringBuilder();

        OperacionResponseDTO reales = optimizacionService.calcularOptimoOperativo();
        contexto.append("DATOS REALES (calculados con la base de datos, objetivo B):\n")
                .append(json(reales)).append('\n')
                .append("HECHOS CALCULADOS DE LOS DATOS REALES:\n")
                .append(json(hechos(reales))).append("\n\n");

        if (simulacionValida(simulacion)) {
            try {
                OperacionResponseDTO simulado = optimizacionService.resolver(
                        simulacion.capacidadPorAdmin(), simulacion.maxSolicitudes(), simulacion.maxAdmins(),
                        simulacion.objetivo().trim().toUpperCase(Locale.ROOT));
                contexto.append("SIMULACIÓN QUE EL MANAGER TIENE EN PANTALLA (parámetros que él eligió, no datos reales):\n")
                        .append(json(simulado)).append('\n')
                        .append("HECHOS CALCULADOS DE LA SIMULACIÓN:\n")
                        .append(json(hechos(simulado))).append('\n');
            } catch (RuntimeException e) {
                contexto.append("La simulación en pantalla no se pudo calcular: ").append(e.getMessage()).append('\n');
            }
        } else {
            contexto.append("El manager no tiene ninguna simulación abierta en pantalla.\n");
        }
        return contexto.toString();
    }

    private boolean simulacionValida(Simulacion s) {
        return s != null
                && s.capacidadPorAdmin() != null
                && s.maxSolicitudes() != null
                && s.maxAdmins() != null
                && s.objetivo() != null
                && !s.objetivo().isBlank();
    }

    /** Cifras derivadas listas para citar: así el asistente no tiene que hacer aritmética. */
    private Map<String, Object> hechos(OperacionResponseDTO r) {
        int total = r.totalSolicitudes();
        int atendidas = r.solicitudesAtendidas();
        int capacidad = r.capacidadPorAdmin();
        int sinResponder = Math.max(0, total - atendidas);
        long capacidadInstalada = (long) r.adminsAsignados() * capacidad;

        Map<String, Object> hechos = new LinkedHashMap<>();
        hechos.put("solicitudesSinResponder", sinResponder);
        hechos.put("coberturaPorCiento", total == 0 ? null : Math.round(100.0 * atendidas / total));
        hechos.put("adminsLibres", r.adminsLiberados());
        hechos.put("adminsAdicionalesNecesarios",
                (sinResponder > 0 && capacidad > 0) ? (int) Math.ceil((double) sinResponder / capacidad) : 0);
        hechos.put("capacidadInstalada", capacidadInstalada);
        hechos.put("utilizacionPorCiento",
                capacidadInstalada == 0 ? null : Math.round(100.0 * atendidas / capacidadInstalada));
        return hechos;
    }

    private String json(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (Exception e) {
            return "{\"error\":\"no se pudo serializar\"}";
        }
    }

    private String normalizarRol(String rol) {
        return "assistant".equalsIgnoreCase(rol)
                || "model".equalsIgnoreCase(rol)
                || "bot".equalsIgnoreCase(rol)
                ? "assistant"
                : "user";
    }

    /** Parámetros del simulador que el manager tiene abierto (opcional). */
    public record Simulacion(Integer capacidadPorAdmin, Integer maxSolicitudes, Integer maxAdmins, String objetivo) {
    }

    public record PeticionChat(String pregunta, List<TurnoChatDTO> historial, Simulacion simulacion) {
    }

    public record TurnoChatDTO(String rol, String texto) {
    }

    public record RespuestaChat(String respuesta, boolean exito) {
    }
}
