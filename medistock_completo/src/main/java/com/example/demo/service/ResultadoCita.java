package com.example.demo.service;

import com.example.demo.Model.Generarcitas;

/**
 * Resultado de solicitar la generación de un código de cita: puede ser un
 * código recién creado o el que ya existía activo.
 */
public class ResultadoCita {

    private final Generarcitas cita;
    private final String mensaje;

    public ResultadoCita(Generarcitas cita, String mensaje) {
        this.cita = cita;
        this.mensaje = mensaje;
    }

    public Generarcitas getCita() {
        return cita;
    }

    public String getMensaje() {
        return mensaje;
    }
}
