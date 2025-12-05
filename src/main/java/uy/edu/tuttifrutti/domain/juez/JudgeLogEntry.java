package uy.edu.tuttifrutti.domain.juez;

import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;

public class JudgeLogEntry {

    private final Jugador jugador;
    private final Categoria categoria;
    private final String motivo;

    public JudgeLogEntry(Jugador jugador, Categoria categoria, String motivo) {
        this.jugador = jugador;
        this.categoria = categoria;
        this.motivo = motivo;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getMotivo() {
        return motivo;
    }
}
