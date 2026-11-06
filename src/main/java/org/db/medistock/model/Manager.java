package org.db.medistock.model;

import jakarta.persistence.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="manager")

public class Manager {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String nombre;
    private String apellido;
    private String usuario;
    private String identificacion;
    private String email;
    private String rol;
    private String codempleado;
    private String password;


}
