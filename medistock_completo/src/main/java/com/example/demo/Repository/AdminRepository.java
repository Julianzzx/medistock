package com.example.demo.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.Model.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Admin findByNomUser(String nomUser);

    Admin findByNomUserAndIdentificacion(String nomUser, String identificacion);

    Page<Admin> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrNomUserContainingIgnoreCase(
            String nombre, String apellido, String nomUser, Pageable pageable);
}
