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
        ResultadoAccion invalida = validarPregunta(pregunta);
        if (invalida != null) {
            return invalida;
        }
        return consultarProveedor(construirInstruccionSistema(usuario), pregunta, historial);
    }

    public ResultadoAccion responderComoAnalista(
            String contextoModelo, String pregunta, List<TurnoChat> historial) {
        ResultadoAccion invalida = validarPregunta(pregunta);
        if (invalida != null) {
            return invalida;
        }
        return consultarProveedor(construirInstruccionAnalista(contextoModelo), pregunta, historial);
    }

    /** Devuelve el error a mostrar, o null si la pregunta es válida y el asistente está configurado. */
    private ResultadoAccion validarPregunta(String pregunta) {
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
        return null;
    }

    private ResultadoAccion consultarProveedor(
            String systemInstruction, String pregunta, List<TurnoChat> historial) {
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

    private String construirInstruccionAnalista(String contextoModelo) {
        return """
                Eres el analista virtual de MediStock para el manager. Responde en español, en texto plano
                (sin Markdown: nada de asteriscos, almohadillas ni tablas; para listas usa guiones), de forma
                clara y breve. Tu trabajo es explicar el modelo de optimización de personal y sus gráficos, y
                sugerir acciones operativas al manager.

                Cómo funciona el modelo, para que lo expliques con tus palabras:
                - X es el número de solicitudes que se responden e Y es el número de administradores asignados.
                - Restricciones: X no puede superar la capacidad por admin multiplicada por Y; X no puede superar
                  las solicitudes disponibles; Y no puede superar los admins disponibles; X e Y no son negativos.
                - Objetivo A: maximizar el volumen atendido (Z = X). Objetivo B: maximizar el rendimiento neto
                  (Z = X - Y), que penaliza tener admins ociosos. El bloque principal usa el objetivo B.
                - El gráfico dibuja la región factible: el plano con las solicitudes en el eje horizontal y los
                  admins en el vertical. El polígono reúne todas las combinaciones posibles y la línea naranja
                  punteada es la capacidad (X = capacidad por Y). El óptimo siempre está en un vértice, el
                  marcado con una estrella. Ese vértice es del modelo continuo, así que puede traer decimales
                  de admins; en la práctica se redondea hacia arriba, y por eso los indicadores de arriba
                  muestran la solución entera.
                - Vértices típicos: el origen (nadie trabaja y nada se atiende), el que asigna todos los admins
                  sin solicitudes, el que asigna todos con toda la demanda, y el óptimo, que usa solo los
                  admins necesarios.

                Reglas:
                - Usa únicamente las cifras de los DATOS de abajo. No hagas cálculos propios ni inventes
                  valores: los HECHOS CALCULADOS ya traen faltantes, cobertura y utilización; úsalos tal cual.
                - Si el estado no es Optimal o la fuente indica que no hay conexión, dilo y no analices.
                - Si datosPorDefecto es true, avisa que son valores de ejemplo y no datos reales.
                - Distingue los DATOS REALES (base de datos) de la SIMULACIÓN (parámetros que el manager probó
                  en pantalla) y di de cuál hablas.
                - Las sugerencias deben salir de los hechos (admins libres, solicitudes sin responder, admins
                  adicionales necesarios) y presentarse como recomendaciones: la decisión es del manager.
                - Hablas solo de la operación de MediStock y de este modelo; no des consejos médicos.

                %s
                """.formatted(contextoModelo);
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
