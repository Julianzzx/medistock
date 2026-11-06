package org.db.medistock;

import org.db.medistock.model.Admin;
import org.db.medistock.model.Manager;
import org.db.medistock.model.Usuario;
import org.db.medistock.repository.AdminRepository;
import org.db.medistock.repository.EpsRepository;
import org.db.medistock.repository.ManagerRepository;
import org.db.medistock.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("seed-accounts")
public class TemporaryAccountSeeder implements CommandLineRunner {

    private final AdminRepository admins;
    private final ManagerRepository managers;
    private final UsuarioRepository usuarios;
    private final EpsRepository eps;
    private final PasswordEncoder passwordEncoder;

    public TemporaryAccountSeeder(
            AdminRepository admins,
            ManagerRepository managers,
            UsuarioRepository usuarios,
            EpsRepository eps,
            PasswordEncoder passwordEncoder) {
        this.admins = admins;
        this.managers = managers;
        this.usuarios = usuarios;
        this.eps = eps;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        boolean createManager = managers.findByUsuario("manager.demo") == null;
        boolean createAdmin = admins.findByUsuario("admin.demo") == null;
        String userEmail = "usuario.demo@example.test";
        boolean createUser = usuarios.findByEmail(userEmail).isEmpty();

        var assignedEps = createAdmin
                ? eps.findAll().stream().findFirst().orElseThrow(() ->
                        new IllegalStateException(
                                "No hay una EPS en la base de datos; crea una antes del administrador."))
                : null;

        String managerPassword = createManager ? requiredPassword("SEED_MANAGER_PASSWORD") : null;
        String adminPassword = createAdmin ? requiredPassword("SEED_ADMIN_PASSWORD") : null;
        String userPassword = createUser ? requiredPassword("SEED_USER_PASSWORD") : null;

        if (createManager) {
            Manager manager = new Manager(
                    null, "Manager", "Temporal", "manager.demo",
                    "TEMP-M-001", "manager.demo@example.test",
                    "MANAGER", "TEMP-001", passwordEncoder.encode(managerPassword));
            managers.save(manager);
        }

        if (createAdmin) {
            Admin admin = new Admin(
                    null, "Admin", "Temporal", "admin.demo",
                    900000001L, passwordEncoder.encode(adminPassword),
                    "ADMIN", assignedEps);
            admins.save(admin);
        }

        if (createUser) {
            Usuario usuario = new Usuario(
                    null, "Usuario", "Temporal", "TEMP-U-001",
                    userEmail, "3000000000",
                    passwordEncoder.encode(userPassword),
                    "Dirección temporal", "USER", 30, "Otro",
                    "Contributivo", false, null);
            usuarios.save(usuario);
        }

        System.out.println("Cuentas temporales creadas o ya existentes.");
    }

    private String requiredPassword(String variable) {
        String password = System.getenv(variable);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Debes definir la variable " + variable);
        }
        return password;
    }
}
