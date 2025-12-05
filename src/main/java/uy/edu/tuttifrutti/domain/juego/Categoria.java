package uy.edu.tuttifrutti.domain.juego;

import java.util.Objects;

public class Categoria {

    private final String nombre;

    public Categoria(String nombre) {
        this.nombre = Objects.requireNonNull(nombre);
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
