package org.db.medistock.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.db.medistock.model.Admin;
import org.db.medistock.model.Manager;
import org.db.medistock.model.Usuario;
import org.db.medistock.repository.AdminRepository;
import org.db.medistock.repository.ManagerRepository;
import org.db.medistock.repository.UsuarioRepository;

@Service
public class Autenticacion implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final AdminRepository adminRepository;
    private final ManagerRepository managerRepository;

    public Autenticacion(AdminRepository adminRepository,
                         ManagerRepository managerRepository,
                         UsuarioRepository usuarioRepository) {
        this.adminRepository = adminRepository;
        this.managerRepository = managerRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. Buscar en Administradores

        Admin admin = adminRepository.findByUsuario(username);
        if (admin != null) {
            String rolAdmin = admin.getRol() != null ? admin.getRol().replace("ROLE_", "") : "ADMIN";
            return User.builder()
                    .username(admin.getUsuario())
                    .password(admin.getPassword()) // Contraseña cifrada con BCrypt en la base de datos
                    .roles(rolAdmin)
                    .build();
        }

        // 2. Buscar en Managers

        Manager manager = managerRepository.findByUsuario(username);
        if (manager != null) {
            String rolManager = manager.getRol() != null ? manager.getRol().replace("ROLE_", "") : "MANAGER";
            return User.builder()
                    .username(manager.getUsuario())
                    .password(manager.getPassword()) // Contraseña cifrada con BCrypt en la base de datos
                    .roles(rolManager)
                    .build();
        }

        // 3. Buscar en Usuarios / Clientes

        Usuario usuario = usuarioRepository.findByEmail(username).orElse(null);
        if (usuario != null) {
            String rolUsuario = usuario.getRol() != null ? usuario.getRol().replace("ROLE_", "") : "USER";
            return User.builder()
                    .username(usuario.getEmail())
                    .password(usuario.getPassword()) // Contraseña cifrada con BCrypt en la base de datos
                    .roles(rolUsuario)
                    .build();
        }

        throw new UsernameNotFoundException("Usuario no encontrado: " + username);
    }
}