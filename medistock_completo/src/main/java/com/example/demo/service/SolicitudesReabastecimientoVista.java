package com.example.demo.service;

import org.springframework.data.domain.Page;

import com.example.demo.Model.SolicitudReabastecimiento;

/**
 * Agrupa las tres páginas (pendientes/aceptadas/rechazadas) que la vista
 * manager/solicitudes necesita, para que el Controller no tenga que
 * orquestar tres consultas condicionales por sí mismo.
 */
public class SolicitudesReabastecimientoVista {

    private final Page<SolicitudReabastecimiento> pendientes;
    private final Page<SolicitudReabastecimiento> aceptadas;
    private final Page<SolicitudReabastecimiento> rechazadas;

    public SolicitudesReabastecimientoVista(Page<SolicitudReabastecimiento> pendientes,
            Page<SolicitudReabastecimiento> aceptadas,
            Page<SolicitudReabastecimiento> rechazadas) {
        this.pendientes = pendientes;
        this.aceptadas = aceptadas;
        this.rechazadas = rechazadas;
    }

    public Page<SolicitudReabastecimiento> getPendientes() {
        return pendientes;
    }

    public Page<SolicitudReabastecimiento> getAceptadas() {
        return aceptadas;
    }

    public Page<SolicitudReabastecimiento> getRechazadas() {
        return rechazadas;
    }
}
