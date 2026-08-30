package com.bitacora.trekking.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Modelo de datos de un checkpoint de la bitácora de ruta.
 *
     * <p>Los atributos se exponen con propiedades observables de JavaFX para que la
     * interfaz pueda reaccionar automáticamente a los cambios.</p>
 */
public class Checkpoint {

    private final LongProperty id;
    private final StringProperty nombre;
    private final StringProperty hora;
    private final DoubleProperty latitud;
    private final DoubleProperty longitud;
    private final StringProperty descripcion;

    public Checkpoint(Long id, String nombre, String hora, double latitud, double longitud, String descripcion) {
        this.id = new SimpleLongProperty(id);
        this.nombre = new SimpleStringProperty(nombre);
        this.hora = new SimpleStringProperty(hora);
        this.latitud = new SimpleDoubleProperty(latitud);
        this.longitud = new SimpleDoubleProperty(longitud);
        this.descripcion = new SimpleStringProperty(descripcion);
    }

    public long getId() {
        return id.get();
    }

    public LongProperty idProperty() {
        return id;
    }

    public void setId(long id) {
        this.id.set(id);
    }

    public String getNombre() {
        return nombre.get();
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public String getHora() {
        return hora.get();
    }

    public StringProperty horaProperty() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora.set(hora);
    }

    public double getLatitud() {
        return latitud.get();
    }

    public DoubleProperty latitudProperty() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud.set(latitud);
    }

    public double getLongitud() {
        return longitud.get();
    }

    public DoubleProperty longitudProperty() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud.set(longitud);
    }

    public String getDescripcion() {
        return descripcion.get();
    }

    public StringProperty descripcionProperty() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion.set(descripcion);
    }
}
