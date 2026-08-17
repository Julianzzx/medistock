package com.example.demo.Model;

/**
 * DTO para transportar el resultado de la predicción WEKA a la vista Thymeleaf.
 */
public class PrediccionResultado {

    private String medicamentoPredicho;
    private String mes;
    private String anio;
    private String nombreAgrupado;
    private String laboratorio;
    private String presentacion;
    private double cantidadInv;
    private double demandaGlobal;
    private double edad;
    private String genero;
    private String regimen;
    private double epsIdUsr;
    private double farmaciaId;
    private double epsIdFarm;
    private String error;

    public PrediccionResultado() {}

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getMedicamentoPredicho()              { return medicamentoPredicho; }
    public void setMedicamentoPredicho(String v)        { this.medicamentoPredicho = v; }

    public String getMes()                              { return mes; }
    public void setMes(String mes)                      { this.mes = mes; }

    public String getAnio()                             { return anio; }
    public void setAnio(String anio)                    { this.anio = anio; }

    public String getNombreAgrupado()                   { return nombreAgrupado; }
    public void setNombreAgrupado(String v)             { this.nombreAgrupado = v; }

    public String getLaboratorio()                      { return laboratorio; }
    public void setLaboratorio(String v)                { this.laboratorio = v; }

    public String getPresentacion()                     { return presentacion; }
    public void setPresentacion(String v)               { this.presentacion = v; }

    public double getCantidadInv()                      { return cantidadInv; }
    public void setCantidadInv(double v)                { this.cantidadInv = v; }

    public double getDemandaGlobal()                    { return demandaGlobal; }
    public void setDemandaGlobal(double v)              { this.demandaGlobal = v; }

    public double getEdad()                             { return edad; }
    public void setEdad(double v)                       { this.edad = v; }

    public String getGenero()                           { return genero; }
    public void setGenero(String v)                     { this.genero = v; }

    public String getRegimen()                          { return regimen; }
    public void setRegimen(String v)                    { this.regimen = v; }

    public double getEpsIdUsr()                         { return epsIdUsr; }
    public void setEpsIdUsr(double v)                   { this.epsIdUsr = v; }

    public double getFarmaciaId()                       { return farmaciaId; }
    public void setFarmaciaId(double v)                 { this.farmaciaId = v; }

    public double getEpsIdFarm()                        { return epsIdFarm; }
    public void setEpsIdFarm(double v)                  { this.epsIdFarm = v; }

    public String getError()                            { return error; }
    public void setError(String v)                      { this.error = v; }

    public boolean tieneError()                         { return error != null && !error.isEmpty(); }
}
