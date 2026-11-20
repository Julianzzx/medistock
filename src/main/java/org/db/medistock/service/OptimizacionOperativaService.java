package org.db.medistock.service;

import org.db.medistock.dto.FarmaciaInputDTO;
import org.db.medistock.dto.OperacionRequestDTO;
import org.db.medistock.dto.OperacionResponseDTO;
import org.db.medistock.model.Farmacia;
import org.db.medistock.repository.AdminRepository;
import org.db.medistock.repository.FarmaciaRepository;
import org.db.medistock.repository.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class OptimizacionOperativaService {

    private static final Logger logger = LoggerFactory.getLogger(OptimizacionOperativaService.class);

    private static final int CAPACIDAD_POR_ADMIN = 100;
    private static final int DEMANDA_POR_DEFECTO = 300;
    private static final int ADMINS_POR_DEFECTO = 4;
    private static final int LIMITE = 1_000_000;
    private static final String OBJETIVO_BLOQUE_EJECUTIVO = "B";
    private static final String ENDPOINT = "/api/v1/optimizar-operacion";

    private final FarmaciaRepository farmaciaRepository;
    private final SolicitudRepository solicitudRepository;
    private final AdminRepository adminRepository;
    private final RestClient restClient;
    private final String optimizerUrl;

    public OptimizacionOperativaService(FarmaciaRepository farmaciaRepository,
            SolicitudRepository solicitudRepository,
            AdminRepository adminRepository,
            @Value("${optimizer.api.url:http://127.0.0.1:8000}") String optimizerUrl) {
        this.farmaciaRepository = farmaciaRepository;
        this.solicitudRepository = solicitudRepository;
        this.adminRepository = adminRepository;
        this.optimizerUrl = optimizerUrl;

        // Timeouts cortos: la vista del manager no debe quedarse colgada si el
        // microservicio está caído.
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().baseUrl(optimizerUrl).requestFactory(factory).build();
    }

    public OperacionResponseDTO calcularOptimoOperativo() {
        return calcularOptimoOperativoPorFarmacia(null);
    }

    public OperacionResponseDTO calcularOptimoOperativoPorFarmacia(Long farmaciaId) {
        List<Farmacia> todasLasFarmacias = farmaciaRepository.findAll();
        List<Farmacia> objetivoFarmacias = new ArrayList<>();

        if (farmaciaId != null) {
            for (Farmacia f : todasLasFarmacias) {
                if (farmaciaId.equals(f.getId())) {
                    objetivoFarmacias.add(f);
                    break;
                }
            }
        } else {
            objetivoFarmacias.addAll(todasLasFarmacias);
        }

        int totalAdminsBD = limitar(adminRepository.count());
        boolean porDefecto = false;

        List<Integer> conteosSolicitudes = new ArrayList<>();
        long sumaSolicitudes = 0;

        for (Farmacia f : objetivoFarmacias) {
            int count = limitar(solicitudRepository.countByMedicamento_Farmacia_Id(f.getId()));
            conteosSolicitudes.add(count);
            sumaSolicitudes += count;
        }

        int totalSolicitudes = limitar(sumaSolicitudes);
        if (totalSolicitudes == 0) {
            totalSolicitudes = DEMANDA_POR_DEFECTO;
            porDefecto = true;
        }

        int adminsDisponibles = totalAdminsBD;
        if (adminsDisponibles == 0) {
            adminsDisponibles = ADMINS_POR_DEFECTO;
            porDefecto = true;
        }

        List<FarmaciaInputDTO> farmaciasInput = construirListaFarmacias(
                objetivoFarmacias, conteosSolicitudes, totalSolicitudes, adminsDisponibles);

        OperacionRequestDTO request = new OperacionRequestDTO(
                "BD", CAPACIDAD_POR_ADMIN, OBJETIVO_BLOQUE_EJECUTIVO, farmaciasInput);

        try {
            return consultar(request).conDatosPorDefecto(porDefecto);
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("Optimización no disponible: {}", e.getMessage());
            return OperacionResponseDTO.noDisponible(request, e.getMessage()).conDatosPorDefecto(porDefecto);
        }
    }

    public OperacionResponseDTO resolver(int capacidadPorAdmin, int maxSolicitudes, int maxAdmins, String objetivo) {
        int cap = Math.max(1, capacidadPorAdmin);
        int sol = Math.max(0, maxSolicitudes);
        int adm = Math.max(0, maxAdmins);
        String obj = (objetivo == null || objetivo.isBlank()) ? OBJETIVO_BLOQUE_EJECUTIVO : objetivo.trim().toUpperCase();

        List<Farmacia> farmaciasBD = farmaciaRepository.findAll();
        List<FarmaciaInputDTO> farmaciasInput = construirListaFarmacias(farmaciasBD, List.of(), sol, adm);

        OperacionRequestDTO request = new OperacionRequestDTO("SIMULACION", cap, obj, farmaciasInput);
        try {
            return consultar(request);
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("Simulación de optimización no disponible: {}", e.getMessage());
            return OperacionResponseDTO.noDisponible(request, e.getMessage());
        }
    }

    private List<FarmaciaInputDTO> construirListaFarmacias(
            List<Farmacia> farmacias,
            List<Integer> conteosIndividuales,
            int totalSolicitudes,
            int totalAdmins) {

        if (farmacias == null || farmacias.isEmpty()) {
            return List.of(new FarmaciaInputDTO(1L, "Operación General", totalSolicitudes, totalAdmins));
        }

        int n = farmacias.size();
        boolean usarConteosIndividuales = conteosIndividuales != null
                && conteosIndividuales.size() == n
                && conteosIndividuales.stream().mapToInt(Integer::intValue).sum() == totalSolicitudes
                && totalSolicitudes > 0;

        List<FarmaciaInputDTO> resultado = new ArrayList<>(n);
        int baseSol = totalSolicitudes / n;
        int restoSol = totalSolicitudes % n;

        int baseAdm = totalAdmins / n;
        int restoAdm = totalAdmins % n;

        for (int i = 0; i < n; i++) {
            Farmacia f = farmacias.get(i);
            long id = f.getId() != null ? f.getId() : (i + 1L);
            String nombre = (f.getNombre() != null && !f.getNombre().isBlank())
                    ? f.getNombre()
                    : ("Farmacia #" + id);

            int solFarmacia = usarConteosIndividuales
                    ? conteosIndividuales.get(i)
                    : (baseSol + (i < restoSol ? 1 : 0));

            int admFarmacia = baseAdm + (i < restoAdm ? 1 : 0);

            resultado.add(new FarmaciaInputDTO(id, nombre, solFarmacia, admFarmacia));
        }

        return resultado;
    }

    private OperacionResponseDTO consultar(OperacionRequestDTO request) {
        try {
            OperacionResponseDTO respuesta = restClient.post()
                    .uri(ENDPOINT)
                    .body(request)
                    .retrieve()
                    .body(OperacionResponseDTO.class);
            if (respuesta == null) {
                throw new IllegalStateException("El microservicio de optimización respondió sin contenido.");
            }
            return respuesta;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                String detalle = e.getResponseBodyAsString();
                throw new IllegalArgumentException(
                        detalle == null || detalle.isBlank() ? "Parámetros inválidos para el modelo." : detalle);
            }
            throw new IllegalStateException(
                    "El microservicio de optimización devolvió un error (HTTP " + e.getStatusCode().value() + ").");
        } catch (ResourceAccessException e) {
            logger.warn("Sin conexión con {}: {}", optimizerUrl, e.getMessage());
            throw new IllegalStateException(
                    "No se pudo conectar con el microservicio de optimización en " + optimizerUrl + ". "
                            + "Inícialo con: uvicorn main:app --host 127.0.0.1 --port 8000 (carpeta optimizer-api). "
                            + "Detalle: " + recortar(e.getMessage()));
        } catch (RestClientException e) {
            logger.error("Error al intercambiar datos con el microservicio de optimización", e);
            throw new IllegalStateException(
                    "El microservicio respondió, pero Spring no pudo procesar el intercambio ("
                            + e.getClass().getSimpleName() + "): " + recortar(e.getMessage()));
        }
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return "sin detalle";
        }
        return texto.length() > 250 ? texto.substring(0, 250) + "…" : texto;
    }

    private static int limitar(long valor) {
        return (int) Math.min(valor, LIMITE);
    }

    public int obtenerTotalSolicitudesReales() {
        long total = solicitudRepository.count();
        if (total <= 0) {
            return DEMANDA_POR_DEFECTO; // 300 si no hay registros
        }
        return limitar(total);
    }

    public int obtenerTotalAdminsReales() {
        long total = adminRepository.count();
        if (total <= 0) {
            return ADMINS_POR_DEFECTO; // 4 si no hay registros
        }
        return limitar(total);
    }
}