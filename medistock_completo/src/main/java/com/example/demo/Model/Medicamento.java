package com.example.demo.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    private String concentracion;
    private String presentacion;
    private int    cantidad;
    private String laboratorio;

    // ── Columnas para modelo predictivo WEKA ──────────────────────────────
    private String nombreAgrupado;   // principio activo agrupado
    private int    demandaGlobal;    // demanda histórica acumulada

    @ManyToOne(optional = true)
    @JoinColumn(name = "farmacia_id")
    private Farmacia farmacia;

    public Medicamento() {}

    public Medicamento(String nombre, String descripcion, String concentracion,
                       String presentacion, int cantidad, String laboratorio,
                       Farmacia farmacia) {
        this.nombre        = nombre;
        this.descripcion   = descripcion;
        this.concentracion = concentracion;
        this.presentacion  = presentacion;
        this.cantidad      = cantidad;
        this.laboratorio   = laboratorio;
        this.farmacia      = farmacia;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public Long getId()                          { return id; }

    public String getNombre()                    { return nombre; }
    public void   setNombre(String v)            { this.nombre = v; }

    public String getDescripcion()               { return descripcion; }
    public void   setDescripcion(String v)       { this.descripcion = v; }

    public String getConcentracion()             { return concentracion; }
    public void   setConcentracion(String v)     { this.concentracion = v; }

    public String getPresentacion()              { return presentacion; }
    public void   setPresentacion(String v)      { this.presentacion = v; }

    public int    getCantidad()                  { return cantidad; }
    public void   setCantidad(int v)             { this.cantidad = v; }

    public String getLaboratorio()               { return laboratorio; }
    public void   setLaboratorio(String v)       { this.laboratorio = v; }

    public String getNombreAgrupado()            { return nombreAgrupado; }
    public void   setNombreAgrupado(String v)    { this.nombreAgrupado = v; }

    public int    getDemandaGlobal()             { return demandaGlobal; }
    public void   setDemandaGlobal(int v)        { this.demandaGlobal = v; }

    public Farmacia getFarmacia()                { return farmacia; }
    public void     setFarmacia(Farmacia v)      { this.farmacia = v; }
}
