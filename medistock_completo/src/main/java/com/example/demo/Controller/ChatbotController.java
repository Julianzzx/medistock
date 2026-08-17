package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Model.Usuario;
import com.example.demo.service.ChatbotService;
import com.example.demo.service.ChatbotService.TurnoChat;
import com.example.demo.service.ResultadoAccion;
import com.example.demo.service.UsuarioService;

/**
 * Endpoint del asistente conversacional flotante. Solo accesible para
 * usuarios autenticados con rol Usuario (cubierto por
 * SecurityConfig: "/usuario/**" exige autenticación). No contiene lógica de
 * negocio del chatbot: solo recibe la petición, resuelve el Usuario y
 * delega en ChatbotService.
 */
@RestController
@RequestMapping("/usuario/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/mensaje")
    public RespuestaChat mensaje(@RequestBody PeticionChat peticion, Authentication auth) {
        Usuario usuario = usuarioService.obtenerOCrearUsuario(auth.getName());

        List<TurnoChat> historial = peticion.historial() == null
                ? List.of()
                : peticion.historial().stream()
                        .map(t -> new TurnoChat(t.rol(), t.texto()))
                        .toList();

        ResultadoAccion resultado = chatbotService.responder(usuario, peticion.pregunta(), historial);

        return new RespuestaChat(resultado.getMensaje(), !"error".equals(resultado.getTipo()));
    }

    /** Cuerpo de la petición del widget: la pregunta y el historial de la conversación en curso. */
    public record PeticionChat(String pregunta, List<TurnoChatDTO> historial) {
    }

    /** Un turno previo tal como lo guarda el widget en el navegador (rol "user"/"model" + texto). */
    public record TurnoChatDTO(String rol, String texto) {
    }

    public record RespuestaChat(String respuesta, boolean exito) {
    }
}
