package org.db.medistock.model;

import jakarta.persistence.*;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name="admin")

public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String nombre;
    private String apellido;
    private String usuario;
    private Long identificacion;
    private String password;
    private String rol;

    @ManyToOne
    @JoinColumn(name = "eps_id", nullable = false)
    private Eps eps;

}
