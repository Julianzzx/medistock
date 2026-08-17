package com.example.demo.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(unique = true, nullable = false)
    private String identificacion;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String rol = "USER";

    @Column(nullable = false)
    private Integer edad = 0;

    @Column(nullable = false)
    private Long contacto = 0L;

    @Column(nullable = false)
    private String genero = "No especificado";

    @Column(nullable = false)
    private String regimen = "No especificado";

    @ManyToOne
    @JoinColumn(name = "eps_id")
    private Eps eps;

    public Usuario() {
    }

    public Usuario(String nombre, String apellido, String identificacion,
            String direccion, String correo, String rol,
            Integer edad, Long contacto, String genero, String regimen, Eps eps) {
        this.nombre = nombre != null ? nombre : "Sin nombre";
        this.apellido = apellido != null ? apellido : "Sin apellido";
        this.identificacion = identificacion;
        this.direccion = direccion != null ? direccion : "No especificado";
        this.correo = correo;
        this.rol = rol != null ? rol : "USER";
        this.edad = edad != null ? edad : 0;
        this.contacto = contacto != null ? contacto : 0L;
        this.genero = genero != null ? genero : "No especificado";
        this.regimen = regimen != null ? regimen : "No especificado";
        this.eps = eps;
    }

    private boolean epsBloqueada = false;

    public boolean isEpsBloqueada() {
        return epsBloqueada;
    }

    public void setEpsBloqueada(boolean epsBloqueada) {
        this.epsBloqueada = epsBloqueada;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre != null ? nombre : "Sin nombre";
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido != null ? apellido : "Sin apellido";
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion != null ? direccion : "No especificado";
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol != null ? rol : "USER";
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad != null ? edad : 0;
    }

    public Long getContacto() {
        return contacto;
    }

    public void setContacto(Long contacto) {
        this.contacto = contacto != null ? contacto : 0L;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero != null ? genero : "No especificado";
    }

    public String getRegimen() {
        return regimen;
    }

    public void setRegimen(String regimen) {
        this.regimen = regimen != null ? regimen : "No especificado";
    }

    public Eps getEps() {
        return eps;
    }

    public void setEps(Eps eps) {
        this.eps = eps;
    }
}
