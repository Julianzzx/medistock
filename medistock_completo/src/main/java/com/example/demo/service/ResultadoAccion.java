package com.example.demo.service;

/**
 * Resultado de una operación de negocio que el Controller necesita
 * traducir a un mensaje flash (tipo + texto) sin conocer los detalles
 * de por qué la operación tuvo ese resultado.
 */
public class ResultadoAccion {

    private final String tipo; // success | error | warning
    private final String mensaje;

    public ResultadoAccion(String tipo, String mensaje) {
        this.tipo = tipo;
        this.mensaje = mensaje;
    }

    public static ResultadoAccion exito(String mensaje) {
        return new ResultadoAccion("success", mensaje);
    }

    public static ResultadoAccion error(String mensaje) {
        return new ResultadoAccion("error", mensaje);
    }

    public static ResultadoAccion advertencia(String mensaje) {
        return new ResultadoAccion("warning", mensaje);
    }

    public String getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }
}
