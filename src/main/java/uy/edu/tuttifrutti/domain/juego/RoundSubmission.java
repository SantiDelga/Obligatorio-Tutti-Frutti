package uy.edu.tuttifrutti.domain.juego;

import java.util.Collections;
import java.util.Map;

public class RoundSubmission {

    private final char letra;
    private final Map<Jugador, Map<Categoria, String>> respuestas;

    public RoundSubmission(char letra, Map<Jugador, Map<Categoria, String>> respuestas) {
        this.letra = letra;
        this.respuestas = Collections.unmodifiableMap(respuestas);
    }

    public char getLetra() {
        return letra;
    }

    public Map<Jugador, Map<Categoria, String>> getRespuestas() {
        return respuestas;
    }
}
