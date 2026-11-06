package org.db.medistock.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="reabastecimiento")

public class Reabastecimiento {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)

    private Long id;
    private int cantidad;
    private LocalDateTime fechasolicitud;

    @ManyToOne
    @JoinColumn(name="medicamento_id")
    private Medicamento medicamento;

    @Enumerated(EnumType.STRING)
    private EstadoReabastecimiento estado;
    public enum  EstadoReabastecimiento {
        EN_PROCESO,
        ACEPTADA,
        RECHAZADA
    }

}
