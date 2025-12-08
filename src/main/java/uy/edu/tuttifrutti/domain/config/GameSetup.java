package uy.edu.tuttifrutti.domain.config;

import java.util.List;

public class GameSetup {

    private final int cantidadJugadores;
    private final int cantidadRondas;
    private final int tiempoSegundos;

    private final List<String> temasSeleccionados;
    private final List<String> letrasSeleccionadas;

    public GameSetup(int cantidadJugadores,
                     int cantidadRondas,
                     int tiempoSegundos,
                     List<String> temasSeleccionados,
                     List<String> letrasSeleccionadas) {

        this.cantidadJugadores = cantidadJugadores;
        this.cantidadRondas = cantidadRondas;
        this.tiempoSegundos = tiempoSegundos;
        this.temasSeleccionados = List.copyOf(temasSeleccionados);
        this.letrasSeleccionadas = List.copyOf(letrasSeleccionadas);
    }

    public int getCantidadJugadores() {
        return cantidadJugadores;
    }

    public int getCantidadRondas() {
        return cantidadRondas;
    }

    public int getTiempoSegundos() {
        return tiempoSegundos;
    }

    public List<String> getTemasSeleccionados() {
        return temasSeleccionados;
    }

    public List<String> getLetrasSeleccionadas() {
        return letrasSeleccionadas;
    }

    @Override
    public String toString() {
        return "GameSetup {" +
                "jugadores=" + cantidadJugadores +
                ", rondas=" + cantidadRondas +
                ", tiempo=" + tiempoSegundos +
                ", temas=" + temasSeleccionados +
                ", letras=" + letrasSeleccionadas +
                '}';
    }
}
