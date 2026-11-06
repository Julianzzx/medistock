package org.db.medistock.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="eps")
public class Eps {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String direccion;
    private String email;
    private String telefono;
    private String nit;

    @OneToMany(mappedBy = "eps")
    private List<Usuario> usuarios;

    @OneToMany(mappedBy = "eps")
    private List<Farmacia> farmacias;

    @OneToMany(mappedBy = "eps")
    private List<Admin> admins;
}