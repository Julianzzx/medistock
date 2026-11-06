package org.db.medistock.model;

import jakarta.persistence.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="usuario")

public class Usuario {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)

    private  Long id;
    private  String nombre;
    private  String apellido;
    private String identificacion;
    private  String email;
    private  String telefono;
    private  String password;
    private  String direccion;
    private String rol;
    private int edad;
    private String genero;
    private String regimen;
    private boolean epsBloqueada = false;

    @ManyToOne
    @JoinColumn(name = "eps_id")
    private Eps eps;

}
