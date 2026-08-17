package com.example.demo.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.demo.Model.Admin;
import com.example.demo.Model.Manager;
import com.example.demo.Model.Usuario;
import com.example.demo.Repository.AdminRepository;
import com.example.demo.Repository.ManagerRepository;
import com.example.demo.Repository.UsuarioRepository;

@Service
public class Autenticacion implements UserDetailsService {

    private final ManagerRepository managerRepo;
    private final UsuarioRepository usuarioRepo;
    private final AdminRepository adminRepo;

    public Autenticacion(ManagerRepository managerRepo, UsuarioRepository usuarioRepo, AdminRepository adminRepo) {
        this.managerRepo = managerRepo;
        this.usuarioRepo = usuarioRepo;
        this.adminRepo = adminRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Admin admin = adminRepo.findByNomUser(username);
        if (admin != null) {
            return User.builder()
                    .username(admin.getNomUser())
                    .password(admin.getIdentificacion())
                    .roles("ADMIN")
                    .build();
        }

        Manager manager = managerRepo.findByIdentificacion(username);
        if (manager != null) {
            return User.builder()
                    .username(manager.getIdentificacion())
                    .password(manager.getCodigoEmpleado())
                    .roles("MANAGER")
                    .build();
        }

        Usuario usuario = usuarioRepo.findByCorreo(username);
        if (usuario != null) {
            return User.builder()
                    .username(usuario.getCorreo())
                    .password(usuario.getIdentificacion())
                    .roles("USER")
                    .build();
        }

        throw new UsernameNotFoundException("Usuario no encontrado: " + username);
    }
}
