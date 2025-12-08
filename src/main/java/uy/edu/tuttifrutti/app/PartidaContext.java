package uy.edu.tuttifrutti.app;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Jugador;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class PartidaContext {

    public enum ModoPartida {
        SINGLEPLAYER,
        MULTIJUGADOR
    }

    private final ModoPartida modo;
    private final GameConfig gameConfig;
    private final List<Jugador> jugadores;
    private final int rondasTotales;
    private int rondaActual;
    private final List<String> letrasDisponibles;
    private String letraActual;
    private int puntajeAcumulado = 0;

    private final Random random = new Random();

    public PartidaContext(ModoPartida modo,
                          GameConfig gameConfig,
                          List<Jugador> jugadores,
                          int rondasTotales,
                          List<String> letrasDisponibles) {

        this.modo = Objects.requireNonNull(modo);
        this.gameConfig = Objects.requireNonNull(gameConfig);
        this.jugadores = List.copyOf(jugadores);
        this.rondasTotales = rondasTotales;
        this.rondaActual = 1;
        this.letrasDisponibles = List.copyOf(letrasDisponibles);
    }

    // ---------- GETTERS ----------

    public ModoPartida getModo() {
        return modo;
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    public List<Jugador> getJugadores() {
        return Collections.unmodifiableList(jugadores);
    }

    public int getRondasTotales() {
        return rondasTotales;
    }

    public int getRondaActual() {
        return rondaActual;
    }

    public String getLetraActual() {
        return letraActual;
    }

    public List<String> getLetrasDisponibles() {
        return letrasDisponibles;
    }

    public int getPuntajeAcumulado() {
        return puntajeAcumulado;
    }

    // ---------- LÓGICA DE PARTIDA ----------

    /** Avanza a la siguiente ronda si es posible. Devuelve true si avanzó, false si ya no hay más. */
    public boolean avanzarRonda() {
        if (rondaActual < rondasTotales) {
            rondaActual++;
            return true;
        }
        return false;
    }

    /** Sortea una letra de la lista de letras disponibles y la guarda como letra actual. */
    public String sortearLetra() {
        if (letrasDisponibles.isEmpty()) {
            letraActual = null;
        } else {
            int idx = random.nextInt(letrasDisponibles.size());
            letraActual = letrasDisponibles.get(idx);
        }
        return letraActual;
    }

    /** Suma el puntaje de una ronda al acumulado de la partida. */
    public void sumarPuntajeRonda(int puntajeRonda) {
        this.puntajeAcumulado += puntajeRonda;
    }

    /** Opcional: por si querés reusar la misma partida más adelante. */
    public void reiniciar() {
        this.rondaActual = 1;
        this.puntajeAcumulado = 0;
        this.letraActual = null;
    }
}
