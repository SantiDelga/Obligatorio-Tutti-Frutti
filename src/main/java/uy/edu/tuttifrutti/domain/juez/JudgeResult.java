package uy.edu.tuttifrutti.domain.juez;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;

public class JudgeResult {

    public enum EstadoRespuesta {
        VALIDA_UNICA,
        VALIDA_DUPLICADA,
        INVALIDA,
        VACIA
    }

    private final Map<Jugador, Integer> puntajes;
    private final Map<Jugador, Map<Categoria, EstadoRespuesta>> estados;
    private final List<JudgeLogEntry> logs;

    public JudgeResult(Map<Jugador, Integer> puntajes,
                       Map<Jugador, Map<Categoria, EstadoRespuesta>> estados,
                       List<JudgeLogEntry> logs) {
        this.puntajes = Collections.unmodifiableMap(puntajes);
        this.estados = Collections.unmodifiableMap(estados);
        this.logs = List.copyOf(logs);
    }

    public Map<Jugador, Integer> getPuntajes() {
        return puntajes;
    }

    public Map<Jugador, Map<Categoria, EstadoRespuesta>> getEstados() {
        return estados;
    }

    public List<JudgeLogEntry> getLogs() {
        return logs;
    }
}
