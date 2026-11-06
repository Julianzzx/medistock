package org.db.medistock.service;

import org.db.medistock.model.Eps;
import org.db.medistock.model.Usuario;
import org.db.medistock.repository.EpsRepository;
import org.db.medistock.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EpsRepository epsRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, EpsRepository epsRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.epsRepository = epsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Buscar Usuario Por ID

    @Transactional(readOnly = true)
    public Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se ha encontrado el usuario"));
    }

    // Mostrar Usuario

    @Transactional(readOnly = true)
    public Page<Usuario> mostrarUsuarios(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return usuarioRepository.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrIdentificacionContainingIgnoreCase(
                    keyword, keyword, keyword, pageable);
        }
        return usuarioRepository.findAll(pageable);
    }

    // Crear Usuario

    @Transactional
    public Usuario crearUsuario(Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
            usuario.setRol("USER");
        }

        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }

        if (usuario.getEps() != null && usuario.getEps().getId() != null) {
            Eps eps = epsRepository.findById(usuario.getEps().getId()).orElse(null);
            usuario.setEps(eps);
        } else {
            usuario.setEps(null);
        }

        return usuarioRepository.save(usuario);
    }

    // Editar Usuario

    @Transactional
    public Usuario editarUsuario(Long id, Usuario usuarioEditado) {
        Usuario usuario = buscarUsuario(id);

        usuario.setNombre(usuarioEditado.getNombre());
        usuario.setApellido(usuarioEditado.getApellido());
        usuario.setIdentificacion(usuarioEditado.getIdentificacion());
        usuario.setEmail(usuarioEditado.getEmail());
        usuario.setTelefono(usuarioEditado.getTelefono());
        usuario.setDireccion(usuarioEditado.getDireccion());
        usuario.setRol(usuarioEditado.getRol());
        usuario.setEdad(usuarioEditado.getEdad());
        usuario.setGenero(usuarioEditado.getGenero());
        usuario.setRegimen(usuarioEditado.getRegimen());

        if (usuarioEditado.getPassword() != null && !usuarioEditado.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(usuarioEditado.getPassword()));
        }

        if (usuarioEditado.getEps() != null && usuarioEditado.getEps().getId() != null) {
            Eps eps = epsRepository.findById(usuarioEditado.getEps().getId()).orElse(null);
            usuario.setEps(eps);
        } else {
            usuario.setEps(null);
        }

        return usuarioRepository.save(usuario);
    }

    // Eliminar Usuario

    @Transactional
    public boolean eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            return false;
        }
        usuarioRepository.deleteById(id);
        return true;
    }

    // Registrar Usuario

    @Transactional
    public Usuario registrarUsuario(Usuario usuario) {
        if (usuario.getRol() == null || usuario.getRol().isEmpty()) {
            usuario.setRol("USER");
        }

        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }

        if (usuario.getEps() != null && usuario.getEps().getId() != null) {
            Eps eps = epsRepository.findById(usuario.getEps().getId())
                    .orElseThrow(() -> new RuntimeException("No se ha encontrado la EPS seleccionada"));
            usuario.setEps(eps);
        } else {
            usuario.setEps(null);
        }

        return usuarioRepository.save(usuario);
    }

    // Actualizar Usuario

    @Transactional
    public Usuario actualizarPerfilUsuario(Long id, Usuario usuarioEditado) {
        Usuario usuario = buscarUsuario(id);

        usuario.setNombre(usuarioEditado.getNombre());
        usuario.setApellido(usuarioEditado.getApellido());
        usuario.setEmail(usuarioEditado.getEmail());
        usuario.setTelefono(usuarioEditado.getTelefono());
        usuario.setDireccion(usuarioEditado.getDireccion());
        usuario.setRegimen(usuarioEditado.getRegimen());
        usuario.setEdad(usuarioEditado.getEdad());
        usuario.setGenero(usuarioEditado.getGenero());

        if (usuarioEditado.getPassword() != null && !usuarioEditado.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(usuarioEditado.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    // Actualizar Unicamente EPS

    @Transactional
    public Usuario actualizarEpsUsuario(Long id, Long epsId) {
        Usuario usuario = buscarUsuario(id);

        if (usuario.isEpsBloqueada()) {
            throw new RuntimeException("Ya has cambiado tu EPS. Cierra sesión e inicia nuevamente para cambiarla de nuevo.");
        }

        Eps nuevaEps = epsRepository.findById(epsId)
                .orElseThrow(() -> new RuntimeException("No se ha encontrado la EPS seleccionada"));

        usuario.setEps(nuevaEps);
        usuario.setEpsBloqueada(true);

        return usuarioRepository.save(usuario);
    }

    // Buscar Usuario por Email

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No se ha encontrado el usuario autenticado"));
    }
}