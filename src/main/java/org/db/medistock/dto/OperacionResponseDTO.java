package org.db.medistock.dto;

import java.util.List;

public record OperacionResponseDTO(
        String status,
        int solicitudesAtendidas,
        int adminsAsignados,
        int adminsLiberados,
        double rendimientoNeto,
        double z,
        String recomendacion,
        int totalSolicitudes,
        int adminsDisponibles,
        int capacidadPorAdmin,
        String objetivo,
        boolean optimosAlternativos,
        List<VerticeDTO> vertices,
        String fuente,
        boolean datosPorDefecto,
        List<DetalleFarmaciaDTO> detallePorFarmacia
) {
    public static final String OPTIMAL = "Optimal";
    public static final String NO_DISPONIBLE = "No disponible";

    public OperacionResponseDTO conDatosPorDefecto(boolean valor) {
        return new OperacionResponseDTO(status, solicitudesAtendidas, adminsAsignados, adminsLiberados,
                rendimientoNeto, z, recomendacion, totalSolicitudes, adminsDisponibles, capacidadPorAdmin,
                objetivo, optimosAlternativos, vertices, fuente, valor, detallePorFarmacia);
    }

    /** Resultado vacío para cuando no hay respuesta del microservicio: la vista muestra el mensaje en lugar de cifras. */
    public static OperacionResponseDTO noDisponible(OperacionRequestDTO request, String mensaje) {
        return new OperacionResponseDTO(NO_DISPONIBLE, 0, 0, 0, 0, 0, mensaje,
                request.totalSolicitudesCalculado(), request.adminsDisponiblesCalculados(),
                request.capacidad_por_admin(), request.objetivo(), false, List.of(),
                "Sin conexión", false, List.of());
    }
}