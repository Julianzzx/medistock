package com.example.demo.Controller;

import com.example.demo.Model.Eps;
import com.example.demo.Model.Usuario;
import com.example.demo.Repository.EpsRepository;
import com.example.demo.Repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
public class RegistroController {

    private final EpsRepository epsRepository;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public RegistroController(EpsRepository epsRepository, UsuarioRepository usuarioRepository) {
        this.epsRepository = epsRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        List<Eps> epsList = epsRepository.findAll();
        model.addAttribute("epsList", epsList);
        model.addAttribute("usuario", new Usuario());
        return "Registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute("usuario") Usuario usuario,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {

            String identificacionEncriptada = passwordEncoder.encode(usuario.getIdentificacion());
            usuario.setIdentificacion(identificacionEncriptada);

            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("mensajeExito", "Registro exitoso. Ahora puedes iniciar sesión.");
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("mensajeError", " Error al registrar el usuario: " + e.getMessage());
            model.addAttribute("epsList", epsRepository.findAll());
            return "Registro";
        }
    }
}
