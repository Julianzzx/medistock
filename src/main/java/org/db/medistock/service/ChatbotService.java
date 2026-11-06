package org.db.medistock.service;

import org.db.medistock.model.Eps;
import org.db.medistock.model.Farmacia;
import org.db.medistock.model.Medicamento;
import org.db.medistock.model.Solicitud;
import org.db.medistock.model.Usuario;
import org.db.medistock.repository.FarmaciaRepository;
import org.db.medistock.repository.MedicamentoRepository;
import org.db.medistock.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class ChatbotService {

    private static final int MAX_FARMACIAS = 100;
    private static final int MAX_MEDICAMENTOS_EN_CONTEXTO = 80;
    private static final int MAX_SOLICITUDES_EN_CONTEXTO = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(ChatbotService.class);

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.api.model:openai/gpt-oss-20b}")
    private String modelo;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    private final FarmaciaRepository farmaciaRepo;
    private final MedicamentoRepository medicamentoRepo;
    private final SolicitudRepository solicitudRepo;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public ChatbotService(
            FarmaciaRepository farmaciaRepo,
            MedicamentoRepository medicamentoRepo,
            SolicitudRepository solicitudRepo,
            ObjectMapper objectMapper) {
        this.farmaciaRepo = farmaciaRepo;
        this.medicamentoRepo = medicamentoRepo;
        this.solicitudRepo = solicitudRepo;
        this.objectMapper = objectMapper;
    }

    public ResultadoAccion responder(Usuario usuario, String pregunta, List<TurnoChat> historial) {
        if (pregunta == null || pregunta.isBlank()) {
            return ResultadoAccion.error("Escribe una pregunta.");
        }
        if (pregunta.length() > 1000) {
            return ResultadoAccion.error("La pregunta no puede superar los 1000 caracteres.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return ResultadoAccion.error(
                    "El asistente todavía no está configurado. Inténtalo más tarde.");
        }

        String systemInstruction = construirInstruccionSistema(usuario);
        try {
            return ResultadoAccion.exito(llamarGroq(systemInstruction, pregunta, historial));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("La solicitud al proveedor del chatbot fue interrumpida.");
            return ResultadoAccion.error("No pude comunicarme con el asistente. Intenta de nuevo.");
        } catch (IOException e) {
            logger.warn("No se pudo procesar la respuesta del proveedor del chatbot.", e);
            return ResultadoAccion.error("No pude comunicarme con el asistente. Intenta de nuevo.");
        } catch (ProveedorChatbotException e) {
            return ResultadoAccion.error(e.getMessage());
        }
    }

    private String construirInstruccionSistema(Usuario usuario) {
        return """
                Eres el asistente virtual de MediStock. Responde en español, de forma breve y cordial.
                Limita tus respuestas al uso de MediStock, disponibilidad de medicamentos en la red
                de farmacias de la EPS del usuario, y el estado de sus solicitudes.
                No des diagnósticos, dosis, consejos médicos ni recomendaciones de tratamiento.
                Para preguntas médicas, indica que debe consultar con un profesional de salud.
                Usa solo los datos proporcionados. Si faltan datos, dilo claramente y no inventes.
                La lista de inventario puede estar limitada; no afirmes que un medicamento no existe
                ni que no hay stock si no puedes confirmarlo con los datos incluidos.

                Usuario autenticado: %s
                EPS asignada: %s

                Solicitudes recientes de este usuario:
                %s

                Inventario de farmacias de la EPS de este usuario:
                %s
                """.formatted(
                nombreUsuario(usuario),
                usuario.getEps() == null ? "Sin EPS asignada" : usuario.getEps().getNombre(),
                construirContextoSolicitudes(usuario),
                construirContextoInventario(usuario.getEps()));
    }

    private String nombreUsuario(Usuario usuario) {
        String nombre = ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
        return nombre.isBlank() ? "No disponible" : nombre;
    }

    private String construirContextoSolicitudes(Usuario usuario) {
        Page<Solicitud> pagina = solicitudRepo.findByUsuario(
                usuario, PageRequest.of(0, MAX_SOLICITUDES_EN_CONTEXTO));
        if (pagina.isEmpty()) {
            return "El usuario no tiene solicitudes registradas.";
        }

        StringBuilder contexto = new StringBuilder();
        for (Solicitud solicitud : pagina.getContent()) {
            contexto.append("- Solicitud #").append(solicitud.getId())
                    .append(" | Estado: ").append(solicitud.getEstado());
            Medicamento medicamento = solicitud.getMedicamento();
            if (medicamento != null) {
                contexto.append(" | Medicamento: ").append(medicamento.getNombre());
                if (medicamento.getConcentracion() != null
                        && !medicamento.getConcentracion().isBlank()) {
                    contexto.append(" ").append(medicamento.getConcentracion());
                }
                if (medicamento.getFarmacia() != null) {
                    contexto.append(" | Farmacia: ").append(medicamento.getFarmacia().getNombre());
                }
            }
            contexto.append('\n');
        }
        if (pagina.getTotalElements() > MAX_SOLICITUDES_EN_CONTEXTO) {
            contexto.append("(Se muestran solo las ")
                    .append(MAX_SOLICITUDES_EN_CONTEXTO)
                    .append(" solicitudes más recientes.)");
        }
        return contexto.toString();
    }

    private String construirContextoInventario(Eps eps) {
        if (eps == null || eps.getId() == null) {
            return "El usuario no tiene una EPS asignada.";
        }

        Page<Farmacia> paginaFarmacias = farmaciaRepo.findByEpsId(
                eps.getId(), PageRequest.of(0, MAX_FARMACIAS));
        List<Farmacia> farmacias = paginaFarmacias.getContent();
        if (farmacias.isEmpty()) {
            return "No hay farmacias registradas para la EPS del usuario.";
        }

        StringBuilder contexto = new StringBuilder();
        int cantidadIncluida = 0;
        for (Farmacia farmacia : farmacias) {
            List<Medicamento> medicamentos = medicamentoRepo.findByFarmaciaId(farmacia.getId());
            if (medicamentos.isEmpty()) {
                continue;
            }
            contexto.append("Farmacia: ").append(farmacia.getNombre())
                    .append(" — ").append(farmacia.getDireccion()).append('\n');
            for (Medicamento medicamento : medicamentos) {
                if (cantidadIncluida >= MAX_MEDICAMENTOS_EN_CONTEXTO) {
                    contexto.append("(Inventario limitado a los primeros ")
                            .append(MAX_MEDICAMENTOS_EN_CONTEXTO)
                            .append(" registros.)");
                    return contexto.toString();
                }
                contexto.append("- ").append(medicamento.getNombre());
                if (medicamento.getConcentracion() != null
                        && !medicamento.getConcentracion().isBlank()) {
                    contexto.append(" ").append(medicamento.getConcentracion());
                }
                if (medicamento.getPresentacion() != null
                        && !medicamento.getPresentacion().isBlank()) {
                    contexto.append(" (").append(medicamento.getPresentacion()).append(")");
                }
                contexto.append(" | Stock: ").append(medicamento.getCantidad()).append('\n');
                cantidadIncluida++;
            }
        }

        if (cantidadIncluida == 0) {
            return "No hay medicamentos registrados en las farmacias de la EPS del usuario.";
        }
        if (paginaFarmacias.getTotalElements() > MAX_FARMACIAS) {
            contexto.append("(Se consideran solo las primeras ")
                    .append(MAX_FARMACIAS)
                    .append(" farmacias.)");
        }
        return contexto.toString();
    }

    private String llamarGroq(
            String instruccionSistema, String pregunta, List<TurnoChat> historial)
            throws IOException, InterruptedException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", modelo);
        payload.put("temperature", 0.3);
        payload.put("max_completion_tokens", 1024);
        payload.put("reasoning_effort", "low");

        ArrayNode mensajes = payload.putArray("messages");
        mensajes.addObject().put("role", "system").put("content", instruccionSistema);

        if (historial != null) {
            for (TurnoChat turno : historial) {
                if (turno == null || turno.texto() == null || turno.texto().isBlank()) {
                    continue;
                }
                mensajes.addObject()
                        .put("role", normalizarRol(turno.rol()))
                        .put("content", turno.texto());
            }
        }
        mensajes.addObject().put("role", "user").put("content", pregunta);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.warn("El proveedor del chatbot respondió con HTTP {}.", response.statusCode());
            throw new ProveedorChatbotException(response.statusCode() == 429
                    ? "El asistente está ocupado. Intenta de nuevo en unos segundos."
                    : "No pude comunicarme con el asistente. Intenta de nuevo.");
        }

        JsonNode texto = objectMapper.readTree(response.body())
                .path("choices").path(0).path("message").path("content");
        if (!texto.isTextual() || texto.asText().isBlank()) {
            logger.warn("El proveedor del chatbot respondió sin texto.");
            return "El asistente no pudo generar una respuesta. Intenta de nuevo.";
        }
        return texto.asText();
    }

    private String normalizarRol(String rol) {
        if (rol == null) {
            return "user";
        }
        return switch (rol.toLowerCase(Locale.ROOT)) {
            case "assistant", "model", "bot" -> "assistant";
            default -> "user";
        };
    }

    public record TurnoChat(String rol, String texto) {
    }

    public record ResultadoAccion(String mensaje, boolean exito) {
        private static ResultadoAccion exito(String mensaje) {
            return new ResultadoAccion(mensaje, true);
        }

        private static ResultadoAccion error(String mensaje) {
            return new ResultadoAccion(mensaje, false);
        }
    }

    private static final class ProveedorChatbotException extends RuntimeException {
        private ProveedorChatbotException(String mensaje) {
            super(mensaje);
        }
    }
}
