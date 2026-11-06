package org.db.medistock.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="farmacia")

public class Farmacia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private  Long id;
    private  String nombre;
    private String direccion;
    private  String telefono;

    @OneToMany(mappedBy = "farmacia")
    private List<Medicamento> medicamentos;

    @ManyToOne
    @JoinColumn(name="eps_id")
    private  Eps eps;

}
