package org.db.medistock.repository;

import org.db.medistock.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerRepository extends JpaRepository<Manager,Long> {

    Manager findByUsuario(String username);
}
