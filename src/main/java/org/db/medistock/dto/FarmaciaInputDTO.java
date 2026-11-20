package org.db.medistock.dto;

public record FarmaciaInputDTO(
        long farmacia_id,
        String farmacia_nombre,
        int total_solicitudes,
        int admins_disponibles
) {
}