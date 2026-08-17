package com.example.demo.service;

import com.example.demo.Repository.WekaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

@Service
public class WekaService {

    private static final Logger log = Logger.getLogger(WekaService.class.getName());

    @Autowired
    private WekaRepository wekaRepository;

    private J48       classifier;
    private Instances dataset;

    // ── Métricas reales calculadas al arrancar ────────────────────────────
    private double accuracyCv     = 0.0;
    private double kappa          = 0.0;
    private int    numInstancias  = 0;
    private int    numAtributos   = 0;
    private int    numClases      = 0;
    private int    profundidadArbol = 0;
    private int    numHojas       = 0;
    private String fechaEntrenamiento = "";

    @PostConstruct
    public void init() {
        try {
            ClassPathResource res = new ClassPathResource("weka/medistock_modelo.arff");
            try (InputStream is = res.getInputStream()) {
                ArffLoader loader = new ArffLoader();
                loader.setSource(is);
                dataset = loader.getDataSet();
                dataset.setClassIndex(dataset.numAttributes() - 1);
            }

            classifier = new J48();
            classifier.setConfidenceFactor(0.25f);
            classifier.setMinNumObj(5);
            classifier.setUnpruned(false);
            classifier.buildClassifier(dataset);

            // Métricas reales CV-10
            Evaluation eval = new Evaluation(dataset);
            eval.crossValidateModel(classifier, dataset, 10, new Random(42));
            accuracyCv          = eval.pctCorrect();
            kappa               = eval.kappa();
            numInstancias       = dataset.numInstances();
            numAtributos        = dataset.numAttributes() - 1;
            numClases           = dataset.classAttribute().numValues();
            numHojas            = (int) classifier.measureNumLeaves();
            String treeStr      = classifier.graph();
            profundidadArbol    = calcularProfundidad(treeStr);
            fechaEntrenamiento  = LocalDateTime.now()
                                  .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            log.info("[WekaService] J48 listo — accuracy=" + String.format("%.2f%%", accuracyCv)
                    + " kappa=" + String.format("%.4f", kappa)
                    + " instancias=" + numInstancias
                    + " atributos=" + numAtributos
                    + " clases=" + numClases
                    + " hojas=" + numHojas);

        } catch (Exception e) {
            throw new RuntimeException("Error inicializando WekaService: " + e.getMessage(), e);
        }
    }

    // ── Predicción con filtros reales desde BD ─────────────────────────────
    /**
     * Busca en la BD una solicitud aceptada que coincida con los filtros
     * y usa sus atributos reales para alimentar el modelo Weka.
     * Si no hay coincidencias con los filtros, usa los datos más recientes disponibles.
     */
    public String predictPorFiltros(String presentacion, String medicamento, String laboratorio) throws Exception {
        LocalDateTime ahora = LocalDateTime.now();

        // Normalizar filtros vacíos a null para que las queries los ignoren
        String pres = (presentacion != null && !presentacion.isBlank()) ? presentacion.trim() : null;
        String lab  = (laboratorio  != null && !laboratorio.isBlank())  ? laboratorio.trim()  : null;

        // Buscar datos reales en BD que coincidan con los filtros
        Object[] row = null;
        try {
            row = wekaRepository.obtenerDatosConFiltros(pres, lab);
        } catch (Exception e) {
            log.warning("[WekaService] Error buscando con filtros: " + e.getMessage());
        }

        // Si no hay datos con los filtros, usar el dato más reciente general
        if (row == null) {
            Page<Object[]> page = wekaRepository.obtenerDatosParaPrediccionPage(PageRequest.of(0, 1));
            List<Object[]> rows = page.getContent();
            if (rows != null && !rows.isEmpty()) {
                row = rows.get(0);
            }
        }

        if (row != null) {
            // Usar los atributos reales de la BD, pero respetar los filtros del usuario
            // si estos fueron especificados (para que el modelo reciba exactamente lo solicitado)
            String nombreAgrupado = str(row[1]);
            String labUsado  = (lab  != null) ? lab  : str(row[2]);
            String presUsado = (pres != null) ? pres : str(row[3]);

            return predictManual(ahora,
                    toDouble(row[0]),
                    nombreAgrupado,
                    labUsado,
                    presUsado,
                    toDouble(row[4]),
                    toDouble(row[5]),
                    toDouble(row[6]),
                    str(row[7]),
                    str(row[8]),
                    toDouble(row[9]),
                    toDouble(row[10]),
                    toDouble(row[11]));
        }

        // Fallback: sin datos en BD, predecir con valores mínimos
        return predictManual(ahora,
                1,
                "Otro_med",
                (lab  != null ? lab  : "Genfar"),
                (pres != null ? pres : "Tabletas"),
                50, 10, 30,
                "No_especificado", "No_especificado",
                1, 1, 1);
    }

    // ── Predicción desde BD (sin filtros, dato más reciente) ─────────────

    public String predictDesdeDB() throws Exception {
        Page<Object[]> page = wekaRepository.obtenerDatosParaPrediccionPage(PageRequest.of(0, 1));
        List<Object[]> rows = page.getContent();

        if (rows == null || rows.isEmpty()) {
            return predictManual(LocalDateTime.now(),
                    1, "Otro_med", "Genfar", "Tabletas",
                    50, 10, 30, "No_especificado", "No_especificado", 1, 1, 1);
        }
        Object[] r = rows.get(0);
        return predictManual(LocalDateTime.now(),
                toDouble(r[0]), str(r[1]), str(r[2]), str(r[3]),
                toDouble(r[4]), toDouble(r[5]), toDouble(r[6]),
                str(r[7]), str(r[8]),
                toDouble(r[9]), toDouble(r[10]), toDouble(r[11]));
    }

    public String predictManual(LocalDateTime fechaRef,
                                double cantidadSol,
                                String nombreAgrupado, String laboratorio, String presentacion,
                                double cantidadInv, double demandaGlobal,
                                double edad, String genero, String regimen,
                                double epsIdUsr, double farmaciaId, double epsIdFarm)
            throws Exception {

        DenseInstance inst = new DenseInstance(dataset.numAttributes());
        inst.setDataset(dataset);
        inst.setValue(0,  fechaRef.getMonthValue());
        inst.setValue(1,  fechaRef.getYear());
        inst.setValue(2,  weekOfYear(fechaRef));
        inst.setValue(3,  fechaRef.getDayOfMonth());
        inst.setValue(4,  fechaRef.getDayOfWeek().getValue() - 1);
        inst.setValue(5,  quarter(fechaRef));
        inst.setValue(6,  cantidadSol);
        inst.setValue(7,  nomIdx(7,  normalizar(nombreAgrupado)));
        inst.setValue(8,  nomIdx(8,  normalizar(laboratorio)));
        inst.setValue(9,  nomIdx(9,  normalizar(presentacion)));
        inst.setValue(10, cantidadInv);
        inst.setValue(11, demandaGlobal);
        inst.setValue(12, edad);
        inst.setValue(13, nomIdx(13, normalizar(genero)));
        inst.setValue(14, nomIdx(14, normalizar(regimen)));
        inst.setValue(15, epsIdUsr);
        inst.setValue(16, farmaciaId);
        inst.setValue(17, epsIdFarm);
        inst.setClassMissing();

        double idx = classifier.classifyInstance(inst);
        return dataset.classAttribute().value((int) idx);
    }

    // ── Estadísticas BD ───────────────────────────────────────────────────

    public String medicamentoRealMasSolicitado() {
        try {
            Object[] row = wekaRepository.medicamentoMasSolicitadoReal();
            if (row == null) return "Sin datos";
            Object first = row[0];
            if (first instanceof Object[]) first = ((Object[]) first)[0];
            return first != null ? first.toString().trim() : "Sin datos";
        } catch (Exception e) { return "Sin datos"; }
    }

    public long totalSolicitudesAceptadas() {
        try {
            Long c = wekaRepository.contarSolicitudesAceptadas();
            return c != null ? c : 0L;
        } catch (Exception e) { return 0L; }
    }

    public List<Map<String,Object>> topHistorico() {
        try { return parsearTop(wekaRepository.topHistorico()); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    public List<Map<String,Object>> topDelMes() {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            return parsearTop(wekaRepository.topDelMes(ahora.getMonthValue(), ahora.getYear()));
        } catch (Exception e) { return new ArrayList<>(); }
    }

    /**
     * Top histórico filtrado por presentación y/o laboratorio.
     * Si ambos son nulos/vacíos retorna el histórico general.
     */
    public List<Map<String,Object>> topHistoricoFiltrado(String presentacion, String laboratorio) {
        try {
            String pres = (presentacion != null && !presentacion.isBlank()) ? presentacion.trim() : null;
            String lab  = (laboratorio  != null && !laboratorio.isBlank())  ? laboratorio.trim()  : null;
            return parsearTop(wekaRepository.topHistoricoFiltrado(pres, lab));
        } catch (Exception e) { return new ArrayList<>(); }
    }

    /**
     * Top del mes actual filtrado por presentación y/o laboratorio.
     */
    public List<Map<String,Object>> topDelMesFiltrado(String presentacion, String laboratorio) {
        try {
            LocalDateTime ahora = LocalDateTime.now();
            String pres = (presentacion != null && !presentacion.isBlank()) ? presentacion.trim() : null;
            String lab  = (laboratorio  != null && !laboratorio.isBlank())  ? laboratorio.trim()  : null;
            return parsearTop(wekaRepository.topDelMesFiltrado(
                    ahora.getMonthValue(), ahora.getYear(), pres, lab));
        } catch (Exception e) { return new ArrayList<>(); }
    }

    public List<String> obtenerPresentacionesUnicas() {
        try {
            return wekaRepository.obtenerPresentacionesUnicas();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<String> obtenerLaboratoriosUnicos() {
        try {
            return wekaRepository.obtenerLaboratoriosUnicos();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ── Getters métricas modelo ───────────────────────────────────────────

    public String getAccuracyFormateado()    { return String.format("%.2f%%", accuracyCv); }
    public String getKappaFormateado()       { return String.format("%.4f", kappa); }
    public int    getNumInstancias()         { return numInstancias; }
    public int    getNumAtributos()          { return numAtributos; }
    public int    getNumClases()             { return numClases; }
    public int    getProfundidadArbol()      { return profundidadArbol; }
    public int    getNumHojas()              { return numHojas; }
    public String getFechaEntrenamiento()    { return fechaEntrenamiento; }

    // ── Helpers privados ──────────────────────────────────────────────────

    private List<Map<String,Object>> parsearTop(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return new ArrayList<>();
        long totalGeneral = rows.stream().mapToLong(r -> toLong(r[1])).sum();
        List<Map<String,Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] r : rows) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("rank",             rank++);
            m.put("medicamento",      str(r[0]));
            m.put("totalSolicitudes", toLong(r[1]));
            m.put("totalUnidades",    toLong(r[2]));
            double pct = totalGeneral > 0 ? (toLong(r[1]) * 100.0 / totalGeneral) : 0;
            m.put("porcentaje",       String.format("%.1f", pct));
            result.add(m);
        }
        return result;
    }

    private double nomIdx(int attrIdx, String value) {
        int i = dataset.attribute(attrIdx).indexOfValue(value);
        return i < 0 ? 0 : i;
    }
    private String normalizar(String v) {
        if (v == null || v.isBlank()) return "Otro_med";
        return v.trim().replace(" ", "_");
    }
    private double toDouble(Object o) {
        if (o == null) return 0;
        try { return Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return 0; }
    }
    private String str(Object o) { return o == null ? "" : o.toString().trim(); }
    private long toLong(Object o) {
        if (o == null) return 0;
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return 0; }
    }
    private int weekOfYear(LocalDateTime dt) {
        return dt.toLocalDate().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }
    private int quarter(LocalDateTime dt) { return (dt.getMonthValue() - 1) / 3 + 1; }

    /** Calcula la profundidad máxima del árbol a partir de su representación DOT. */
    private int calcularProfundidad(String dotGraph) {
        if (dotGraph == null || dotGraph.isEmpty()) return 0;
        long edges = dotGraph.lines()
                .filter(l -> l.contains("->"))
                .count();
        return numHojas > 0 ? (int)(Math.log(numHojas) / Math.log(2)) + 1 : (int) edges;
    }
}
