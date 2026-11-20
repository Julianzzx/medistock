package org.db.medistock.dto;

public record DetalleFarmaciaDTO(
        Long farmaciaId,
        String farmaciaNombre,
        int solicitudesTotales,
        int solicitudesAtendidas,
        int solicitudesSinAtender,
        int adminsDisponibles,
        int adminsAsignados,
        int adminsLiberados,
        int rendimientoNeto
) {
}