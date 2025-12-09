package uy.edu.tuttifrutti.domain.multiplayer;

import uy.edu.tuttifrutti.domain.config.GameConfig;

import java.util.List;

public class Sala {

    private final String id;
    private final String nombre;
    private final int maxJugadores;
    private int jugadoresActuales;

    private final GameConfig config;
    private final int rondas;
    private final List<String> letras;

    public Sala(String id,
                String nombre,
                int maxJugadores,
                int jugadoresActuales,
                GameConfig config,
                int rondas,
                List<String> letras) {

        this.id = id;
        this.nombre = nombre;
        this.maxJugadores = maxJugadores;
        this.jugadoresActuales = jugadoresActuales;
        this.config = config;
        this.rondas = rondas;
        this.letras = List.copyOf(letras);
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getMaxJugadores() {
        return maxJugadores;
    }

    public int getJugadoresActuales() {
        return jugadoresActuales;
    }

    public void setJugadoresActuales(int jugadoresActuales) {
        this.jugadoresActuales = jugadoresActuales;
    }

    public GameConfig getConfig() {
        return config;
    }

    public int getRondas() {
        return rondas;
    }

    public List<String> getLetras() {
        return letras;
    }
}
