package org.db.medistock.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="solicitud")

public class Solicitud {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)

    private Long id;
    private int cantidad;
    private LocalDateTime fechasolicitud;

    @ManyToOne
    @JoinColumn(name="usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name="medicamento_id")
    private Medicamento medicamento;

    @ManyToOne
    @JoinColumn(name="cita_id")
    private Cita cita;


    @Enumerated(EnumType.STRING)
   private EstadoSolicitud estado;
    public  enum EstadoSolicitud {
        EN_PROCESO,
        ACEPTADA,
        RECHAZADA
    }

}
