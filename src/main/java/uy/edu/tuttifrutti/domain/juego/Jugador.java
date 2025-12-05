package uy.edu.tuttifrutti.domain.juego;

import java.util.Objects;

public class Jugador {

    private final String nombre;

    public Jugador(String nombre) {
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
