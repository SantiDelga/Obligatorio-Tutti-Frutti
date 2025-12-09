package uy.edu.tuttifrutti.application.multiplayer;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.infrastructure.net.ClientHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalaServer {

    public enum Estado {
        LOBBY, EN_JUEGO, FINALIZADA
    }

    private final String id;
    private final String nombre;
    private final int maxJugadores;
    private final GameConfig config;
    private final int rondas;
    private final List<String> letras;

    private Estado estado = Estado.LOBBY;
    private int rondaActual = 1;

    // Jugadores conectados (ordenados según entran)
    private final Map<Jugador, ClientHandler> jugadores = new LinkedHashMap<>();

    // 🔥 Constructor obligatorio que inicializa todos los campos final
    public SalaServer(
            String id,
            String nombre,
            int maxJugadores,
            GameConfig config,
            int rondas,
            List<String> letras,
            Jugador host,
            ClientHandler handlerHost
    ) {
        this.id = id;
        this.nombre = nombre;
        this.maxJugadores = maxJugadores;
        this.config = config;
        this.rondas = rondas;
        this.letras = letras;

        // Registramos el host como primer jugador
        this.jugadores.put(host, handlerHost);
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Estado getEstado() {
        return estado;
    }

    public Map<Jugador, ClientHandler> getJugadores() {
        return jugadores;
    }
}
