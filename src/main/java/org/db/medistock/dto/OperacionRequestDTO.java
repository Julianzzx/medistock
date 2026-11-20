package org.db.medistock.dto;

import java.util.List;

public record OperacionRequestDTO(
        String modo,
        int capacidad_por_admin,
        String objetivo,
        List<FarmaciaInputDTO> farmacias
) {
    public int totalSolicitudesCalculado() {
        if (farmacias == null) return 0;
        return farmacias.stream().mapToInt(FarmaciaInputDTO::total_solicitudes).sum();
    }

    public int adminsDisponiblesCalculados() {
        if (farmacias == null) return 0;
        return farmacias.stream().mapToInt(FarmaciaInputDTO::admins_disponibles).sum();
    }
}