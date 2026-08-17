package com.example.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.Model.Eps;
import com.example.demo.Model.Farmacia;
import com.example.demo.Model.Medicamento;
import com.example.demo.Model.Usuario;
import com.example.demo.Repository.FarmaciaRepository;
import com.example.demo.Repository.MedicamentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Asistente conversacional de MediStock, restringido por instrucción de
 * sistema a responder únicamente sobre el aplicativo (medicamentos,
 * farmacias, stock, solicitudes, citas). Usa la API de Gemini
 * (generateContent) y le da como contexto el inventario real de la EPS del
 * usuario que pregunta, para que pueda responder cosas como "¿en qué
 * farmacia hay Acetaminofén?".
 *
 * La cantidad de mensajes que recuerda es solo la de la conversación en
 * curso (se le reenvía el historial en cada llamada); no persiste nada.
 */
@Service
public class ChatbotService {

    private static final int MAX_MEDICAMENTOS_EN_CONTEXTO = 300;
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String modelo;

    private final FarmaciaRepository farmaciaRepo;
    private final MedicamentoRepository medicamentoRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public ChatbotService(FarmaciaRepository farmaciaRepo, MedicamentoRepository medicamentoRepo) {
        this.farmaciaRepo = farmaciaRepo;
        this.medicamentoRepo = medicamentoRepo;
    }

    /**
     * Responde una pregunta del usuario. historial son los turnos previos de
     * ESTA conversación (rol "user"/"model" + texto), enviados por el
     * frontend para dar continuidad sin guardar nada en el servidor.
     */
    public ResultadoAccion responder(Usuario usuario, String pregunta, List<TurnoChat> historial) {
        if (pregunta == null || pregunta.isBlank()) {
            return ResultadoAccion.error("Escribe una pregunta.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return ResultadoAccion.error(
                    "El asistente no está configurado todavía (falta la clave de la API de Gemini).");
        }

        String contexto = construirContextoInventario(usuario);
        String systemInstruction = construirInstruccionSistema(contexto);

        try {
            String textoRespuesta = llamarGemini(systemInstruction, pregunta, historial);
            return ResultadoAccion.exito(textoRespuesta);
        } catch (Exception e) {
            return ResultadoAccion.error("No pude comunicarme con el asistente en este momento. Intenta de nuevo.");
        }
    }

    /**
     * Instrucción de sistema que limita el alcance del asistente: solo
     * MediStock (medicamentos, farmacias, stock, solicitudes, citas, uso de
     * la plataforma). Cualquier otro tema se rechaza cortésmente.
     */
    private String construirInstruccionSistema(String contextoInventario) {
        return """
                Eres el asistente virtual de MediStock, una plataforma de gestión de medicamentos
                para farmacias afiliadas a distintas EPS.

                REGLAS ESTRICTAS:
                1. SOLO respondes preguntas relacionadas con MediStock: disponibilidad y stock de
                   medicamentos, en qué farmacia se encuentra un medicamento, cómo generar un
                   código de cita, cómo funciona una solicitud, el perfil del usuario, o el uso
                   general de la plataforma.
                2. Si te preguntan algo que no tiene relación con MediStock (temas generales,
                   consejos médicos personalizados, diagnósticos, otros productos, temas fuera de
                   la aplicación, etc.), responde amablemente que solo puedes ayudar con temas de
                   MediStock y redirige la conversación hacia lo que sí puedes hacer.
                3. NUNCA des consejos médicos, dosis, ni recomendaciones de tratamiento. Si
                   preguntan eso, indica que deben consultar a su médico y que tú solo puedes
                   informar sobre disponibilidad/stock.
                4. Usa exclusivamente la información de inventario que se te entrega a
                   continuación para responder sobre stock; si el medicamento no aparece en la
                   lista, di que no está disponible en las farmacias de su EPS actualmente.
                5. Sé breve, claro y cordial. Responde en español.

                INVENTARIO ACTUAL (farmacia → medicamentos con stock) DE LA EPS DEL USUARIO:
                %s
                """.formatted(contextoInventario);
    }

    /**
     * Arma un resumen del inventario de las farmacias de la EPS del usuario,
     * para que el modelo pueda responder preguntas de disponibilidad reales
     * sin inventar datos. Se limita el número de líneas para no disparar el
     * tamaño del prompt.
     */
    private String construirContextoInventario(Usuario usuario) {
        Eps eps = usuario.getEps();
        if (eps == null) {
            return "El usuario todavía no tiene una EPS asignada, por lo que no hay inventario disponible.";
        }

        List<Farmacia> farmacias = farmaciaRepo.findByEpsId(eps.getId());
        if (farmacias.isEmpty()) {
            return "La EPS " + eps.getNombre() + " no tiene farmacias registradas actualmente.";
        }

        StringBuilder sb = new StringBuilder();
        int lineas = 0;

        for (Farmacia farmacia : farmacias) {
            if (lineas >= MAX_MEDICAMENTOS_EN_CONTEXTO) {
                sb.append("(lista truncada, hay más farmacias/medicamentos de los mostrados)\n");
                break;
            }
            List<Medicamento> medicamentos = medicamentoRepo.findByFarmaciaId(farmacia.getId());
            sb.append("- Farmacia \"").append(farmacia.getNombre()).append("\" (")
                    .append(farmacia.getDireccion()).append("):\n");

            if (medicamentos.isEmpty()) {
                sb.append("    (sin medicamentos registrados)\n");
                continue;
            }

            for (Medicamento m : medicamentos) {
                if (lineas >= MAX_MEDICAMENTOS_EN_CONTEXTO) {
                    break;
                }
                sb.append("    * ").append(m.getNombre());
                if (m.getConcentracion() != null && !m.getConcentracion().isBlank()) {
                    sb.append(" ").append(m.getConcentracion());
                }
                if (m.getPresentacion() != null && !m.getPresentacion().isBlank()) {
                    sb.append(" (").append(m.getPresentacion()).append(")");
                }
                sb.append(" — stock: ").append(m.getCantidad()).append("\n");
                lineas++;
            }
        }

        return sb.toString();
    }

    /**
     * Llama al endpoint generateContent de la API de Gemini y devuelve solo
     * el texto de la respuesta.
     */
    private String llamarGemini(String systemInstruction, String pregunta, List<TurnoChat> historial)
            throws Exception {

        ObjectNode raiz = objectMapper.createObjectNode();

        ObjectNode systemNode = raiz.putObject("system_instruction");
        ArrayNode systemParts = systemNode.putArray("parts");
        systemParts.addObject().put("text", systemInstruction);

        ArrayNode contents = raiz.putArray("contents");

        if (historial != null) {
            for (TurnoChat turno : historial) {
                ObjectNode turnoNode = contents.addObject();
                turnoNode.put("role", turno.rol());
                ArrayNode parts = turnoNode.putArray("parts");
                parts.addObject().put("text", turno.texto());
            }
        }

        ObjectNode ultimoTurno = contents.addObject();
        ultimoTurno.put("role", "user");
        ultimoTurno.putArray("parts").addObject().put("text", pregunta);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelo
                + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(raiz)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Gemini respondió " + response.statusCode() + ": " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode textoNode = json.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textoNode.isMissingNode()) {
            throw new IllegalStateException("Respuesta de Gemini sin contenido de texto: " + response.body());
        }

        return textoNode.asText();
    }

    /** Un turno de la conversación (rol "user" o "model" + texto). */
    public record TurnoChat(String rol, String texto) {
    }
}
