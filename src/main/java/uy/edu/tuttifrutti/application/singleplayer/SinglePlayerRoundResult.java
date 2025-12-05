package uy.edu.tuttifrutti.application.singleplayer;

import java.util.Map;
import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.juez.JudgeResult;

public class SinglePlayerRoundResult {

    private final char letra;
    private final GameConfig config;
    private final Jugador jugador;
    private final Map<Categoria, String> respuestas;
    private final JudgeResult judgeResult;
    private final int puntajeTotal;

    public SinglePlayerRoundResult(char letra,
                                   GameConfig config,
                                   Jugador jugador,
                                   Map<Categoria, String> respuestas,
                                   JudgeResult judgeResult,
                                   int puntajeTotal) {
        this.letra = letra;
        this.config = config;
        this.jugador = jugador;
        this.respuestas = respuestas;
        this.judgeResult = judgeResult;
        this.puntajeTotal = puntajeTotal;
    }

    public char getLetra() {
        return letra;
    }

    public GameConfig getConfig() {
        return config;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public Map<Categoria, String> getRespuestas() {
        return respuestas;
    }

    public JudgeResult getJudgeResult() {
        return judgeResult;
    }

    public int getPuntajeTotal() {
        return puntajeTotal;
    }
}
