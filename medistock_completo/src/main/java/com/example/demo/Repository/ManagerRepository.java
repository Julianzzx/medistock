package com.example.demo.Repository;

import com.example.demo.Model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Manager findByIdentificacion(String identificacion);
}
