package org.db.medistock.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="cita")

public class Cita {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)

    private Long id;
    private String codigo;
    private LocalDateTime fechacita;
    private boolean codestado;

    @ManyToOne
    @JoinColumn(name="usuario_id")
    private Usuario usuario;

}
