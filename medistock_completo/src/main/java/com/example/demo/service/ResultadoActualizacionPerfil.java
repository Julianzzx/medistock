package com.example.demo.service;

/**
 * Resultado de actualizar el perfil de un usuario: el Controller necesita
 * saber si el correo cambió para decidir si debe invalidar la sesión.
 */
public class ResultadoActualizacionPerfil {

    private final boolean encontrado;
    private final boolean correoCambiado;

    public ResultadoActualizacionPerfil(boolean encontrado, boolean correoCambiado) {
        this.encontrado = encontrado;
        this.correoCambiado = correoCambiado;
    }

    public boolean isEncontrado() {
        return encontrado;
    }

    public boolean isCorreoCambiado() {
        return correoCambiado;
    }
}
