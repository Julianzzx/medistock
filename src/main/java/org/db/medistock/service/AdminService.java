package org.db.medistock.service;

import org.db.medistock.model.Admin;
import org.db.medistock.model.Eps;
import org.db.medistock.repository.AdminRepository;
import org.db.medistock.repository.EpsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final AdminRepository adminRepo;
    private final EpsRepository epsRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepo, EpsRepository epsRepo, PasswordEncoder passwordEncoder) {
        this.adminRepo = adminRepo;
        this.epsRepo = epsRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // Buscar Admin

    @Transactional(readOnly = true)
    public Page<Admin> listarAdmins(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return adminRepo.findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrUsuarioContainingIgnoreCase(
                    keyword, keyword, keyword, pageable);
        }
        return adminRepo.findAll(pageable);
    }

    // Buscar Admin Por ID

    @Transactional(readOnly = true)
    public Admin buscarAdmin(Long id) {
        return adminRepo.findById(id).orElse(null);
    }

    // Buscar Admin Por Nombre de Usuario
    @Transactional(readOnly = true)
    public Admin buscarPorUsuario(String usuario) {
        return adminRepo.findByUsuario(usuario);
    }

    // Crear Admin asignando su EPS

    @Transactional
    public Admin crearAdmin(Admin admin, Long epsId) {
        if (admin.getPassword() != null && !admin.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }

        if (admin.getRol() == null || admin.getRol().isEmpty()) {
            admin.setRol("ROLE_ADMIN");
        }

        if (epsId != null) {
            Eps eps = epsRepo.findById(epsId).orElse(null);
            admin.setEps(eps);
        } else {
            admin.setEps(null);
        }

        return adminRepo.save(admin);
    }

    // Editar Admin actualizando su EPS

    @Transactional
    public Admin editarAdmin(Long id, Admin adminEditado, Long epsId) {
        Admin admin = buscarAdmin(id);
        if (admin == null) {
            throw new RuntimeException("Administrador no encontrado");
        }

        admin.setNombre(adminEditado.getNombre());
        admin.setApellido(adminEditado.getApellido());
        admin.setUsuario(adminEditado.getUsuario());
        admin.setIdentificacion(adminEditado.getIdentificacion());
        admin.setRol(adminEditado.getRol());

        if (adminEditado.getPassword() != null && !adminEditado.getPassword().isBlank()) {
            admin.setPassword(passwordEncoder.encode(adminEditado.getPassword()));
        }

        if (epsId != null) {
            Eps eps = epsRepo.findById(epsId).orElse(null);
            admin.setEps(eps);
        } else {
            admin.setEps(null);
        }

        return adminRepo.save(admin);
    }

    // Eliminar Admin

    @Transactional
    public boolean eliminarAdmin(Long id) {
        Admin admin = buscarAdmin(id);
        if (admin == null) {
            return false;
        }
        adminRepo.delete(admin);
        return true;
    }

    // Obtener La EPS Asociada Al Admin Logueado

    @Transactional(readOnly = true)
    public Eps obtenerEpsDelAdmin(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }

        String username = auth.getName();
        Admin admin = adminRepo.findByUsuario(username);

        if (admin == null) {
            return null;
        }

        return admin.getEps();
    }
}