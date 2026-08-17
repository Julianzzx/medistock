package com.example.demo.service;

/**
 * Resultado de intentar crear una solicitud de medicamento: además de
 * éxito/mensaje, el Controller necesita el id de la farmacia para construir
 * el redirect de vuelta a la vista de medicamentos.
 */
public class ResultadoSolicitud {

    private final boolean exito;
    private final String mensaje;
    private final Long farmaciaId;

    public ResultadoSolicitud(boolean exito, String mensaje, Long farmaciaId) {
        this.exito = exito;
        this.mensaje = mensaje;
        this.farmaciaId = farmaciaId;
    }

    public boolean isExito() {
        return exito;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Long getFarmaciaId() {
        return farmaciaId;
    }
}
