package org.db.medistock.controller;

import org.db.medistock.model.Usuario;
import org.db.medistock.repository.UsuarioRepository;
import org.db.medistock.service.ChatbotService;
import org.db.medistock.service.ChatbotService.ResultadoAccion;
import org.db.medistock.service.ChatbotService.TurnoChat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/usuario/chatbot")
public class ChatbotController {

    private static final int MAX_TURNOS = 10;
    private static final int MAX_TEXTO_TURNO = 2000;

    private final ChatbotService chatbotService;
    private final UsuarioRepository usuarioRepository;

    public ChatbotController(
            ChatbotService chatbotService,
            UsuarioRepository usuarioRepository) {
        this.chatbotService = chatbotService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/mensaje")
    public RespuestaChat mensaje(
            @RequestBody PeticionChat peticion,
            Authentication authentication) {
        if (peticion == null || peticion.pregunta() == null
                || peticion.pregunta().isBlank()) {
            return new RespuestaChat("Escribe una pregunta.", false);
        }
        if (peticion.historial() != null && peticion.historial().size() > MAX_TURNOS) {
            return new RespuestaChat("La conversación es demasiado larga. Inicia un chat nuevo.", false);
        }

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No se encontró el usuario autenticado."));

        List<TurnoChat> historial = peticion.historial() == null
                ? List.of()
                : peticion.historial().stream()
                        .filter(turno -> turno != null && turno.texto() != null)
                        .map(turno -> new TurnoChat(
                                normalizarRol(turno.rol()),
                                turno.texto().substring(
                                        0, Math.min(turno.texto().length(), MAX_TEXTO_TURNO))))
                        .toList();

        ResultadoAccion resultado = chatbotService.responder(
                usuario, peticion.pregunta(), historial);
        return new RespuestaChat(resultado.mensaje(), resultado.exito());
    }

    private String normalizarRol(String rol) {
        return "assistant".equalsIgnoreCase(rol)
                || "model".equalsIgnoreCase(rol)
                || "bot".equalsIgnoreCase(rol)
                ? "assistant"
                : "user";
    }

    public record PeticionChat(String pregunta, List<TurnoChatDTO> historial) {
    }

    public record TurnoChatDTO(String rol, String texto) {
    }

    public record RespuestaChat(String respuesta, boolean exito) {
    }
}
