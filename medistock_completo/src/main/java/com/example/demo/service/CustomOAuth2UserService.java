package com.example.demo.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import com.example.demo.Model.Eps;
import com.example.demo.Model.Usuario;
import com.example.demo.Repository.EpsRepository;
import com.example.demo.Repository.UsuarioRepository;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UsuarioRepository usuarioRepository;
    private final EpsRepository epsRepository;
    private final Random random = new Random();

    public CustomOAuth2UserService(UsuarioRepository usuarioRepository,
            EpsRepository epsRepository) {
        this.usuarioRepository = usuarioRepository;
        this.epsRepository = epsRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String correo;
        String nombre;
        String apellido;

        if ("github".equals(provider)) {
            String login = oAuth2User.getAttribute("login");
            String name = oAuth2User.getAttribute("name");
            String emailAttr = oAuth2User.getAttribute("email");

            correo = (emailAttr != null && !emailAttr.isEmpty()) ? emailAttr : login + "@github.com";
            nombre = (name != null && !name.isEmpty()) ? name : login;
            apellido = "Sin apellido";
        } else {
            String emailAttr = oAuth2User.getAttribute("email");
            String fullName = oAuth2User.getAttribute("name");

            if (emailAttr == null || emailAttr.isEmpty()) {
                throw new OAuth2AuthenticationException("No se pudo obtener el correo desde Google");
            }

            correo = emailAttr;
            nombre = fullName != null ? fullName.split(" ")[0] : "Sin nombre";
            apellido = (fullName != null && fullName.split(" ").length > 1)
                    ? fullName.substring(fullName.indexOf(" ") + 1)
                    : "Sin apellido";
        }

        Usuario usuario = usuarioRepository.findOptionalByCorreo(correo).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setCorreo(correo);
            nuevo.setNombre(nombre);
            nuevo.setApellido(apellido);
            nuevo.setIdentificacion("OAUTH-" + UUID.randomUUID());
            nuevo.setDireccion("No especificada");
            nuevo.setRol("USER");

            nuevo.setEdad(18);
            nuevo.setContacto(0L);
            nuevo.setGenero("No especificado");
            nuevo.setRegimen("No especificado");

            List<Eps> epsDisponibles = epsRepository.findAll();
            if (!epsDisponibles.isEmpty()) {
                Eps seleccionada = epsDisponibles.get(random.nextInt(epsDisponibles.size()));
                nuevo.setEps(seleccionada);
            }

            return usuarioRepository.save(nuevo);
        });

        boolean cambios = false;
        if (!Objects.equals(usuario.getNombre(), nombre)) {
            usuario.setNombre(nombre);
            cambios = true;
        }
        if (!Objects.equals(usuario.getApellido(), apellido)) {
            usuario.setApellido(apellido);
            cambios = true;
        }
        if (cambios) {
            usuarioRepository.save(usuario);
        }

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", correo);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + usuario.getRol())),
                attributes,
                "email");
    }
}
