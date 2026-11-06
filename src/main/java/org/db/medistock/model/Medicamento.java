package org.db.medistock.model;


import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="medicamento")

public class Medicamento {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)

    private Long id;
    private String nombre;
    private String descripcion;
    private String concentracion;
    private String presentacion;
    private int cantidad;
    private String laboratorio;

    @ManyToOne
    @JoinColumn(name="farmacia_id")
    private Farmacia farmacia;
}
